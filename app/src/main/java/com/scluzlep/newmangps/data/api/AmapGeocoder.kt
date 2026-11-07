package com.scluzlep.newmangps.data.api

import com.google.gson.JsonParser
import com.scluzlep.newmangps.BuildConfig
import com.scluzlep.newmangps.data.util.CoordinateTransform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class AmapPlace(
    val name: String,
    val address: String,
    val point: CoordinateTransform.LatLng
)

class AmapGeocoder {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun reverse(point: CoordinateTransform.LatLng): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = HttpUrl.Builder()
                    .scheme("https")
                    .host("restapi.amap.com")
                    .addPathSegments("v3/geocode/regeo")
                    .addQueryParameter("key", BuildConfig.AMAP_WEB_SERVICE_KEY)
                    .addQueryParameter(
                        "location",
                        point.longitude.toString() + "," + point.latitude.toString()
                    )
                    .addQueryParameter("output", "JSON")
                    .addQueryParameter("extensions", "base")
                    .build()
                val request = Request.Builder().url(url).get().build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("地址查询失败 (HTTP " + response.code + ")")
                    val body = response.body?.string() ?: error("地址服务返回空响应")
                    val json = JsonParser.parseString(body).asJsonObject
                    if (json.get("status")?.asString != "1") {
                        error(json.get("info")?.asString ?: "地址查询失败")
                    }
                    json.getAsJsonObject("regeocode")
                        ?.get("formatted_address")
                        ?.asString
                        ?.takeIf(String::isNotBlank)
                        ?: error("地址服务未返回详细地址")
                }
            }
        }

    suspend fun searchPlaces(
        keyword: String,
        around: CoordinateTransform.LatLng?
    ): Result<List<AmapPlace>> = withContext(Dispatchers.IO) {
        if (keyword.isBlank()) return@withContext Result.success(emptyList())
        runCatching {
            val urlBuilder = HttpUrl.Builder()
                .scheme("https")
                .host("restapi.amap.com")
                .addPathSegments("v5/place/text")
                .addQueryParameter("key", BuildConfig.AMAP_WEB_SERVICE_KEY)
                .addQueryParameter("keywords", keyword)
                .addQueryParameter("show_fields", "business")
            if (around != null) {
                urlBuilder.addQueryParameter("location", "${around.longitude},${around.latitude}")
            }
            val request = Request.Builder().url(urlBuilder.build()).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("地点搜索失败 (HTTP ${response.code})")
                val json = JsonParser.parseString(response.body?.string() ?: error("地点服务返回空响应")).asJsonObject
                if (json.get("status")?.asString != "1") error(json.get("info")?.asString ?: "地点搜索失败")
                json.getAsJsonArray("pois")?.mapNotNull { item ->
                    val poi = item.asJsonObject
                    val coords = poi.get("location")?.asString?.split(',')
                    if (coords?.size != 2) return@mapNotNull null
                    val lon = coords[0].toDoubleOrNull() ?: return@mapNotNull null
                    val lat = coords[1].toDoubleOrNull() ?: return@mapNotNull null
                    AmapPlace(
                        name = poi.get("name")?.asString.orEmpty(),
                        address = poi.get("address")?.let { if (it.isJsonPrimitive) it.asString else it.toString() }.orEmpty(),
                        point = CoordinateTransform.LatLng(lat, lon)
                    )
                }.orEmpty()
            }
        }
    }
}
