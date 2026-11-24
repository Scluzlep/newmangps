package com.scluzlep.newmangps.ui.screens.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.scluzlep.newmangps.BuildConfig
import com.scluzlep.newmangps.data.MapSource
import com.scluzlep.newmangps.data.util.CoordinateTransform
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import okhttp3.Cache
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URLEncoder
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh

object MapProjection {
    const val MIN_LAT = -85.05112878
    const val MAX_LAT = 85.05112878
    const val BASE_TILE_DP = 256.0

    fun latLonToWorldPx(
        lat: Double,
        lon: Double,
        zoom: Double,
        tileSizePx: Double = BASE_TILE_DP
    ): Pair<Double, Double> {
        val worldSize = tileSizePx * 2.0.pow(zoom)
        val clampedLat = lat.coerceIn(MIN_LAT, MAX_LAT)
        val clampedLon = lon.coerceIn(-180.0, 180.0)
        val x = ((clampedLon + 180.0) / 360.0) * worldSize
        val sinLat = sin(Math.toRadians(clampedLat)).coerceIn(-0.999999, 0.999999)
        val y = (0.5 - ln((1.0 + sinLat) / (1.0 - sinLat)) / (4.0 * PI)) * worldSize
        return x to y
    }

    fun worldPxToLatLon(
        wx: Double,
        wy: Double,
        zoom: Double,
        tileSizePx: Double = BASE_TILE_DP
    ): CoordinateTransform.LatLng {
        val worldSize = tileSizePx * 2.0.pow(zoom)
        val rawLon = (wx / worldSize) * 360.0 - 180.0
        val wrappedLon = if (rawLon in -180.0..180.0) {
            rawLon
        } else {
            (((rawLon + 180.0) % 360.0) + 360.0) % 360.0 - 180.0
        }
        val clampedWy = wy.coerceIn(0.0, worldSize)
        val n = PI - (2.0 * PI * clampedWy) / worldSize
        val lat = Math.toDegrees(atan(sinh(n))).coerceIn(-85.0, 85.0)
        return CoordinateTransform.LatLng(lat, wrappedLon)
    }

    fun metersToPixels(
        meters: Double,
        lat: Double,
        zoom: Double,
        tileSizePx: Double = BASE_TILE_DP
    ): Double {
        if (meters <= 0.0 || !meters.isFinite()) return 0.0
        val cosLat = cos(Math.toRadians(lat.coerceIn(MIN_LAT, MAX_LAT))).coerceAtLeast(1e-6)
        val metersPer256Px = 156543.03392 * cosLat / 2.0.pow(zoom)
        return (meters / metersPer256Px.coerceAtLeast(1e-6)) * (tileSizePx / BASE_TILE_DP)
    }

    fun fitBoundsZoom(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        viewportWidthPx: Double,
        viewportHeightPx: Double,
        padLeft: Double = 0.0,
        padTop: Double = 0.0,
        padRight: Double = 0.0,
        padBottom: Double = 0.0,
        tileSizePx: Double = BASE_TILE_DP,
        minZoom: Double = 2.0,
        maxZoom: Double = 17.0,
        currentZoom: Double = 15.0
    ): Pair<CoordinateTransform.LatLng, Double> {
        val clampedMinLat = minLat.coerceIn(MIN_LAT, MAX_LAT)
        val clampedMaxLat = maxLat.coerceIn(MIN_LAT, MAX_LAT)
        val clampedMinLon = minLon.coerceIn(-180.0, 180.0)
        val clampedMaxLon = maxLon.coerceIn(-180.0, 180.0)
        val isZeroArea = abs(clampedMaxLat - clampedMinLat) < 1e-7 &&
            abs(clampedMaxLon - clampedMinLon) < 1e-7

        val zoom = if (isZeroArea) {
            max(currentZoom, 15.0).coerceIn(minZoom, maxZoom)
        } else {
            val availW = (viewportWidthPx - padLeft - padRight).coerceAtLeast(48.0)
            val availH = (viewportHeightPx - padTop - padBottom).coerceAtLeast(48.0)
            val (nwX, nwY) = latLonToWorldPx(clampedMaxLat, clampedMinLon, 0.0, tileSizePx)
            val (seX, seY) = latLonToWorldPx(clampedMinLat, clampedMaxLon, 0.0, tileSizePx)
            val spanX = abs(seX - nwX).coerceAtLeast(1e-6)
            val spanY = abs(seY - nwY).coerceAtLeast(1e-6)
            val zoomX = ln(availW / spanX) / ln(2.0)
            val zoomY = ln(availH / spanY) / ln(2.0)
            min(zoomX, zoomY).coerceIn(minZoom, maxZoom)
        }

        val midLat = (clampedMinLat + clampedMaxLat) / 2.0
        val midLon = (clampedMinLon + clampedMaxLon) / 2.0
        val (midWx, midWy) = latLonToWorldPx(midLat, midLon, zoom, tileSizePx)
        // Offset world center so the target bounds sit centered inside the padded viewport area
        val offsetX = (padLeft - padRight) / 2.0
        val offsetY = (padTop - padBottom) / 2.0
        val center = worldPxToLatLon(midWx - offsetX, midWy - offsetY, zoom, tileSizePx)
        return center to zoom
    }
}

