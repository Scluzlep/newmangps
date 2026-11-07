package com.scluzlep.newmangps.data.model

import com.google.gson.annotations.SerializedName
import com.scluzlep.newmangps.data.util.CoordinateTransform
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ApiResponse(
    @SerializedName("m_isResultOk") val isResultOk: Int = 0,
    @SerializedName("m_strServerVersion") val serverVersion: String? = null,
    @SerializedName("m_strRemoteIP") val remoteIP: String? = null,
    @SerializedName("m_arrField") val fields: List<String>? = null,
    @SerializedName("m_arrRecord") val records: List<List<String>>? = null
) {
    fun toRows(): List<Map<String, String>> {
        val fList = fields ?: return emptyList()
        val rList = records ?: return emptyList()
        return rList.map { row ->
            fList.indices.associate { index ->
                fList[index] to (if (index < row.size) row[index] else "")
            }
        }
    }
}

data class DeviceSummary(
    val teid: String,
    val name: String,
    val mileageInitKm: Double = 0.0
) {
    companion object {
        fun fromRow(row: Map<String, String>): DeviceSummary? {
            val teid = row["strTEID"]?.takeIf { it.isNotBlank() } ?: return null
            return DeviceSummary(
                teid = teid,
                name = row["strCarNum"].orEmpty().ifBlank { teid },
                mileageInitKm = row["nMileageInit"]?.toDoubleOrNull() ?: 0.0
            )
        }
    }
}

data class SubUserInfo(
    val username: String,
    val displayName: String,
    val phone: String,
    val company: String,
    val address: String,
    val email: String,
    val remark: String
) {
    companion object {
        fun fromRow(row: Map<String, String>): SubUserInfo? {
            val username = row["strUser"]?.takeIf { it.isNotBlank() } ?: return null
            return SubUserInfo(
                username = username,
                displayName = row["strName"].orEmpty(),
                phone = row["strTel"].orEmpty(),
                company = row["strCompany"].orEmpty(),
                address = row["strAddress"].orEmpty(),
                email = row["strEmail"].orEmpty(),
                remark = row["strRemark"].orEmpty()
            )
        }
    }
}

data class DevicePosition(
    val teid: String,
    val timeSeconds: Long,
    val rawLon: Double,
    val rawLat: Double,
    val speed: Int,
    val direction: Int,
    val gsmSignal: Int,
    val gpsSignal: Int,
    val mileage: Int,
    val teState: Long,
    val carState: Long,
    val alarmState: Long,
    val parkTimeSeconds: Long,
    val runTimeSeconds: Long
) {
    // Exact battery extraction formula decoded from gps-go.js
    val batteryPercent: Int
        get() = ((teState and 0x00FF0000L) shr 16).toInt().coerceIn(0, 100)

    val isOnline: Boolean
        get() = timeSeconds > 0 && (System.currentTimeMillis() / 1000 - timeSeconds) in 0..899

    val isSuperPowerSaving: Boolean
        get() = (teState and 0x80000000L) != 0L

    val mileageKm: Double
        get() = mileage / 1000.0

    val parkDuration: String
        get() {
            val elapsed = System.currentTimeMillis() / 1000 - parkTimeSeconds
            if (speed > 0 || parkTimeSeconds <= 0 || elapsed < 600) return "--"
            return "${elapsed / 3600}小时${elapsed % 3600 / 60}分钟"
        }

    // High-precision GCJ-02 coordinates for AutoNavi/AMap
    val gcjLatLng: CoordinateTransform.LatLng
        get() = CoordinateTransform.wgs84ToGcj02(rawLat, rawLon)

    val formattedTime: String
        get() = formatTimestamp(timeSeconds)

    val formattedParkTime: String
        get() = formatTimestamp(parkTimeSeconds)

    companion object {
        fun fromRow(row: Map<String, String>): DevicePosition {
            return DevicePosition(
                teid = row["strTEID"] ?: "",
                timeSeconds = parseDeviceTime(row["nTime"] ?: row["dTime"]),
                rawLon = parseCoordinate(row, "dbLon", "nLon"),
                rawLat = parseCoordinate(row, "dbLat", "nLat"),
                speed = (row["nSpeed"]?.toIntOrNull() ?: 0).coerceAtLeast(0),
                direction = row["nDirection"]?.toIntOrNull() ?: 0,
                gsmSignal = row["nGSMSignal"]?.toIntOrNull() ?: 0,
                gpsSignal = row["nGPSSignal"]?.toIntOrNull() ?: 0,
                mileage = row["nMileage"]?.toIntOrNull() ?: 0,
                teState = row["nTEState"]?.toLongOrNull() ?: 0L,
                carState = row["nCarState"]?.toLongOrNull() ?: 0L,
                alarmState = row["nAlarmState"]?.toLongOrNull() ?: 0L,
                parkTimeSeconds = row["nParkTime"]?.toLongOrNull() ?: 0L,
                runTimeSeconds = row["nRunTime"]?.toLongOrNull() ?: 0L
            )
        }
    }
}

