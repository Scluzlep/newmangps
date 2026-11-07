package com.scluzlep.newmangps.data.api

import com.google.gson.Gson
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
            val bodyString = response.body?.string() ?: throw IllegalStateException("Empty response body")
            gson.fromJson(bodyString, ApiResponse::class.java)
        }
    }
}