data class MapTileLayerSpec(
    val id: String,
    val isOptionalOverlay: Boolean,
    val minZoom: Int = 2,
    val maxZoom: Int = 18,
    val urlFor: (z: Int, x: Int, y: Int) -> String
)

internal fun bingQuadKey(z: Int, x: Int, y: Int): String {
    val sb = StringBuilder(z.coerceAtLeast(0))
    for (i in z downTo 1) {
        var digit = 0
        val mask = 1 shl (i - 1)
        if ((x and mask) != 0) digit += 1
        if ((y and mask) != 0) digit += 2
        sb.append(digit)
    }
    return sb.toString()
}

internal fun tmsY(z: Int, y: Int): Int = (1 shl z) - 1 - y

fun buildTileLayerSpecs(
    source: MapSource,
    satellite: Boolean,
    traffic: Boolean,
    tianmapKey: String = BuildConfig.TIANMAP_KEY
): List<MapTileLayerSpec> = when (source) {
    MapSource.AMAP -> buildList {
        if (satellite) {
            add(
                MapTileLayerSpec(id = "amap-sat", isOptionalOverlay = false, maxZoom = 18) { z, x, y ->
                    val sub = Math.floorMod(x + y, 4) + 1
                    "https://webst0$sub.is.autonavi.com/appmaptile?style=6&x=$x&y=$y&z=$z"
                }
            )
            add(
                MapTileLayerSpec(id = "amap-sat-label", isOptionalOverlay = true, maxZoom = 18) { z, x, y ->
                    val sub = Math.floorMod(x + y, 4) + 1
                    "https://wprd0$sub.is.autonavi.com/appmaptile?lang=zh_cn&size=1&style=8&x=$x&y=$y&z=$z&scl=1"
                }
            )
        } else {
            add(
                MapTileLayerSpec(id = "amap-vec", isOptionalOverlay = false, maxZoom = 18) { z, x, y ->
                    val sub = Math.floorMod(x + y, 4) + 1
                    "https://webrd0$sub.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x=$x&y=$y&z=$z"
                }
            )
        }
        if (traffic) {
            add(
                MapTileLayerSpec(
                    id = "amap-traffic",
                    isOptionalOverlay = true,
                    minZoom = 7,
                    maxZoom = 18
                ) { z, x, y ->
                    "https://tm.amap.com/trafficengine/mapabc/traffictile?v=1.0&t=1&x=$x&y=$y&z=$z"
                }
            )
        }
    }

    MapSource.TENCENT -> if (satellite) {
        listOf(
            MapTileLayerSpec(id = "tencent-sat", isOptionalOverlay = false, maxZoom = 18) { z, x, y ->
                val sub = Math.floorMod(x + y, 4)
                val ty = tmsY(z, y)
                "https://p$sub.map.gtimg.com/sateTiles/$z/${x / 16}/${ty / 16}/${x}_${ty}.jpg"
            },
            MapTileLayerSpec(id = "tencent-sat-label", isOptionalOverlay = true, maxZoom = 18) { z, x, y ->
                val sub = Math.floorMod(x + y, 4)
                val ty = tmsY(z, y)
                "https://rt$sub.map.gtimg.com/tile?z=$z&x=$x&y=$ty&styleid=2&version=297"
            }
        )
    } else {
        listOf(
            MapTileLayerSpec(id = "tencent-vec", isOptionalOverlay = false, maxZoom = 18) { z, x, y ->
                val sub = Math.floorMod(x + y, 4)
                val ty = tmsY(z, y)
                "https://rt$sub.map.gtimg.com/realtimerender?z=$z&x=$x&y=$ty&type=vector&style=0"
            }
        )
    }

    MapSource.TIANMAP -> {
        val tk = URLEncoder.encode(tianmapKey, "UTF-8")
        val baseLayer = if (satellite) "img" else "vec"
        val labelLayer = if (satellite) "cia" else "cva"
        listOf(
            MapTileLayerSpec(id = "tianmap-$baseLayer", isOptionalOverlay = false, maxZoom = 18) { z, x, y ->
                val sub = Math.floorMod(x + y, 8)
                "https://t$sub.tianditu.gov.cn/${baseLayer}_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=$baseLayer&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX=$z&TILEROW=$y&TILECOL=$x&tk=$tk"
            },
            MapTileLayerSpec(id = "tianmap-$labelLayer", isOptionalOverlay = true, maxZoom = 18) { z, x, y ->
                val sub = Math.floorMod(x + y, 8)
                "https://t$sub.tianditu.gov.cn/${labelLayer}_w/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=$labelLayer&STYLE=default&TILEMATRIXSET=w&FORMAT=tiles&TILEMATRIX=$z&TILEROW=$y&TILECOL=$x&tk=$tk"
            }
        )
    }

    MapSource.BING -> if (satellite) {
        listOf(
            MapTileLayerSpec(id = "bing-sat", isOptionalOverlay = false, maxZoom = 18) { z, x, y ->
                val sub = Math.floorMod(x + y, 4)
                val quad = bingQuadKey(z, x, y)
                "https://ecn.t$sub.tiles.virtualearth.net/tiles/h$quad.jpeg?g=9675&mkt=zh-cn"
            }
        )
    } else {
        listOf(
            MapTileLayerSpec(id = "bing-vec", isOptionalOverlay = false, maxZoom = 18) { z, x, y ->
                val sub = Math.floorMod(x + y, 4)
                val quad = bingQuadKey(z, x, y)
                "https://r$sub.tiles.ditu.live.com/tiles/r$quad.png?g=100&mkt=zh-cn"
            }
        )
    }

    MapSource.ARCGIS -> if (satellite) {
        listOf(
            MapTileLayerSpec(id = "arcgis-img", isOptionalOverlay = false, maxZoom = 18) { z, x, y ->
                "https://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/$z/$y/$x"
            },
            MapTileLayerSpec(id = "arcgis-ref", isOptionalOverlay = true, maxZoom = 18) { z, x, y ->
                "https://services.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/$z/$y/$x"
            }
        )
    } else {
        listOf(
            MapTileLayerSpec(id = "arcgis-vec", isOptionalOverlay = false, maxZoom = 18) { z, x, y ->
                "https://services.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/$z/$y/$x"
            }
        )
    }

    MapSource.OSM -> listOf(
        MapTileLayerSpec(id = "osm-vec", isOptionalOverlay = false, maxZoom = 19) { z, x, y ->
            "https://tile.openstreetmap.org/$z/$x/$y.png"
        }
    )

    MapSource.GOOGLE -> {
        val lyrs = if (satellite) "y" else "m"
        listOf(
            MapTileLayerSpec(id = "google-$lyrs", isOptionalOverlay = false, maxZoom = 19) { z, x, y ->
                val sub = Math.floorMod(x + y, 4)
                "https://mt$sub.google.com/vt/lyrs=$lyrs&hl=zh-CN&gl=cn&x=$x&y=$y&z=$z"
            }
        )
    }
}

