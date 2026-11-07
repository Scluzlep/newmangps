package com.scluzlep.newmangps.data.repository

import com.scluzlep.newmangps.data.api.NewsmyApiService
import com.scluzlep.newmangps.data.api.NewsmySigner
import com.scluzlep.newmangps.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AlarmPage(val alarms: List<AlarmItem>, val hasMore: Boolean)

class NewsmyRepository(
    private val apiService: NewsmyApiService = NewsmyApiService()
) {
    private val _currentUser = MutableStateFlow<String?>(null)
    val currentUser: StateFlow<String?> = _currentUser.asStateFlow()

    private val _currentPassword = MutableStateFlow<String?>(null)
    private val _loginType = MutableStateFlow<Int>(2) // 2: IMEI, 0: Username

    /**
     * Login via Device IMEI and Password
     */
    suspend fun loginIMEI(imei: String, pass: String): Result<Boolean> {
        return try {
            val res = apiService.executeRpc(
                user = imei,
                cmd = "Proc_LoginIMEI",
                params = listOf(imei, pass)
            )
            val success = isLoginAccepted(res)
            if (success) {
                _currentUser.value = imei
                _currentPassword.value = pass
                _loginType.value = 2
                Result.success(true)
            } else {
                Result.failure(Exception("IMEI或密码错误"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Login via Standard Username and Password
     */
    suspend fun loginUser(user: String, pass: String): Result<Boolean> {
        return try {
            val primary = apiService.executeRpc(
                user = user,
                cmd = "Proc_Login",
                params = listOf(user, pass)
            )
            val primaryAccepted = isLoginAccepted(primary)
            val res = if (primaryAccepted) primary else apiService.executeRpc(
                user = user,
                cmd = "Proc_LoginAnother",
                params = listOf(user, pass)
            )
            val success = isLoginAccepted(res)
            if (success) {
                _currentUser.value = if (primaryAccepted) user
                    else res.records?.firstOrNull()?.firstOrNull()?.takeIf { it.isNotBlank() } ?: user
                _currentPassword.value = pass
                _loginType.value = if (primaryAccepted) 0 else 1
                Result.success(true)
            } else {
                Result.failure(Exception("用户名或密码错误"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDevices(): Result<List<DeviceSummary>> {
        val user = _currentUser.value ?: return Result.failure(IllegalStateException("尚未登录"))
        return try {
            val res = apiService.executeRpc(user, "Proc_GetCarAppMini", listOf(user))
            requireOk(res)
            Result.success(res.toRows().mapNotNull(DeviceSummary::fromRow).distinctBy { it.teid })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Register Newsmy GeTui / Push Client ID to official backend
     */
    suspend fun registerPushId(pushCid: String): Result<Boolean> {
        val user = _currentUser.value ?: return Result.failure(Exception("Not logged in"))
        val pass = _currentPassword.value ?: return Result.failure(Exception("Password missing"))
        val type = _loginType.value

        return try {
            val pwdMd5 = NewsmySigner.md5(pass.lowercase())
            val res = apiService.executeRpc(
                user = user,
                cmd = "Proc_UserSwitch",
                params = listOf(1, pushCid, user, pwdMd5, type, NewsmySigner.DEFAULT_SERVER)
            )
            Result.success(res.isResultOk != 0)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Query Real-time Position
     */
    suspend fun getLastPosition(imei: String): Result<DevicePosition?> {
        return try {
            val res = apiService.executeRpc(
                user = currentUserFor(imei),
                cmd = "Proc_GetLastPosition",
                params = listOf(imei)
            )
            requireOk(res)
            val row = res.toRows().firstOrNull()
            Result.success(row?.let { DevicePosition.fromRow(it) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Query Detailed Device Profile & Expiration Status
     */
    suspend fun getDeviceDetail(imei: String): Result<DeviceDetail?> {
        return try {
            val res = apiService.executeRpc(
                user = currentUserFor(imei),
                cmd = "Proc_GetCarInfo",
                params = listOf(imei)
            )
            requireOk(res)
            val row = res.toRows().firstOrNull()
            Result.success(row?.let { DeviceDetail.fromRow(it) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Query Historical Track Points
     */
    suspend fun getTrackHistory(
        imei: String,
        startTimeSec: Long,
        endTimeSec: Long,
        limit: Int = 10000
    ): Result<List<TrackPoint>> {
        return try {
            val res = apiService.executeRpc(
                user = currentUserFor(imei),
                cmd = "Proc_GetTrack",
                params = listOf(imei, startTimeSec, endTimeSec, limit)
            )
            requireOk(res)
            val points = res.toRows().map(TrackPoint::fromRow)
                .filter(TrackPoint::isUsable)
                .sortedBy(TrackPoint::timeSeconds)
                .distinctBy { Triple(it.timeSeconds, it.rawLat, it.rawLon) }
            Result.success(points)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Query Geofences
     */
    suspend fun getGeofences(imei: String): Result<List<GeofenceItem>> {
        return try {
            val res = apiService.executeRpc(
                user = currentUserFor(imei),
                cmd = "Proc_GetUserCarFence",
                params = listOf(currentUserFor(imei), "")
            )
            requireOk(res)
            val fences = res.toRows().map(GeofenceItem::fromRow)
                .filter { it.teid.isBlank() || it.teid == imei }
            Result.success(fences)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Poll Alarm Queue for fallback notifications
     */
    suspend fun pollAlarmQueue(
        user: String,
        startTimeSec: Long,
        endTimeSec: Long,
        offset: Int = 0
    ): Result<AlarmPage> {
        return try {
            val res = apiService.executeRpc(
                user = user,
                cmd = "Proc_GetTrackAlarmQueue",
                params = listOf(user, startTimeSec, endTimeSec, offset, 200, 0),
                field = "strCarNum,strTEID,nTime,nAlarmState,strOther"
            )
            requireOk(res)
            val rows = res.toRows()
            Result.success(AlarmPage(rows.mapNotNull(AlarmItem::fromRow), rows.size == 200))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendDeviceCommand(teid: String, action: String): Result<Unit> {
        val user = _currentUser.value ?: return Result.failure(IllegalStateException("尚未登录"))
        val code = when (action) {
            "CDeviceRestart" -> "0x0208"
            "EmergencyLocate" -> "0x0281"
            else -> return Result.failure(IllegalArgumentException("不支持的设备命令"))
        }
        return try {
            when (apiService.executeCommand(user, teid, code)) {
                1 -> Result.success(Unit)
                2 -> Result.failure(IllegalStateException("设备不支持该指令"))
                else -> Result.failure(IllegalStateException("指令下发失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun currentUserFor(teid: String): String = _currentUser.value ?: teid

    private fun isLoginAccepted(response: ApiResponse): Boolean =
        response.isResultOk != 0 &&
            response.toRows().firstOrNull()?.get("Result")?.let { it == "1" } != false

    private fun requireOk(response: ApiResponse) {
        if (response.isResultOk == 0) throw IllegalStateException("服务器拒绝了请求")
    }
}

