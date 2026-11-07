package com.scluzlep.newmangps.data.api

import java.security.MessageDigest
import java.util.Base64
import kotlin.random.Random

/**
 * Newsmy / 纽曼卫士 GPS Protocol Signer & Token Packer.
 * Implements the proprietary RPC protocol over /APP/AppJson.asp.
 */
object NewsmySigner {
    const val DEFAULT_SERVER = "https://server.newsmy-nmgps.com"
    const val CLUSTER_SERVER = "http://www.appxmg.com"
    const val API_ENDPOINT = "/APP/AppJson.asp"

    // Protocol Delimiters (ASCII Control Characters)
    private const val SERVER_ROW = "\u0011"    // DC1 (ASCII 17)
    private const val SERVER_TABLE = "\u001b"  // ESC (ASCII 27)

    /**
     * Compute MD5 hexadecimal string (lowercase)
     */
    fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Base64 encode string with standard charset
     */
    fun base64Encode(input: String): String {
        return Base64.getEncoder().encodeToString(input.toByteArray(Charsets.UTF_8))
    }

    /**
     * Build signed form-urlencoded key-value parameters ready to POST
     */
    fun buildSignedParams(
        serverUrl: String = DEFAULT_SERVER,
        user: String,
        cmd: String,
        params: List<Any>,
        field: String = "",
        pageSize: Int = 0,
        absPage: Int = 0
    ): Map<String, String> {
        // 1. Calculate strAppID: lowercase host with trailing '/' padded to multiple of 3
        var host = serverUrl.lowercase()
            .replace("http://", "")
            .replace("https://", "")
        while (host.length % 3 != 0) {
            host += "/"
        }
        val strAppID = base64Encode(host)

        // 2. Format SQL Server Stored Procedure Parameters: N'p1',N'p2'
        val dataStr = params.joinToString(",") {
            val escaped = it.toString().replace("'", "''")
            "N'$escaped'"
        }

        // 3. Assemble Token payload with delimiters
        val tokenBuilder = StringBuilder()
        tokenBuilder.append(cmd).append(SERVER_ROW)
            .append(dataStr).append(SERVER_ROW)
            .append(field).append(SERVER_ROW)

        if (pageSize > 0 && absPage > 0) {
            tokenBuilder.append(pageSize).append(SERVER_ROW)
                .append(absPage).append(SERVER_ROW)
        }
        tokenBuilder.append(SERVER_TABLE)

        // 4. Generate random string and padding
        val randomDigits = Random.nextLong(10_000_000_000_000L, 100_000_000_000_000L).toString()
        val rndFull = "MGTS_$randomDigits"
        val padChar = if (rndFull.length > 7) rndFull[7] else '0'

        while (tokenBuilder.length % 3 != 0) {
            tokenBuilder.append(padChar)
        }

        val strToken = base64Encode(tokenBuilder.toString())
        val nTimeStamp = System.currentTimeMillis()
        val strRandom = randomDigits

        // 5. Calculate strSign = MD5(nTimeStamp + strRandom + strUser + strAppID + strToken)
        val signRaw = "$nTimeStamp$strRandom$user$strAppID$strToken"
        val strSign = md5(signRaw)

        return mapOf(
            "strAppID" to strAppID,
            "strUser" to user,
            "nTimeStamp" to nTimeStamp.toString(),
            "strRandom" to strRandom,
            "strSign" to strSign,
            "strToken" to strToken
        )
    }
}