sealed class TileFetchResult {
    data class Ready(val bitmap: ImageBitmap) : TileFetchResult()
    data class Failed(val error: String) : TileFetchResult()
}

object MapTileRepository {
    private const val TAG = "MapTileRepo"
    private const val DISK_CACHE_BYTES = 64L * 1024L * 1024L
    private const val REFERER = "https://app.newsmy-nmgps.com/"
    private const val CHROME_UA =
        "Mozilla/5.0 (Linux; Android 15; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
    private val osmUserAgent = "NewmanGPS/${BuildConfig.VERSION_NAME} (Android)"

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    @Volatile
    private var client: OkHttpClient? = null

    private val tileCacheBytes = (Runtime.getRuntime().maxMemory() / 8L)
        .coerceIn(8L * 1024L * 1024L, 24L * 1024L * 1024L)
        .toInt()

    private val tileCache = object : LruCache<String, ImageBitmap>(tileCacheBytes) {
        override fun sizeOf(key: String, value: ImageBitmap): Int =
            (value.width * value.height * 4).coerceAtLeast(1)
    }

    private val iconCache = LruCache<String, ImageBitmap>(32)
    private val failedKeys = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
    private val inFlight = ConcurrentHashMap<String, Deferred<TileFetchResult>>()
    private val iconInFlight = ConcurrentHashMap<String, Deferred<ImageBitmap?>>()

    fun tileCacheKey(layerId: String, z: Int, x: Int, y: Int): String = "$layerId:$z:$x:$y"

    fun getCachedTile(key: String): ImageBitmap? = tileCache.get(key)

    fun getCachedIcon(url: String): ImageBitmap? =
        if (url.isBlank()) null else iconCache.get(url)

    fun clearFailedTiles() {
        failedKeys.clear()
    }

