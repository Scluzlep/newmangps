package com.scluzlep.newmangps.ui.screens.map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.MotionEvent
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.scluzlep.newmangps.BuildConfig
import com.scluzlep.newmangps.data.MapSource
import com.scluzlep.newmangps.data.util.CoordinateTransform
import com.scluzlep.newmangps.ui.LocalAppLanguage
import org.json.JSONArray
import org.json.JSONObject

data class MapDeviceMarker(
    val teid: String,
    val name: String,
    val longitude: Double,
    val latitude: Double,
    val direction: Int,
    val status: String,
    val iconUrl: String = ""
)

data class MapFenceOverlay(
    val name: String,
    val isPolygon: Boolean,
    val center: CoordinateTransform.LatLng? = null,
    val radiusMeters: Int = 0,
    val vertices: List<CoordinateTransform.LatLng> = emptyList()
)

internal fun isValidMapCoordinate(latitude: Double, longitude: Double): Boolean =
    latitude.isFinite() && longitude.isFinite() &&
        latitude in -85.0..85.0 && longitude in -180.0..180.0

private fun CoordinateTransform.LatLng.toMapJsonObject(mapSource: MapSource): JSONObject {
    val converted = mapSource.toMapCoord(this)
    return JSONObject()
        .put("lat", converted.latitude)
        .put("lon", converted.longitude)
}

internal fun serializeMapPointsJson(
    points: List<CoordinateTransform.LatLng>,
    mapSource: MapSource
): String {
    val array = JSONArray()
    points.forEach { point ->
        if (isValidMapCoordinate(point.latitude, point.longitude)) {
            array.put(point.toMapJsonObject(mapSource))
        }
    }
    return array.toString()
}

internal fun serializeDeviceMarkersJson(
    markers: List<MapDeviceMarker>,
    mapSource: MapSource
): String {
    val array = JSONArray()
    markers.forEach { marker ->
        if (isValidMapCoordinate(marker.latitude, marker.longitude)) {
            val converted = mapSource.toMapCoord(
                CoordinateTransform.LatLng(marker.latitude, marker.longitude)
            )
            array.put(
                JSONObject()
                    .put("teid", marker.teid)
                    .put("name", marker.name)
                    .put("lat", converted.latitude)
                    .put("lon", converted.longitude)
                    .put("direction", marker.direction)
                    .put("status", marker.status)
                    .put("iconUrl", marker.iconUrl)
            )
        }
    }
    return array.toString()
}

internal fun serializeFencesJson(
    fences: List<MapFenceOverlay>,
    mapSource: MapSource
): String {
    val array = JSONArray()
    fences.forEach { fence ->
        val obj = JSONObject()
            .put("name", fence.name)
            .put("isPolygon", fence.isPolygon)
            .put("radiusMeters", fence.radiusMeters.coerceAtLeast(0))
        val center = fence.center
        if (center != null && isValidMapCoordinate(center.latitude, center.longitude)) {
            obj.put("center", center.toMapJsonObject(mapSource))
        } else {
            obj.put("center", JSONObject.NULL)
        }
        val verticesArray = JSONArray()
        fence.vertices.forEach { vertex ->
            if (isValidMapCoordinate(vertex.latitude, vertex.longitude)) {
                verticesArray.put(vertex.toMapJsonObject(mapSource))
            }
        }
        obj.put("vertices", verticesArray)
        array.put(obj)
    }
    return array.toString()
}

internal fun serializeSpeedsJson(speeds: List<Int>): String =
    JSONArray().apply { speeds.forEach { put(it) } }.toString()

internal fun serializeLabelsJson(labels: Map<String, String>): String =
    JSONObject().apply { labels.forEach { (key, value) -> put(key, value) } }.toString()

@Composable
fun AmapJsMap(
    points: List<CoordinateTransform.LatLng>,
    markerIndex: Int,
    mapSource: MapSource = MapSource.AMAP,
    satellite: Boolean = false,
    traffic: Boolean = false,
    onMapTap: ((CoordinateTransform.LatLng) -> Unit)? = null,
    circleRadiusMeters: Int = 0,
    markerName: String = "",
    markerDirection: Int = 0,
    markerOnline: Boolean = true,
    markerStatus: String = if (markerOnline) "online" else "offline",
    markerIconUrl: String = "",
    followMarker: Boolean = false,
    autoCenter: Boolean = true,
    focusRequest: String = "",
    routeSpeeds: List<Int> = emptyList(),
    controlsTopPx: Int = 12,
    otherMarkers: List<MapDeviceMarker> = emptyList(),
    polygonPoints: List<CoordinateTransform.LatLng> = emptyList(),
    fences: List<MapFenceOverlay> = emptyList(),
    onDeviceTap: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val language = LocalAppLanguage.current
    val labels = mapOf(
        "error" to language.text("MapTileError", "地图瓦片加载失败，请检查网络"),
        "start" to language.text("StartLocation", "起点"),
        "end" to language.text("EndLocation", "终点"),
        "retry" to language.text("Retry", "重试")
    )
    AndroidView(
        factory = { context ->
            AmapMapView(
                context = context,
                initialMapSource = mapSource,
                initialSatellite = satellite,
                initialTraffic = traffic,
                onMapTap = onMapTap,
                onDeviceTap = onDeviceTap,
                controlsTopPx = controlsTopPx,
                initialLabels = labels
            )
        },
        update = { view ->
            view.setOnMapTap(onMapTap)
            view.setOnDeviceTap(onDeviceTap)
            view.updateLabels(labels)
            view.updateMap(
                points = points,
                markerIndex = markerIndex,
                mapSource = mapSource,
                satellite = satellite,
                traffic = traffic,
                circleRadiusMeters = circleRadiusMeters,
                markerName = markerName,
                markerDirection = markerDirection,
                markerOnline = markerOnline,
                markerStatus = markerStatus,
                markerIconUrl = markerIconUrl,
                followMarker = followMarker,
                autoCenter = autoCenter,
                focusRequest = focusRequest,
                routeSpeeds = routeSpeeds,
                otherMarkers = otherMarkers,
                polygonPoints = polygonPoints,
                fences = fences
            )
        },
        onRelease = { view ->
            view.stopLoading()
            view.destroy()
        },
        modifier = modifier
    )
}

