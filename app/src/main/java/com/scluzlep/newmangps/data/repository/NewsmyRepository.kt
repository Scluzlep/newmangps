package com.scluzlep.newmangps.data.repository

import com.scluzlep.newmangps.data.api.NewsmyApiService
import com.scluzlep.newmangps.data.api.AmapGeocoder
import com.scluzlep.newmangps.data.api.AmapPlace
import com.scluzlep.newmangps.data.api.NewsmySigner
import com.scluzlep.newmangps.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URI
import java.util.Base64
import android.net.Uri
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.google.gson.JsonParser
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

data class AlarmPage(val alarms: List<AlarmItem>, val hasMore: Boolean)
data class PhonebookEntry(val id: Int, val phone: String, val name: String)
data class PublicDeviceInfo(
    val name: String, val owner: String, val phone: String, val address: String,
    val detail: String, val message: String, val visible: Boolean
)

class NewsmyRepository(
    private val apiService: NewsmyApiService = NewsmyApiService(),
    private val amapGeocoder: AmapGeocoder = AmapGeocoder()
) {
    private var serverUrl: String = NewsmySigner.DEFAULT_SERVER
    private val paymentMutex = Mutex()
    private val parameterMutex = Mutex()
    private val completedOrders = mutableSetOf<String>()
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

    suspend fun getDeviceModels(): Result<List<DeviceModelCapability>> = runCatching {
        val user = _currentUser.value ?: error("尚未登录")
        val command = if (_loginType.value == 2) "Proc_GetDeviceModel" else "Proc_DeviceModel"
        val params = if (_loginType.value == 2) listOf(user) else listOf(0)
        val response = apiService.executeRpc(user, command, params)
        requireOk(response)
        response.toRows().map(DeviceModelCapability::fromRow)
    }

    val loginType: Int get() = _loginType.value
    val currentServer: String get() = serverUrl
    val availableServers = listOf(
        NewsmySigner.DEFAULT_SERVER to "纽曼卫士",
        NewsmySigner.CLUSTER_SERVER to "纽曼 GPS 之家"
    )

    fun restoreServer(server: String) {
        if (availableServers.any { it.first == server }) {
            serverUrl = server
            apiService.setServerUrl(server)
        }
    }

    suspend fun switchServer(server: String): Result<Boolean> {
        if (availableServers.none { it.first == server }) {
            return Result.failure(IllegalArgumentException("不支持的服务器"))
        }
        val identity = _loginIdentity.value ?: return Result.failure(IllegalStateException("尚未登录"))
        val password = _currentPassword.value ?: return Result.failure(IllegalStateException("登录密码不可用"))
        val previous = serverUrl
        val previousUser = _currentUser.value
        val previousType = _loginType.value
        apiService.setServerUrl(server)
        return try {
            val login = if (previousType == 2) loginIMEI(identity, password) else loginUser(identity, password)
            val deviceCheck = if (login.isSuccess && previousType != 2) getDevices() else null
            if (login.isSuccess && (deviceCheck == null || deviceCheck.isSuccess)) {
                serverUrl = server
                Result.success(true)
            } else {
                apiService.setServerUrl(previous)
                _currentUser.value = previousUser
                _loginType.value = previousType
                Result.failure(login.exceptionOrNull() ?: deviceCheck?.exceptionOrNull()
                    ?: IllegalStateException("目标服务器拒绝了当前账户"))
            }
        } catch (error: Exception) {
            apiService.setServerUrl(previous)
            _currentUser.value = previousUser
            _loginType.value = previousType
            Result.failure(error)
        }
    }

    suspend fun switchLoginType(imeiLogin: Boolean): Result<Boolean> {
        val identity = _loginIdentity.value ?: return Result.failure(IllegalStateException("尚未登录"))
        val password = _currentPassword.value ?: return Result.failure(IllegalStateException("登录密码不可用"))
        val previousUser = _currentUser.value
        val previousType = _loginType.value
        val result = if (imeiLogin) loginIMEI(identity, password) else loginUser(identity, password)
        if (result.isFailure) {
            _currentUser.value = previousUser
            _loginType.value = previousType
            return result
        }
        val devices = if (imeiLogin) Result.success(emptyList()) else getDevices()
        if (devices.isFailure) {
            _currentUser.value = previousUser
            _loginType.value = previousType
            return Result.failure(devices.exceptionOrNull() ?: IllegalStateException("设备列表不可用"))
        }
        return Result.success(true)
    }

    fun logout() {
        _currentUser.value = null
        _currentPassword.value = null
        _loginIdentity.value = null
        _loginType.value = 2
        restoreServer(NewsmySigner.DEFAULT_SERVER)
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

    suspend fun getSupportInfo(): Result<Map<String, String>> = runCatching {
        val user = _currentUser.value ?: error("尚未登录")
        val service = apiService.executeRpc(user, "Proc_UserService",
            listOf(if (_loginType.value == 2) -2 else -1, user))
        requireOk(service)
        val row = service.toRows().firstOrNull().orEmpty()
        if (!row["strAfterSaleURL"].isNullOrBlank()) row
        else {
            val provider = apiService.executeRpc(user, "Proc_GetServiceProvider", listOf(user, _loginType.value))
            requireOk(provider)
            provider.toRows().firstOrNull().orEmpty()
        }
    }

    suspend fun getUserPrivilege(username: String): Result<SubUserPrivilege> = runCatching {
        val user = _currentUser.value ?: error("尚未登录")
        val response = apiService.executeRpc(user, "Proc_GetUser", listOf(username))
        requireOk(response)
        SubUserPrivilege.fromRow(response.toRows().firstOrNull() ?: error("未找到用户权限"))
    }

    suspend fun getUserTree(): Result<List<UserRelation>> = runCatching {
        val user = _currentUser.value ?: error("尚未登录")
        val response = apiService.executeRpc(
            user, "Proc_GetUserStruct", listOf(user), field = "strUser,strSubUser"
        )
        requireOk(response)
        response.toRows().mapNotNull(UserRelation::fromRow)
    }

    suspend fun assignSubUser(username: String, newParent: String): Result<Boolean> = runCatching {
        val user = _currentUser.value ?: error("尚未登录")
        require(_loginType.value == 0 && username != user) { "当前账号不能分配此用户" }
        require(username != newParent && newParent.isNotBlank()) { "不能分配给自己" }
        val relations = getUserTree().getOrThrow()
        val descendants = mutableSetOf(username)
        var changed: Boolean
        do {
            val previous = descendants.size
            relations.filter { it.parent in descendants }.forEach { descendants.add(it.child) }
            changed = descendants.size != previous
        } while (changed)
        require(newParent !in descendants) { "不能分配给该用户的下级账号" }
        require(newParent == user || relations.any { it.child == newParent }) { "目标账号不在当前用户树中" }
        val response = apiService.executeRpc(user, "Proc_UserDistribute", listOf(newParent, username))
        requireOk(response)
        isActionSuccessful(response)
    }

    suspend fun saveUserPrivilege(privilege: SubUserPrivilege): Result<Boolean> = runCatching {
        val user = _currentUser.value ?: error("尚未登录")
        require(_loginType.value == 0) { "当前账号不能修改权限" }
        val response = apiService.executeRpc(
            user, "Proc_ModSubUserPrivilege",
            listOf(user, privilege.username, privilege.limitSubUsers, privilege.limitCars,
                privilege.limitControlDevices, privilege.flags)
        )
        requireOk(response)
        isActionSuccessful(response)
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

    /** Bind a CID issued for this application, using the same Auth/Token sequence as the web client. */
    suspend fun registerPushId(pushCid: String, pushAppId: String): Result<Boolean> = runCatching {
        val user = _currentUser.value ?: error("尚未登录")
        require(pushCid.isNotBlank() && pushAppId.isNotBlank())
        val auth = apiService.executeRpc(user, "Proc_Auth",
            listOf(1, user, _loginType.value, "", "", "", pushCid, ""))
        requireOk(auth)
        val token = apiService.executeRpc(user, "Proc_Token",
            listOf(4, pushCid, _loginType.value, user, 3, pushAppId, "Android", "NewmanGPS"))
        requireOk(token)
        isActionSuccessful(token)
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

    suspend fun getChildrenPositions(cursorTeid: String): Result<List<DevicePosition>> = runCatching {
        val user = _currentUser.value ?: error("尚未登录")
        val response = apiService.executeRpc(
            user, "Proc_GetChildrenPos", listOf(user, 1, 100, cursorTeid),
            field = "strTEID,nTime,dbLon,dbLat,nSpeed,nDirection,nCarState,nTEState,nAlarmState"
        )
        requireOk(response)
        response.toRows().map(DevicePosition::fromRow)
            .filter { it.rawLat in -85.0..85.0 && it.rawLon in -180.0..180.0 && it.rawLat != 0.0 && it.rawLon != 0.0 }
    }

    suspend fun getPositionsForDevices(teids: List<String>, anchorTeid: String): Result<List<DevicePosition>> =
        runCatching {
            val requested = teids.filter(String::isNotBlank).distinct()
            if (requested.isEmpty()) return@runCatching emptyList()
            val firstBatch = getChildrenPositions(anchorTeid).getOrNull().orEmpty()
                .filter { it.teid in requested }
            val found = firstBatch.associateBy(DevicePosition::teid).toMutableMap()
            requested.filterNot(found::containsKey).chunked(8).forEach { batch ->
                val more = coroutineScope {
                    batch.map { teid -> async { getLastPosition(teid).getOrNull() } }.awaitAll()
                }
                more.filterNotNull().filter { it.rawLat in -85.0..85.0 &&
                    it.rawLon in -180.0..180.0 && (it.rawLat != 0.0 || it.rawLon != 0.0)
                }.forEach { found[it.teid] = it }
            }
            found.values.toList()
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

    private fun mediaUrl(folder: String, relativePath: String): String {
        val normalized = relativePath.replace('\\', '/').trimStart('/')
        val base = URI("$serverUrl${NewsmySigner.API_ENDPOINT}").resolve("./$folder/")
        val path = URI(null, null, base.path + normalized, null).rawPath
        return "${base.scheme}://${base.authority}$path"
    }

    suspend fun getLastPhotos(teid: String, count: Int = 10): Result<List<DevicePhoto>> = runCatching {
        val response = apiService.executeRpc(currentUserFor(teid), "Proc_GetLastPhoto", listOf(teid, count.coerceIn(1, 999)))
        requireOk(response)
        response.toRows().mapNotNull { DevicePhoto.fromRow(it) { path -> mediaUrl("Photo", path) } }
    }

    suspend fun getMonitorErrorCodes(teid: String): Result<List<Map<String, String>>> = runCatching {
        val response = apiService.executeRpc(currentUserFor(teid), "Proc_GetOBDErrorCode", listOf(1, teid))
        requireOk(response)
        response.toRows()
    }

    suspend fun getMonitorObd(teid: String): Result<List<Map<String, String>>> = runCatching {
        val response = apiService.executeRpc(
            currentUserFor(teid), "Proc_GetOBD", listOf(0, teid, 0, 0, 1_000_000)
        )
        requireOk(response)
        response.toRows()
    }

    suspend fun getMonitorCan(teid: String): Result<List<Map<String, String>>> = runCatching {
        val response = apiService.executeRpc(
            currentUserFor(teid), "Proc_GetCAN", listOf(0, teid, 0, 0, 100_000)
        )
        requireOk(response)
        response.toRows()
    }

    suspend fun getMonitorRfid(teid: String): Result<List<Map<String, String>>> = runCatching {
        val response = apiService.executeRpc(currentUserFor(teid), "Proc_GetRFIDHistoryLast", listOf(teid))
        requireOk(response)
        response.toRows()
    }

    suspend fun getPhonebook(teid: String): Result<List<PhonebookEntry>> = runCatching {
        val response = apiService.executeRpc(currentUserFor(teid), "Proc_Driver", listOf(0, teid))
        requireOk(response)
        response.toRows().firstOrNull()?.get("strTelInfo").orEmpty()
            .split('\u0010').mapNotNull { record ->
                val idAndValue = record.split('\u001f', limit = 2)
                if (idAndValue.size != 2) return@mapNotNull null
                val id = idAndValue[0].toIntOrNull() ?: return@mapNotNull null
                val phoneAndName = idAndValue[1].split('\u0018', limit = 2)
                PhonebookEntry(id, phoneAndName[0], phoneAndName.getOrElse(1) { "" })
            }
    }

    suspend fun savePhonebookEntry(
        teid: String, previousId: Int?, phone: String, name: String,
        delete: Boolean = false
    ): Result<Boolean> = runCatching {
        if (!delete) require(phone.isNotBlank() && name.isNotBlank()) { "请输入号码和姓名" }
        val existing = getPhonebook(teid).getOrThrow()
        val id = previousId ?: ((existing.maxOfOrNull(PhonebookEntry::id) ?: 0) + 1)
        require(previousId == null || existing.any { it.id == previousId }) { "电话簿已变化，请刷新后重试" }
        val updated = existing.filterNot { it.id == id }.toMutableList()
        if (!delete) updated += PhonebookEntry(id, phone.trim(), name.trim())
        val wholeBook = updated.joinToString("\u0010") { entry ->
            "${entry.id}\u001f${entry.phone}\u0018${entry.name}"
        }
        sendDeviceCommand(teid, "Phonebook", wholeBook).getOrThrow()
        val entryInfo = "$id\u001f${if (delete) "" else phone.trim()}\u0018${if (delete) "" else name.trim()}"
        val response = apiService.executeRpc(
            currentUserFor(teid), "Proc_Driver", listOf(1, teid, entryInfo, "", "")
        )
        requireOk(response)
        check(isActionSuccessful(response)) { "设备已接收电话簿，但服务器保存失败，请刷新核对" }
        true
    }

    suspend fun getDeviceParameter(teid: String, key: String): Result<String> = runCatching {
        val response = apiService.executeRpc(currentUserFor(teid), "Proc_Driver", listOf(0, teid))
        requireOk(response)
        val row = response.toRows().firstOrNull() ?: error("设备参数不可用")
        row["strParamInfo"].orEmpty().split('\u0010').firstNotNullOfOrNull { record ->
            val parts = record.split('\u001f', limit = 2)
            if (parts.size == 2 && parts[0] == key) parts[1] else null
        }.orEmpty()
    }

    suspend fun saveScheduledSetting(teid: String, key: String, action: String, value: String): Result<Boolean> =
        runCatching {
            require(key in setOf("SET_ALARM_CLOCK", "SET_IN_CLASS_DISABLE"))
            val before = apiService.executeRpc(currentUserFor(teid), "Proc_Driver", listOf(0, teid))
            requireOk(before)
            val row = before.toRows().firstOrNull() ?: error("无法读取现有设备参数")
            val oldValue = row["strParamInfo"].orEmpty()
            val records = oldValue.split('\u0010').filter(String::isNotBlank)
                .filterNot { it.substringBefore('\u001f') == key }
            val updated = (records + "$key\u001f$value").joinToString("\u0010")
            sendDeviceCommand(teid, action, value).getOrThrow()
            val saved = apiService.executeRpc(
                currentUserFor(teid), "Proc_Driver", listOf(3, teid, "", "", updated)
            )
            requireOk(saved)
            check(isActionSuccessful(saved)) { "设备已接收设置，但服务器保存失败，请重新读取核对" }
            true
        }

    suspend fun getPhotos(teid: String, start: Long, end: Long): Result<List<DevicePhoto>> = runCatching {
        val response = apiService.executeRpc(currentUserFor(teid), "Proc_GetPhoto", listOf(teid, start, end, 1_000_000))
        requireOk(response)
        response.toRows().mapNotNull { DevicePhoto.fromRow(it) { path -> mediaUrl("Photo", path) } }
    }

    suspend fun getRecordings(teid: String, start: Long, end: Long): Result<List<DeviceRecording>> = runCatching {
        val response = apiService.executeRpc(currentUserFor(teid), "Proc_GetSound", listOf(teid, start, end, 500))
        requireOk(response)
        response.toRows().mapNotNull { DeviceRecording.fromRow(it) { path -> mediaUrl("Sound", path) } }
            .sortedBy { it.timeSeconds }
    }

    suspend fun sendVoice(teid: String, durationSeconds: Int, mp3: ByteArray): Result<Boolean> = runCatching {
        require(durationSeconds in 1..60 && mp3.size > 1000) { "录音过短或超过 60 秒" }
        val uploaded = apiService.uploadVoice(teid, durationSeconds, mp3)
        require(uploaded.get("success")?.asBoolean == true) { "语音上传失败" }
        val time = uploaded.get("nTime")?.asLong ?: error("语音上传缺少时间")
        val relative = uploaded.get("strRelativePath")?.asString ?: error("语音上传缺少文件路径")
        val absolute = uploaded.get("strAbsolutePath")?.asString ?: error("语音上传缺少设备路径")
        val saved = apiService.executeRpc(
            currentUserFor(teid), "Proc_SaveSound",
            listOf(teid, time, 1, durationSeconds, 1, relative, absolute)
        )
        requireOk(saved)
        sendDeviceCommand(teid, "SendVoice", "$time&$durationSeconds&$absolute").getOrThrow()
        true
    }

    suspend fun setRecordingState(teid: String, timeSeconds: Long, action: Int? = null): Result<Boolean> = runCatching {
        require(action == null || action == -1 || action == -2)
        val params = listOfNotNull(teid, timeSeconds, action)
        val response = apiService.executeRpc(currentUserFor(teid), "Proc_SetSoundState", params)
        requireOk(response)
        response.isResultOk != 0
    }

    suspend fun getUserStats(): Result<UserStats> = runCatching {
        val user = _currentUser.value ?: error("尚未登录")
        val response = apiService.executeRpc(user, "Proc_GetUserStats", listOf(user))
        requireOk(response)
        UserStats.fromRow(response.toRows().firstOrNull() ?: error("统计数据为空"))
    }

    suspend fun getHealth(teid: String): Result<List<HealthSample>> = runCatching {
        val response = apiService.executeRpc(currentUserFor(teid), "Proc_GetHealth", listOf(teid))
        requireOk(response)
        response.toRows().mapNotNull(HealthSample::fromRow).sortedBy { it.timeSeconds }
    }

    suspend fun resetJumpRope(teid: String): Result<Boolean> = runCatching {
        val response = apiService.executeRpc(
            currentUserFor(teid), "Proc_SaveHealth",
            listOf(teid, System.currentTimeMillis() / 1000, "nJump", 0)
        )
        requireOk(response)
        isActionSuccessful(response)
    }

    suspend fun getInstallStats(): Result<List<MonthlyInstallCount>> = runCatching {
        val user = _currentUser.value ?: error("尚未登录")
        val response = apiService.executeRpc(user, "Proc_InstallStats", listOf(user))
        requireOk(response)
        response.toRows().map(MonthlyInstallCount::fromRow)
    }

    suspend fun getPayPlans(): Result<List<PayPlan>> = runCatching {
        val user = _currentUser.value ?: error("尚未登录")
        val candidates = if (_loginType.value == 2) listOf(
            "Proc_GetDevicePayPlan" to listOf(user), "Proc_PayPlan" to listOf(0)
        ) else listOf(
            "Proc_PayPlan" to listOf(0), "Proc_GetDevicePayPlan" to listOf(user)
        )
        candidates.firstNotNullOfOrNull { (command, params) ->
            runCatching {
                val response = apiService.executeRpc(user, command, params)
                requireOk(response)
                response.toRows().takeIf { it.isNotEmpty() }?.map(PayPlan::fromRow)
            }.getOrNull()
        }.orEmpty()
    }

    suspend fun getPayPrices(): Result<List<PayPrice>> = runCatching {
        val user = _currentUser.value ?: error("尚未登录")
        val candidates = if (_loginType.value == 2) listOf(
            "Proc_GetDevicePayPrice" to listOf(user), "Proc_PayPrice" to listOf(0)
        ) else listOf(
            "Proc_PayPrice" to listOf(0), "Proc_GetDevicePayPrice" to listOf(user)
        )
        candidates.firstNotNullOfOrNull { (command, params) ->
            runCatching {
                val response = apiService.executeRpc(user, command, params)
                requireOk(response)
                response.toRows().takeIf { it.isNotEmpty() }?.map(PayPrice::fromRow)
            }.getOrNull()
        }.orEmpty()
    }

    suspend fun getPayHistory(teid: String): Result<List<PayOrder>> = runCatching {
        val end = System.currentTimeMillis() / 1000 + 1_512_000
        val response = apiService.executeRpc(currentUserFor(teid), "Proc_GetPay", listOf(1, teid, 10, end))
        requireOk(response)
        response.toRows().map(PayOrder::fromRow).sortedByDescending { it.timeSeconds }
    }

    suspend fun getPaymentMethods(teid: String, buyType: Int): Result<List<PaymentMethod>> = runCatching {
        val user = _currentUser.value ?: error("尚未登录")
        val response = apiService.executeRpc(user, "Proc_GetPayKey", listOf(user, teid, buyType))
        requireOk(response)
        response.toRows().mapNotNull { row ->
            val method = row["strPayMeth"].orEmpty().lowercase(Locale.ROOT)
            val merchant = row["strKey"].orEmpty()
            if (method !in setOf("wxpay", "alipay") || merchant.isBlank()) null
            else PaymentMethod(method, merchant, row["nDiscount"]?.toDoubleOrNull() ?: 0.0)
        }
    }

    suspend fun startPayment(
        teid: String, price: PayPrice, title: String, method: PaymentMethod
    ): Result<PendingPayment> = runCatching {
        val user = _currentUser.value ?: error("尚未登录")
        require(price.currency == "CNY" && price.price > 0) { "当前币种尚不支持此支付渠道" }
        val now = System.currentTimeMillis() / 1000
        val amount = (price.price - method.discount).coerceAtLeast(1.0)
        val orderId = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
            "_${(System.currentTimeMillis() % 1000).toString().padStart(3, '0')}_${price.buyType}_${price.buyValue}"
        val detail = "设备:$teid, $title, ${price.buyValue}天, $amount CNY"
        val params = mapOf(
            "strPayTitle" to title, "strPayContent" to detail,
            "nPayAmount" to amount.toString(), "strCurrency" to price.currency,
            "strTradeID" to orderId, "strProductID" to "${user}_${now}_${price.buyType}"
        ) + if (method.method == "wxpay") mapOf(
            "strWechatPayMchID" to method.merchantId,
            "strTradeType" to "MWEB", "strOpenID" to ""
        ) else mapOf("strAlipayID" to method.merchantId)
        val path = if (method.method == "wxpay") "app/weixin/WechatPayOrder.asp"
        else "app/Alipay/AlipayOrder.asp"
        val body = apiService.paymentGet(path, params)
        val paymentUrl = if (method.method == "wxpay") {
            val xml = parsePaymentXml(body)
            require(xml["return_code"].orEmpty().contains("SUCCESS", true)) { "微信下单失败" }
            xml["mweb_url"].orEmpty()
        } else body.trim().trim('"')
        require(paymentUrl.startsWith("https://") || paymentUrl.startsWith("http://")) {
            "支付服务未返回可打开的付款地址"
        }
        val pending = PendingPayment(user, serverUrl, teid, orderId, method.method, method.merchantId,
            price.buyType, price.buyValue, amount, price.currency, title, detail, now,
            paymentUrl, method.discount)
        val event = apiService.executeRpc(user, "Proc_AddPayEvent",
            listOf(0, 0, now, method.method, "", "", "") + paymentParameters(pending, "CREATED"))
        requireOk(event)
        pending
    }

    suspend fun checkPayment(pending: PendingPayment): Result<Boolean> = runCatching {
        require(_currentUser.value == pending.user && serverUrl == pending.server) {
            "请切回创建订单时的账号和服务器"
        }
        val params = mapOf(
            "isQuery" to "1", "strPayTitle" to pending.title,
            "strPayContent" to pending.detail, "nPayAmount" to pending.amount.toString(),
            "strCurrency" to pending.currency, "strTradeID" to pending.orderId,
            "strProductID" to "${pending.user}_${pending.timeSeconds}_${pending.buyType}"
        ) + if (pending.method == "wxpay") mapOf(
            "strWechatPayMchID" to pending.merchantId,
            "strTradeType" to "MWEB", "strOpenID" to ""
        ) else mapOf("strAlipayID" to pending.merchantId)
        val path = if (pending.method == "wxpay") "app/weixin/WechatPayOrder.asp"
        else "app/Alipay/AlipayOrder.asp"
        val body = apiService.paymentGet(path, params)
        if (pending.method == "wxpay") {
            parsePaymentXml(body)["err_code"].equals("ORDERPAID", true)
        } else {
            val response = JsonParser.parseString(body).asJsonObject
                .getAsJsonObject("alipay_trade_query_response")
            response != null && response.get("code")?.asString == "10000" &&
                response.get("out_trade_no")?.asString == pending.orderId &&
                response.get("trade_status")?.asString in setOf("TRADE_SUCCESS", "TRADE_FINISHED")
        }
    }

    suspend fun finishPayment(pending: PendingPayment): Result<Boolean> {
        paymentMutex.lock()
        return try {
            runCatching {
            if (pending.orderId in completedOrders) return@runCatching true
            check(checkPayment(pending).getOrThrow()) { "服务器尚未确认付款" }
            val user = _currentUser.value ?: error("尚未登录")
            val response = apiService.executeRpc(user, "Proc_AddPay", paymentParameters(pending, "ORDERPAID"))
            requireOk(response)
            check(isActionSuccessful(response)) { "付款已确认，但服务开通未完成，请保留订单号联系售后" }
            completedOrders += pending.orderId
            true
            }
        } finally {
            paymentMutex.unlock()
        }
    }

    private fun paymentParameters(payment: PendingPayment, status: String): List<Any> {
        val user = _currentUser.value ?: error("尚未登录")
        val info = "Platform\u001fAndroid\u0010Server\u001f$serverUrl\u0010Status\u001f$status" +
            "\u0010PayMeth\u001f${payment.method}"
        return listOf(user, _loginType.value, payment.teid, payment.timeSeconds,
            payment.amount, payment.currency, payment.buyType, payment.buyValue,
            "", payment.orderId, info, payment.merchantId)
    }

    private fun parsePaymentXml(body: String): Map<String, String> {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(body))
        val result = mutableMapOf<String, String>()
        var current = ""
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> current = parser.name
                XmlPullParser.TEXT -> if (current.isNotBlank()) result[current] = parser.text.orEmpty()
                XmlPullParser.END_TAG -> current = ""
            }
            parser.next()
        }
        return result
    }

    suspend fun getReport(teid: String, kind: String, start: Long, end: Long): Result<List<ReportSample>> = runCatching {
        require(end >= start && start >= 0)
        val (command, limit) = when (kind) {
            "mileage" -> "Proc_GetMileageEveryDay" to 365
            "run", "park" -> "Proc_GetTrackRunData" to 20_000
            "fuel" -> "Proc_GetTrackTimeFuel" to 200_000
            "temperature" -> "Proc_GetTrackTimeTemp" to 500_000
            else -> error("未知报表类型")
        }
        val response = apiService.executeRpc(currentUserFor(teid), command, listOf(teid, start, end, limit))
        requireOk(response)
        response.toRows().map(ReportSample::fromRow).sortedBy { it.timeSeconds }
    }

    suspend fun getShareEnabled(teid: String): Result<Boolean> = sharePosition(teid, 0)

    suspend fun setShareEnabled(teid: String, enabled: Boolean): Result<Boolean> =
        sharePosition(teid, if (enabled) 1 else 2)

    private suspend fun sharePosition(teid: String, action: Int): Result<Boolean> = runCatching {
        val response = apiService.executeRpc(currentUserFor(teid), "Proc_SharePos", listOf(action, teid))
        requireOk(response)
        response.toRows().firstOrNull()?.entries?.firstOrNull { it.key.equals("Result", true) }
            ?.value == "1"
    }

    fun sharePositionUrl(teid: String): String {
        val padded = teid + "&".repeat((3 - teid.length % 3) % 3)
        val encoded = Base64.getEncoder().encodeToString(padded.toByteArray(Charsets.UTF_8))
        return "https://app.newsmy-nmgps.com/?SharePos=$encoded"
    }

    suspend fun getRadarUri(teid: String, deviceName: String): Result<Uri> = runCatching {
        require(deviceName.isNotBlank()) { "设备名称为空" }
        val config = apiService.getMiniAppConfig()
        val appId = config.get("strMiniAppID")?.asString?.takeIf { it.isNotBlank() }
            ?: error("未获取到雷达小程序配置")
        val query = "strTEID=$teid&strDeviceName=${Uri.encode(deviceName)}&"
        Uri.parse("weixin://dl/business/?appid=${Uri.encode(appId)}&path=pages/Bluetooth/Bluetooth&query=${Uri.encode(query)}")
    }

    suspend fun getVoiceCallUri(teid: String, deviceName: String): Result<Uri> = runCatching {
        require(deviceName.isNotBlank()) { "设备名称为空" }
        var serial = ""
        for (attempt in 0..2) {
            val response = apiService.executeRpc(currentUserFor(teid), "Proc_VoIPSN", listOf(teid))
            if (response.isResultOk != 0) serial = response.toRows().firstOrNull()?.get("strSN").orEmpty()
            if (serial.isNotBlank()) break
            if (attempt < 2) {
                sendDeviceCommand(teid, "VoipPrepare").getOrThrow()
                delay(5_000)
            }
        }
        require(serial.isNotBlank()) { "设备尚未返回通话序列号" }
        val config = apiService.getMiniAppConfig()
        val appId = config.get("strMiniAppID")?.asString?.takeIf { it.isNotBlank() } ?: error("通话小程序不可用")
        val appKey = config.get("strJuphoonAppKey")?.asString?.takeIf { it.isNotBlank() } ?: error("通话配置不完整")
        val modelId = config.get("strMiniAppModelID")?.asString?.takeIf { it.isNotBlank() } ?: error("通话配置不完整")
        val callId = "$teid" + "_" + SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(Date())
        val query = listOf(
            "strAppID=${Uri.encode(appId)}", "strAppKey=${Uri.encode(appKey)}", "strModeID=${Uri.encode(modelId)}", "strSN=${Uri.encode(serial)}",
            "strCallID=$callId", "strTEID=$teid", "strDeviceName=${Uri.encode(deviceName)}", "nLoginType=3"
        ).joinToString("&")
        sendDeviceCommand(teid, "VoipPrepare")
        Uri.parse("weixin://dl/business/?appid=${Uri.encode(appId)}&path=pages/Phone/Phone&query=${Uri.encode(query)}")
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

    suspend fun modifyDevice(
        detail: DeviceDetail, name: String, sim: String, group: String,
        owner: String, phone: String, address: String, remark: String,
        iconId: String? = null, info: String? = null
    ): Result<Boolean> = runCatching {
        require(name.isNotBlank()) { "设备名称不能为空" }
        val user = _currentUser.value ?: error("尚未登录")
        val latest = getDeviceDetail(detail.teid).getOrThrow() ?: error("设备资料不可用")
        val raw = latest.rawFields
        require(listOf("strPassword", "nSwitchType", "strDeviceModel", "strIMEI", "strNoticeEmail1")
            .all(raw::containsKey)) { "服务器未返回完整设备资料，已取消保存" }
        val params = listOf(
            detail.teid, name, sim, iconId ?: raw["strIconID"].orEmpty(), group,
            owner, phone, address, remark, raw["strPassword"].orEmpty(), 0,
            raw["nSwitchType"].orEmpty(), info ?: raw["strInfo"].orEmpty(),
            raw["strDeviceModel"].orEmpty(), raw["strIMEI"].orEmpty(),
            raw["strNoticeEmail1"].orEmpty()
        )
        val response = apiService.executeRpc(user, "Proc_ModCar", params)
        requireOk(response)
        response.toRows().firstOrNull()?.get("Result") == "1"
    }

    fun deviceIconUrl(iconId: String): String =
        if (iconId.matches(Regex("[A-Za-z0-9]{1,16}")) && iconId != "0")
            URI("${serverUrl.trimEnd('/')}/").resolve("Icon/$iconId.jpg").toString()
        else ""

    suspend fun setDeviceIcon(teid: String, iconId: String): Result<Boolean> = runCatching {
        val detail = getDeviceDetail(teid).getOrThrow() ?: error("设备资料不可用")
        val raw = detail.rawFields
        modifyDevice(
            detail, detail.carNum, detail.sim, raw["strGroupName"].orEmpty(),
            raw["strOwnerName"].orEmpty(), raw["strOwnerTel"].orEmpty(),
            raw["strOwnerAddress"].orEmpty(), detail.remark, iconId
        ).getOrThrow()
    }

    fun publicDeviceInfo(detail: DeviceDetail): PublicDeviceInfo {
        val values = detail.rawFields["strInfo"].orEmpty().split('\u001f').mapNotNull { entry ->
            val pair = entry.split('\u0010', limit = 2)
            if (pair.size == 2) pair[0] to pair[1] else null
        }.toMap()
        return PublicDeviceInfo(detail.carNum, detail.rawFields["strOwnerName"].orEmpty(),
            detail.rawFields["strOwnerTel"].orEmpty(), detail.rawFields["strOwnerAddress"].orEmpty(),
            values["DTIN"].orEmpty(), values["REIN"].orEmpty(), values["SSCI"] == "true")
    }

    suspend fun savePublicDeviceInfo(teid: String, value: PublicDeviceInfo): Result<Boolean> = runCatching {
        require((!value.visible || value.name.isNotBlank()) && value.name.length <= 23) { "设备名称需为 1 到 23 个字符" }
        require(listOf(value.owner, value.phone, value.address, value.detail, value.message)
            .all { it.length <= 512 }) { "公开资料不能超过 512 个字符" }
        val detail = getDeviceDetail(teid).getOrThrow() ?: error("设备资料不可用")
        val raw = detail.rawFields
        val old = raw["strInfo"].orEmpty().split('\u001f').filter(String::isNotBlank)
            .filterNot { it.substringBefore('\u0010') in setOf("SSCI", "DTIN", "REIN") }
        val updated = (old + listOf(
            "SSCI\u0010${value.visible}", "DTIN\u0010${value.detail}", "REIN\u0010${value.message}"
        )).joinToString("\u001f")
        val name = value.name.ifBlank { detail.carNum.takeIf { it.length in 1..23 } ?: teid }
        modifyDevice(detail, name, detail.sim, raw["strGroupName"].orEmpty(),
            value.owner, value.phone, value.address, detail.remark, info = updated).getOrThrow()
    }

    suspend fun setAlarmEmails(teid: String, emails: List<String>): Result<Boolean> = runCatching {
        require(emails.size == 3) { "需要三个邮箱字段" }
        val response = apiService.executeRpc(
            currentUserFor(teid), "Proc_SetCarAlarmNoticeEmail", listOf(teid) + emails
        )
        requireOk(response)
        isActionSuccessful(response)
    }

    suspend fun setInitialMileage(teid: String, kilometers: Int): Result<Boolean> = runCatching {
        require(kilometers >= 0) { "里程不能为负数" }
        val response = apiService.executeRpc(
            currentUserFor(teid), "Proc_SetMileageInit", listOf(teid, kilometers)
        )
        requireOk(response)
        isActionSuccessful(response)
    }

    suspend fun setFuelCalibration(
        teid: String, tankLiters: Int, emptyReading: Int, fullReading: Int
    ): Result<Boolean> = runCatching {
        require(tankLiters > 0 && fullReading > emptyReading) { "请填写有效的油箱容量和标定读数" }
        val scale = "0,$emptyReading;100,$fullReading"
        val response = apiService.executeRpc(
            currentUserFor(teid), "Proc_SetFuelBoxSize", listOf(teid, tankLiters, scale)
        )
        requireOk(response)
        isActionSuccessful(response)
    }

    suspend fun setLbsDisabled(teid: String, disabled: Boolean): Result<Boolean> = runCatching {
        val response = apiService.executeRpc(
            currentUserFor(teid), "Proc_SetCarConfig", listOf(teid, 33_554_432, if (disabled) 1 else 0, 0)
        )
        requireOk(response)
        isActionSuccessful(response)
    }

    suspend fun getWifiMarks(teid: String, scanned: Boolean = false): Result<List<WifiMark>> = runCatching {
        val response = apiService.executeRpc(
            currentUserFor(teid), "Proc_WifiMark", listOf(if (scanned) 4 else 0, teid)
        )
        requireOk(response)
        response.toRows().map(WifiMark::fromRow)
    }

    suspend fun getIndoorOffsets(teid: String): Result<List<IndoorOffset>> = runCatching {
        val response = apiService.executeRpc(currentUserFor(teid), "Proc_PosOffset", listOf(0, teid))
        requireOk(response)
        response.toRows().map(IndoorOffset::fromRow)
    }

    suspend fun addIndoorOffset(
        teid: String, source: DevicePosition, targetLon: Double, targetLat: Double, address: String
    ): Result<Boolean> = runCatching {
        require(source.rawLat in -85.0..85.0 && source.rawLon in -180.0..180.0 &&
            (source.rawLat != 0.0 || source.rawLon != 0.0)) { "设备没有有效的原始定位" }
        require(targetLat in -85.0..85.0 && targetLon in -180.0..180.0) { "修正坐标无效" }
        val meters = distanceMeters(source.rawLat, source.rawLon, targetLat, targetLon)
        require(meters >= 10) { "修正位置与原位置至少相距 10 米" }
        val radius = when {
            meters < 100 -> 50
            meters < 1_000 -> 100
            meters < 10_000 -> 500
            meters < 100_000 -> 1_000
            else -> 2_000
        }
        val name = "${kotlin.math.floor(source.rawLon * 1e7) / 1e7}_${kotlin.math.floor(source.rawLat * 1e7) / 1e7}"
        val response = apiService.executeRpc(currentUserFor(teid), "Proc_PosOffset",
            listOf(1, teid, name, address.ifBlank { name }, 1,
                source.rawLon, source.rawLat, radius, targetLon, targetLat))
        requireOk(response)
        isActionSuccessful(response)
    }

    suspend fun deleteIndoorOffset(teid: String, name: String): Result<Boolean> = runCatching {
        val response = apiService.executeRpc(currentUserFor(teid), "Proc_PosOffset", listOf(3, teid, name))
        requireOk(response)
        isActionSuccessful(response)
    }

    suspend fun saveWifiMark(mark: WifiMark): Result<Boolean> = runCatching {
        require(mark.name.isNotBlank() && mark.wifi.isNotBlank()) { "请填写名称和 Wi-Fi 标识" }
        val response = apiService.executeRpc(
            currentUserFor(mark.teid), "Proc_WifiMark",
            listOf(1, mark.teid, mark.timeSeconds, mark.longitude, mark.latitude,
                mark.wifi, mark.near, mark.name, mark.address)
        )
        requireOk(response)
        val saved = isActionSuccessful(response)
        if (saved) sendDeviceCommand(mark.teid, "SyncWifiMarks")
        saved
    }

    suspend fun deleteWifiMark(mark: WifiMark): Result<Boolean> = runCatching {
        val response = apiService.executeRpc(
            currentUserFor(mark.teid), "Proc_WifiMark",
            listOf(2, mark.teid, mark.timeSeconds, mark.longitude, mark.latitude,
                mark.wifi, mark.near, mark.name)
        )
        requireOk(response)
        val deleted = isActionSuccessful(response)
        if (deleted) sendDeviceCommand(mark.teid, "SyncWifiMarks")
        deleted
    }

    suspend fun addDevice(teid: String, name: String, sim: String, password: String): Result<Boolean> = runCatching {
        val user = _currentUser.value ?: error("尚未登录")
        require(_loginType.value == 0) { "当前账号不能添加设备" }
        require(teid.isNotBlank() && name.isNotBlank()) { "请输入设备编号和名称" }
        val response = apiService.executeRpc(
            user, "Proc_AddCar",
            listOf(user, teid, name, sim, "", "", "", "", "", "", password, 0, 0, "", "", "")
        )
        requireOk(response)
        isActionSuccessful(response)
    }

    suspend fun deleteDevice(teid: String): Result<Boolean> = runCatching {
        val user = _currentUser.value ?: error("尚未登录")
        require(_loginType.value == 0) { "当前账号不能删除设备" }
        val response = apiService.executeRpc(user, "Proc_DelCar", listOf(user, teid))
        requireOk(response)
        isActionSuccessful(response)
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

    private fun distanceMeters(latA: Double, lonA: Double, latB: Double, lonB: Double): Double {
        val lat1 = Math.toRadians(latA)
        val lat2 = Math.toRadians(latB)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(lonB - lonA)
        val h = kotlin.math.sin(dLat / 2).let { it * it } +
            kotlin.math.cos(lat1) * kotlin.math.cos(lat2) *
            kotlin.math.sin(dLon / 2).let { it * it }
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

    suspend fun getUserShapes(): Result<List<GeofenceItem>> = runCatching {
        val user = _currentUser.value ?: error("尚未登录")
        val response = apiService.executeRpc(user, "Proc_GetUserShape", listOf(user))
        requireOk(response)
        response.toRows().map(GeofenceItem::fromRow).filter { it.alarmType == 0 }
    }

    suspend fun deleteUserShape(name: String): Result<Boolean> = runCatching {
        val user = _currentUser.value ?: error("尚未登录")
        val response = apiService.executeRpc(user, "Proc_DelUserShape", listOf(user, name))
        requireOk(response)
        isActionSuccessful(response)
    }

    suspend fun addUserShape(name: String, geometry: String): Result<Boolean> = runCatching {
        val user = _currentUser.value ?: error("尚未登录")
        require(name.isNotBlank() && geometry.isNotBlank()) { "形状名称与顶点不能为空" }
        val response = apiService.executeRpc(
            user, "Proc_AddUserShape", listOf(user, name, 3, 200, geometry, 0, 0, "")
        )
        requireOk(response)
        isActionSuccessful(response)
    }

    suspend fun updateUserShape(name: String, geometry: String): Result<Boolean> = runCatching {
        val user = _currentUser.value ?: error("尚未登录")
        require(name.isNotBlank() && geometry.isNotBlank()) { "形状名称与顶点不能为空" }
        val response = apiService.executeRpc(
            user, "Proc_ModUserFence", listOf(user, name, 3, 200, geometry, 0, 0, "")
        )
        requireOk(response)
        isActionSuccessful(response)
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
            isActionSuccessful(response)
        }
    }

    suspend fun deleteUserFence(teid: String, name: String, alarmType: Int): Result<Boolean> {
        val user = _currentUser.value ?: return Result.failure(IllegalStateException("尚未登录"))
        return runCatching {
            val pc = _loginType.value == 0
            val response = apiService.executeRpc(
                user,
                if (pc) "Proc_DelFenceFromUser" else "Proc_CarFence",
                if (pc) listOf(user, name) else listOf(2, teid, alarmType, name)
            )
            requireOk(response)
            isActionSuccessful(response)
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
            val pc = _loginType.value == 0
            val params = if (bind) {
                listOf(fenceUserFor(teid), teid, name, alarmType)
            } else if (pc) {
                listOf(user, teid, name)
            } else {
                listOf(4, teid, alarmType, name)
            }
            val response = apiService.executeRpc(
                user,
                if (bind) "Proc_AddFenceToUserCar" else if (pc) "Proc_DelFenceFromUserCar" else "Proc_CarFence",
                params
            )
            requireOk(response)
            isActionSuccessful(response)
        }
    }

    private fun fenceUserFor(teid: String): String =
        if (_loginType.value == 0) _currentUser.value ?: teid else teid.takeLast(15)

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

    suspend fun getTrackAlarms(
        teid: String,
        startTimeSec: Long,
        endTimeSec: Long,
        offset: Int = 0,
        limit: Int = 2000
    ): Result<List<AlarmItem>> {
        val user = _currentUser.value ?: return Result.failure(IllegalStateException("尚未登录"))
        return runCatching {
            val response = apiService.executeRpc(
                user,
                "Proc_GetTrackAlarm",
                listOf(teid, startTimeSec, endTimeSec, offset, limit),
                field = "strTEID,nTime,nAlarmState,strOther"
            )
            requireOk(response)
            response.toRows().mapNotNull(AlarmItem::fromRow)
        }
    }

    suspend fun sendDeviceCommand(
        teid: String, action: String, parameter: String = "", confirmationPassword: String = ""
    ): Result<Unit> {
        val user = _currentUser.value ?: return Result.failure(IllegalStateException("尚未登录"))
        val guarded = setOf(
            "CutFuel", "RestoreFuel", "CutCircuit", "RestoreCircuit", "ArmDevice", "DisarmDevice",
            "TurnOnDevice", "TurnOffDevice", "LockDoor", "UnlockDoor", "FactoryReset", "Shutdown"
        )
        if (action in guarded && confirmationPassword != _currentPassword.value) {
            return Result.failure(IllegalArgumentException("请确认当前登录密码"))
        }
        val code = when (action) {
            "CDeviceRestart" -> "0x0208"
            "EmergencyLocate" -> "0x0281"
            "SetTrackInterval", "SetUploadTrackInterval", "SetPositionMode" -> "0x020A"
            "SetSavingPowerInterval" -> if ('&' in parameter) "0x0270" else "0x020A"
            "SetShakeSensitivity" -> "0x025D"
            "SetPowerSavingMode" -> "0x0247"
            "TakePhoto" -> "0x0203"
            "RecordBack" -> "0x0266"
            "RecordMode" -> "0x0267"
            "StopAlarm" -> "0x0227"
            "UrgentMode" -> "0x0272"
            "SendText" -> "0x020E"
            "SendSMS" -> "0x024C"
            "SetParkInterval" -> "0x0232"
            "SetOverSpeed" -> "0x020B"
            "SetSosNumbers" -> "0x020C"
            "SetCenterNumber" -> "0x0225"
            "SetSleepMode" -> "0x0284"
            "SetShutdownSchedule" -> "0x0261"
            "SetShutdownDelay" -> "0x026B"
            "Shutdown" -> "0x0268"
            "RemotePower" -> "0x026E"
            "SetPowerSavingInterval" -> "0x0270"
            "AuxiliaryPosition" -> "0x0271"
            "SetSceneMode" -> "0x0275"
            "RejectUnknownCall" -> "0x0276"
            "CollectSMS" -> "0x027C"
            "PhoneMute" -> "0x0282"
            "SetAlarmClock" -> "0x027A"
            "ClassDisable" -> "0x027B"
            "Phonebook" -> "0x0245"
            "ShakeAlarm" -> "0x026D"
            "TeardownAlarm" -> "0x026F"
            "ShakeMode" -> "0x025E"
            "LowBatteryAlarm" -> "0x0277"
            "SosAlarm" -> "0x0278"
            "FallAlarm" -> "0x0279"
            "IgnitionAlarm" -> "0x028E"
            "CutFuel" -> "0x0204"
            "RestoreFuel" -> "0x0205"
            "CutCircuit" -> "0x0206"
            "RestoreCircuit" -> "0x0207"
            "ArmDevice" -> "0x022B"
            "DisarmDevice" -> "0x022C"
            "TurnOnDevice" -> "0x021C"
            "TurnOffDevice" -> "0x021D"
            "LockDoor" -> "0x021A"
            "UnlockDoor" -> "0x021B"
            "ControlLock" -> "0x0237"
            "ReleaseLock" -> "0x0236"
            "SetExpel" -> "0x0283"
            "SoundLight" -> "0x026C"
            "RestoreTrumpet" -> "0x0273"
            "RestoreKeypress" -> "0x0274"
            "FactoryReset" -> "0x0209"
            "GetDeviceConfig" -> "0x024A"
            "ActivateAI" -> "0x028B"
            "JumpRope" -> "0x028F"
            "FindBluetooth" -> "0x028D"
            "VoipPrepare" -> "0x0285"
            "SendVoice" -> "0x028C"
            "ScanWifi" -> "0x0290"
            "SyncWifiMarks" -> "0x0291"
            else -> return Result.failure(IllegalArgumentException("不支持的设备命令"))
        }
        return try {
            val devicePayload = if (action == "SetExpel") parameter.substringBefore(';') else parameter
            when (apiService.executeCommand(user, teid, code, devicePayload)) {
                1 -> {
                    synchronizeCommand(teid, action, parameter)
                    Result.success(Unit)
                }
                2 -> Result.failure(IllegalStateException("设备不支持该指令"))
                else -> Result.failure(IllegalStateException("指令下发失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun synchronizeCommand(teid: String, action: String, value: String) {
        val parameterKeys = mapOf(
            "SetTrackInterval" to "SET_INTERVAL", "SetUploadTrackInterval" to "TRACK_INTERVAL",
            "SetPositionMode" to "POS_MODE", "SetSavingPowerInterval" to "SAVINGPOWER_INTERVAL",
            "SetPowerSavingInterval" to "POWERSAVING_SEC", "SetParkInterval" to "PARK_INTERVAL",
            "SetShakeSensitivity" to "SHAKE_SEN", "SetPowerSavingMode" to "POWER_SMODE",
            "SetSleepMode" to "SUPER_STANDBY_MODE", "ShakeAlarm" to "SHAKE_ALARM",
            "ShakeMode" to "SHAKE_ONOFF",
            "SetSosNumbers" to "ALARM_TEL", "SetCenterNumber" to "CENTER_TEL",
            "SetOverSpeed" to "OVERSPEED", "SetShutdownSchedule" to "AUTO_SU_SD",
            "SetShutdownDelay" to "STARTUP_SHUTDOWN_DELAY", "RemotePower" to "DEVICE_ONOFF",
            "SoundLight" to "SOUND_LIGHT", "TeardownAlarm" to "TEAR_ALARM",
            "SetSceneMode" to "SCENE_MODE", "RejectUnknownCall" to "REJECT_UNKNOWN_CALL",
            "CollectSMS" to "COLLECT_SMS", "PhoneMute" to "SET_PHONE_MUTE",
            "LowBatteryAlarm" to "SET_LOW_BATTERY_ALARM", "SosAlarm" to "SET_SOS_ALARM",
            "FallAlarm" to "SET_FALL_ALARM", "IgnitionAlarm" to "IGNITION_ALARM",
            "RestoreTrumpet" to "CTRL_TRUMPET", "RestoreKeypress" to "CTRL_KEYPRESS",
            "SetExpel" to "EXPEL_STRENGTH", "ActivateAI" to "ACTIVATE_AI"
        )
        val modeIndex = when (action) {
            "SetPositionMode" -> mapOf("10" to -1, "30" to 0, "300" to 1, "86401" to 2)[value]
            "SetUploadTrackInterval" -> mapOf("10" to -1, "30" to 0, "600" to 1, "3600" to 2, "43200" to 3)[value]
            "SetSavingPowerInterval" -> mapOf("10" to -1, "30" to 0, "3&600" to 1, "3&3600" to 2, "3&43200" to 3)[value]
            else -> null
        }
        val flag = when {
            modeIndex == -1 -> 131072
            modeIndex == 0 -> 8388608
            action == "SetPositionMode" && modeIndex == 2 -> 1048576
            action == "SetUploadTrackInterval" && modeIndex == 1 -> 8388608
            modeIndex == 3 -> 1048576
            modeIndex != null -> 262144
            action == "SetPowerSavingInterval" -> when (value.substringBefore('&')) {
                "1" -> 4194304; "2" -> 2097152; "3" -> 1048576; else -> null
            }
            action == "SetPowerSavingMode" -> when (value) {
                "21", "22" -> 2097152; "20" -> 8388608; else -> null
            }
            else -> null
        }
        if (flag != null) {
            val response = apiService.executeRpc(currentUserFor(teid), "Proc_SetCarConfig", listOf(teid, flag, 0, 16515072))
            requireOk(response)
            check(isActionSuccessful(response)) { "设备已接收指令，但模式状态保存失败，请刷新核对" }
        }
        val config = when (action) {
            "SetPowerSavingMode", "SetShutdownSchedule" ->
                Triple(16777216, if (action == "SetPowerSavingMode" && value != "22" ||
                    action == "SetShutdownSchedule" && value.startsWith("0&")) 1 else 0, 0)
            "SetSleepMode" -> Triple(65536, if (value == "1") 0 else 1, 2164195328L)
            "PhoneMute" -> Triple(4096, if (value == "1") 1 else 0, 0)
            else -> null
        }
        if (config != null) {
            val (bit, inverse, mask) = config
            val response = apiService.executeRpc(currentUserFor(teid), "Proc_SetCarConfig", listOf(teid, bit, inverse, mask))
            requireOk(response)
            check(isActionSuccessful(response)) { "设备已接收指令，但配置状态保存失败，请刷新核对" }
        }
        if (action == "RemotePower") {
            val response = apiService.executeRpc(currentUserFor(teid), "Proc_DeviceOnOff", listOf(teid, value.toInt()))
            requireOk(response)
            check(isActionSuccessful(response)) { "设备已接收指令，但开关机状态保存失败，请刷新核对" }
        }
        val key = parameterKeys[action] ?: return
        val stored = modeIndex?.toString() ?: when (action) {
            "RemotePower" -> if (value == "1") "0" else "1"
            "SetPowerSavingMode" -> mapOf("21" to "0", "22" to "1", "20" to "2")[value] ?: value
            "SetSleepMode" -> if (value == "1") "1" else "2"
            "ShakeAlarm", "ShakeMode", "TeardownAlarm", "LowBatteryAlarm", "SosAlarm",
            "FallAlarm", "IgnitionAlarm" -> if (value == "1") "0" else "1"
            "SetSosNumbers" -> value.removePrefix("&&").replace('&', ',')
            "SetShutdownSchedule" -> value.substringAfter('&').replace('&', ',').let {
                "${if (value.startsWith("1&")) 1 else 0},$it"
            }
            "SetShutdownDelay" -> value.split('&').let { parts ->
                "${parts.firstOrNull() == "1"},${parts.getOrNull(1) ?: "180"},${parts.getOrNull(2) ?: "3600"}"
            }
            "SetExpel" -> value.substringBefore(';') + ";" + value.substringAfter(';', "")
            else -> value
        }
        parameterMutex.withLock {
            val before = apiService.executeRpc(currentUserFor(teid), "Proc_Driver", listOf(0, teid))
            requireOk(before)
            val row = before.toRows().firstOrNull() ?: error("设备已接收指令，但无法读取参数，请刷新核对")
            val old = row["strParamInfo"].orEmpty().split('\u0010')
                .filter { it.isNotBlank() && it.substringBefore('\u001f') != key }
            val updated = (old + "$key\u001f$stored").joinToString("\u0010")
            val saved = apiService.executeRpc(currentUserFor(teid), "Proc_Driver", listOf(3, teid, "", "", updated))
            requireOk(saved)
            check(isActionSuccessful(saved)) { "设备已接收指令，但参数保存失败，请刷新核对" }
        }
    }

    private fun currentUserFor(teid: String): String = _currentUser.value ?: teid

    private fun isLoginAccepted(response: ApiResponse): Boolean =
        response.isResultOk != 0 &&
            response.toRows().firstOrNull()?.get("Result")?.let { it == "1" } != false

    private fun requireOk(response: ApiResponse) {
        if (response.isResultOk == 0) throw IllegalStateException("服务器拒绝了请求")
    }

    private fun isActionSuccessful(response: ApiResponse): Boolean =
        response.toRows().firstOrNull()?.entries?.firstOrNull { it.key.equals("Result", true) }
            ?.value == "1"
}