    private fun ensureClient(context: Context): OkHttpClient {
        client?.let { return it }
        synchronized(lock) {
            client?.let { return it }
            val appCtx = context.applicationContext
            val dispatcher = Dispatcher().apply {
                maxRequests = 24
                maxRequestsPerHost = 8
            }
            val diskCache = runCatching {
                Cache(File(appCtx.cacheDir, "map_tiles"), DISK_CACHE_BYTES)
            }.getOrNull()
            return OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .apply { if (diskCache != null) cache(diskCache) }
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(12, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
                .also { client = it }
        }
    }

    suspend fun loadTile(
        context: Context,
        source: MapSource,
        layer: MapTileLayerSpec,
        z: Int,
        x: Int,
        y: Int
    ): TileFetchResult {
        val key = tileCacheKey(layer.id, z, x, y)
        tileCache.get(key)?.let { return TileFetchResult.Ready(it) }
        if (key in failedKeys) return TileFetchResult.Failed("Previous tile failure")

        val deferred = inFlight.getOrPut(key) {
            repoScope.async {
                try {
                    fetchTileSync(context.applicationContext, source, layer, z, x, y, key)
                } finally {
                    inFlight.remove(key)
                }
            }
        }
        return deferred.await()
    }

    private fun fetchTileSync(
        appContext: Context,
        source: MapSource,
        layer: MapTileLayerSpec,
        z: Int,
        x: Int,
        y: Int,
        key: String
    ): TileFetchResult {
        return try {
            val url = layer.urlFor(z, x, y)
            val requestBuilder = Request.Builder().url(url)
            if (source == MapSource.OSM) {
                requestBuilder.header("User-Agent", osmUserAgent)
            } else {
                requestBuilder.header("User-Agent", CHROME_UA)
                requestBuilder.header("Referer", REFERER)
            }
            ensureClient(appContext).newCall(requestBuilder.build()).execute().use { response ->
                val bytes = response.body?.bytes()
                if (!response.isSuccessful || bytes == null || bytes.isEmpty()) {
                    val msg = "HTTP ${response.code}"
                    markFailed(key, "${layer.id} $msg")
                    return TileFetchResult.Failed(msg)
                }
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = if (layer.isOptionalOverlay) {
                        Bitmap.Config.ARGB_8888
                    } else {
                        Bitmap.Config.RGB_565
                    }
                }
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                if (bitmap == null) {
                    markFailed(key, "${layer.id} invalid image")
                    return TileFetchResult.Failed("Invalid image")
                }
                val imageBitmap = bitmap.asImageBitmap()
                tileCache.put(key, imageBitmap)
                failedKeys.remove(key)
                TileFetchResult.Ready(imageBitmap)
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (oom: OutOfMemoryError) {
            tileCache.trimToSize(tileCache.maxSize() / 2)
            markFailed(key, "${layer.id} OOM")
            TileFetchResult.Failed("Memory limit")
        } catch (e: Exception) {
            val safe = redactSecrets(e.message ?: e.javaClass.simpleName)
            markFailed(key, "${layer.id} $safe")
            TileFetchResult.Failed(safe)
        }
    }

    suspend fun loadIcon(context: Context, url: String): ImageBitmap? {
        if (url.isBlank() || (!url.startsWith("https://") && !url.startsWith("http://"))) {
            return null
        }
        iconCache.get(url)?.let { return it }
        val deferred = iconInFlight.getOrPut(url) {
            repoScope.async {
                try {
                    val req = Request.Builder()
                        .url(url)
                        .header("User-Agent", CHROME_UA)
                        .build()
                    ensureClient(context.applicationContext).newCall(req).execute().use { response ->
                        val bytes = response.body?.bytes()
                        if (!response.isSuccessful || bytes == null || bytes.isEmpty()) return@async null
                        val opts = BitmapFactory.Options().apply {
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                        }
                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                            ?.asImageBitmap()
                        if (bmp != null) iconCache.put(url, bmp)
                        bmp
                    }
                } catch (ce: CancellationException) {
                    throw ce
                } catch (_: Throwable) {
                    null
                } finally {
                    iconInFlight.remove(url)
                }
            }
        }
        return deferred.await()
    }

    private fun markFailed(key: String, detail: String) {
        if (failedKeys.size > 4096) failedKeys.clear()
        failedKeys.add(key)
        Log.w(TAG, "Tile load failed: ${redactSecrets(detail)}")
    }

    private fun redactSecrets(text: String): String = text
        .replace(BuildConfig.TIANMAP_KEY, "<redacted>")
        .replace(BuildConfig.AMAP_WEB_KEY, "<redacted>")
        .replace(BuildConfig.AMAP_SECURITY_CODE, "<redacted>")
}
