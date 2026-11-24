package com.scluzlep.newmangps.ui.screens.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scluzlep.newmangps.data.MapSource
import com.scluzlep.newmangps.data.util.CoordinateTransform
import com.scluzlep.newmangps.ui.LocalAppLanguage
import com.scluzlep.newmangps.ui.theme.HyperBlue
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

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

private class CameraTrackingState {
    var hasFitted = false
    var previousFocusRequest = ""
    var previousRawRoutePoints = emptyList<CoordinateTransform.LatLng>()
    var previousRawMarkers = emptyList<MapDeviceMarker>()
    var previousRawPolygon = emptyList<CoordinateTransform.LatLng>()
    var previousRawRadius = 0
    var previousRawFences = emptyList<MapFenceOverlay>()
}

private data class RenderedDeviceMarker(
    val teid: String,
    val name: String,
    val mapCoord: CoordinateTransform.LatLng,
    val direction: Int,
    val status: String,
    val iconUrl: String
)

private data class RenderedFence(
    val name: String,
    val isPolygon: Boolean,
    val centerMap: CoordinateTransform.LatLng?,
    val radiusMeters: Int,
    val verticesMap: List<CoordinateTransform.LatLng>
)

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
    val context = LocalContext.current
    val density = LocalDensity.current
    val viewConfig = LocalViewConfiguration.current
    val language = LocalAppLanguage.current
    val darkTheme = isSystemInDarkTheme()
    val textMeasurer = rememberTextMeasurer()

    val errorLabel = language.text("MapTileError", "地图瓦片加载失败，请检查网络")
    val startLabel = language.text("StartLocation", "起点")
    val endLabel = language.text("EndLocation", "终点")
    val retryLabel = language.text("Retry", "重试")

    val validPoints = remember(points) {
        points.filter { isValidMapCoordinate(it.latitude, it.longitude) }
    }
    val validMarkers = remember(otherMarkers) {
        otherMarkers.filter { isValidMapCoordinate(it.latitude, it.longitude) }
    }
    val validPolygon = remember(polygonPoints) {
        polygonPoints.filter { isValidMapCoordinate(it.latitude, it.longitude) }
    }
    val normalizedRadius = circleRadiusMeters.coerceAtLeast(0)
    val clampedIndex = markerIndex.coerceIn(0, (validPoints.size - 1).coerceAtLeast(0))

    val tileSizePx = MapProjection.BASE_TILE_DP * density.density
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var centerWgs by remember { mutableStateOf(CoordinateTransform.LatLng(35.86, 104.19)) }
    var zoom by remember { mutableDoubleStateOf(5.0) }
    val tracker = remember { CameraTrackingState() }

    val maxZoom = mapSource.maxZoom.toDouble()
    LaunchedEffect(maxZoom) {
        if (zoom > maxZoom) zoom = maxZoom
    }

    val layerSpecs = remember(mapSource, satellite, traffic) {
        buildTileLayerSpecs(mapSource, satellite, traffic)
    }

    var retryNonce by remember { mutableIntStateOf(0) }
    var tileRevision by remember { mutableIntStateOf(0) }
    var iconRevision by remember { mutableIntStateOf(0) }
    var loadedBaseTiles by remember(mapSource, satellite, retryNonce) { mutableIntStateOf(0) }
    var failedBaseTiles by remember(mapSource, satellite, retryNonce) { mutableIntStateOf(0) }

    val mapPoints = remember(validPoints, mapSource) {
        validPoints.map { mapSource.toMapCoord(it) }
    }
    val mapMarkers = remember(validMarkers, mapSource) {
        validMarkers.map {
            RenderedDeviceMarker(
                teid = it.teid,
                name = it.name.ifBlank { it.teid },
                mapCoord = mapSource.toMapCoord(CoordinateTransform.LatLng(it.latitude, it.longitude)),
                direction = it.direction,
                status = it.status.ifBlank { "offline" },
                iconUrl = it.iconUrl
            )
        }
    }
    val mapPolygon = remember(validPolygon, mapSource) {
        validPolygon.map { mapSource.toMapCoord(it) }
    }
    val mapFences = remember(fences, mapSource) {
        fences.map { fence ->
            val centerMap = fence.center
                ?.takeIf { isValidMapCoordinate(it.latitude, it.longitude) }
                ?.let { mapSource.toMapCoord(it) }
            val vertsMap = fence.vertices
                .filter { isValidMapCoordinate(it.latitude, it.longitude) }
                .map { mapSource.toMapCoord(it) }
            RenderedFence(
                name = fence.name,
                isPolygon = fence.isPolygon,
                centerMap = centerMap,
                radiusMeters = fence.radiusMeters.coerceAtLeast(0),
                verticesMap = vertsMap
            )
        }
    }

    val currentMapPoint = mapPoints.getOrNull(clampedIndex)

    val padX = min(36.0 * density.density, viewportSize.width * 0.1)
    val padTop = min(controlsTopPx * density.density + 16.0 * density.density, viewportSize.height * 0.2)
    val padBottom = if (controlsTopPx > 24) {
        min(120.0 * density.density, viewportSize.height * 0.24)
    } else {
        min(32.0 * density.density, viewportSize.height * 0.14)
    }

    // Smart camera fitting & follow updates
    LaunchedEffect(
        validPoints,
        validMarkers,
        validPolygon,
        normalizedRadius,
        fences,
        clampedIndex,
        focusRequest,
        autoCenter,
        followMarker,
        viewportSize
    ) {
        if (viewportSize.width <= 0 || viewportSize.height <= 0) return@LaunchedEffect

        val rawRouteChanged = validPoints != tracker.previousRawRoutePoints
        val rawMarkersChanged = validMarkers != tracker.previousRawMarkers
        val markersExpandedFromEmpty = tracker.previousRawMarkers.isEmpty() && validMarkers.isNotEmpty()
        val focusChanged = focusRequest != tracker.previousFocusRequest
        val radiusChanged = normalizedRadius != tracker.previousRawRadius
        val polygonAppendedOne = validPolygon.size == tracker.previousRawPolygon.size + 1 &&
            validPolygon.dropLast(1) == tracker.previousRawPolygon
        val polygonResetOrReplaced = validPolygon != tracker.previousRawPolygon &&
            !polygonAppendedOne &&
            !(validPolygon.size + 1 == tracker.previousRawPolygon.size &&
                tracker.previousRawPolygon.dropLast(1) == validPolygon)
        val fenceChanged = radiusChanged || polygonResetOrReplaced || fences != tracker.previousRawFences

        val hasContent = validPoints.isNotEmpty() || validMarkers.isNotEmpty() ||
            validPolygon.isNotEmpty() || fences.isNotEmpty()

        val focusPrimary = tracker.hasFitted && focusChanged && !markersExpandedFromEmpty
        val fitRoute = hasContent && (
            !tracker.hasFitted ||
                (focusChanged && !focusPrimary) ||
                fenceChanged ||
                (autoCenter && (rawRouteChanged || rawMarkersChanged))
            )

        tracker.previousFocusRequest = focusRequest
        tracker.previousRawRoutePoints = validPoints
        tracker.previousRawMarkers = validMarkers
        tracker.previousRawPolygon = validPolygon
        tracker.previousRawRadius = normalizedRadius
        tracker.previousRawFences = fences

        if (focusPrimary && currentMapPoint != null) {
            val targetZoom = max(zoom, 15.0).coerceIn(2.0, min(17.0, maxZoom))
            val (centerMap, newZoom) = MapProjection.fitBoundsZoom(
                minLat = currentMapPoint.latitude,
                maxLat = currentMapPoint.latitude,
                minLon = currentMapPoint.longitude,
                maxLon = currentMapPoint.longitude,
                viewportWidthPx = viewportSize.width.toDouble(),
                viewportHeightPx = viewportSize.height.toDouble(),
                padLeft = padX,
                padTop = padTop,
                padRight = padX,
                padBottom = padBottom,
                tileSizePx = tileSizePx,
                maxZoom = min(17.0, maxZoom),
                currentZoom = targetZoom
            )
            centerWgs = mapSource.fromMapCoord(centerMap)
            zoom = newZoom
            tracker.hasFitted = true
        } else if (fitRoute) {
            var minLat = 90.0
            var maxLat = -90.0
            var minLon = 180.0
            var maxLon = -180.0
            var anyBound = false

            fun includePoint(lat: Double, lon: Double) {
                anyBound = true
                if (lat < minLat) minLat = lat
                if (lat > maxLat) maxLat = lat
                if (lon < minLon) minLon = lon
                if (lon > maxLon) maxLon = lon
            }

            fun includeCircle(lat: Double, lon: Double, radiusMeters: Int) {
                if (radiusMeters <= 0) {
                    includePoint(lat, lon)
                    return
                }
                val dLat = Math.toDegrees(radiusMeters / 6_378_137.0)
                val cosLat = cos(Math.toRadians(lat.coerceIn(-80.0, 80.0))).coerceAtLeast(1e-4)
                val dLon = Math.toDegrees(radiusMeters / (6_378_137.0 * cosLat))
                includePoint(lat - dLat, lon - dLon)
                includePoint(lat + dLat, lon + dLon)
            }

            mapPoints.forEach { includePoint(it.latitude, it.longitude) }
            mapMarkers.forEach { includePoint(it.mapCoord.latitude, it.mapCoord.longitude) }
            mapPolygon.forEach { includePoint(it.latitude, it.longitude) }
            if (normalizedRadius > 0 && currentMapPoint != null) {
                includeCircle(currentMapPoint.latitude, currentMapPoint.longitude, normalizedRadius)
            }
            mapFences.forEach { fence ->
                if (fence.isPolygon) {
                    fence.verticesMap.forEach { includePoint(it.latitude, it.longitude) }
                } else if (fence.centerMap != null && fence.radiusMeters > 0) {
                    includeCircle(fence.centerMap.latitude, fence.centerMap.longitude, fence.radiusMeters)
                }
            }

            if (anyBound) {
                val (centerMap, newZoom) = MapProjection.fitBoundsZoom(
                    minLat = minLat,
                    maxLat = maxLat,
                    minLon = minLon,
                    maxLon = maxLon,
                    viewportWidthPx = viewportSize.width.toDouble(),
                    viewportHeightPx = viewportSize.height.toDouble(),
                    padLeft = padX,
                    padTop = padTop,
                    padRight = padX,
                    padBottom = padBottom,
                    tileSizePx = tileSizePx,
                    maxZoom = min(17.0, maxZoom),
                    currentZoom = zoom
                )
                centerWgs = mapSource.fromMapCoord(centerMap)
                zoom = newZoom
                tracker.hasFitted = true
            }
        } else if (currentMapPoint != null && followMarker) {
            val camMap = mapSource.toMapCoord(centerWgs)
            val (camWx, camWy) = MapProjection.latLonToWorldPx(camMap.latitude, camMap.longitude, zoom, tileSizePx)
            val (ptWx, ptWy) = MapProjection.latLonToWorldPx(
                currentMapPoint.latitude,
                currentMapPoint.longitude,
                zoom,
                tileSizePx
            )
            val sx = viewportSize.width / 2.0 + (ptWx - camWx)
            val sy = viewportSize.height / 2.0 + (ptWy - camWy)
            val marginX = viewportSize.width * 0.1
            val marginY = viewportSize.height * 0.1
            if (sx < marginX || sx > viewportSize.width - marginX ||
                sy < marginY || sy > viewportSize.height - marginY
            ) {
                centerWgs = mapSource.fromMapCoord(currentMapPoint)
            }
        }
    }

    // Load device marker icons if provided
    val iconUrls = remember(markerIconUrl, validMarkers) {
        buildList {
            if (markerIconUrl.isNotBlank()) add(markerIconUrl)
            validMarkers.forEach { if (it.iconUrl.isNotBlank()) add(it.iconUrl) }
        }.distinct()
    }
    LaunchedEffect(iconUrls) {
        iconUrls.forEach { url ->
            if (MapTileRepository.getCachedIcon(url) == null) {
                launch {
                    val bmp = MapTileRepository.loadIcon(context, url)
                    if (bmp != null) iconRevision++
                }
            }
        }
    }

    // Compute visible slippy tile range
    val tileZ = floor(zoom).toInt().coerceIn(2, mapSource.maxZoom)
    val nTiles = 1 shl tileZ
    val worldSpanAtTileZ = tileSizePx * 2.0.pow(zoom - tileZ)
    val camMapNow = mapSource.toMapCoord(centerWgs)
    val (camWxNow, camWyNow) = MapProjection.latLonToWorldPx(
        camMapNow.latitude,
        camMapNow.longitude,
        zoom,
        tileSizePx
    )
    val vpLeftWx = camWxNow - viewportSize.width / 2.0
    val vpTopWy = camWyNow - viewportSize.height / 2.0
    val vpRightWx = camWxNow + viewportSize.width / 2.0
    val vpBottomWy = camWyNow + viewportSize.height / 2.0

    val minTileX = if (viewportSize.width > 0) floor(vpLeftWx / worldSpanAtTileZ).toInt() - 1 else 0
    val maxTileX = if (viewportSize.width > 0) ceil(vpRightWx / worldSpanAtTileZ).toInt() + 1 else -1
    val minTileY = if (viewportSize.height > 0) {
        (floor(vpTopWy / worldSpanAtTileZ).toInt() - 1).coerceAtLeast(0)
    } else 0
    val maxTileY = if (viewportSize.height > 0) {
        (ceil(vpBottomWy / worldSpanAtTileZ).toInt() + 1).coerceAtMost(nTiles - 1)
    } else -1

    // Fetch missing visible tiles concurrently
    LaunchedEffect(
        mapSource,
        satellite,
        traffic,
        tileZ,
        minTileX,
        maxTileX,
        minTileY,
        maxTileY,
        retryNonce
    ) {
        if (maxTileX < minTileX || maxTileY < minTileY) return@LaunchedEffect
        val activeLayers = layerSpecs.filter { tileZ in it.minZoom..it.maxZoom }
        for (ty in minTileY..maxTileY) {
            for (rawTx in minTileX..maxTileX) {
                val tx = Math.floorMod(rawTx, nTiles)
                for (layer in activeLayers) {
                    val key = MapTileRepository.tileCacheKey(layer.id, tileZ, tx, ty)
                    val cached = MapTileRepository.getCachedTile(key)
                    if (cached != null) {
                        if (!layer.isOptionalOverlay) loadedBaseTiles++
                    } else {
                        launch {
                            when (MapTileRepository.loadTile(context, mapSource, layer, tileZ, tx, ty)) {
                                is TileFetchResult.Ready -> {
                                    if (!layer.isOptionalOverlay) loadedBaseTiles++
                                    tileRevision++
                                }
                                is TileFetchResult.Failed -> {
                                    if (!layer.isOptionalOverlay) failedBaseTiles++
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val latestState = remember {
        object {
            var centerWgs = CoordinateTransform.LatLng(35.86, 104.19)
            var zoom = 5.0
            var mapSource = MapSource.AMAP
            var markers = emptyList<RenderedDeviceMarker>()
            var onMapTap: ((CoordinateTransform.LatLng) -> Unit)? = null
            var onDeviceTap: ((String) -> Unit)? = null
        }
    }
    latestState.centerWgs = centerWgs
    latestState.zoom = zoom
    latestState.mapSource = mapSource
    latestState.markers = mapMarkers
    latestState.onMapTap = onMapTap
    latestState.onDeviceTap = onDeviceTap

    val bgColor = if (darkTheme) Color(0xFF16181C) else Color(0xFFF1F4F6)
    val gridColor = if (darkTheme) Color(0xFF23272F) else Color(0xFFE3E8EC)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .clipToBounds()
            .onSizeChanged { viewportSize = it }
            .pointerInput(tileSizePx) {
                val touchSlop = viewConfig.touchSlop
                val markerHitRadiusPx = 28.dp.toPx()
                val doubleTapSlopPx = 48.dp.toPx()
                var lastTapTime = 0L
                var lastTapPos = Offset.Zero

                awaitEachGesture {
                    val firstDown = awaitFirstDown(requireUnconsumed = false)
                    val downPos = firstDown.position
                    var pastTouchSlop = false
                    var isMultiTouch = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.count { it.pressed }
                        if (pressed >= 2) isMultiTouch = true

                        val zoomFactor = event.calculateZoom()
                        val pan = event.calculatePan()
                        val centroid = event.calculateCentroid(useCurrent = true)

                        val movedDist = (event.changes.firstOrNull()?.position?.let {
                            hypot((it.x - downPos.x).toDouble(), (it.y - downPos.y).toDouble()).toFloat()
                        } ?: 0f)
                        if (!pastTouchSlop && (isMultiTouch || abs(zoomFactor - 1f) > 0.01f || movedDist > touchSlop)) {
                            pastTouchSlop = true
                        }

                        if (pastTouchSlop && (zoomFactor != 1f || pan != Offset.Zero)) {
                            val src = latestState.mapSource
                            val oldZoom = latestState.zoom
                            val newZoom = (oldZoom + ln(zoomFactor.toDouble().coerceAtLeast(0.1)) / ln(2.0))
                                .coerceIn(2.0, src.maxZoom.toDouble())
                            val curCenterMap = src.toMapCoord(latestState.centerWgs)
                            val (camWxOld, camWyOld) = MapProjection.latLonToWorldPx(
                                curCenterMap.latitude,
                                curCenterMap.longitude,
                                oldZoom,
                                tileSizePx
                            )
                            val vpW = viewportSize.width.toDouble()
                            val vpH = viewportSize.height.toDouble()

                            // Keep the world coordinate under the pinch centroid anchored, then apply pan
                            val anchorOffsetX = if (centroid != Offset.Unspecified) centroid.x - vpW / 2.0 else 0.0
                            val anchorOffsetY = if (centroid != Offset.Unspecified) centroid.y - vpH / 2.0 else 0.0
                            val scaleRatio = 2.0.pow(newZoom - oldZoom)
                            val anchorWxNew = (camWxOld + anchorOffsetX) * scaleRatio
                            val anchorWyNew = (camWyOld + anchorOffsetY) * scaleRatio
                            val newCamWx = anchorWxNew - anchorOffsetX - pan.x.toDouble()
                            val newCamWy = anchorWyNew - anchorOffsetY - pan.y.toDouble()

                            val nextMapCenter = MapProjection.worldPxToLatLon(newCamWx, newCamWy, newZoom, tileSizePx)
                            val nextWgsCenter = src.fromMapCoord(nextMapCenter)
                            latestState.centerWgs = nextWgsCenter
                            latestState.zoom = newZoom
                            centerWgs = nextWgsCenter
                            zoom = newZoom

                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }

                        if (pressed == 0) {
                            if (!pastTouchSlop && !isMultiTouch) {
                                val upPos = event.changes.firstOrNull()?.position ?: downPos
                                val src = latestState.mapSource
                                val curZoom = latestState.zoom
                                val curCenterMap = src.toMapCoord(latestState.centerWgs)
                                val (camWx, camWy) = MapProjection.latLonToWorldPx(
                                    curCenterMap.latitude,
                                    curCenterMap.longitude,
                                    curZoom,
                                    tileSizePx
                                )

                                // Check marker tap first (in reverse draw order)
                                val tappedMarker = latestState.markers.asReversed().firstOrNull { marker ->
                                    val (mx, my) = MapProjection.latLonToWorldPx(
                                        marker.mapCoord.latitude,
                                        marker.mapCoord.longitude,
                                        curZoom,
                                        tileSizePx
                                    )
                                    val sx = (viewportSize.width / 2.0 + (mx - camWx)).toFloat()
                                    val sy = (viewportSize.height / 2.0 + (my - camWy)).toFloat()
                                    hypot((upPos.x - sx).toDouble(), (upPos.y - sy).toDouble()) <= markerHitRadiusPx
                                }

                                if (tappedMarker != null && latestState.onDeviceTap != null) {
                                    lastTapTime = 0L
                                    latestState.onDeviceTap?.invoke(tappedMarker.teid)
                                } else {
                                    val mapTapCb = latestState.onMapTap
                                    val now = System.currentTimeMillis()
                                    val isDoubleTap = mapTapCb == null &&
                                        (now - lastTapTime <= 280L) &&
                                        hypot((upPos.x - lastTapPos.x).toDouble(), (upPos.y - lastTapPos.y).toDouble()) <= doubleTapSlopPx

                                    if (isDoubleTap) {
                                        lastTapTime = 0L
                                        val nextZoom = (curZoom + 1.0).coerceAtMost(src.maxZoom.toDouble())
                                        if (nextZoom > curZoom) {
                                            val tapOffsetX = upPos.x - viewportSize.width / 2.0
                                            val tapOffsetY = upPos.y - viewportSize.height / 2.0
                                            val scale = 2.0.pow(nextZoom - curZoom)
                                            val nextCamWx = (camWx + tapOffsetX) * scale - tapOffsetX
                                            val nextCamWy = (camWy + tapOffsetY) * scale - tapOffsetY
                                            val nextWgs = src.fromMapCoord(
                                                MapProjection.worldPxToLatLon(nextCamWx, nextCamWy, nextZoom, tileSizePx)
                                            )
                                            centerWgs = nextWgs
                                            zoom = nextZoom
                                        }
                                    } else {
                                        lastTapTime = now
                                        lastTapPos = upPos
                                        if (mapTapCb != null) {
                                            val tapWx = camWx + (upPos.x - viewportSize.width / 2.0)
                                            val tapWy = camWy + (upPos.y - viewportSize.height / 2.0)
                                            val tapMapCoord = MapProjection.worldPxToLatLon(tapWx, tapWy, curZoom, tileSizePx)
                                            val tapWgs = src.fromMapCoord(tapMapCoord)
                                            if (isValidMapCoordinate(tapWgs.latitude, tapWgs.longitude)) {
                                                mapTapCb.invoke(tapWgs)
                                            }
                                        }
                                    }
                                }
                            }
                            break
                        }
                    }
                }
            }
    ) {
        val statusOnlineText = markerStatus.ifBlank { if (markerOnline) "online" else "offline" }

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Read invalidation counters so tile/icon completions redraw the canvas immediately
            val tileTick = tileRevision
            val iconTick = iconRevision
            if (tileTick < 0 || iconTick < 0) return@Canvas
            if (size.width <= 0f || size.height <= 0f) return@Canvas

            val camMap = mapSource.toMapCoord(centerWgs)
            val (camWx, camWy) = MapProjection.latLonToWorldPx(
                camMap.latitude,
                camMap.longitude,
                zoom,
                tileSizePx
            )

            fun toScreen(lat: Double, lon: Double): Offset {
                val (wx, wy) = MapProjection.latLonToWorldPx(lat, lon, zoom, tileSizePx)
                return Offset(
                    x = (size.width / 2.0 + (wx - camWx)).toFloat(),
                    y = (size.height / 2.0 + (wy - camWy)).toFloat()
                )
            }

            // 1. Subtle placeholder grid lines while tiles load
            val tileSpan = tileSizePx * 2.0.pow(zoom - tileZ)
            val offsetX = size.width / 2.0 - camWx
            val offsetY = size.height / 2.0 - camWy
            val gxStart = floor((-offsetX) / tileSpan).toInt()
            val gxEnd = ceil((size.width - offsetX) / tileSpan).toInt()
            val gyStart = max(0, floor((-offsetY) / tileSpan).toInt())
            val gyEnd = min(nTiles, ceil((size.height - offsetY) / tileSpan).toInt())

            for (gx in gxStart..gxEnd) {
                val sx = (offsetX + gx * tileSpan).toFloat()
                drawLine(gridColor, Offset(sx, 0f), Offset(sx, size.height), strokeWidth = 1f)
            }
            for (gy in gyStart..gyEnd) {
                val sy = (offsetY + gy * tileSpan).toFloat()
                drawLine(gridColor, Offset(0f, sy), Offset(size.width, sy), strokeWidth = 1f)
            }

            // 2. Render tile layers (base with parent-tile fallback, then optional overlays)
            val activeLayers = layerSpecs.filter { tileZ in it.minZoom..it.maxZoom }
            for (layer in activeLayers) {
                for (ty in minTileY..maxTileY) {
                    val dstTop = (offsetY + ty * tileSpan).roundToInt()
                    val dstBottom = (offsetY + (ty + 1) * tileSpan).roundToInt()
                    val dstH = (dstBottom - dstTop).coerceAtLeast(1)
                    if (dstBottom <= 0 || dstTop >= size.height) continue

                    for (rawTx in minTileX..maxTileX) {
                        val dstLeft = (offsetX + rawTx * tileSpan).roundToInt()
                        val dstRight = (offsetX + (rawTx + 1) * tileSpan).roundToInt()
                        val dstW = (dstRight - dstLeft).coerceAtLeast(1)
                        if (dstRight <= 0 || dstLeft >= size.width) continue

                        val tx = Math.floorMod(rawTx, nTiles)
                        val key = MapTileRepository.tileCacheKey(layer.id, tileZ, tx, ty)
                        val bmp = MapTileRepository.getCachedTile(key)
                        if (bmp != null) {
                            drawImage(
                                image = bmp,
                                dstOffset = IntOffset(dstLeft, dstTop),
                                dstSize = IntSize(dstW, dstH)
                            )
                        } else if (!layer.isOptionalOverlay && tileZ > 2) {
                            // Parent tile quadrant fallback so zooming/panning never flashes blank
                            val pKey = MapTileRepository.tileCacheKey(layer.id, tileZ - 1, tx shr 1, ty shr 1)
                            val pBmp = MapTileRepository.getCachedTile(pKey)
                            if (pBmp != null) {
                                val halfW = pBmp.width / 2
                                val halfH = pBmp.height / 2
                                val srcX = if ((tx and 1) != 0) halfW else 0
                                val srcY = if ((ty and 1) != 0) halfH else 0
                                drawImage(
                                    image = pBmp,
                                    srcOffset = IntOffset(srcX, srcY),
                                    srcSize = IntSize(halfW.coerceAtLeast(1), halfH.coerceAtLeast(1)),
                                    dstOffset = IntOffset(dstLeft, dstTop),
                                    dstSize = IntSize(dstW, dstH)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Render saved geofences
            val fenceStroke = 2.dp.toPx()
            val fenceColor = Color(0xFF007DFF)
            for (fence in mapFences) {
                var labelPos: Offset? = null
                if (fence.isPolygon && fence.verticesMap.size >= 3) {
                    val pts = fence.verticesMap.map { toScreen(it.latitude, it.longitude) }
                    val path = Path().apply {
                        moveTo(pts[0].x, pts[0].y)
                        for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
                        close()
                    }
                    drawPath(path, fenceColor.copy(alpha = 0.10f), style = Fill)
                    drawPath(path, fenceColor.copy(alpha = 0.85f), style = Stroke(width = fenceStroke))
                    labelPos = Offset(
                        pts.sumOf { it.x.toDouble() }.toFloat() / pts.size,
                        pts.sumOf { it.y.toDouble() }.toFloat() / pts.size
                    )
                } else if (!fence.isPolygon && fence.centerMap != null && fence.radiusMeters > 0) {
                    val centerScreen = toScreen(fence.centerMap.latitude, fence.centerMap.longitude)
                    val radiusPx = MapProjection.metersToPixels(
                        fence.radiusMeters.toDouble(),
                        fence.centerMap.latitude,
                        zoom,
                        tileSizePx
                    ).toFloat().coerceAtLeast(4f)
                    drawCircle(fenceColor.copy(alpha = 0.10f), radius = radiusPx, center = centerScreen)
                    drawCircle(
                        fenceColor.copy(alpha = 0.85f),
                        radius = radiusPx,
                        center = centerScreen,
                        style = Stroke(width = fenceStroke)
                    )
                    labelPos = centerScreen
                }
                if (labelPos != null && fence.name.isNotBlank()) {
                    drawPillLabel(
                        textMeasurer = textMeasurer,
                        text = fence.name,
                        anchorCenter = labelPos,
                        backgroundColor = Color.White.copy(alpha = 0.92f),
                        textColor = Color(0xFF0055B3),
                        borderColor = fenceColor.copy(alpha = 0.45f)
                    )
                }
            }

            // 4. Render active geofence circle & polygon editor overlay
            val activeFenceColor = Color(0xFFE5484D)
            if (normalizedRadius > 0 && currentMapPoint != null) {
                val centerScreen = toScreen(currentMapPoint.latitude, currentMapPoint.longitude)
                val radiusPx = MapProjection.metersToPixels(
                    normalizedRadius.toDouble(),
                    currentMapPoint.latitude,
                    zoom,
                    tileSizePx
                ).toFloat().coerceAtLeast(4f)
                drawCircle(activeFenceColor.copy(alpha = 0.12f), radius = radiusPx, center = centerScreen)
                drawCircle(
                    activeFenceColor.copy(alpha = 0.9f),
                    radius = radiusPx,
                    center = centerScreen,
                    style = Stroke(width = fenceStroke)
                )
            }

            if (mapPolygon.size >= 3) {
                val pts = mapPolygon.map { toScreen(it.latitude, it.longitude) }
                val path = Path().apply {
                    moveTo(pts[0].x, pts[0].y)
                    for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
                    close()
                }
                drawPath(path, activeFenceColor.copy(alpha = 0.12f), style = Fill)
                drawPath(path, activeFenceColor.copy(alpha = 0.9f), style = Stroke(width = fenceStroke))
            } else if (mapPolygon.size == 2) {
                val p0 = toScreen(mapPolygon[0].latitude, mapPolygon[0].longitude)
                val p1 = toScreen(mapPolygon[1].latitude, mapPolygon[1].longitude)
                drawLine(
                    color = activeFenceColor.copy(alpha = 0.9f),
                    start = p0,
                    end = p1,
                    strokeWidth = fenceStroke,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
                )
            }
            for (v in mapPolygon) {
                val pt = toScreen(v.latitude, v.longitude)
                drawCircle(Color.White, radius = 6.dp.toPx(), center = pt)
                drawCircle(activeFenceColor, radius = 6.dp.toPx(), center = pt, style = Stroke(width = 2.dp.toPx()))
            }

            // 5. Render route polylines + Start/End badges
            if (mapPoints.size > 1) {
                val routeStroke = 5.dp.toPx()
                val screenPts = mapPoints.map { toScreen(it.latitude, it.longitude) }
                var runColor = speedColor(routeSpeeds.getOrNull(0) ?: 0)
                var runPath = Path().apply {
                    moveTo(screenPts[0].x, screenPts[0].y)
                    lineTo(screenPts[1].x, screenPts[1].y)
                }
                for (i in 2 until screenPts.size) {
                    val segColor = speedColor(routeSpeeds.getOrNull(i - 1) ?: 0)
                    if (segColor == runColor) {
                        runPath.lineTo(screenPts[i].x, screenPts[i].y)
                    } else {
                        drawPath(
                            path = runPath,
                            color = runColor.copy(alpha = 0.92f),
                            style = Stroke(width = routeStroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        runColor = segColor
                        runPath = Path().apply {
                            moveTo(screenPts[i - 1].x, screenPts[i - 1].y)
                            lineTo(screenPts[i].x, screenPts[i].y)
                        }
                    }
                }
                drawPath(
                    path = runPath,
                    color = runColor.copy(alpha = 0.92f),
                    style = Stroke(width = routeStroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                drawPillLabel(
                    textMeasurer = textMeasurer,
                    text = startLabel,
                    anchorCenter = Offset(screenPts.first().x, screenPts.first().y - 18.dp.toPx()),
                    backgroundColor = Color(0xFF19BE6B),
                    textColor = Color.White,
                    borderColor = null
                )
                drawPillLabel(
                    textMeasurer = textMeasurer,
                    text = endLabel,
                    anchorCenter = Offset(screenPts.last().x, screenPts.last().y - 18.dp.toPx()),
                    backgroundColor = Color(0xFFED4014),
                    textColor = Color.White,
                    borderColor = null
                )
            }

            // 6. Render secondary device markers
            for (device in mapMarkers) {
                val pos = toScreen(device.mapCoord.latitude, device.mapCoord.longitude)
                if (pos.x < -160f || pos.x > size.width + 160f || pos.y < -80f || pos.y > size.height + 80f) continue
                drawDeviceMarker(
                    center = pos,
                    name = device.name,
                    direction = device.direction,
                    status = device.status,
                    iconBitmap = MapTileRepository.getCachedIcon(device.iconUrl),
                    textMeasurer = textMeasurer
                )
            }

            // 7. Render primary device marker on top
            if (currentMapPoint != null) {
                val pos = toScreen(currentMapPoint.latitude, currentMapPoint.longitude)
                drawDeviceMarker(
                    center = pos,
                    name = markerName,
                    direction = markerDirection,
                    status = statusOnlineText,
                    iconBitmap = MapTileRepository.getCachedIcon(markerIconUrl),
                    textMeasurer = textMeasurer
                )
            }
        }

        if (failedBaseTiles >= 4 && loadedBaseTiles == 0) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = controlsTopPx.dp, start = 16.dp, end = 16.dp)
                    .widthIn(max = 360.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                shadowElevation = 4.dp,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = errorLabel,
                        color = Color(0xFFA52332),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            MapTileRepository.clearFailedTiles()
                            failedBaseTiles = 0
                            loadedBaseTiles = 0
                            retryNonce++
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = HyperBlue)
                    ) {
                        Text(retryLabel, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private fun speedColor(speed: Int): Color = when {
    speed < 20 -> Color(0xFF2D8CF0)
    speed < 40 -> Color(0xFF5CADFF)
    speed < 60 -> Color(0xFF19BE6B)
    speed < 80 -> Color(0xFF99CC33)
    speed < 100 -> Color(0xFFFF9900)
    speed < 120 -> Color(0xFFED4014)
    else -> Color(0xFF990033)
}

private fun statusMarkerColor(status: String): Color = when (status) {
    "offline" -> Color(0xFF8B949E)
    "moving" -> Color(0xFF1677FF)
    "alarm" -> Color(0xFFDB3F44)
    "sleep" -> Color(0xFFD48D18)
    else -> Color(0xFF148047)
}

private fun DrawScope.drawDeviceMarker(
    center: Offset,
    name: String,
    direction: Int,
    status: String,
    iconBitmap: ImageBitmap?,
    textMeasurer: TextMeasurer
) {
    val radius = 14.dp.toPx()
    val borderWidth = 2.dp.toPx()

    // Soft drop shadow
    drawCircle(
        color = Color.Black.copy(alpha = 0.25f),
        radius = radius + 1.5.dp.toPx(),
        center = Offset(center.x, center.y + 1.5.dp.toPx())
    )

    // Status circle + white border
    drawCircle(color = statusMarkerColor(status), radius = radius, center = center)
    drawCircle(color = Color.White, radius = radius, center = center, style = Stroke(width = borderWidth))

    // Rotated device icon or directional arrow
    rotate(degrees = direction.toFloat(), pivot = center) {
        if (iconBitmap != null) {
            val iconSizePx = 18.dp.toPx().roundToInt().coerceAtLeast(1)
            val clip = Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        center.x - iconSizePx / 2f,
                        center.y - iconSizePx / 2f,
                        center.x + iconSizePx / 2f,
                        center.y + iconSizePx / 2f
                    )
                )
            }
            clipPath(clip) {
                drawImage(
                    image = iconBitmap,
                    dstOffset = IntOffset(
                        (center.x - iconSizePx / 2f).roundToInt(),
                        (center.y - iconSizePx / 2f).roundToInt()
                    ),
                    dstSize = IntSize(iconSizePx, iconSizePx)
                )
            }
        } else {
            val tipY = center.y - 6.dp.toPx()
            val baseY = center.y + 4.5.dp.toPx()
            val halfW = 5.5.dp.toPx()
            val arrowPath = Path().apply {
                moveTo(center.x, tipY)
                lineTo(center.x + halfW, baseY)
                lineTo(center.x, center.y + 2.dp.toPx())
                lineTo(center.x - halfW, baseY)
                close()
            }
            drawPath(arrowPath, Color.White, style = Fill)
        }
    }

    if (name.isNotBlank()) {
        val style = TextStyle(
            color = Color(0xFF202B36),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        val layout = textMeasurer.measure(
            text = name,
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            constraints = Constraints(maxWidth = 120.dp.toPx().roundToInt())
        )
        val padH = 6.dp.toPx()
        val padV = 2.dp.toPx()
        val pillLeft = center.x + radius + 4.dp.toPx()
        val pillW = layout.size.width + padH * 2f
        val pillH = layout.size.height + padV * 2f
        val pillTop = center.y - pillH / 2f
        val cr = CornerRadius(4.dp.toPx(), 4.dp.toPx())

        drawRoundRect(
            color = Color.Black.copy(alpha = 0.14f),
            topLeft = Offset(pillLeft, pillTop + 1.dp.toPx()),
            size = Size(pillW, pillH),
            cornerRadius = cr
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.95f),
            topLeft = Offset(pillLeft, pillTop),
            size = Size(pillW, pillH),
            cornerRadius = cr
        )
        drawRoundRect(
            color = Color(0xFFD8DFE3),
            topLeft = Offset(pillLeft, pillTop),
            size = Size(pillW, pillH),
            cornerRadius = cr,
            style = Stroke(width = 1.dp.toPx())
        )
        drawText(
            textLayoutResult = layout,
            topLeft = Offset(pillLeft + padH, pillTop + padV)
        )
    }
}

private fun DrawScope.drawPillLabel(
    textMeasurer: TextMeasurer,
    text: String,
    anchorCenter: Offset,
    backgroundColor: Color,
    textColor: Color,
    borderColor: Color?
) {
    val style = TextStyle(
        color = textColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold
    )
    val layout = textMeasurer.measure(
        text = text,
        style = style,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        constraints = Constraints(maxWidth = 140.dp.toPx().roundToInt())
    )
    val padH = 7.dp.toPx()
    val padV = 2.dp.toPx()
    val w = layout.size.width + padH * 2f
    val h = layout.size.height + padV * 2f
    val left = anchorCenter.x - w / 2f
    val top = anchorCenter.y - h / 2f
    val cr = CornerRadius(10.dp.toPx(), 10.dp.toPx())

    drawRoundRect(
        color = Color.Black.copy(alpha = 0.18f),
        topLeft = Offset(left, top + 1.dp.toPx()),
        size = Size(w, h),
        cornerRadius = cr
    )
    drawRoundRect(
        color = backgroundColor,
        topLeft = Offset(left, top),
        size = Size(w, h),
        cornerRadius = cr
    )
    if (borderColor != null) {
        drawRoundRect(
            color = borderColor,
            topLeft = Offset(left, top),
            size = Size(w, h),
            cornerRadius = cr,
            style = Stroke(width = 1.dp.toPx())
        )
    }
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(left + padH, top + padV)
    )
}
