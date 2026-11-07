package com.scluzlep.newmangps.data.repository

import com.scluzlep.newmangps.data.api.NewsmyApiService
import com.scluzlep.newmangps.data.api.NewsmySigner
import com.scluzlep.newmangps.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
            val rows = res.toRows()
            val success = res.isResultOk == 1 && rows.firstOrNull()?.get("Result") == "1"
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
            val res = apiService.executeRpc(
                user = user,
                cmd = "Proc_Login",
                params = listOf(user, pass)
            )
            val rows = res.toRows()
            val success = res.isResultOk == 1 && rows.firstOrNull()?.get("Result") == "1"
            if (success) {
                _currentUser.value = user
                _currentPassword.value = pass
                _loginType.value = 0
                Result.success(true)
            } else {
                Result.failure(Exception("用户名或密码错误"))
            }
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
            Result.success(res.isResultOk == 1)
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
                user = imei,
                cmd = "Proc_GetLastPosition",
                params = listOf(imei)
            )
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
                user = imei,
                cmd = "Proc_GetCarInfo",
                params = listOf(imei)
            )
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
        limit: Int = 1000
    ): Result<List<TrackPoint>> {
        return try {
            val res = apiService.executeRpc(
                user = imei,
                cmd = "Proc_GetTrack",
                params = listOf(imei, startTimeSec, endTimeSec, limit)
            )
            val points = res.toRows().map { TrackPoint.fromRow(it) }
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
                user = imei,
                cmd = "Proc_GetUserCarFence",
                params = listOf(imei, "")
            )
            val fences = res.toRows().map { GeofenceItem.fromRow(it) }
            Result.success(fences)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Poll Alarm Queue for fallback notifications
     */
    suspend fun pollAlarmQueue(imei: String): Result<List<AlarmItem>> {
        return try {
            val res = apiService.executeRpc(
                user = imei,
                cmd = "Proc_GetTrackAlarmQueue",
                params = listOf(imei)
            )
            val alarms = res.toRows().map { row ->
                AlarmItem(
                    id = row["nID"] ?: "",
                    teid = row["strTEID"] ?: imei,
                    timeSeconds = row["nTime"]?.toLongOrNull() ?: 0L,
                    alarmType = row["nAlarmType"] ?: "0",
                    content = row["strContent"] ?: "设备报警"
                )
            }
            Result.success(alarms)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

