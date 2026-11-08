package com.scluzlep.newmangps.data.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.scluzlep.newmangps.data.model.ApiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MultipartBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class NewsmyApiService(
    @Volatile private var serverUrl: String = NewsmySigner.DEFAULT_SERVER
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    fun setServerUrl(url: String) {
        require(url.startsWith("https://") || url.startsWith("http://"))
        serverUrl = url.trimEnd('/')
    }

    suspend fun getMiniAppConfig(): JsonObject = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://app.newsmy-nmgps.com/weixin/WechatVoIPMiniApp.asp?strCmdType=GetConfig")
            .get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("小程序配置请求失败：HTTP ${response.code}")
            gson.fromJson(response.body?.string(), JsonObject::class.java) ?: error("小程序配置为空")
        }
    }

    suspend fun uploadVoice(teid: String, durationSeconds: Int, mp3: ByteArray): JsonObject =
        withContext(Dispatchers.IO) {
            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("nSecond", durationSeconds.toString())
                .addFormDataPart("strTEID", teid)
                .addFormDataPart("upfile", "Recorder.mp3", mp3.toRequestBody("audio/mpeg".toMediaType()))
                .build()
            val request = Request.Builder().url("$serverUrl/Upload/UploadVoice.aspx").post(body).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("语音上传失败：HTTP ${response.code}")
                gson.fromJson(response.body?.string(), JsonObject::class.java) ?: error("语音上传响应为空")
            }
        }

    suspend fun paymentGet(path: String, params: Map<String, String>): String = withContext(Dispatchers.IO) {
        require(path in setOf("app/weixin/WechatPayOrder.asp", "app/Alipay/AlipayOrder.asp"))
        val url = "https://app.newsmy-nmgps.com/$path".toHttpUrl().newBuilder().apply {
            params.forEach { (key, value) -> addQueryParameter(key, value) }
        }.build()
        client.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
            if (!response.isSuccessful) error("支付服务返回 HTTP ${response.code}")
            response.body?.string() ?: error("支付服务未返回内容")
        }
    }

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

    suspend fun executeCommand(user: String, teid: String, command: String, parameter: String = ""): Int =
        withContext(Dispatchers.IO) {
            val params = NewsmySigner.buildCommandParams(
                serverUrl = serverUrl,
                user = user,
                command = command,
                teid = teid,
                parameter = parameter
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