data class DeviceDetail(
    val teid: String,
    val carNum: String,
    val sim: String,
    val iccid: String,
    val remark: String,
    val deviceModel: String,
    val limitTimeSeconds: Long,
    val activeTimeSeconds: Long,
    val addTimeSeconds: Long,
    val soundRemainSec: Int
) {
    val formattedLimitDate: String
        get() = formatTimestamp(limitTimeSeconds)

    val isExpired: Boolean
        get() = limitTimeSeconds > 0 && (System.currentTimeMillis() / 1000) > limitTimeSeconds

    companion object {
        fun fromRow(row: Map<String, String>): DeviceDetail {
            return DeviceDetail(
                teid = row["strTEID"] ?: "",
                carNum = row["strCarNum"] ?: "",
                sim = row["strTESim"] ?: "",
                iccid = row["strICCID"] ?: "",
                remark = row["strRemark"] ?: "",
                deviceModel = row["strDeviceModel"] ?: "",
                limitTimeSeconds = row["nLimitTime"]?.toLongOrNull() ?: 0L,
                activeTimeSeconds = row["nActiveTime"]?.toLongOrNull() ?: 0L,
                addTimeSeconds = row["nAddTime"]?.toLongOrNull() ?: 0L,
                soundRemainSec = row["nSoundRemainSec"]?.toIntOrNull() ?: 0
            )
        }
    }
}

data class TrackPoint(
    val id: String,
    val teid: String,
    val timeSeconds: Long,
    val rawLon: Double,
    val rawLat: Double,
    val speed: Int,
    val direction: Int,
    val mileage: Int,
    val gsmSignal: Int,
    val teState: Long
) {
    val gcjLatLng: CoordinateTransform.LatLng
        get() = CoordinateTransform.wgs84ToGcj02(rawLat, rawLon)

    val batteryPercent: Int
        get() = ((teState and 0x00FF0000L) shr 16).toInt().coerceIn(0, 100)

    val formattedTime: String
        get() = formatTimestamp(timeSeconds)

    val mileageKm: Double
        get() = mileage / 1000.0

    val isUsable: Boolean
        get() = timeSeconds > 0 && rawLat in -85.0..85.0 && rawLon in -180.0..180.0 &&
            rawLat != 0.0 && rawLon != 0.0 &&
            ((teState and 128L) == 0L || (teState and 4096L) != 0L) &&
            ((teState and 8192L) == 0L || (teState and 4096L) != 0L)

    companion object {
        fun fromRow(row: Map<String, String>): TrackPoint {
            return TrackPoint(
                id = row["nID"] ?: "",
                teid = row["strTEID"] ?: "",
                timeSeconds = parseDeviceTime(row["nTime"] ?: row["dTime"]),
                rawLon = parseCoordinate(row, "dbLon", "nLon"),
                rawLat = parseCoordinate(row, "dbLat", "nLat"),
                speed = (row["nSpeed"]?.toIntOrNull() ?: 0).coerceAtLeast(0),
                direction = row["nDirection"]?.toIntOrNull() ?: 0,
                mileage = row["nMileage"]?.toIntOrNull() ?: 0,
                gsmSignal = row["nGSMSignal"]?.toIntOrNull() ?: 0,
                teState = row["nTEState"]?.toLongOrNull() ?: 0L
            )
        }
    }
}

data class GeofenceItem(
    val id: String,
    val user: String,
    val teid: String,
    val name: String,
    val state: Int // 1: In, 2: Out, 3: In & Out
) {
    companion object {
        fun fromRow(row: Map<String, String>): GeofenceItem {
            return GeofenceItem(
                id = row["nID"] ?: "",
                user = row["strUser"] ?: "",
                teid = row["strTEID"] ?: "",
                name = row["strName"] ?: "",
                state = row["nState"]?.toIntOrNull() ?: 3
            )
        }
    }
}

data class AlarmItem(
    val id: String,
    val teid: String,
    val timeSeconds: Long,
    val alarmType: String,
    val content: String
) {
    companion object {
        fun fromRow(row: Map<String, String>): AlarmItem? {
            val teid = row["strTEID"]?.takeIf { it.isNotBlank() } ?: return null
            val time = row["nTime"]?.toLongOrNull() ?: return null
            val state = row["nAlarmState"]?.toLongOrNull() ?: 0L
            val labels = listOf(
                16L to "离开围栏", 32L to "进入围栏", 64L to "超速",
                128L to "紧急报警", 32768L to "低电量"
            ).filter { (bit, _) -> state and bit != 0L }.map { it.second }
            val description = labels.joinToString("、").ifBlank { "设备报警" }
            val other = row["strOther"].orEmpty().trim()
            return AlarmItem(
                id = "$teid:$time:$state:$other",
                teid = teid,
                timeSeconds = time,
                alarmType = state.toString(),
                content = if (other.isBlank()) description else "$description：$other"
            )
        }
    }
}

private fun formatTimestamp(seconds: Long): String {
    if (seconds <= 0) return "--"
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(seconds * 1000))
}

private fun parseCoordinate(row: Map<String, String>, decimalKey: String, scaledKey: String): Double {
    row[decimalKey]?.toDoubleOrNull()?.let { if (it in -180.0..180.0) return it }
    val scaled = row[scaledKey]?.toDoubleOrNull() ?: return 0.0
    return if (kotlin.math.abs(scaled) > 180.0) scaled / 10_000_000.0 else scaled
}

private fun parseDeviceTime(value: String?): Long {
    val number = value?.toLongOrNull()
    if (number != null) return if (number > 10_000_000_000L) number / 1000 else number
    if (value.isNullOrBlank()) return 0L
    val formats = listOf("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss")
    for (format in formats) {
        runCatching {
            return SimpleDateFormat(format, Locale.getDefault()).parse(value)?.time?.div(1000) ?: 0L
        }
    }
    return 0L
}

