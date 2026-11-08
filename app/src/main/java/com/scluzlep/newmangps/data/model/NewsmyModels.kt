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
    val mileageInitKm: Double = 0.0,
    val iconId: String = ""
) {
    companion object {
        fun fromRow(row: Map<String, String>): DeviceSummary? {
            val teid = row["strTEID"]?.takeIf { it.isNotBlank() } ?: return null
            return DeviceSummary(
                teid = teid,
                name = row["strCarNum"].orEmpty().ifBlank { teid },
                mileageInitKm = row["nMileageInit"]?.toDoubleOrNull() ?: 0.0,
                iconId = row["strIconID"].orEmpty()
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
    val soundRemainSec: Int,
    val rawFields: Map<String, String> = emptyMap()
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
                soundRemainSec = row["nSoundRemainSec"]?.toIntOrNull() ?: 0,
                rawFields = row
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
    val state: Int, // 1: In, 2: Out, 3: In & Out
    val type: Int = 1,
    val distanceMeters: Int = 200,
    val geometry: String = "",
    val alarmType: Int = 3,
    val info: String = ""
) {
    companion object {
        fun fromRow(row: Map<String, String>): GeofenceItem {
            return GeofenceItem(
                id = row["nID"] ?: "",
                user = row["strUser"] ?: "",
                teid = row["strTEID"] ?: "",
                name = row["strName"] ?: "",
                state = row["nState"]?.toIntOrNull() ?: 3,
                type = row["nType"]?.toIntOrNull() ?: 1,
                distanceMeters = row["nDistance"]?.toIntOrNull() ?: 200,
                geometry = row["strArrLonLat"].orEmpty(),
                alarmType = row["nAlarmType"]?.toIntOrNull() ?: 3,
                info = row["strInfo"].orEmpty()
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

data class DevicePhoto(
    val teid: String,
    val timeSeconds: Long,
    val type: Int,
    val cameraId: Int,
    val relativePath: String,
    val url: String
) {
    companion object {
        fun fromRow(row: Map<String, String>, urlForPath: (String) -> String): DevicePhoto? {
            val path = row["strRelativePath"]?.takeIf { it.isNotBlank() } ?: return null
            return DevicePhoto(
                row["strTEID"].orEmpty(), row["nTime"]?.toLongOrNull() ?: 0,
                row["nPhotoType"]?.toIntOrNull() ?: 0,
                row["nCameraID"]?.toIntOrNull() ?: 0, path, urlForPath(path)
            )
        }
    }
}

data class DeviceRecording(
    val teid: String,
    val timeSeconds: Long,
    val type: Int,
    val durationSeconds: Int,
    val isNew: Boolean,
    val relativePath: String,
    val url: String
) {
    companion object {
        fun fromRow(row: Map<String, String>, urlForPath: (String) -> String): DeviceRecording? {
            val path = row["strRelativePath"]?.takeIf { it.isNotBlank() } ?: return null
            val seconds = row["nSecond"]?.toIntOrNull() ?: 0
            return DeviceRecording(
                row["strTEID"].orEmpty(), row["nTime"]?.toLongOrNull() ?: 0,
                row["nType"]?.toIntOrNull() ?: 0,
                if (seconds % 10 == 9) seconds + 1 else seconds,
                (row["isNew"]?.toIntOrNull() ?: 0) > 0, path, urlForPath(path)
            )
        }
    }
}

data class UserStats(
    val carTotal: Int,
    val subUserTotal: Int,
    val timeoutTotal: Int,
    val alarmTotal: Int,
    val onlineTotal: Int,
    val offlineTotal: Int,
    val dayOnlineTotal: Int,
    val weekOnlineTotal: Int,
    val monthOnlineTotal: Int,
    val quarterOnlineTotal: Int,
    val yearOnlineTotal: Int
) {
    val unactivatedTotal: Int get() = (carTotal - onlineTotal - offlineTotal).coerceAtLeast(0)

    companion object {
        fun fromRow(row: Map<String, String>) = UserStats(
            row["nCarTotal"].toNumber(), row["nSubUserTotal"].toNumber(),
            row["nTimeoutTotal"].toNumber(), row["nAlarmTotal"].toNumber(),
            row["nOnlineTotal"].toNumber(), row["nOfflineTotal"].toNumber(),
            row["nDayOnlineTotal"].toNumber(), row["nWeekOnlineTotal"].toNumber(),
            row["nMonthOnlineTotal"].toNumber(), row["nQuarterOnlineTotal"].toNumber(),
            row["nYearOnlineTotal"].toNumber()
        )
    }
}

data class SubUserPrivilege(
    val username: String,
    val limitSubUsers: Int,
    val limitCars: Int,
    val limitControlDevices: Int,
    val flags: Int
) {
    companion object {
        fun fromRow(row: Map<String, String>) = SubUserPrivilege(
            row["strUser"].orEmpty(), row["nLimitSubUser"].toNumber(),
            row["nLimitCar"].toNumber(), row["nLimitCtrlTE"].toNumber(),
            row["nLimitClientSoft"].toNumber()
        )
    }
}

data class UserRelation(val parent: String, val child: String) {
    companion object {
        fun fromRow(row: Map<String, String>): UserRelation? {
            val parent = row["strUser"]?.takeIf { it.isNotBlank() } ?: return null
            val child = row["strSubUser"]?.takeIf { it.isNotBlank() } ?: return null
            return if (parent == child) null else UserRelation(parent, child)
        }
    }
}

data class PayPlan(val id: Int, val name: String, val state: Int) {
    companion object {
        fun fromRow(row: Map<String, String>) = PayPlan(
            row["nPayPlan"].toNumber(), row["strName"].orEmpty(), row["nPayState"].toNumber()
        )
    }
}

data class DeviceModelCapability(
    val value: String,
    val name: String,
    val commands: Set<String>,
    val features: Set<String>,
    val config: String,
    val payPlan: Int
) {
    fun hides(section: String, option: String): Boolean = config.split(';').any { entry ->
        entry.substringBefore('=') == section && option in entry.substringAfter('=', "").split(',')
    }

    fun hasOptions(section: String): Boolean = config.split(';').any {
        it.substringBefore('=') == section
    }

    companion object {
        fun fromRow(row: Map<String, String>) = DeviceModelCapability(
            row["nValue"].orEmpty(), row["strName"].orEmpty(),
            row["strCmdInfo"].orEmpty().split(',').filter(String::isNotBlank).toSet(),
            row["strRealInfo"].orEmpty().split(',').filter(String::isNotBlank).toSet(),
            row["strConfigInfo"].orEmpty(), row["nPayPlan"].toNumber()
        )
    }
}

data class WifiMark(
    val teid: String, val timeSeconds: Long, val longitude: Double, val latitude: Double,
    val wifi: String, val near: String, val name: String, val address: String
) {
    companion object {
        fun fromRow(row: Map<String, String>) = WifiMark(
            row["strTEID"].orEmpty(), row["nTime"]?.toLongOrNull() ?: 0,
            row["dbLon"]?.toDoubleOrNull() ?: 0.0, row["dbLat"]?.toDoubleOrNull() ?: 0.0,
            row["strWifi"].orEmpty(), row["strNear"].orEmpty(),
            row["strName"].orEmpty(), row["strAddress"].orEmpty()
        )
    }
}

data class PayPrice(
    val planId: Int, val buyType: Int, val buyValue: Int,
    val price: Double, val currency: String
) {
    companion object {
        fun fromRow(row: Map<String, String>) = PayPrice(
            row["nPayPlan"].toNumber(), row["nBuyType"].toNumber(),
            row["nBuyValue"].toNumber(), row["nPrice"]?.toDoubleOrNull() ?: 0.0,
            row["strCurrency"].orEmpty()
        )
    }
}

data class PayOrder(
    val timeSeconds: Long, val buyType: Int, val buyValue: Int,
    val amount: Double, val currency: String, val orderId: String,
    val title: String, val content: String
) {
    companion object {
        fun fromRow(row: Map<String, String>) = PayOrder(
            row["nTime"]?.toLongOrNull() ?: 0,
            row["nBuyType"].toNumber(), row["nBuyValue"].toNumber(),
            row["nAmount"]?.toDoubleOrNull() ?: 0.0,
            row["strCurrency"].orEmpty(), row["strOrderID"].orEmpty(),
            row["strTitle"].orEmpty(), row["strContent"].orEmpty()
        )
    }
}

data class MonthlyInstallCount(val monthSeconds: Long, val count: Int) {
    companion object {
        fun fromRow(row: Map<String, String>) = MonthlyInstallCount(
            row["nCreateStartTime"]?.toLongOrNull() ?: 0,
            row["nCount"].toNumber()
        )
    }
}

data class ReportSample(
    val timeSeconds: Long,
    val longitude: Double,
    val latitude: Double,
    val speed: Double,
    val mileageKm: Double,
    val fuelRaw: Double?,
    val temperatureC: Double?,
    val other: String,
    val teState: Long = 0,
    val teid: String = "",
    val carNum: String = ""
) {
    val isValidPosition: Boolean get() = latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
        (latitude != 0.0 || longitude != 0.0) && (teState and 4096L != 0L || teState and 128L == 0L)
    val isParked: Boolean get() = speed < 2

    companion object {
        fun fromRow(row: Map<String, String>) = ReportSample(
            row["nTime"]?.toLongOrNull() ?: 0,
            parseCoordinate(row, "dbLon", "nLon"),
            parseCoordinate(row, "dbLat", "nLat"),
            row["nSpeed"]?.toDoubleOrNull() ?: 0.0,
            (row["nRealMileage"] ?: row["nMileage"])?.toDoubleOrNull()?.div(1000) ?: 0.0,
            row["nFuel"]?.toDoubleOrNull(),
            row["nTemp"]?.toDoubleOrNull()?.div(10),
            row["strOther"].orEmpty(),
            row["nTEState"]?.toLongOrNull() ?: 0,
            row["strTEID"].orEmpty(),
            row["strCarNum"].orEmpty()
        )
    }
}

data class HealthSample(val timeSeconds: Long, val name: String, val value: Double) {
    companion object {
        fun fromRow(row: Map<String, String>): HealthSample? {
            val name = row["strName"]?.takeIf { it.isNotBlank() } ?: return null
            val value = row["strValue"]?.toDoubleOrNull() ?: return null
            return HealthSample(row["nTime"]?.toLongOrNull() ?: 0, name, value)
        }
    }
}

private fun String?.toNumber(): Int = this?.toIntOrNull() ?: 0

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

