package com.scluzlep.newmangps.ui.screens.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.LruCache
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.scluzlep.newmangps.data.util.CoordinateTransform
import okhttp3.Cache
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sinh
import kotlin.math.tan

@Composable
fun OsmMap(
    points: List<CoordinateTransform.LatLng>,
    markerIndex: Int,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context -> OsmMapView(context) },
        update = { it.setRoute(points, markerIndex) },
        modifier = modifier.fillMaxSize()
    )
}

private class OsmMapView(context: Context) : View(context) {
    companion object {
        private var sharedClient: OkHttpClient? = null

        @Synchronized
        private fun tileClient(context: Context): OkHttpClient {
            return sharedClient ?: OkHttpClient.Builder()
                .cache(Cache(context.applicationContext.cacheDir.resolve("osm_tiles"), 64L * 1024 * 1024))
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
                .also { sharedClient = it }
        }
    }

    private val tiles = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    private val client = tileClient(context)
    private val loading = mutableSetOf<String>()
    private val failedUntil = mutableMapOf<String, Long>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var route = emptyList<CoordinateTransform.LatLng>()
    private var markerIndex = 0
    private var zoom = 15
    private var centerLat = 0.0
    private var centerLon = 0.0
    private var lastX = 0f
    private var lastY = 0f
    private var scaleAccumulator = 1f
    private val scaleDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleAccumulator *= detector.scaleFactor
                if (scaleAccumulator > 1.5f && zoom < 18) {
                    zoom++
                    scaleAccumulator = 1f
                    invalidate()
                } else if (scaleAccumulator < 0.67f && zoom > 3) {
                    zoom--
                    scaleAccumulator = 1f
                    invalidate()
                }
                return true
            }
        })

    fun setRoute(points: List<CoordinateTransform.LatLng>, index: Int) {
        if (route != points) {
            route = points.filter {
                it.latitude in -85.0..85.0 && it.longitude in -180.0..180.0
            }
            fitRoute()
        }
        markerIndex = index.coerceIn(0, max(0, route.size - 1))
        invalidate()
    }

    private fun fitRoute() {
        if (route.isEmpty()) return
        val minLat = route.minOf { it.latitude }
        val maxLat = route.maxOf { it.latitude }
        val minLon = route.minOf { it.longitude }
        val maxLon = route.maxOf { it.longitude }
        centerLat = (minLat + maxLat) / 2
        centerLon = (minLon + maxLon) / 2
        zoom = 15
        if (route.size > 1 && width > 0 && height > 0) {
            val dx = worldX(maxLon, 0) - worldX(minLon, 0)
            val dy = worldY(minLat, 0) - worldY(maxLat, 0)
            while (zoom > 3 &&
                (dx * (1 shl zoom) > width * 0.7 || dy * (1 shl zoom) > height * 0.7)
            ) zoom--
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (route.isNotEmpty()) fitRoute()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(232, 236, 239))
        if (route.isEmpty()) return
        val centerX = worldX(centerLon, zoom)
        val centerY = worldY(centerLat, zoom)
        val left = centerX - width / 2.0
        val top = centerY - height / 2.0
        val firstX = floor(left / 256).toInt()
        val lastX = floor((left + width) / 256).toInt()
        val firstY = max(0, floor(top / 256).toInt())
        val lastY = min((1 shl zoom) - 1, floor((top + height) / 256).toInt())
        for (x in firstX..lastX) for (y in firstY..lastY) {
            val wrappedX = ((x % (1 shl zoom)) + (1 shl zoom)) % (1 shl zoom)
            val key = "$zoom/$wrappedX/$y"
            val bitmap = tiles.get(key)
            val dest = RectF((x * 256 - left).toFloat(), (y * 256 - top).toFloat(),
                ((x + 1) * 256 - left).toFloat(), ((y + 1) * 256 - top).toFloat())
            if (bitmap != null) {
                paint.color = Color.WHITE
                canvas.drawBitmap(bitmap, null, dest, paint)
            } else {
                requestTile(key)
            }
        }
        if (route.size > 1) {
            val path = Path()
            route.forEachIndexed { index, point ->
                val x = (worldX(point.longitude, zoom) - left).toFloat()
                val y = (worldY(point.latitude, zoom) - top).toFloat()
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 6f * resources.displayMetrics.density
            paint.color = Color.WHITE
            canvas.drawPath(path, paint)
            paint.strokeWidth = 3f * resources.displayMetrics.density
            paint.color = Color.rgb(0, 122, 255)
            canvas.drawPath(path, paint)
            paint.style = Paint.Style.FILL
        }
        val marker = route[markerIndex]
        val mx = (worldX(marker.longitude, zoom) - left).toFloat()
        val my = (worldY(marker.latitude, zoom) - top).toFloat()
        paint.color = Color.WHITE
        canvas.drawCircle(mx, my, 12f * resources.displayMetrics.density, paint)
        paint.color = Color.rgb(0, 122, 255)
        canvas.drawCircle(mx, my, 8f * resources.displayMetrics.density, paint)
        val label = "© OpenStreetMap contributors"
        paint.textSize = 11f * resources.displayMetrics.scaledDensity
        val labelWidth = paint.measureText(label)
        paint.color = Color.argb(210, 255, 255, 255)
        canvas.drawRect(0f, 0f, labelWidth + 12f, 23f * resources.displayMetrics.density, paint)
        paint.color = Color.DKGRAY
        canvas.drawText(label, 6f, 17f * resources.displayMetrics.density, paint)
    }

    private fun requestTile(key: String) {
        if ((failedUntil[key] ?: 0L) > System.currentTimeMillis()) return
        if (!loading.add(key)) return
        val request = Request.Builder()
            .url("https://tile.openstreetmap.org/$key.png")
            .header("User-Agent", "NewmanGPS-Android/1.0 (https://github.com/Scluzlep/newmangps)")
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                post {
                    loading.remove(key)
                    failedUntil[key] = System.currentTimeMillis() + 60_000
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val bitmap = if (it.isSuccessful) {
                        it.body?.byteStream()?.use(BitmapFactory::decodeStream)
                    } else null
                    post {
                        loading.remove(key)
                        if (bitmap != null) {
                            tiles.put(key, bitmap)
                            invalidate()
                        } else {
                            failedUntil[key] = System.currentTimeMillis() + 60_000
                        }
                    }
                }
            }
        })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 1 && !scaleDetector.isInProgress) {
                    val dx = event.x - lastX
                    val dy = event.y - lastY
                    val worldX = worldX(centerLon, zoom) - dx
                    val worldY = worldY(centerLat, zoom) - dy
                    centerLon = worldX / (256.0 * (1 shl zoom)) * 360.0 - 180.0
                    centerLat = inverseY(worldY, zoom)
                    invalidate()
                }
                lastX = event.x
                lastY = event.y
                return true
            }
        }
        return true
    }

    private fun worldX(lon: Double, zoom: Int): Double =
        (lon + 180.0) / 360.0 * 256.0 * (1 shl zoom)

    private fun worldY(lat: Double, zoom: Int): Double {
        val radians = Math.toRadians(lat.coerceIn(-85.0511, 85.0511))
        return (1.0 - asinh(tan(radians)) / PI) / 2.0 * 256.0 * (1 shl zoom)
    }

    private fun inverseY(y: Double, zoom: Int): Double {
        val value = PI * (1.0 - 2.0 * y / (256.0 * (1 shl zoom)))
        return Math.toDegrees(kotlin.math.atan(sinh(value))).coerceIn(-85.0511, 85.0511)
    }
}
