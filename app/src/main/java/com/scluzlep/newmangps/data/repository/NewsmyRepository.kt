package com.scluzlep.newmangps.data.repository

import com.scluzlep.newmangps.data.api.NewsmyApiService
import com.scluzlep.newmangps.data.api.AmapGeocoder
import com.scluzlep.newmangps.data.api.AmapPlace
import com.scluzlep.newmangps.data.api.NewsmySigner
import com.scluzlep.newmangps.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AlarmPage(val alarms: List<AlarmItem>, val hasMore: Boolean)

class NewsmyRepository(
    private val apiService: NewsmyApiService = NewsmyApiService(),
    private val amapGeocoder: AmapGeocoder = AmapGeocoder()
) {
    private var serverUrl: String = NewsmySigner.DEFAULT_SERVER
    private val _currentUser = MutableStateFlow<String?>(null)
    val currentUser: StateFlow<String?> = _currentUser.asStateFlow()

    private val _currentPassword = MutableStateFlow<String?>(null)
    private val _loginType = MutableStateFlow<Int>(2) // 2: IMEI, 0: Username
    private val _loginIdentity = MutableStateFlow<String?>(null)

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
                _loginIdentity.value = imei
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
                _loginIdentity.value = user
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

    val loginType: Int get() = _loginType.value
    val currentServer: String get() = serverUrl
    val availableServers = listOf(
        NewsmySigner.DEFAULT_SERVER to "纽曼卫士",
        NewsmySigner.CLUSTER_SERVER to "纽曼 GPS 之家"
    )

    suspend fun switchServer(server: String): Result<Boolean> {
        if (availableServers.none { it.first == server }) {
            return Result.failure(IllegalArgumentException("不支持的服务器"))
        }
        val identity = _loginIdentity.value ?: return Result.failure(IllegalStateException("尚未登录"))
        val password = _currentPassword.value ?: return Result.failure(IllegalStateException("登录密码不可用"))
        val previous = serverUrl
        apiService.setServerUrl(server)
        return try {
            val command = when (_loginType.value) {
                0 -> "Proc_Login"
                1 -> "Proc_LoginAnother"
                else -> "Proc_LoginIMEI"
            }
            val response = apiService.executeRpc(identity, command, listOf(identity, password))
            if (isLoginAccepted(response)) {
                serverUrl = server
                Result.success(true)
            } else {
                apiService.setServerUrl(previous)
                Result.failure(IllegalStateException("目标服务器拒绝了当前账户"))
            }
        } catch (error: Exception) {
            apiService.setServerUrl(previous)
            Result.failure(error)
        }
    }

    fun logout() {
        _currentUser.value = null
        _currentPassword.value = null
        _loginIdentity.value = null
        _loginType.value = 2
    }

    suspend fun changePassword(oldPassword: String, newPassword: String): Result<Boolean> {
        val user = _currentUser.value ?: return Result.failure(IllegalStateException("尚未登录"))
        val identity = _loginIdentity.value ?: user
        if (oldPassword.isBlank() || newPassword.isBlank()) {
            return Result.failure(IllegalArgumentException("请输入当前密码和新密码"))
        }
        return runCatching {
            val response = apiService.executeRpc(
                user,
                "Proc_ModPassword",
                listOf(_loginType.value, identity, oldPassword, newPassword)
            )
            requireOk(response)
            (response.isResultOk != 0).also { if (it) _currentPassword.value = newPassword }
        }
    }

    suspend fun getSubUsers(): Result<List<SubUserInfo>> {
        val user = _currentUser.value ?: return Result.failure(IllegalStateException("尚未登录"))
        return runCatching {
            apiService.executeRpc(user, "Proc_GetSubUser", listOf(user)).also(::requireOk)
                .toRows().mapNotNull(SubUserInfo::fromRow)
        }
    }

    suspend fun getCurrentProfile(): Result<SubUserInfo?> {
        val user = _currentUser.value ?: return Result.failure(IllegalStateException("尚未登录"))
        return runCatching {
            val response = apiService.executeRpc(user, "Proc_GetUser", listOf(user))
            requireOk(response)
            response.toRows().firstOrNull()?.let(SubUserInfo::fromRow)
        }
    }

    suspend fun saveCurrentProfile(
        name: String,
        phone: String,
        company: String,
        address: String,
        email: String,
        remark: String
    ): Result<Boolean> {
        val user = _currentUser.value ?: return Result.failure(IllegalStateException("尚未登录"))
        val password = _currentPassword.value ?: return Result.failure(IllegalStateException("登录密码不可用"))
        val identity = _loginIdentity.value ?: user
        if (_loginType.value == 2) return Result.failure(IllegalStateException("设备直连账号没有平台资料"))
        return runCatching {
            val response = apiService.executeRpc(
                user,
                "Proc_ModUser",
                listOf(identity, password, name, phone, company, address, email, remark)
            )
            requireOk(response)
            response.isResultOk != 0
        }
    }

    suspend fun addSubUser(
        username: String,
        password: String,
        name: String,
        phone: String,
        company: String,
        address: String,
        email: String,
        remark: String
    ): Result<Boolean> {
        val parent = _currentUser.value ?: return Result.failure(IllegalStateException("尚未登录"))
        if (username.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("子账号和密码不能为空"))
        }
        return runCatching {
            val response = apiService.executeRpc(
                parent,
                "Proc_AddSubUser",
                listOf(parent, username, password, name, phone, company, address, email, remark)
            )
            requireOk(response)
            response.isResultOk != 0
        }
    }

    suspend fun deleteSubUser(username: String): Result<Boolean> {
        val parent = _currentUser.value ?: return Result.failure(IllegalStateException("尚未登录"))
        return runCatching {
            val response = apiService.executeRpc(parent, "Proc_DelSubUser", listOf(parent, username))
            requireOk(response)
            response.isResultOk != 0
        }
    }

    suspend fun assignDeviceToSubUser(username: String, teid: String, assigned: Boolean): Result<Boolean> {
        val parent = _currentUser.value ?: return Result.failure(IllegalStateException("尚未登录"))
        return runCatching {
            val response = apiService.executeRpc(
                parent,
                "Proc_AddCarToSubUser",
                listOf(parent, username, teid, if (assigned) 1 else 0)
            )
            requireOk(response)
            response.isResultOk != 0
        }
    }

    suspend fun modifySubUser(
        username: String,
        password: String,
        name: String,
        phone: String,
        company: String,
        address: String,
        email: String,
        remark: String
    ): Result<Boolean> {
        val user = _currentUser.value ?: return Result.failure(IllegalStateException("尚未登录"))
        if (username.isBlank()) return Result.failure(IllegalArgumentException("子账号不能为空"))
        return runCatching {
            val response = apiService.executeRpc(
                user,
                "Proc_ModUser",
                listOf(username, password, name, phone, company, address, email, remark)
            )
            requireOk(response)
            response.isResultOk != 0
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

    suspend fun reverseGeocode(position: DevicePosition): Result<String> =
        amapGeocoder.reverse(position.gcjLatLng)

    suspend fun searchPlaces(
        keyword: String,
        around: com.scluzlep.newmangps.data.util.CoordinateTransform.LatLng?
    ): Result<List<AmapPlace>> = amapGeocoder.searchPlaces(keyword, around)

    suspend fun savePlace(point: com.scluzlep.newmangps.data.util.CoordinateTransform.LatLng, name: String): Result<Boolean> {
        val user = _currentUser.value ?: return Result.failure(IllegalStateException("尚未登录"))
        if (name.isBlank()) return Result.failure(IllegalArgumentException("地点名称不能为空"))
        return runCatching {
            val response = apiService.executeRpc(
                user,
                "Proc_SaveAddress",
                listOf(point.longitude, point.latitude, name)
            )
            requireOk(response)
            response.isResultOk != 0
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
                .filterTrackNoise()
            Result.success(points)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun List<TrackPoint>.filterTrackNoise(): List<TrackPoint> {
        if (size < 3) return this
        val result = ArrayList<TrackPoint>(size)
        for (point in this) {
            val previous = result.lastOrNull()
            if (previous == null) {
                result += point
                continue
            }
            val distance = distanceMeters(previous, point)
            val seconds = (point.timeSeconds - previous.timeSeconds).coerceAtLeast(1)
            val impliedSpeed = distance * 3.6 / seconds
            val wifi = point.teState and 4096L != 0L
            val baseStation = point.teState and 8192L != 0L
            val invalid = point.teState and 128L != 0L && !wifi
            val duplicate = distance < 2.0 && point.speed == 0 && previous.speed == 0
            val impossibleJump = impliedSpeed > 240.0 && distance > 100.0
            if ((!baseStation || wifi) && !invalid && !duplicate && !impossibleJump) result += point
        }
        return if (result.isEmpty()) this else result
    }

    private fun distanceMeters(a: TrackPoint, b: TrackPoint): Double {
        val lat1 = Math.toRadians(a.rawLat)
        val lat2 = Math.toRadians(b.rawLat)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(b.rawLon - a.rawLon)
        val sinLat = kotlin.math.sin(dLat / 2)
        val sinLon = kotlin.math.sin(dLon / 2)
        val h = sinLat * sinLat + kotlin.math.cos(lat1) * kotlin.math.cos(lat2) * sinLon * sinLon
        return 12_742_000.0 * kotlin.math.asin(kotlin.math.sqrt(h.coerceIn(0.0, 1.0)))
    }

    /**
     * Query Geofences
     */
    suspend fun getGeofences(imei: String): Result<List<GeofenceItem>> {
        return try {
            val res = apiService.executeRpc(
                user = currentUserFor(imei),
                cmd = "Proc_CarFence",
                params = listOf(0, imei)
            )
            requireOk(res)
            val fences = res.toRows().map(GeofenceItem::fromRow)
                .filter { it.teid.isBlank() || it.teid == imei }
            Result.success(fences)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserFences(): Result<List<Map<String, String>>> {
        val user = _currentUser.value ?: return Result.failure(IllegalStateException("尚未登录"))
        return runCatching {
            apiService.executeRpc(user, "Proc_GetUserFence", listOf(user)).also(::requireOk)
                .toRows()
        }
    }

    suspend fun saveUserFence(
        teid: String,
        name: String,
        type: Int,
        distance: Int,
        lonLat: String,
        speed: Int,
        alarmType: Int,
        info: String,
        editing: Boolean = false
    ): Result<Boolean> {
        val user = _currentUser.value ?: return Result.failure(IllegalStateException("尚未登录"))
        if (name.isBlank() || lonLat.isBlank()) {
            return Result.failure(IllegalArgumentException("围栏名称和坐标不能为空"))
        }
        return runCatching {
            val procedure = if (editing) "Proc_ModUserFence" else "Proc_AddFenceToUser"
            val fenceUser = fenceUserFor(teid)
            val response = apiService.executeRpc(
                user,
                procedure,
                listOf(fenceUser, name, type, distance, lonLat, speed, alarmType, info)
            )
            requireOk(response)
            response.isResultOk != 0
        }
    }

    suspend fun deleteUserFence(teid: String, name: String, state: Int): Result<Boolean> {
        val user = _currentUser.value ?: return Result.failure(IllegalStateException("尚未登录"))
        return runCatching {
            val response = apiService.executeRpc(user, "Proc_CarFence", listOf(2, teid, state, name))
            requireOk(response)
            response.isResultOk != 0
        }
    }

    suspend fun bindFenceToDevice(teid: String, name: String, alarmType: Int): Result<Boolean> =
        changeFenceBinding(teid, name, alarmType, bind = true)

    suspend fun unbindFenceFromDevice(teid: String, name: String, alarmType: Int): Result<Boolean> =
        changeFenceBinding(teid, name, alarmType, bind = false)

    private suspend fun changeFenceBinding(
        teid: String,
        name: String,
        alarmType: Int,
        bind: Boolean
    ): Result<Boolean> {
        val user = _currentUser.value ?: return Result.failure(IllegalStateException("尚未登录"))
        return runCatching {
            val params = if (bind) {
                listOf(fenceUserFor(teid), teid, name, alarmType)
            } else {
                listOf(4, teid, alarmType, name)
            }
            val response = apiService.executeRpc(user, if (bind) "Proc_AddFenceToUserCar" else "Proc_CarFence", params)
            requireOk(response)
            response.isResultOk != 0
        }
    }

    private fun fenceUserFor(teid: String): String = teid.takeLast(15)

    /**
     * Poll Alarm Queue for fallback notifications
     */
    suspend fun pollAlarmQueue(
        user: String,
        teid: String,
        startTimeSec: Long,
        endTimeSec: Long,
        offset: Int = 0
    ): Result<AlarmPage> {
        return try {
            val res = apiService.executeRpc(
                user = user,
                cmd = "Proc_GetTrackAlarmQueue",
                params = listOf(teid, startTimeSec, endTimeSec, offset, 200, 0),
                field = "strCarNum,strTEID,nTime,nAlarmState,strOther"
            )
            requireOk(res)
            val rows = res.toRows()
            Result.success(AlarmPage(rows.mapNotNull(AlarmItem::fromRow), rows.size == 200))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendDeviceCommand(teid: String, action: String, parameter: String = ""): Result<Unit> {
        val user = _currentUser.value ?: return Result.failure(IllegalStateException("尚未登录"))
        val code = when (action) {
            "CDeviceRestart" -> "0x0208"
            "EmergencyLocate" -> "0x0281"
            "SetTrackInterval" -> "0x020A"
            "SetShakeSensitivity" -> "0x025D"
            "SetPowerSavingMode" -> "0x0247"
            else -> return Result.failure(IllegalArgumentException("不支持的设备命令"))
        }
        return try {
            when (apiService.executeCommand(user, teid, code, parameter)) {
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

