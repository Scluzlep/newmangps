package com.scluzlep.newmangps.data.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.scluzlep.newmangps.data.model.ApiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class NewsmyApiService(
    private val serverUrl: String = NewsmySigner.DEFAULT_SERVER
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun executeRpc(
        user: String,
        cmd: String,
        params: List<Any>,
        field: String = "",
        pageSize: Int = 0,
        absPage: Int = 0
    ): ApiResponse = withContext(Dispatchers.IO) {
        val signedParams = NewsmySigner.buildSignedParams(
            serverUrl = serverUrl,
            user = user,
            cmd = cmd,
            params = params,
            field = field,
            pageSize = pageSize,
            absPage = absPage
        )

        post(signedParams).let { bodyString ->
            gson.fromJson(bodyString, ApiResponse::class.java)
                ?: throw IllegalStateException("Invalid RPC response")
        }
    }

    suspend fun executeCommand(user: String, teid: String, command: String): Int =
        withContext(Dispatchers.IO) {
            val params = NewsmySigner.buildCommandParams(
                serverUrl = serverUrl,
                user = user,
                command = command,
                teid = teid
            )
            val response = gson.fromJson(post(params), JsonObject::class.java)
                ?: throw IllegalStateException("Invalid command response")
            response.get("nAction")?.asInt
                ?: throw IllegalStateException("Command response has no status")
        }

    private fun post(signedParams: Map<String, String>): String {
        val formBuilder = FormBody.Builder()
        for ((key, value) in signedParams) {
            formBuilder.add(key, value)
        }

        val request = Request.Builder()
            .url("$serverUrl${NewsmySigner.API_ENDPOINT}")
            .post(formBuilder.build())
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 uni-app")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP " + response.code)
            }
            val bodyString = response.body?.string() ?: throw IllegalStateException("Empty response body")
            return bodyString
        }
    }
}