@SuppressLint("SetJavaScriptEnabled")
private class AmapMapView(
    context: Context,
    initialMapSource: MapSource,
    initialSatellite: Boolean,
    initialTraffic: Boolean,
    private var onMapTap: ((CoordinateTransform.LatLng) -> Unit)?,
    private var onDeviceTap: ((String) -> Unit)?,
    private val controlsTopPx: Int,
    initialLabels: Map<String, String>
) : WebView(context) {
    private var pageReady = false
    private var currentMapSource = initialMapSource
    private var currentSatellite = initialSatellite
    private var currentTraffic = initialTraffic
    private var pendingLabelsJson = serializeLabelsJson(initialLabels)
    private var previousLabelsJson = pendingLabelsJson

    private var pendingJson = "[]"
    private var pendingIndex = 0
    private var pendingRadius = 0
    private var pendingName = ""
    private var pendingDirection = 0
    private var pendingOnline = true
    private var pendingStatus = "online"
    private var pendingIconUrl = ""
    private var pendingFollowMarker = false
    private var pendingSpeeds = "[]"
    private var pendingOtherMarkers = "[]"
    private var pendingPolygon = "[]"
    private var pendingFences = "[]"
    private var pendingHasContent = false

    private var allowedTeids = emptySet<String>()
    private var previousRoute = ""
    private var previousSpeeds = "[]"
    private var previousRawRoutePoints = emptyList<CoordinateTransform.LatLng>()
    private var previousRawMarkers = emptyList<MapDeviceMarker>()
    private var previousRawPolygon = emptyList<CoordinateTransform.LatLng>()
    private var previousRawRadius = 0
    private var previousRawFences = emptyList<MapFenceOverlay>()
    private var lastPayload = ""
    private var hasFitted = false
    private var previousFocusRequest = ""

    init {
        setBackgroundColor(Color.rgb(241, 244, 246))
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.defaultTextEncodingName = "UTF-8"

        addJavascriptInterface(object {
            @JavascriptInterface
            fun onMapTap(longitude: Double, latitude: Double) {
                if (isValidMapCoordinate(latitude, longitude)) {
                    post {
                        val wgs = currentMapSource.fromMapCoord(
                            CoordinateTransform.LatLng(latitude, longitude)
                        )
                        onMapTap?.invoke(wgs)
                    }
                }
            }

            @JavascriptInterface
            fun onDeviceTap(teid: String) {
                post {
                    if (teid in allowedTeids) {
                        onDeviceTap?.invoke(teid)
                    }
                }
            }
        }, "NativeMap")

        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                if (message.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                    val safeMessage = redactSecrets(message.message())
                    val safeSource = redactSecrets(message.sourceId())
                    Log.e(TAG, "$safeMessage ($safeSource:${message.lineNumber()})")
                }
                return true
            }
        }

        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                val url = request.url ?: return null
                if (url.host == ASSET_HOST) {
                    val asset = when (url.path) {
                        "/__assets__/leaflet.js" -> "map/leaflet.js" to "application/javascript"
                        "/__assets__/leaflet.css" -> "map/leaflet.css" to "text/css"
                        else -> null
                    }
                    if (asset != null) {
                        return runCatching {
                            WebResourceResponse(
                                asset.second,
                                "UTF-8",
                                context.assets.open(asset.first)
                            )
                        }.getOrElse { error ->
                            Log.e(TAG, "Failed to open map asset ${asset.first}", error)
                            null
                        }
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView, url: String) {
                pageReady = true
                applyLabels()
                applyTileSource()
                applyMapData(
                    fitRoute = pendingHasContent,
                    routeChanged = true,
                    focusPrimary = false
                )
                if (pendingHasContent) {
                    hasFitted = true
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean = request.isForMainFrame && request.url.host != ASSET_HOST
        }

        loadDataWithBaseURL(
            BASE_URL,
            buildMapHtml(initialLabels),
            "text/html",
            "UTF-8",
            null
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE ->
                parent?.requestDisallowInterceptTouchEvent(true)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                parent?.requestDisallowInterceptTouchEvent(false)
        }
        return super.onTouchEvent(event)
    }

    fun setOnMapTap(callback: ((CoordinateTransform.LatLng) -> Unit)?) {
        onMapTap = callback
    }

    fun setOnDeviceTap(callback: ((String) -> Unit)?) {
        onDeviceTap = callback
    }

    fun updateLabels(labels: Map<String, String>) {
        val labelsJson = serializeLabelsJson(labels)
        if (labelsJson == previousLabelsJson) return
        previousLabelsJson = labelsJson
        pendingLabelsJson = labelsJson
        if (pageReady) {
            applyLabels()
        }
    }

    fun updateMap(
        points: List<CoordinateTransform.LatLng>,
        markerIndex: Int,
        mapSource: MapSource,
        satellite: Boolean,
        traffic: Boolean,
        circleRadiusMeters: Int,
        markerName: String,
        markerDirection: Int,
        markerOnline: Boolean,
        markerStatus: String,
        markerIconUrl: String,
        followMarker: Boolean,
        autoCenter: Boolean,
        focusRequest: String,
        routeSpeeds: List<Int>,
        otherMarkers: List<MapDeviceMarker>,
        polygonPoints: List<CoordinateTransform.LatLng>,
        fences: List<MapFenceOverlay>
    ) {
        val tileSourceChanged = mapSource != currentMapSource ||
            satellite != currentSatellite ||
            traffic != currentTraffic
        currentMapSource = mapSource
        currentSatellite = satellite
        currentTraffic = traffic

        val validPoints = points.filter { isValidMapCoordinate(it.latitude, it.longitude) }
        val validMarkers = otherMarkers.filter { isValidMapCoordinate(it.latitude, it.longitude) }
        val validPolygon = polygonPoints.filter { isValidMapCoordinate(it.latitude, it.longitude) }
        val normalizedRadius = circleRadiusMeters.coerceAtLeast(0)

        val routeJson = serializeMapPointsJson(validPoints, mapSource)
        val index = markerIndex.coerceIn(0, (validPoints.size - 1).coerceAtLeast(0))
        val otherJson = serializeDeviceMarkersJson(validMarkers, mapSource)
        val polygonJson = serializeMapPointsJson(validPolygon, mapSource)
        val fencesJson = serializeFencesJson(fences, mapSource)
        val speedsJson = serializeSpeedsJson(routeSpeeds)

        val payload = listOf(
            mapSource.id,
            satellite.toString(),
            traffic.toString(),
            routeJson,
            index.toString(),
            normalizedRadius.toString(),
            markerName,
            markerDirection.toString(),
            markerOnline.toString(),
            markerStatus,
            markerIconUrl,
            followMarker.toString(),
            autoCenter.toString(),
            focusRequest,
            speedsJson,
            otherJson,
            polygonJson,
            fencesJson
        ).joinToString("\u001f")

        if (payload == lastPayload) return
        lastPayload = payload

        val routeChanged = routeJson != previousRoute || speedsJson != previousSpeeds
        val rawRouteChanged = validPoints != previousRawRoutePoints
        val rawMarkersChanged = validMarkers != previousRawMarkers
        val markersExpandedFromEmpty = previousRawMarkers.isEmpty() && validMarkers.isNotEmpty()
        val focusChanged = focusRequest != previousFocusRequest
        val radiusChanged = normalizedRadius != previousRawRadius
        val polygonAppendedOne = validPolygon.size == previousRawPolygon.size + 1 &&
            validPolygon.dropLast(1) == previousRawPolygon
        val polygonResetOrReplaced = validPolygon != previousRawPolygon &&
            !polygonAppendedOne &&
            !(validPolygon.size + 1 == previousRawPolygon.size && previousRawPolygon.dropLast(1) == validPolygon)
        val fenceChanged = radiusChanged || polygonResetOrReplaced || fences != previousRawFences

        previousFocusRequest = focusRequest
        previousRoute = routeJson
        previousSpeeds = speedsJson
        previousRawRoutePoints = validPoints
        previousRawMarkers = validMarkers
        previousRawPolygon = validPolygon
        previousRawRadius = normalizedRadius
        previousRawFences = fences

        pendingJson = routeJson
        pendingIndex = index
        pendingRadius = normalizedRadius
        pendingName = markerName
        pendingDirection = markerDirection
        pendingOnline = markerOnline
        pendingStatus = markerStatus
        pendingIconUrl = markerIconUrl
        pendingFollowMarker = followMarker
        pendingSpeeds = speedsJson
        allowedTeids = validMarkers.map { it.teid }.toSet()
        pendingOtherMarkers = otherJson
        pendingPolygon = polygonJson
        pendingFences = fencesJson
        val hasContent = validPoints.isNotEmpty() || validMarkers.isNotEmpty() ||
            validPolygon.isNotEmpty() || fences.isNotEmpty()
        pendingHasContent = hasContent

        if (pageReady) {
            if (tileSourceChanged) {
                applyTileSource()
            }
            val focusPrimary = hasFitted && focusChanged && !markersExpandedFromEmpty
            val fitRoute = hasContent && (
                !hasFitted ||
                    (focusChanged && !focusPrimary) ||
                    fenceChanged ||
                    (autoCenter && (rawRouteChanged || rawMarkersChanged))
                )
            applyMapData(
                fitRoute = fitRoute,
                routeChanged = routeChanged,
                focusPrimary = focusPrimary
            )
            if (hasContent) {
                hasFitted = true
            }
        }
    }

    private fun applyLabels() {
        val script = "if (window.setMapLabels) window.setMapLabels(" +
            JSONObject.quote(pendingLabelsJson) + ");"
        evaluateJavascript(script, null)
    }

    private fun applyTileSource() {
        val script = "if (window.setTileSource) window.setTileSource(" +
            JSONObject.quote(currentMapSource.id) + "," +
            currentSatellite + "," +
            currentTraffic + ");"
        evaluateJavascript(script, null)
    }

    private fun applyMapData(
        fitRoute: Boolean = true,
        routeChanged: Boolean = true,
        focusPrimary: Boolean = false
    ) {
        val script = "if (window.setMapData) window.setMapData(" +
            JSONObject.quote(pendingJson) + "," +
            pendingIndex + "," +
            fitRoute + "," +
            routeChanged + "," +
            pendingRadius + "," +
            JSONObject.quote(pendingName) + "," +
            pendingDirection + "," +
            pendingOnline + "," +
            JSONObject.quote(pendingStatus) + "," +
            JSONObject.quote(pendingIconUrl) + "," +
            pendingFollowMarker + "," +
            JSONObject.quote(pendingSpeeds) + "," +
            JSONObject.quote(pendingOtherMarkers) + "," +
            JSONObject.quote(pendingPolygon) + "," +
            JSONObject.quote(pendingFences) + "," +
            focusPrimary + ");"
        evaluateJavascript(script, null)
    }

    private fun redactSecrets(text: String): String = text
        .replace(BuildConfig.TIANMAP_KEY, "<redacted>")
        .replace(BuildConfig.AMAP_WEB_KEY, "<redacted>")
        .replace(BuildConfig.AMAP_SECURITY_CODE, "<redacted>")

    private fun buildMapHtml(initialLabels: Map<String, String>): String {
        val quotedLabels = JSONObject.quote(serializeLabelsJson(initialLabels))
        val quotedTianmapKey = JSONObject.quote(BuildConfig.TIANMAP_KEY)
        val quotedSourceId = JSONObject.quote(currentMapSource.id)
        val initSat = currentSatellite
        val initTraffic = currentTraffic
        return """
        <!doctype html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
          <meta charset="utf-8">
          <link rel="stylesheet" href="https://app.newsmy-nmgps.com/__assets__/leaflet.css">
          <style>
            html,body,#map{width:100%;height:100%;padding:0;margin:0;overflow:hidden;background:#f1f4f6}
            #tile-error-banner{display:none;position:absolute;z-index:1000;top:${controlsTopPx}px;left:16px;right:16px;max-width:360px;margin:0 auto;align-items:center;justify-content:space-between;gap:10px;padding:8px 12px;border-radius:12px;background:rgba(255,255,255,0.96);color:#a52332;font:13px/1.35 sans-serif;box-shadow:0 2px 10px rgba(0,0,0,0.16);border:1px solid rgba(165,35,50,0.22)}
            #tile-error-text{flex:1;min-width:0}
            #tile-retry-btn{flex-shrink:0;border:0;border-radius:8px;background:#007dff;color:#fff;padding:6px 12px;font:600 12px sans-serif}
            .device-marker-root{position:relative;width:28px;height:28px;overflow:visible}
            .device-arrow{width:28px;height:28px;box-sizing:border-box;display:flex;align-items:center;justify-content:center;border:2px solid #ffffff;border-radius:50%;background:#148047;color:#ffffff;box-shadow:0 2px 6px rgba(0,0,0,0.35);font-size:14px;line-height:1}
            .device-marker-root.offline .device-arrow{background:#8b949e}
            .device-marker-root.moving .device-arrow{background:#1677ff}
            .device-marker-root.alarm .device-arrow{background:#db3f44}
            .device-marker-root.sleep .device-arrow{background:#d48d18}
            .device-arrow-glyph{display:inline-block;transform-origin:center}
            .device-arrow img{width:20px;height:20px;object-fit:contain;display:block;border-radius:50%;transform-origin:center}
            .device-name{position:absolute;left:32px;top:50%;transform:translateY(-50%);max-width:130px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;background:rgba(255,255,255,0.95);color:#202b36;border:1px solid #d8dfe3;border-radius:4px;padding:2px 6px;font:12px/1.3 sans-serif;box-shadow:0 1px 4px rgba(0,0,0,0.18)}
            .endpoint-anchor{width:0;height:0;overflow:visible}
            .endpoint-pill{position:absolute;left:0;bottom:10px;transform:translateX(-50%);white-space:nowrap;padding:2px 7px;border-radius:10px;font:600 11px/1.3 sans-serif;color:#ffffff;box-shadow:0 1px 4px rgba(0,0,0,0.28)}
            .endpoint-pill.start{background:#19be6b}
            .endpoint-pill.end{background:#ed4014}
          </style>
          <script src="https://app.newsmy-nmgps.com/__assets__/leaflet.js"></script>
        </head>
        <body>
          <div id="map"></div>
          <div id="tile-error-banner">
            <span id="tile-error-text"></span>
            <button id="tile-retry-btn" type="button"></button>
          </div>
          <script>
            (function(){
              var labels = JSON.parse($quotedLabels);
              var TIANMAP_KEY = $quotedTianmapKey;
              var SOURCE_CRS = {
                amap: 'GCJ02',
                tencent: 'GCJ02',
                tianmap: 'WGS84',
                bing: 'GCJ02',
                arcgis: 'WGS84',
                osm: 'WGS84',
                google: 'GCJ02'
              };
              var SOURCE_MAX_ZOOM = {
                amap: 18,
                tencent: 18,
                tianmap: 18,
                bing: 18,
                arcgis: 18,
                osm: 19,
                google: 19
              };

              var PI_VAL = Math.PI, A_AXIS = 6378245.0, EE = 0.00669342162296594323;
              function isOutOfChina(lat, lon) {
                return lon < 72.004 || lon > 137.8347 || lat < 0.8293 || lat > 55.8271;
              }
              function transformLat(x, y) {
                var r = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
                r += (20.0 * Math.sin(6.0 * x * PI_VAL) + 20.0 * Math.sin(2.0 * x * PI_VAL)) * 2.0 / 3.0;
                r += (20.0 * Math.sin(y * PI_VAL) + 40.0 * Math.sin(y / 3.0 * PI_VAL)) * 2.0 / 3.0;
                r += (160.0 * Math.sin(y / 12.0 * PI_VAL) + 320.0 * Math.sin(y * PI_VAL / 30.0)) * 2.0 / 3.0;
                return r;
              }
              function transformLon(x, y) {
                var r = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
                r += (20.0 * Math.sin(6.0 * x * PI_VAL) + 20.0 * Math.sin(2.0 * x * PI_VAL)) * 2.0 / 3.0;
                r += (20.0 * Math.sin(x * PI_VAL) + 40.0 * Math.sin(x / 3.0 * PI_VAL)) * 2.0 / 3.0;
                r += (150.0 * Math.sin(x / 12.0 * PI_VAL) + 300.0 * Math.sin(x / 30.0 * PI_VAL)) * 2.0 / 3.0;
                return r;
              }
              function calcDelta(lat, lon) {
                var dLat = transformLat(lon - 105.0, lat - 35.0);
                var dLon = transformLon(lon - 105.0, lat - 35.0);
                var radLat = lat / 180.0 * PI_VAL;
                var magic = Math.sin(radLat);
                magic = 1 - EE * magic * magic;
                var sqrtMagic = Math.sqrt(magic);
                dLat = (dLat * 180.0) / ((A_AXIS * (1 - EE)) / (magic * sqrtMagic) * PI_VAL);
                dLon = (dLon * 180.0) / (A_AXIS / sqrtMagic * Math.cos(radLat) * PI_VAL);
                return { dLat: dLat, dLon: dLon };
              }
              function wgs84ToGcj02(lat, lon) {
                if (isOutOfChina(lat, lon)) return { lat: lat, lon: lon };
                var d = calcDelta(lat, lon);
                return { lat: lat + d.dLat, lon: lon + d.dLon };
              }
              function gcj02ToWgs84(lat, lon) {
                if (isOutOfChina(lat, lon)) return { lat: lat, lon: lon };
                var d = calcDelta(lat, lon);
                return { lat: lat - d.dLat, lon: lon - d.dLon };
              }

              var bannerEl = document.getElementById('tile-error-banner');
              var errorTextEl = document.getElementById('tile-error-text');
              var retryBtnEl = document.getElementById('tile-retry-btn');
              function syncLabelTexts() {
                if (errorTextEl) errorTextEl.textContent = labels.error || '';
                if (retryBtnEl) retryBtnEl.textContent = labels.retry || 'Retry';
              }
              syncLabelTexts();

              if (typeof L === 'undefined') {
                if (bannerEl) bannerEl.style.display = 'flex';
                return;
              }

              L.DomEvent.disableClickPropagation(bannerEl);
              L.DomEvent.disableScrollPropagation(bannerEl);

              var map = L.map('map', {
                center: [35.86, 104.19],
                zoom: 5,
                minZoom: 2,
                maxZoom: 18,
                zoomControl: false,
                attributionControl: false,
                preferCanvas: true
              });

              var routeGroup = L.layerGroup().addTo(map);
              var overlayGroup = L.layerGroup().addTo(map);

              var activeTileLayers = [];
              var currentSourceId = '';
              var currentSatellite = false;
              var currentTraffic = false;
              var failedTiles = 0;
              var loadedTiles = 0;

              function showTileError() {
                if (bannerEl) bannerEl.style.display = 'flex';
              }
              function hideTileError() {
                if (bannerEl) bannerEl.style.display = 'none';
              }
              function onTileLoad() {
                loadedTiles++;
                hideTileError();
              }
              function onTileError() {
                failedTiles++;
                if (failedTiles >= 4 && loadedTiles === 0) {
                  showTileError();
                }
              }

              retryBtnEl.addEventListener('click', function(ev) {
                L.DomEvent.stopPropagation(ev);
                failedTiles = 0;
                loadedTiles = 0;
                hideTileError();
                for (var i = 0; i < activeTileLayers.length; i++) {
                  if (activeTileLayers[i].redraw) activeTileLayers[i].redraw();
                }
              });

              var TencentTileLayer = L.TileLayer.extend({
                initialize: function(urlFn, options) {
                  this._urlFn = urlFn;
                  L.TileLayer.prototype.initialize.call(this, '', options);
                },
                getTileUrl: function(coords) {
                  var z = coords.z, x = coords.x, y = coords.y;
                  var tmsY = Math.pow(2, z) - 1 - y;
                  var sub = Math.abs(x + y) % 4;
                  return this._urlFn(z, x, y, tmsY, sub);
                }
              });

              var BingTileLayer = L.TileLayer.extend({
                initialize: function(urlFn, options) {
                  this._urlFn = urlFn;
                  L.TileLayer.prototype.initialize.call(this, '', options);
                },
                getTileUrl: function(coords) {
                  var z = coords.z, x = coords.x, y = coords.y, quad = '';
                  for (var i = z; i > 0; i--) {
                    var digit = 0, mask = 1 << (i - 1);
                    if ((x & mask) !== 0) digit++;
                    if ((y & mask) !== 0) digit += 2;
                    quad += digit;
                  }
                  var sub = Math.abs(x + y) % 4;
                  return this._urlFn(quad, sub);
                }
              });

              function buildTileLayers(sourceId, satellite, traffic) {
                var layers = [];
                if (sourceId === 'tencent') {
                  if (satellite) {
                    layers.push(new TencentTileLayer(function(z, x, y, tmsY, sub) {
                      return 'https://p' + sub + '.map.gtimg.com/sateTiles/' + z + '/' + Math.floor(x / 16) + '/' + Math.floor(tmsY / 16) + '/' + x + '_' + tmsY + '.jpg';
                    }, { maxZoom: 18, minZoom: 2, zIndex: 1 }));
                    layers.push(new TencentTileLayer(function(z, x, y, tmsY, sub) {
                      return 'https://rt' + sub + '.map.gtimg.com/tile?z=' + z + '&x=' + x + '&y=' + tmsY + '&styleid=2&version=297';
                    }, { maxZoom: 18, minZoom: 2, zIndex: 2 }));
                  } else {
                    layers.push(new TencentTileLayer(function(z, x, y, tmsY, sub) {
                      return 'https://rt' + sub + '.map.gtimg.com/realtimerender?z=' + z + '&x=' + x + '&y=' + tmsY + '&type=vector&style=0';
                    }, { maxZoom: 18, minZoom: 2, zIndex: 1 }));
                  }
                } else if (sourceId === 'tianmap') {
                  var tk = encodeURIComponent(TIANMAP_KEY || '');
                  var baseLayer = satellite ? 'img' : 'vec';
                  var labelLayer = satellite ? 'cia' : 'cva';
                  layers.push(L.tileLayer('https://t{s}.tianditu.gov.cn/' + baseLayer + '_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=' + baseLayer + '&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=' + tk, {
                    subdomains: '01234567', maxZoom: 18, minZoom: 2, zIndex: 1
                  }));
                  layers.push(L.tileLayer('https://t{s}.tianditu.gov.cn/' + labelLayer + '_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=' + labelLayer + '&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}&tk=' + tk, {
                    subdomains: '01234567', maxZoom: 18, minZoom: 2, zIndex: 2
                  }));
                } else if (sourceId === 'bing') {
                  if (satellite) {
                    layers.push(new BingTileLayer(function(quad, sub) {
                      return 'https://ecn.t' + sub + '.tiles.virtualearth.net/tiles/h' + quad + '.jpeg?g=9675&mkt=zh-cn';
                    }, { maxZoom: 18, minZoom: 2, zIndex: 1 }));
                  } else {
                    layers.push(new BingTileLayer(function(quad, sub) {
                      return 'https://r' + sub + '.tiles.ditu.live.com/tiles/r' + quad + '.png?g=100&mkt=zh-cn';
                    }, { maxZoom: 18, minZoom: 2, zIndex: 1 }));
                  }
                } else if (sourceId === 'arcgis') {
                  if (satellite) {
                    layers.push(L.tileLayer('https://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
                      maxZoom: 18, minZoom: 2, zIndex: 1
                    }));
                    layers.push(L.tileLayer('https://services.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/{z}/{y}/{x}', {
                      maxZoom: 18, minZoom: 2, zIndex: 2
                    }));
                  } else {
                    layers.push(L.tileLayer('https://services.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/{z}/{y}/{x}', {
                      maxZoom: 18, minZoom: 2, zIndex: 1
                    }));
                  }
                } else if (sourceId === 'osm') {
                  layers.push(L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    maxZoom: 19, minZoom: 2, zIndex: 1
                  }));
                } else if (sourceId === 'google') {
                  var lyrs = satellite ? 'y' : 'm';
                  layers.push(L.tileLayer('https://mt{s}.google.com/vt/lyrs=' + lyrs + '&hl=zh-CN&gl=cn&x={x}&y={y}&z={z}', {
                    subdomains: '0123', maxZoom: 19, minZoom: 2, zIndex: 1
                  }));
                } else {
                  if (satellite) {
                    layers.push(L.tileLayer('https://webst0{s}.is.autonavi.com/appmaptile?style=6&x={x}&y={y}&z={z}', {
                      subdomains: '1234', maxZoom: 18, minZoom: 2, zIndex: 1
                    }));
                    layers.push(L.tileLayer('https://wprd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&style=8&x={x}&y={y}&z={z}&scl=2', {
                      subdomains: '1234', maxZoom: 18, minZoom: 2, zIndex: 2
                    }));
                  } else {
                    layers.push(L.tileLayer('https://wprd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&style=7&x={x}&y={y}&z={z}&scl=2', {
                      subdomains: '1234', maxZoom: 18, minZoom: 2, zIndex: 1
                    }));
                  }
                  if (traffic) {
                    layers.push(L.tileLayer('https://tm.amap.com/trafficengine/mapabc/traffictile?v=1.0&t=1&x={x}&y={y}&z={z}', {
                      maxZoom: 18, minZoom: 2, zIndex: 10
                    }));
                  }
                }
                return layers;
              }

              window.setTileSource = function(sourceId, satellite, traffic) {
                sourceId = sourceId || 'amap';
                satellite = !!satellite;
                traffic = !!traffic;
                if (sourceId === currentSourceId && satellite === currentSatellite && traffic === currentTraffic && activeTileLayers.length > 0) {
                  return;
                }
                var prevCrs = SOURCE_CRS[currentSourceId] || 'GCJ02';
                var nextCrs = SOURCE_CRS[sourceId] || 'GCJ02';
                if (currentSourceId && prevCrs !== nextCrs) {
                  var c = map.getCenter();
                  var shifted = nextCrs === 'WGS84' ? gcj02ToWgs84(c.lat, c.lng) : wgs84ToGcj02(c.lat, c.lng);
                  map.setView([shifted.lat, shifted.lng], map.getZoom(), { animate: false });
                }
                for (var i = 0; i < activeTileLayers.length; i++) {
                  activeTileLayers[i].off('tileload', onTileLoad);
                  activeTileLayers[i].off('tileerror', onTileError);
                  map.removeLayer(activeTileLayers[i]);
                }
                activeTileLayers = [];
                currentSourceId = sourceId;
                currentSatellite = satellite;
                currentTraffic = traffic;
                failedTiles = 0;
                loadedTiles = 0;
                hideTileError();

                map.setMaxZoom(SOURCE_MAX_ZOOM[sourceId] || 18);
                var nextLayers = buildTileLayers(sourceId, satellite, traffic);
                for (var j = 0; j < nextLayers.length; j++) {
                  nextLayers[j].on('tileload', onTileLoad);
                  nextLayers[j].on('tileerror', onTileError);
                  nextLayers[j].addTo(map);
                  activeTileLayers.push(nextLayers[j]);
                }
              };

              window.setTileSource($quotedSourceId, $initSat, $initTraffic);

              if (typeof ResizeObserver !== 'undefined') {
                new ResizeObserver(function() { map.invalidateSize(false); }).observe(document.getElementById('map'));
              }
              window.addEventListener('resize', function() { map.invalidateSize(false); });
              setTimeout(function() { map.invalidateSize(false); }, 150);
              setTimeout(function() { map.invalidateSize(false); }, 500);

              map.on('click', function(event) {
                if (window.NativeMap && event && event.latlng) {
                  NativeMap.onMapTap(event.latlng.lng, event.latlng.lat);
                }
              });

              function speedColor(speed) {
                var s = Number(speed || 0);
                if (s < 20) return '#2d8cf0';
                if (s < 40) return '#5cadff';
                if (s < 60) return '#19be6b';
                if (s < 80) return '#99CC33';
                if (s < 100) return '#ff9900';
                if (s < 120) return '#ed4014';
                return '#990033';
              }

              function createDeviceIcon(name, direction, status, iconUrl) {
                var root = document.createElement('div');
                root.className = 'device-marker-root ' + (status || 'online');
                var arrow = document.createElement('div');
                arrow.className = 'device-arrow';
                var rot = 'rotate(' + Number(direction || 0) + 'deg)';
                var fallbackGlyph = function() {
                  arrow.textContent = '';
                  var glyph = document.createElement('span');
                  glyph.className = 'device-arrow-glyph';
                  glyph.textContent = '▲';
                  glyph.style.transform = rot;
                  arrow.appendChild(glyph);
                };
                if (iconUrl) {
                  var img = document.createElement('img');
                  img.src = iconUrl;
                  img.style.transform = rot;
                  img.onerror = fallbackGlyph;
                  arrow.appendChild(img);
                } else {
                  fallbackGlyph();
                }
                root.appendChild(arrow);
                if (name) {
                  var title = document.createElement('span');
                  title.className = 'device-name';
                  title.textContent = name;
                  root.appendChild(title);
                }
                return L.divIcon({
                  className: '',
                  html: root,
                  iconSize: [28, 28],
                  iconAnchor: [14, 14]
                });
              }

              function createEndpointIcon(text, isEnd) {
                var pill = document.createElement('span');
                pill.className = 'endpoint-pill ' + (isEnd ? 'end' : 'start');
                pill.textContent = text;
                return L.divIcon({
                  className: 'endpoint-anchor',
                  html: pill,
                  iconSize: [0, 0],
                  iconAnchor: [0, 0]
                });
              }

              var endMarkerRefs = [];
              function refreshEndpointLabels() {
                if (endMarkerRefs.length === 2) {
                  endMarkerRefs[0].setIcon(createEndpointIcon(labels.start || 'Start', false));
                  endMarkerRefs[1].setIcon(createEndpointIcon(labels.end || 'End', true));
                }
              }

              window.setMapLabels = function(labelsJson) {
                try {
                  labels = JSON.parse(labelsJson || '{}');
                  syncLabelTexts();
                  refreshEndpointLabels();
                } catch (e) {}
              };

              function isValidPoint(p) {
                return p && Number.isFinite(p.lat) && Number.isFinite(p.lon) &&
                  p.lat >= -85 && p.lat <= 85 && p.lon >= -180 && p.lon <= 180;
              }

              function extendBoundsWithLatLng(bounds, lat, lon) {
                bounds.extend([lat, lon]);
              }

              window.setMapData = function(
                serialized,
                index,
                fitRoute,
                routeChanged,
                radius,
                name,
                direction,
                online,
                status,
                iconUrl,
                followMarker,
                speedsJson,
                otherJson,
                polygonJson,
                fencesJson,
                focusPrimary
              ) {
                map.invalidateSize(false);
                var points = JSON.parse(serialized || '[]').filter(isValidPoint);
                var speeds = JSON.parse(speedsJson || '[]');
                var others = JSON.parse(otherJson || '[]').filter(isValidPoint);
                var vertices = JSON.parse(polygonJson || '[]').filter(isValidPoint);
                var fences = JSON.parse(fencesJson || '[]');

                overlayGroup.clearLayers();

                if (routeChanged) {
                  routeGroup.clearLayers();
                  endMarkerRefs = [];
                  if (points.length > 1) {
                    var runColor = speedColor(speeds[0]);
                    var runCoords = [[points[0].lat, points[0].lon], [points[1].lat, points[1].lon]];
                    for (var i = 2; i < points.length; i++) {
                      var segColor = speedColor(speeds[i - 1]);
                      if (segColor === runColor) {
                        runCoords.push([points[i].lat, points[i].lon]);
                      } else {
                        L.polyline(runCoords, { color: runColor, weight: 5, opacity: 0.9 }).addTo(routeGroup);
                        runColor = segColor;
                        runCoords = [[points[i - 1].lat, points[i - 1].lon], [points[i].lat, points[i].lon]];
                      }
                    }
                    L.polyline(runCoords, { color: runColor, weight: 5, opacity: 0.9 }).addTo(routeGroup);

                    var startPoint = points[0];
                    var endPoint = points[points.length - 1];
                    var startMarker = L.marker([startPoint.lat, startPoint.lon], {
                      icon: createEndpointIcon(labels.start || 'Start', false),
                      interactive: false
                    }).addTo(routeGroup);
                    var endMarker = L.marker([endPoint.lat, endPoint.lon], {
                      icon: createEndpointIcon(labels.end || 'End', true),
                      interactive: false
                    }).addTo(routeGroup);
                    endMarkerRefs = [startMarker, endMarker];
                  }
                }

                var bounds = L.latLngBounds([]);
                for (var pIdx = 0; pIdx < points.length; pIdx++) {
                  extendBoundsWithLatLng(bounds, points[pIdx].lat, points[pIdx].lon);
                }

                var current = points.length > 0 ? points[Math.max(0, Math.min(index, points.length - 1))] : null;
                if (current) {
                  L.marker([current.lat, current.lon], {
                    icon: createDeviceIcon(name, direction, status || (online ? 'online' : 'offline'), iconUrl),
                    zIndexOffset: 500,
                    interactive: false
                  }).addTo(overlayGroup);
                }

                for (var dIdx = 0; dIdx < others.length; dIdx++) {
                  (function(device) {
                    extendBoundsWithLatLng(bounds, device.lat, device.lon);
                    var item = L.marker([device.lat, device.lon], {
                      icon: createDeviceIcon(device.name || device.teid, device.direction, device.status || 'offline', device.iconUrl),
                      bubblingMouseEvents: false
                    });
                    item.on('click', function(ev) {
                      L.DomEvent.stopPropagation(ev);
                      if (ev.originalEvent && ev.originalEvent.stopPropagation) {
                        ev.originalEvent.stopPropagation();
                      }
                      if (window.NativeMap) NativeMap.onDeviceTap(device.teid);
                    });
                    item.addTo(overlayGroup);
                  })(others[dIdx]);
                }

                if (radius > 0 && current) {
                  L.circle([current.lat, current.lon], {
                    radius: radius,
                    color: '#e5484d',
                    weight: 2,
                    opacity: 0.9,
                    fillColor: '#e5484d',
                    fillOpacity: 0.12,
                    interactive: false
                  }).addTo(overlayGroup);
                  bounds.extend(L.latLng(current.lat, current.lon).toBounds(radius * 2));
                }

                if (vertices.length >= 3) {
                  L.polygon(vertices.map(function(v) { return [v.lat, v.lon]; }), {
                    color: '#e5484d',
                    weight: 2,
                    opacity: 0.9,
                    fillColor: '#e5484d',
                    fillOpacity: 0.12,
                    interactive: false
                  }).addTo(overlayGroup);
                } else if (vertices.length === 2) {
                  L.polyline([[vertices[0].lat, vertices[0].lon], [vertices[1].lat, vertices[1].lon]], {
                    color: '#e5484d',
                    weight: 2,
                    opacity: 0.9,
                    dashArray: '6,4',
                    interactive: false
                  }).addTo(overlayGroup);
                }
                for (var vIdx = 0; vIdx < vertices.length; vIdx++) {
                  var vertex = vertices[vIdx];
                  extendBoundsWithLatLng(bounds, vertex.lat, vertex.lon);
                  L.circleMarker([vertex.lat, vertex.lon], {
                    radius: 6,
                    color: '#e5484d',
                    weight: 2,
                    fillColor: '#ffffff',
                    fillOpacity: 1,
                    interactive: false
                  }).addTo(overlayGroup);
                }

                for (var fIdx = 0; fIdx < fences.length; fIdx++) {
                  var fence = fences[fIdx];
                  var fenceLayer = null;
                  if (fence.isPolygon && Array.isArray(fence.vertices)) {
                    var fv = fence.vertices.filter(isValidPoint);
                    if (fv.length >= 3) {
                      fenceLayer = L.polygon(fv.map(function(v) { return [v.lat, v.lon]; }), {
                        color: '#007dff',
                        weight: 2,
                        opacity: 0.85,
                        fillColor: '#007dff',
                        fillOpacity: 0.10,
                        bubblingMouseEvents: false
                      }).addTo(overlayGroup);
                      for (var fvIdx = 0; fvIdx < fv.length; fvIdx++) {
                        extendBoundsWithLatLng(bounds, fv[fvIdx].lat, fv[fvIdx].lon);
                      }
                    }
                  } else if (!fence.isPolygon && isValidPoint(fence.center) && Number(fence.radiusMeters) > 0) {
                    var fc = fence.center;
                    var fr = Number(fence.radiusMeters);
                    fenceLayer = L.circle([fc.lat, fc.lon], {
                      radius: fr,
                      color: '#007dff',
                      weight: 2,
                      opacity: 0.85,
                      fillColor: '#007dff',
                      fillOpacity: 0.10,
                      bubblingMouseEvents: false
                    }).addTo(overlayGroup);
                    bounds.extend(L.latLng(fc.lat, fc.lon).toBounds(fr * 2));
                  }
                  if (fenceLayer && fence.name) {
                    var tipEl = document.createElement('span');
                    tipEl.textContent = fence.name;
                    fenceLayer.bindTooltip(tipEl, { direction: 'center', sticky: true });
                  }
                }

                if (focusPrimary && current) {
                  map.setView([current.lat, current.lon], Math.max(map.getZoom() || 0, 15));
                } else if (fitRoute && bounds.isValid()) {
                  var sw = bounds.getSouthWest();
                  var ne = bounds.getNorthEast();
                  var isZeroArea = Math.abs(sw.lat - ne.lat) < 1e-7 && Math.abs(sw.lng - ne.lng) < 1e-7;
                  if (isZeroArea) {
                    map.setView([sw.lat, sw.lng], Math.max(map.getZoom() || 0, 15));
                  } else {
                    var sz = map.getSize();
                    var padX = Math.min(36, Math.floor(sz.x * 0.1));
                    var padTop = Math.min(48, Math.floor(sz.y * 0.12));
                    var padBottom = Math.min(180, Math.floor(sz.y * 0.25));
                    map.fitBounds(bounds, {
                      paddingTopLeft: [padX, padTop],
                      paddingBottomRight: [padX, padBottom],
                      maxZoom: 17
                    });
                  }
                } else if (current && followMarker && !map.getBounds().contains(L.latLng(current.lat, current.lon))) {
                  map.panTo([current.lat, current.lon]);
                }
              };
            })();
          </script>
        </body>
        </html>
        """.trimIndent()
    }

    private companion object {
        const val TAG = "AmapJsMap"
        const val ASSET_HOST = "app.newsmy-nmgps.com"
        const val BASE_URL = "https://app.newsmy-nmgps.com/"
    }
}
