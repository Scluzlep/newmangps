package com.scluzlep.newmangps.data.api

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

data class SimProvider(
    val name: String,
    val purchaseUrl: String,
    val detected: Boolean,
    val expiry: String? = null,
    val remainingFlow: String? = null,
    val expired: Boolean = false,
    val expiringSoon: Boolean = false,
    val lowFlow: Boolean = false,
    val probeError: Boolean = false,
)

class SimCardService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    suspend fun providers(sim: String): List<SimProvider> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(sim.trim(), "UTF-8")
        val upper = URLEncoder.encode(sim.trim().uppercase(), "UTF-8")
        val definitions = listOf(
            Triple("迪信", "https://piccwechat.18gps.net/GetDataService.aspx?method=QuerySimApi&w=DY_getBasicNewestInfo&sim=$encoded",
                "https://piccwechat.18gps.net/SIM/MobileOpenSIM.aspx?id=$encoded"),
            Triple("三维智达", "https://cmp.api.zdm2m.com/smsApi.do?querycard/unsign&msisdn=$encoded",
                "https://cmp.zdm2m.com/tWxHyCardInfoController.do?tWxHyCardPackageInfoh5&cardInfo=$encoded"),
            Triple("深圳共展", "https://wx.szgzwlw.com/card/detail?iccid=$upper",
                "https://h5.szgzwlw.com/#/pages/detail/detail?source=h5&sim=$upper&markType=app"),
            Triple("流信", "https://api.cmpyun.com/api/serviceAccept/getJtCardMC?iccid=$encoded",
                "https://h5hk.newsimcloud.com/api/ipc/$encoded"),
            Triple("巨升", "https://jsps.qssite.cn/api/applet/getSim?iccid=$upper",
                "https://jsps.qssite.cn/$upper"),
            Triple("拓普互联", "https://api.toptolink.com/api/v1/card/checkCard?cardId=$upper",
                "https://h5.toptolink.com/mobile/webPayPkg/toFlowIndexPage?iccid=$upper"),
            Triple("联域", "", "https://wx.linksfield.net/?deviceCode=$upper"),
            Triple("飞讯飞通", "https://openapi.fxftcar.com/Web/CustomApi/CardInfoQuery?appid=CUSTOMAPIVIEW&cNo=$upper",
                "https://wx-iot.fxftcar.com/webpay?voice=1&period=1&card_no=$upper"),
            Triple("富朗威", "", "https://gps.franwell.com.cn/#/packagePurchase?iccid=$upper"),
            Triple("中网云通", "", "https://www.zwytco.com/iotdi2Un6/#/iotdh5i2Un6/7/$encoded")
        )
        coroutineScope {
            definitions.map { (name, checkUrl, purchaseUrl) ->
                async {
                    val probe = when (name) {
                        "联域" -> SimProvider(name, purchaseUrl,
                            sim.trim().startsWith("893144") || sim.trim().startsWith("894301"))
                        "富朗威" -> checkFranwell(sim.trim().uppercase(), purchaseUrl)
                        "中网云通" -> checkZwyt(sim.trim(), purchaseUrl)
                        else -> checkProvider(checkUrl, name, purchaseUrl)
                    }
                    probe
                }
            }.awaitAll().sortedByDescending(SimProvider::detected)
        }
    }

    private fun checkProvider(url: String, name: String, purchaseUrl: String): SimProvider = runCatching {
        client.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
            if (!response.isSuccessful) return@use SimProvider(name, purchaseUrl, false, probeError = true)
            val body = response.body?.string() ?: return@use SimProvider(name, purchaseUrl, false, probeError = true)
            val json = JsonParser.parseString(body)
            val obj = json.takeIf { it.isJsonObject }?.asJsonObject
                ?: return@use SimProvider(name, purchaseUrl, false, probeError = true)
            val detected = when (name) {
                "迪信" -> obj.get("data")?.takeIf { it.isJsonArray }?.asJsonArray
                    ?.firstOrNull()?.asJsonObject?.let { hasText(it, "ICCID") && hasText(it, "SimCard") } == true
                "三维智达" -> obj.get("resultcode")?.asString?.isNotBlank() == true &&
                    obj.get("cardmsg")?.asJsonObject?.let { hasText(it, "iccid") } == true
                "深圳共展" -> obj.get("code")?.asInt == 0 && obj.get("msg")?.asString == "success" &&
                    obj.get("data")?.asJsonObject?.let { hasText(it, "sim") } == true
                "流信" -> obj.get("code")?.asInt == 0
                "巨升" -> obj.get("code")?.asInt == 0 && obj.get("message")?.asString == "success"
                "飞讯飞通" -> obj.get("code")?.asString?.isNotBlank() == true &&
                    obj.get("data")?.asJsonObject?.let { hasText(it, "iccid") } == true
                "拓普互联" -> obj.get("code")?.asString == "0" && obj.get("state")?.asString == "ok"
                else -> false
            }
            val result = obj.get("result")?.takeIf { it.isJsonObject }?.asJsonObject
            val expiry = if (name == "流信" && detected) obj.stringOrNull("end_time") else null
            val remaining = if (name == "巨升" && detected) result?.stringOrNull("surplusFlow") else null
            SimProvider(name, purchaseUrl, detected, expiry = expiry, remainingFlow = remaining,
                expired = expiryStatus(expiry) == -1 ||
                    name == "巨升" && detected && result?.get("isExpire").truthy(),
                expiringSoon = expiryStatus(expiry) == 1,
                lowFlow = remaining?.let { "^[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)".toRegex()
                    .find(it.trim())?.value?.toDoubleOrNull() }?.let { it < 1 } == true)
        }
    }.getOrElse { SimProvider(name, purchaseUrl, false, probeError = true) }

    private fun checkFranwell(card: String, purchaseUrl: String): SimProvider = runCatching {
        val name = "富朗威"
        val request = Request.Builder()
            .url("https://gps-api.franwell.com/gpsSimRecord/checkCardIccid/$card")
            .header("token", "fwe98754h125")
            .post(ByteArray(0).toRequestBody())
            .build()
        client.newCall(request).execute().use { response ->
            val obj = if (response.isSuccessful) response.body?.string()?.let {
                JsonParser.parseString(it).takeIf { value -> value.isJsonObject }?.asJsonObject
            } else null
            val data = obj?.get("data")?.takeIf { it.isJsonObject }?.asJsonObject
            val detected = obj?.get("code").truthy() && data?.get("iccid").truthy() && data?.get("valid").truthy()
            val expiry = if (detected) data?.stringOrNull("endDate") else null
            SimProvider(name, purchaseUrl, detected, expiry = expiry,
                expired = expiryStatus(expiry) == -1, expiringSoon = expiryStatus(expiry) == 1,
                probeError = !response.isSuccessful)
        }
    }.getOrElse { SimProvider("富朗威", purchaseUrl, false, probeError = true) }

    private fun checkZwyt(card: String, purchaseUrl: String): SimProvider = runCatching {
        val name = "中网云通"
        val json = JsonObject().apply { addProperty("iccid", card) }.toString()
        val request = Request.Builder()
            .url("https://www.zwytco.com/japi/iot-japi/V1.1/baseData/getCardActiveTime")
            .header("japi-appid", "6606312786505807387")
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            val obj = if (response.isSuccessful) response.body?.string()?.let {
                JsonParser.parseString(it).takeIf { value -> value.isJsonObject }?.asJsonObject
            } else null
            val data = obj?.get("data")?.takeIf { it.isJsonObject }?.asJsonObject
            SimProvider(name, purchaseUrl,
                obj?.get("code")?.asInt == 0 && data?.get("iccid").truthy() && data?.get("msisdn").truthy(),
                probeError = !response.isSuccessful)
        }
    }.getOrElse { SimProvider("中网云通", purchaseUrl, false, probeError = true) }

    private fun JsonObject.stringOrNull(key: String): String? =
        get(key)?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.isNotBlank() }

    /** -1 expired, 1 expires within 31 days, 0 otherwise or unknown format. */
    private fun expiryStatus(value: String?): Int {
        val text = value ?: return 0
        val parsed = runCatching {
            LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                .atZone(ZoneId.systemDefault()).toEpochSecond()
        }.getOrNull() ?: return 0
        val remaining = parsed - System.currentTimeMillis() / 1000
        return when { remaining < 0 -> -1; remaining <= 31L * 86400 -> 1; else -> 0 }
    }

    private fun com.google.gson.JsonElement?.truthy(): Boolean = when {
        this == null || isJsonNull -> false
        isJsonPrimitive && asJsonPrimitive.isBoolean -> asBoolean
        isJsonPrimitive && asJsonPrimitive.isNumber -> asDouble != 0.0
        isJsonPrimitive && asJsonPrimitive.isString -> asString.isNotEmpty()
        else -> true
    }

    private fun hasText(obj: JsonObject, key: String): Boolean =
        obj.get(key)?.takeIf { it.isJsonPrimitive }?.asString?.isNotBlank() == true

    suspend fun whitelistUrl(iccid: String): String? = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(iccid.trim(), "UTF-8")
        if (encoded.isBlank()) return@withContext null
        val shenzhen = "https://wx.szgzwlw.com/card/detail?iccid=$encoded"
        val sanwei = "https://cmp.api.zdm2m.com/smsApi.do?querycard/unsign&msisdn=$encoded"
        val found = coroutineScope {
            listOf(
                async { if (checkProvider(shenzhen, "深圳共展", "").detected)
                    "https://h5.szgzwlw.com/#/pages/white/white?source=h5&sim=$encoded" else null },
                async { if (checkWhitelistSanwei(sanwei))
                    "https://cmp.zdm2m.com/tWxHyCardInfoController.do?tWxHyVoiceCardWhiteInfo&cardInfo=$encoded" else null }
            ).awaitAll()
        }
        found.firstOrNull { it != null }
    }

    private fun checkWhitelistSanwei(url: String): Boolean = runCatching {
        client.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
            if (!response.isSuccessful) return@use false
            val row = JsonParser.parseString(response.body?.string().orEmpty()).asJsonObject
            row.get("cardmsg")?.asJsonObject?.let { hasText(it, "cardno") } == true
        }
    }.getOrDefault(false)
}
