package com.scluzlep.newmangps.data.util

import kotlin.math.*

/**
 * Coordinate transformation utility:
 * - WGS-84: Standard Global GPS coordinates (reported by Newsmy GPS hardware)
 * - GCJ-02: Mars coordinates used by AutoNavi / AMap (高德) & Tencent Map
 * - BD-09: Baidu Map coordinates
 */
object CoordinateTransform {
    private const val X_PI = PI * 3000.0 / 180.0
    private const val PI_VAL = PI
    private const val A = 6378245.0 // Semi-major axis
    private const val EE = 0.00669342162296594323 // Eccentricity squared

    data class LatLng(val latitude: Double, val longitude: Double)

    /**
     * Convert WGS-84 to GCJ-02 (for AMap / Tencent Map display)
     */
    fun wgs84ToGcj02(lat: Double, lon: Double): LatLng {
        if (isOutOfChina(lat, lon)) {
            return LatLng(lat, lon)
        }
        var dLat = transformLat(lon - 105.0, lat - 35.0)
        var dLon = transformLon(lon - 105.0, lat - 35.0)
        val radLat = lat / 180.0 * PI_VAL
        var magic = sin(radLat)
        magic = 1 - EE * magic * magic
        val sqrtMagic = sqrt(magic)
        dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI_VAL)
        dLon = (dLon * 180.0) / (A / sqrtMagic * cos(radLat) * PI_VAL)
        return LatLng(lat + dLat, lon + dLon)
    }

    /**
     * Convert GCJ-02 to WGS-84 (for geofence saving back to server)
     */
    fun gcj02ToWgs84(lat: Double, lon: Double): LatLng {
        if (isOutOfChina(lat, lon)) {
            return LatLng(lat, lon)
        }
        var dLat = transformLat(lon - 105.0, lat - 35.0)
        var dLon = transformLon(lon - 105.0, lat - 35.0)
        val radLat = lat / 180.0 * PI_VAL
        var magic = sin(radLat)
        magic = 1 - EE * magic * magic
        val sqrtMagic = sqrt(magic)
        dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI_VAL)
        dLon = (dLon * 180.0) / (A / sqrtMagic * cos(radLat) * PI_VAL)
        val mgLat = lat + dLat
        val mgLon = lon + dLon
        return LatLng(lat * 2 - mgLat, lon * 2 - mgLon)
    }

    /**
     * Convert GCJ-02 to BD-09 (for Baidu Map)
     */
    fun gcj02ToBd09(lat: Double, lon: Double): LatLng {
        val z = sqrt(lon * lon + lat * lat) + 0.00002 * sin(lat * X_PI)
        val theta = atan2(lat, lon) + 0.000003 * cos(lon * X_PI)
        val bdLon = z * cos(theta) + 0.0065
        val bdLat = z * sin(theta) + 0.006
        return LatLng(bdLat, bdLon)
    }

    private fun isOutOfChina(lat: Double, lon: Double): Boolean {
        return lon < 72.004 || lon > 137.8347 || lat < 0.8293 || lat > 55.8271
    }

    private fun transformLat(x: Double, y: Double): Double {
        var ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * PI_VAL) + 20.0 * sin(2.0 * x * PI_VAL)) * 2.0 / 3.0
        ret += (20.0 * sin(y * PI_VAL) + 40.0 * sin(y / 3.0 * PI_VAL)) * 2.0 / 3.0
        ret += (160.0 * sin(y / 12.0 * PI_VAL) + 320 * sin(y * PI_VAL / 30.0)) * 2.0 / 3.0
        return ret
    }

    private fun transformLon(x: Double, y: Double): Double {
        var ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * PI_VAL) + 20.0 * sin(2.0 * x * PI_VAL)) * 2.0 / 3.0
        ret += (20.0 * sin(x * PI_VAL) + 40.0 * sin(x / 3.0 * PI_VAL)) * 2.0 / 3.0
        ret += (150.0 * sin(x / 12.0 * PI_VAL) + 300.0 * sin(x / 30.0 * PI_VAL)) * 2.0 / 3.0
        return ret
    }
}
