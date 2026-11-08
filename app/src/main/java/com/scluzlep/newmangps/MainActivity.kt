package com.scluzlep.newmangps

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.scluzlep.newmangps.data.model.DeviceDetail
import com.scluzlep.newmangps.data.model.DevicePosition
import com.scluzlep.newmangps.data.model.DeviceSummary
import com.scluzlep.newmangps.data.model.GeofenceItem
import com.scluzlep.newmangps.data.model.SubUserInfo
import com.scluzlep.newmangps.data.model.UserRelation
import com.scluzlep.newmangps.data.model.DeviceModelCapability
import com.scluzlep.newmangps.data.model.AlarmItem
import com.scluzlep.newmangps.data.api.AmapPlace
import com.scluzlep.newmangps.data.util.CoordinateTransform
import com.scluzlep.newmangps.data.repository.NewsmyRepository
import com.scluzlep.newmangps.data.LoginSessionStore
import com.scluzlep.newmangps.data.SavedLogin
import com.scluzlep.newmangps.data.AppSettingsStore
import com.scluzlep.newmangps.data.push.AlarmPollingWorker
import com.scluzlep.newmangps.ui.screens.device.DeviceDashboardScreen
import com.scluzlep.newmangps.ui.screens.device.DeviceManagementScreen
import com.scluzlep.newmangps.ui.screens.device.DeviceCommandsScreen
import com.scluzlep.newmangps.ui.screens.device.DeviceListScreen
import com.scluzlep.newmangps.ui.screens.device.DeviceDetailScreen
import com.scluzlep.newmangps.ui.screens.device.DeviceIconScreen
import com.scluzlep.newmangps.ui.screens.device.PhonebookScreen
import com.scluzlep.newmangps.ui.screens.device.ScheduleSettingsScreen
import com.scluzlep.newmangps.ui.screens.device.MonitoringScreen
import com.scluzlep.newmangps.ui.screens.device.PublicDeviceInfoScreen
import com.scluzlep.newmangps.ui.screens.device.BulkImportScreen
import com.scluzlep.newmangps.ui.screens.device.SimCardScreen
import com.scluzlep.newmangps.ui.screens.device.SimWhitelistScreen
import com.scluzlep.newmangps.ui.screens.device.DeviceParametersScreen
import com.scluzlep.newmangps.ui.screens.device.WifiMarksScreen
import com.scluzlep.newmangps.ui.screens.account.AccountManagementScreen
import com.scluzlep.newmangps.ui.screens.account.SubUserPrivilegeScreen
import com.scluzlep.newmangps.ui.screens.account.MembershipScreen
import com.scluzlep.newmangps.ui.screens.account.SupportScreen
import com.scluzlep.newmangps.ui.screens.account.SystemSettingsScreen
import com.scluzlep.newmangps.ui.screens.alarm.AlarmHistoryScreen
import com.scluzlep.newmangps.ui.screens.login.LoginScreen
import com.scluzlep.newmangps.ui.screens.map.TrackingMapScreen
import com.scluzlep.newmangps.ui.screens.map.AreaMeasureScreen
import com.scluzlep.newmangps.ui.screens.map.IndoorOffsetScreen
import com.scluzlep.newmangps.ui.screens.track.TrackReplayScreen
import com.scluzlep.newmangps.ui.screens.media.DeviceMediaScreen
import com.scluzlep.newmangps.ui.screens.media.VoiceSendScreen
import com.scluzlep.newmangps.ui.screens.report.ReportsScreen
import com.scluzlep.newmangps.ui.screens.report.StatsScreen
import com.scluzlep.newmangps.ui.screens.health.HealthScreen
import com.scluzlep.newmangps.ui.theme.HyperTheme
import com.scluzlep.newmangps.ui.AppLanguageStore
import com.scluzlep.newmangps.ui.LocalAppLanguage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val alarmIntent = kotlinx.coroutines.flow.MutableStateFlow<Intent?>(null)
    private val repository = NewsmyRepository()
    private val languageStore by lazy { AppLanguageStore(this) }
    private val settingsStore by lazy { AppSettingsStore(this) }
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        alarmIntent.value = intent
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        alarmIntent.value = intent
        enableEdgeToEdge()

        setContent {
            val language by languageStore.state.collectAsState()
            val settings by settingsStore.state.collectAsState()
            LaunchedEffect(Unit) { languageStore.loadSaved() }
            CompositionLocalProvider(LocalAppLanguage provides language) {
            HyperTheme {
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()
                val sessionStore = remember { LoginSessionStore(this@MainActivity) }

                var currentImei by remember { mutableStateOf("") }
                var selectedServer by remember { mutableStateOf(repository.currentServer) }
                var devices by remember { mutableStateOf<List<DeviceSummary>>(emptyList()) }
                var positionState by remember { mutableStateOf<DevicePosition?>(null) }
                var detailState by remember { mutableStateOf<DeviceDetail?>(null) }
                var fencesState by remember { mutableStateOf<List<GeofenceItem>>(emptyList()) }
                var fencesFailed by remember { mutableStateOf(false) }
                var alarmHistory by remember { mutableStateOf<List<AlarmItem>>(emptyList()) }
                var alarmHistoryError by remember { mutableStateOf<String?>(null) }
                var alarmHistoryLoading by remember { mutableStateOf(false) }
                var alarmPeriodHours by remember { mutableIntStateOf(24) }
                var accountUsers by remember { mutableStateOf<List<SubUserInfo>>(emptyList()) }
                var userRelations by remember { mutableStateOf<List<UserRelation>>(emptyList()) }
                var modelCapabilities by remember { mutableStateOf<List<DeviceModelCapability>>(emptyList()) }
                var commandPermissionDisabled by remember { mutableStateOf(false) }
                var accountProfile by remember { mutableStateOf<SubUserInfo?>(null) }
                var accountLoading by remember { mutableStateOf(false) }
                var deviceListLoading by remember { mutableStateOf(false) }
                var deviceListError by remember { mutableStateOf<String?>(null) }
                var accountError by remember { mutableStateOf<String?>(null) }
                var addressText by remember { mutableStateOf("") }
                var mapPlaces by remember { mutableStateOf<List<AmapPlace>>(emptyList()) }
                var selectedPlace by remember { mutableStateOf<AmapPlace?>(null) }
                var mapFocusPoint by remember { mutableStateOf<CoordinateTransform.LatLng?>(null) }
                var otherPositions by remember { mutableStateOf<List<DevicePosition>>(emptyList()) }
                var showAllDevices by remember { mutableStateOf(false) }
                var isLoginLoading by remember { mutableStateOf(false) }
                var loginError by remember { mutableStateOf<String?>(null) }
                var isRefreshing by remember { mutableStateOf(false) }
                var dataError by remember { mutableStateOf<String?>(null) }
                var showFences by remember { mutableStateOf(false) }
                var showFenceEditor by remember { mutableStateOf(false) }
                var fenceName by remember { mutableStateOf("") }
                var fenceRadius by remember { mutableStateOf("500") }
                var fenceType by remember { mutableIntStateOf(1) }
                var fenceAlarmType by remember { mutableIntStateOf(3) }
                var fenceVertices by remember { mutableStateOf<List<CoordinateTransform.LatLng>>(emptyList()) }
                var editingFence by remember { mutableStateOf<GeofenceItem?>(null) }
                var fenceCenter by remember { mutableStateOf("") }
                var selectedFencePoint by remember { mutableStateOf<com.scluzlep.newmangps.data.util.CoordinateTransform.LatLng?>(null) }
                var pendingCommand by remember { mutableStateOf<String?>(null) }
                var showShareDialog by remember { mutableStateOf(false) }
                var showDeviceEditor by remember { mutableStateOf(false) }
                var deviceEditFields by remember { mutableStateOf(List(7) { "" }) }
                var shareEnabled by remember { mutableStateOf(false) }
                var shareLoading by remember { mutableStateOf(false) }
                var shareError by remember { mutableStateOf<String?>(null) }
                var commandParameter by remember { mutableStateOf("") }
                var loadGeneration by remember { mutableIntStateOf(0) }

                suspend fun reloadCommandCapabilities() {
                    modelCapabilities = emptyList()
                    commandPermissionDisabled = true
                    modelCapabilities = repository.getDeviceModels().getOrNull().orEmpty()
                    commandPermissionDisabled = repository.getUserPrivilege(
                        repository.currentUser.value.orEmpty()
                    ).getOrNull()?.flags?.let { it and 256 != 0 } ?: false
                }

                fun canSendMapCommand(command: String): Boolean {
                    if (detailState?.isExpired != false || commandPermissionDisabled) return false
                    val component = when (command) {
                        "EmergencyLocate" -> "CSetLocateMode"
                        "CDeviceRestart" -> "CDeviceRestart"
                        "SetTrackInterval" -> "CSetInterval"
                        "SetShakeSensitivity" -> "CSetShakeLevel"
                        "SetPowerSavingMode" -> "CSetPowerMode"
                        else -> return false
                    }
                    val model = modelCapabilities.firstOrNull {
                        it.value.equals(detailState?.deviceModel, ignoreCase = true)
                    } ?: modelCapabilities.firstOrNull { it.value == "0" }
                    return component in (model?.commands ?: emptySet())
                }

                fun loadDeviceData(imei: String) {
                    if (imei.isBlank()) return
                    val generation = ++loadGeneration
                    scope.launch {
                        isRefreshing = true
                        val posResult = repository.getLastPosition(imei)
                        if (settings.quickLoadDevices && currentImei == imei && generation == loadGeneration) {
                            positionState = posResult.getOrNull()
                        }
                        val detailResult = repository.getDeviceDetail(imei)
                        val fenceResult = repository.getGeofences(imei)
                        if (currentImei != imei || generation != loadGeneration) return@launch
                        val loadedPosition = posResult.getOrNull()
                        if (loadedPosition == null ||
                            loadedPosition.timeSeconds >= (positionState?.timeSeconds ?: 0)
                        ) positionState = loadedPosition
                        detailState = detailResult.getOrNull()
                        fencesState = fenceResult.getOrNull().orEmpty()
                        fencesFailed = fenceResult.isFailure
                        dataError = listOfNotNull(
                            posResult.exceptionOrNull()?.message,
                            detailResult.exceptionOrNull()?.message,
                            fenceResult.exceptionOrNull()?.message
                        )
                            .distinct()
                            .joinToString("；")
                            .ifBlank { null }
                        if (loadedPosition == null || settings.hideAddress) {
                            addressText = ""
                        } else {
                            val positionTime = loadedPosition.timeSeconds
                            scope.launch {
                                repository.reverseGeocode(loadedPosition).getOrNull()?.let { address ->
                                    if (currentImei == imei &&
                                        positionState?.timeSeconds == positionTime
                                    ) addressText = address
                                }
                            }
                        }
                        isRefreshing = false
                    }
                }

                fun sendCommand(command: String, parameter: String = "") {
                    if (currentImei.isBlank()) return
                    if (!canSendMapCommand(command)) {
                        Toast.makeText(this@MainActivity, language.text("CommandUnavailable", "当前设备无法下发此指令"), Toast.LENGTH_LONG).show()
                        return
                    }
                    scope.launch {
                        val result = repository.sendDeviceCommand(currentImei, command, parameter)
                        val message = result.fold(
                            onSuccess = { language.text("CommandAccepted", "服务器已受理指令") },
                            onFailure = { it.message ?: language.text("CommandSendFailed", "指令下发失败") }
                        )
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                    }
                }

                fun loadAccountUsers() {
                    scope.launch {
                        accountLoading = true
                        if (repository.loginType == 0) {
                            val result = repository.getSubUsers()
                            accountUsers = result.getOrNull().orEmpty()
                            accountError = result.exceptionOrNull()?.message
                            userRelations = repository.getUserTree().getOrNull().orEmpty()
                        } else {
                            accountUsers = emptyList()
                            userRelations = emptyList()
                            accountError = null
                        }
                        accountProfile = if (repository.loginType != 2) {
                            repository.getCurrentProfile().getOrNull()
                        } else null
                        accountLoading = false
                    }
                }

                fun loadAlarmHistory(hours: Int = alarmPeriodHours) {
                    alarmPeriodHours = hours
                    scope.launch {
                        alarmHistoryLoading = true
                        alarmHistoryError = null
                        val end = System.currentTimeMillis() / 1000
                        val result = repository.getTrackAlarms(currentImei, end - hours * 3600L, end)
                        alarmHistory = result.getOrNull().orEmpty()
                        alarmHistoryError = result.exceptionOrNull()?.message
                        alarmHistoryLoading = false
                    }
                }

                fun refreshDevices() {
                    scope.launch {
                        deviceListLoading = true
                        val result = repository.getDevices()
                        if (result.isSuccess) {
                            devices = result.getOrThrow()
                            val alarmUser = repository.currentUser.value.orEmpty()
                            if (alarmUser.isNotBlank()) {
                                if (settings.pushAlarm && devices.isNotEmpty()) AlarmPollingWorker.schedule(
                                    this@MainActivity, alarmUser, devices.map { it.teid }, repository.currentServer
                                ) else AlarmPollingWorker.cancel(this@MainActivity, alarmUser)
                            }
                            if (devices.none { it.teid == currentImei }) {
                                currentImei = devices.firstOrNull()?.teid.orEmpty()
                                loadDeviceData(currentImei)
                            }
                        }
                        deviceListError = result.exceptionOrNull()?.message
                        deviceListLoading = false
                    }
                }

                fun enterDashboard(account: String, available: List<DeviceSummary>) {
                    devices = available
                    val requested = alarmIntent.value?.takeIf {
                        it.getStringExtra("alarm_server") == repository.currentServer &&
                            it.getStringExtra("alarm_user") == repository.currentUser.value
                    }?.getStringExtra("alarm_teid")
                    currentImei = available.firstOrNull { it.teid == requested }?.teid ?: available.first().teid
                    loadDeviceData(currentImei)
                    scope.launch {
                        reloadCommandCapabilities()
                    }
                    if (settings.pushAlarm) AlarmPollingWorker.schedule(
                        this@MainActivity, repository.currentUser.value ?: account,
                        available.map { it.teid }, repository.currentServer
                    )
                    if (Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
                    ) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    val previousRoute = navController.currentDestination?.route ?: "login"
                    navController.navigate("tracking") { popUpTo(previousRoute) { inclusive = true } }
                }

                val incomingAlarm by alarmIntent.collectAsState()
                LaunchedEffect(incomingAlarm, devices) {
                    val incoming = incomingAlarm ?: return@LaunchedEffect
                    val target = incoming.getStringExtra("alarm_teid") ?: return@LaunchedEffect
                    if (repository.currentUser.value != incoming.getStringExtra("alarm_user") ||
                        repository.currentServer != incoming.getStringExtra("alarm_server")) return@LaunchedEffect
                    if (devices.any { it.teid == target }) {
                        currentImei = target
                        positionState = null
                        detailState = null
                        addressText = ""
                        mapFocusPoint = null
                        selectedPlace = null
                        loadDeviceData(target)
                        navController.navigate("tracking") { launchSingleTop = true }
                        alarmIntent.value = null
                    }
                }

                LaunchedEffect(Unit) {
                    val saved = sessionStore.load()
                    if (saved == null) {
                        navController.navigate("login") { popUpTo("startup") { inclusive = true } }
                        return@LaunchedEffect
                    }
                    repository.restoreServer(saved.server)
                    selectedServer = repository.currentServer
                    isLoginLoading = true
                    val result = if (saved.isImei) repository.loginIMEI(saved.account, saved.password)
                    else repository.loginUser(saved.account, saved.password)
                    val available = if (result.isSuccess) {
                        if (saved.isImei) Result.success(listOf(DeviceSummary(saved.account, saved.account)))
                        else repository.getDevices()
                    } else Result.failure(result.exceptionOrNull() ?: IllegalStateException("自动登录失败"))
                    if (available.isSuccess && available.getOrNull().orEmpty().isNotEmpty()) {
                        enterDashboard(saved.account, available.getOrThrow())
                    } else {
                        loginError = available.exceptionOrNull()?.message ?: language.text("AutoLoginFailed", "自动登录失败")
                        navController.navigate("login") { popUpTo("startup") { inclusive = true } }
                    }
                    isLoginLoading = false
                }

                NavHost(
                    navController = navController,
                    startDestination = "startup"
                ) {
                    composable("startup") {
                        Column(Modifier.fillMaxWidth().padding(32.dp)) {
                            Text(language.text("RestoringLogin", "正在恢复登录"))
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        }
                    }
                    composable("login") {
                        LoginScreen(
                            serverName = repository.availableServers.firstOrNull { it.first == selectedServer }?.second ?: selectedServer,
                            servers = repository.availableServers,
                            onServerSelected = {
                                repository.restoreServer(it)
                                selectedServer = repository.currentServer
                            },
                            onLoginClick = { account, password, isImei ->
                                scope.launch {
                                    isLoginLoading = true
                                    loginError = null
                                    if (account.isBlank() || password.isBlank()) {
                                        loginError = language.text("EnterCredentials", "请输入账号和密码")
                                        isLoginLoading = false
                                        return@launch
                                    }
                                    val result = if (isImei) {
                                        repository.loginIMEI(account, password)
                                    } else {
                                        repository.loginUser(account, password)
                                    }
                                    val available = if (result.isSuccess) {
                                        if (isImei) Result.success(listOf(DeviceSummary(account, account)))
                                        else repository.getDevices()
                                    } else {
                                        Result.failure(result.exceptionOrNull() ?: IllegalStateException("登录失败"))
                                    }
                                    if (available.isSuccess && available.getOrNull().orEmpty().isNotEmpty()) {
                                        runCatching {
                                            sessionStore.save(SavedLogin(account, password, isImei, repository.currentServer))
                                        }.onFailure {
                                            Toast.makeText(this@MainActivity, language.text("SaveLoginFailed", "登录成功，但自动登录保存失败"), Toast.LENGTH_LONG).show()
                                        }
                                        enterDashboard(account, available.getOrThrow())
                                    } else {
                                        loginError = available.exceptionOrNull()?.message ?: language.text("NoAccountDevices", "该账号下没有设备")
                                    }
                                    isLoginLoading = false
                                }
                            },
                            isLoading = isLoginLoading,
                            errorMessage = loginError
                        )
                    }

                    composable("dashboard") {
                        DeviceDashboardScreen(
                            position = positionState,
                            detail = detailState,
                            model = modelCapabilities.firstOrNull {
                                it.value.equals(detailState?.deviceModel, ignoreCase = true)
                            } ?: modelCapabilities.firstOrNull { it.value == "0" },
                            devices = devices,
                            selectedTeid = currentImei,
                            canManageAccount = true,
                            fences = fencesState,
                            fencesFailed = fencesFailed,
                            address = addressText,
                            isRefreshing = isRefreshing,
                            errorMessage = dataError,
                            onRefresh = { loadDeviceData(currentImei) },
                            onSelectDevice = { device ->
                                currentImei = device.teid
                                mapPlaces = emptyList()
                                mapFocusPoint = null
                                selectedPlace = null
                                positionState = null
                                addressText = ""
                                detailState = null
                                fencesState = emptyList()
                                fencesFailed = false
                                dataError = null
                                loadDeviceData(device.teid)
                            },
                            onNavigateTracking = { navController.navigate("tracking") },
                            onNavigateTrackHistory = { navController.navigate("replay") },
                            onNavigateAlarms = { loadAlarmHistory(); navController.navigate("alarms") },
                            onNavigateGeofence = { showFences = true },
                            onNavigateMedia = { navController.navigate("media") },
                            onNavigateReports = { navController.navigate("reports") },
                            onNavigateHealth = { navController.navigate("health") },
                            onNavigateArea = { navController.navigate("area") },
                            onNavigateDeviceList = { navController.navigate("device_list") },
                            onNavigateMonitoring = { navController.navigate("monitoring") },
                            onOpenRadar = {
                                val detail = detailState
                                if (detail?.isExpired == true) {
                                    Toast.makeText(this@MainActivity, language.text("DeviceServiceExpired", "设备服务已到期"), Toast.LENGTH_LONG).show()
                                } else scope.launch {
                                    val name = detail?.carNum?.ifBlank { currentImei } ?: currentImei
                                    val uri = repository.getRadarUri(currentImei, name)
                                    if (uri.isSuccess) {
                                        repository.sendDeviceCommand(currentImei, "FindBluetooth", "1")
                                        runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri.getOrThrow())) }
                                            .onFailure { Toast.makeText(this@MainActivity, "${language.text("OpenWeChatFailed", "无法打开微信")}：${it.message}", Toast.LENGTH_LONG).show() }
                                    } else Toast.makeText(this@MainActivity, uri.exceptionOrNull()?.message ?: language.text("RadarUnavailable", "雷达不可用"), Toast.LENGTH_LONG).show()
                                }
                            },
                            onVoiceCall = {
                                val detail = detailState
                                if (detail?.isExpired == true) {
                                    Toast.makeText(this@MainActivity, language.text("DeviceServiceExpired", "设备服务已到期"), Toast.LENGTH_LONG).show()
                                } else scope.launch {
                                    val name = detail?.carNum?.ifBlank { currentImei } ?: currentImei
                                    val uri = repository.getVoiceCallUri(currentImei, name)
                                    if (uri.isSuccess) {
                                        runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri.getOrThrow())) }
                                            .onFailure { Toast.makeText(this@MainActivity, "${language.text("OpenWeChatFailed", "无法打开微信")}：${it.message}", Toast.LENGTH_LONG).show() }
                                    } else Toast.makeText(this@MainActivity, uri.exceptionOrNull()?.message ?: language.text("CallUnavailable", "通话不可用"), Toast.LENGTH_LONG).show()
                                }
                            },
                            onSharePosition = {
                                showShareDialog = true
                                shareLoading = true
                                scope.launch {
                                    val result = repository.getShareEnabled(currentImei)
                                    shareEnabled = result.getOrDefault(false)
                                    shareError = result.exceptionOrNull()?.message
                                    shareLoading = false
                                }
                            },
                            onEditDevice = {
                                detailState?.let { detail ->
                                    val raw = detail.rawFields
                                    deviceEditFields = listOf(
                                        detail.carNum, detail.sim, raw["strGroupName"].orEmpty(),
                                        raw["strOwnerName"].orEmpty(), raw["strOwnerTel"].orEmpty(),
                                        raw["strOwnerAddress"].orEmpty(), detail.remark
                                    )
                                    showDeviceEditor = true
                                }
                            },
                            onDeviceDetail = { navController.navigate("device_detail") },
                            onDeviceIcon = { navController.navigate("device_icon") },
                            onPublicDeviceInfo = { navController.navigate("public_device_info") },
                            onIndoorOffset = { navController.navigate("indoor_offset") },
                            onSimCard = { navController.navigate("sim_card") },
                            onDeviceParameters = { navController.navigate("device_parameters") },
                            onWifiMarks = { navController.navigate("wifi_marks") },
                            onManageAccount = { loadAccountUsers(); navController.navigate("account") },
                            onDeviceCommand = { navController.navigate("commands") }
                        )
                    }

                    composable("account") {
                        AccountManagementScreen(
                            users = accountUsers,
                            relations = userRelations,
                            currentUsername = repository.currentUser.value.orEmpty(),
                            devices = devices,
                            profile = accountProfile,
                            servers = repository.availableServers,
                            currentServer = repository.currentServer,
                            loginType = repository.loginType,
                            loading = accountLoading,
                            errorMessage = accountError,
                            onBack = { navController.popBackStack() },
                            onMap = { navController.navigate("tracking") { launchSingleTop = true } },
                            onFeatures = { navController.navigate("dashboard") { launchSingleTop = true } },
                            onRefresh = { loadAccountUsers() },
                            onStats = { navController.navigate("stats") },
                            onMembership = { navController.navigate("membership") },
                            onSettings = { navController.navigate("system_settings") },
                            onSupport = { navController.navigate("support") },
                            versionName = BuildConfig.VERSION_NAME,
                            languageCode = language.code,
                            onLanguageSelected = languageStore::select,
                            onManageDevices = { refreshDevices(); navController.navigate("devices") },
                            onManagePrivileges = { username ->
                                navController.navigate("privileges/${android.net.Uri.encode(username)}")
                            },
                            onAssignUser = { username, parent ->
                                scope.launch {
                                    val result = repository.assignSubUser(username, parent)
                                    if (result.getOrDefault(false)) loadAccountUsers()
                                    else Toast.makeText(this@MainActivity, result.exceptionOrNull()?.message ?: language.text("AssignFailed", "分配失败"), Toast.LENGTH_LONG).show()
                                }
                            },
                            onSaveProfile = { fields ->
                                scope.launch {
                                    val result = repository.saveCurrentProfile(
                                        fields[0], fields[1], fields[2], fields[3], fields[4], fields[5]
                                    )
                                    Toast.makeText(
                                        this@MainActivity,
                                        if (result.getOrDefault(false)) language.text("ProfileSaved", "资料已保存") else result.exceptionOrNull()?.message ?: language.text("SaveFailed", "保存失败"),
                                        Toast.LENGTH_LONG
                                    ).show()
                                    if (result.isSuccess) loadAccountUsers()
                                }
                            },
                            onSaveSubUser = { fields, editing ->
                                scope.launch {
                                    val result = if (editing) {
                                        repository.modifySubUser(
                                            username = fields[0], password = fields[1], name = fields[2],
                                            phone = fields[3], company = fields[4], address = fields[5],
                                            email = fields[6], remark = fields[7]
                                        )
                                    } else {
                                        repository.addSubUser(
                                            username = fields[0], password = fields[1], name = fields[2],
                                            phone = fields[3], company = fields[4], address = fields[5],
                                            email = fields[6], remark = fields[7]
                                        )
                                    }
                                    if (result.isSuccess) loadAccountUsers()
                                    else Toast.makeText(this@MainActivity, result.exceptionOrNull()?.message ?: language.text("CreateFailed", "创建失败"), Toast.LENGTH_LONG).show()
                                }
                            },
                            onDeleteSubUser = { username ->
                                scope.launch {
                                    val result = repository.deleteSubUser(username)
                                    if (result.isSuccess) loadAccountUsers()
                                    else Toast.makeText(this@MainActivity, result.exceptionOrNull()?.message ?: language.text("DeleteFailed", "删除失败"), Toast.LENGTH_LONG).show()
                                }
                            },
                            onAssignDevice = { username, teid, assigned ->
                                scope.launch {
                                    val result = repository.assignDeviceToSubUser(username, teid, assigned)
                                    Toast.makeText(
                                        this@MainActivity,
                                        if (result.getOrDefault(false)) language.text("DeviceAssignmentUpdated", "设备分配已更新") else result.exceptionOrNull()?.message ?: language.text("AssignFailed", "分配失败"),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            onSwitchServer = { server ->
                                scope.launch {
                                    val result = repository.switchServer(server)
                                    Toast.makeText(
                                        this@MainActivity,
                                        if (result.getOrDefault(false)) language.text("ServerSwitched", "服务器已切换") else result.exceptionOrNull()?.message ?: language.text("SwitchFailed", "切换失败"),
                                        Toast.LENGTH_LONG
                                    ).show()
                                    if (result.isSuccess) {
                                        selectedServer = repository.currentServer
                                        reloadCommandCapabilities()
                                        sessionStore.load()?.let { sessionStore.save(it.copy(server = server)) }
                                        val previousImei = currentImei
                                        devices = emptyList()
                                        currentImei = ""
                                        positionState = null
                                        detailState = null
                                        val switchedDevices = if (repository.loginType == 2)
                                            Result.success(listOf(DeviceSummary(previousImei, previousImei)))
                                        else repository.getDevices()
                                        if (switchedDevices.isSuccess) {
                                            devices = switchedDevices.getOrThrow()
                                            currentImei = devices.firstOrNull()?.teid.orEmpty()
                                            loadDeviceData(currentImei)
                                            if (settings.pushAlarm) AlarmPollingWorker.schedule(
                                                this@MainActivity,
                                                repository.currentUser.value.orEmpty(),
                                                devices.map { it.teid }, repository.currentServer
                                            )
                                        } else deviceListError = switchedDevices.exceptionOrNull()?.message
                                        loadAccountUsers()
                                    }
                                }
                            },
                            onSwitchLoginType = {
                                scope.launch {
                                    val oldUser = repository.currentUser.value
                                    val targetImei = repository.loginType != 2
                                    val result = repository.switchLoginType(targetImei)
                                    if (result.getOrDefault(false)) {
                                        reloadCommandCapabilities()
                                        if (oldUser != null) AlarmPollingWorker.cancel(this@MainActivity, oldUser)
                                        val saved = sessionStore.load()
                                        if (saved != null) sessionStore.save(saved.copy(isImei = targetImei))
                                        val available = if (targetImei) listOfNotNull(saved?.let {
                                            DeviceSummary(it.account, it.account)
                                        }) else repository.getDevices().getOrNull().orEmpty()
                                        devices = available
                                        currentImei = available.firstOrNull()?.teid.orEmpty()
                                        positionState = null
                                        detailState = null
                                        if (currentImei.isNotBlank()) loadDeviceData(currentImei)
                                        if (available.isNotEmpty() && settings.pushAlarm) AlarmPollingWorker.schedule(
                                            this@MainActivity, repository.currentUser.value.orEmpty(),
                                            available.map { it.teid }, repository.currentServer
                                        )
                                        loadAccountUsers()
                                    }
                                    Toast.makeText(this@MainActivity,
                                        if (result.getOrDefault(false)) language.text("LoginTypeSwitched", "登录类型已切换") else
                                            result.exceptionOrNull()?.message ?: language.text("SwitchFailed", "切换失败"), Toast.LENGTH_LONG).show()
                                }
                            },
                            onChangePassword = { oldPassword, newPassword ->
                                scope.launch {
                                    val result = repository.changePassword(oldPassword, newPassword)
                                    Toast.makeText(
                                        this@MainActivity,
                                        if (result.getOrDefault(false)) language.text("PasswordUpdated", "密码已更新") else result.exceptionOrNull()?.message ?: language.text("UpdateFailed", "修改失败"),
                                        Toast.LENGTH_LONG
                                    ).show()
                                    if (result.getOrDefault(false)) {
                                        sessionStore.load()?.let { sessionStore.save(it.copy(password = newPassword)) }
                                    }
                                }
                            },
                            onLogout = {
                                val user = repository.currentUser.value
                                if (user != null) AlarmPollingWorker.cancel(this@MainActivity, user)
                                repository.logout()
                                selectedServer = repository.currentServer
                                sessionStore.clear()
                                loadGeneration++
                                devices = emptyList()
                                currentImei = ""
                                positionState = null
                                detailState = null
                                fencesState = emptyList()
                                otherPositions = emptyList()
                                addressText = ""
                                accountProfile = null
                                accountUsers = emptyList()
                                navController.navigate("login") {
                                    popUpTo(navController.graph.id) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable("alarms") {
                        AlarmHistoryScreen(
                            alarms = alarmHistory,
                            periodHours = alarmPeriodHours,
                            loading = alarmHistoryLoading,
                            errorMessage = alarmHistoryError,
                            onBack = { navController.popBackStack() },
                            onRefresh = { loadAlarmHistory() },
                            onPeriodSelected = { loadAlarmHistory(it) }
                        )
                    }

                    composable("tracking") {
                        LaunchedEffect(showAllDevices, currentImei, devices) {
                            if (showAllDevices && repository.loginType != 2) {
                                while (true) {
                                    repository.getPositionsForDevices(devices.map { it.teid }, currentImei)
                                        .getOrNull()?.let { otherPositions = it }
                                    delay(180_000)
                                }
                            } else otherPositions = emptyList()
                        }
                        LaunchedEffect(currentImei, settings.refreshSeconds, settings.hideAddress) {
                            val teid = currentImei
                            while (teid.isNotBlank()) {
                                delay(settings.refreshSeconds * 1_000L)
                                repository.getLastPosition(teid).getOrNull()?.let {
                                    if (currentImei == teid) {
                                        positionState = it
                                        if (!settings.hideAddress) repository.reverseGeocode(it).getOrNull()?.let { address ->
                                            if (currentImei == teid) addressText = address
                                        }
                                    }
                                }
                            }
                        }
                        TrackingMapScreen(
                            position = positionState,
                            selectedTeid = currentImei,
                            address = addressText,
                            hideAddress = settings.hideAddress,
                            autoCenter = settings.autoCenterMap,
                            places = mapPlaces,
                            selectedPlace = selectedPlace,
                            focusPoint = mapFocusPoint,
                            deviceName = detailState?.carNum?.ifBlank { currentImei } ?: currentImei,
                            otherPositions = otherPositions,
                            devices = devices,
                            iconUrlFor = repository::deviceIconUrl,
                            showAllDevices = showAllDevices,
                            isLoading = isRefreshing,
                            canRestart = canSendMapCommand("CDeviceRestart"),
                            canEmergencyLocate = canSendMapCommand("EmergencyLocate"),
                            onBack = {
                                if (!navController.popBackStack()) navController.navigate("dashboard")
                            },
                            onReturnToDevice = {
                                mapFocusPoint = null; selectedPlace = null; mapPlaces = emptyList()
                            },
                            onFeatures = { navController.navigate("dashboard") { launchSingleTop = true } },
                            onAccount = { loadAccountUsers(); navController.navigate("account") { launchSingleTop = true } },
                            onToggleAllDevices = { showAllDevices = !showAllDevices },
                            onSelectDevice = { teid ->
                                devices.firstOrNull { it.teid == teid }?.let { device ->
                                    currentImei = device.teid
                                    mapFocusPoint = null
                                    selectedPlace = null
                                    positionState = null
                                    detailState = null
                                    addressText = ""
                                    loadDeviceData(device.teid)
                                }
                            },
                            onCommandClick = { pendingCommand = it },
                            onSearchPlace = { keyword ->
                                scope.launch {
                                    val result = repository.searchPlaces(keyword, positionState?.gcjLatLng)
                                    mapPlaces = result.getOrNull().orEmpty()
                                    result.exceptionOrNull()?.let {
                                        Toast.makeText(this@MainActivity, it.message ?: language.text("SearchFailed", "搜索失败"), Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            onSelectPlace = { place ->
                                mapFocusPoint = place.point
                                selectedPlace = place
                                mapPlaces = emptyList()
                            },
                            onSavePlace = { place ->
                                scope.launch {
                                    val result = repository.savePlace(place.point, place.name)
                                    Toast.makeText(
                                        this@MainActivity,
                                        if (result.getOrDefault(false)) language.text("PlaceSaved", "地点已收藏") else result.exceptionOrNull()?.message ?: language.text("PlaceSaveFailed", "收藏失败"),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        )
                    }

                    composable("replay") {
                        TrackReplayScreen(
                            teid = currentImei,
                            repository = repository,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("media") {
                        DeviceMediaScreen(
                            teid = currentImei,
                            repository = repository,
                            onBack = { navController.popBackStack() },
                            onSendVoice = { navController.navigate("voice_send") }
                        )
                    }

                    composable("voice_send") {
                        VoiceSendScreen(currentImei, repository, onBack = { navController.popBackStack() })
                    }

                    composable("reports") {
                        ReportsScreen(currentImei, detailState, repository, onBack = { navController.popBackStack() })
                    }

                    composable("stats") {
                        StatsScreen(repository, onBack = { navController.popBackStack() })
                    }

                    composable("system_settings") {
                        SystemSettingsScreen(settingsStore,
                            onBack = { navController.popBackStack() },
                            onAlarmChanged = { enabled ->
                                val user = repository.currentUser.value.orEmpty()
                                if (user.isNotBlank()) {
                                    if (enabled && devices.isNotEmpty()) AlarmPollingWorker.schedule(
                                        this@MainActivity, user, devices.map { it.teid }, repository.currentServer
                                    ) else AlarmPollingWorker.cancel(this@MainActivity, user)
                                }
                            })
                    }

                    composable("support") {
                        SupportScreen(repository, onBack = { navController.popBackStack() })
                    }

                    composable("health") {
                        HealthScreen(currentImei, repository, onBack = { navController.popBackStack() })
                    }

                    composable("area") {
                        AreaMeasureScreen(
                            currentImei, positionState?.gcjLatLng, repository,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("indoor_offset") {
                        IndoorOffsetScreen(currentImei, positionState, repository,
                            onBack = { navController.popBackStack() })
                    }

                    composable("devices") {
                        DeviceManagementScreen(
                            devices = devices,
                            loading = deviceListLoading,
                            error = deviceListError,
                            onBack = { navController.popBackStack() },
                            onRefresh = { refreshDevices() },
                            onBulkImport = { navController.navigate("bulk_import") },
                            onAdd = { teid, name, sim, password ->
                                scope.launch {
                                    val result = repository.addDevice(teid, name, sim, password)
                                    if (result.getOrDefault(false)) refreshDevices()
                                    else deviceListError = result.exceptionOrNull()?.message ?: language.text("AddFailed", "添加失败")
                                }
                            },
                            onDelete = { teid ->
                                scope.launch {
                                    val result = repository.deleteDevice(teid)
                                    if (result.getOrDefault(false)) refreshDevices()
                                    else deviceListError = result.exceptionOrNull()?.message ?: language.text("DeleteFailed", "删除失败")
                                }
                            }
                        )
                    }

                    composable("bulk_import") {
                        BulkImportScreen(repository, onBack = { navController.popBackStack() },
                            onImported = { refreshDevices() })
                    }

                    composable("privileges/{username}") { entry ->
                        SubUserPrivilegeScreen(
                            username = entry.arguments?.getString("username").orEmpty(),
                            repository = repository,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("commands") {
                        val model = modelCapabilities.firstOrNull {
                            it.value.equals(detailState?.deviceModel, ignoreCase = true)
                        } ?: modelCapabilities.firstOrNull { it.value == "0" }
                        DeviceCommandsScreen(
                            currentImei, repository, detailState, model, commandPermissionDisabled,
                            onPhonebook = { navController.navigate("phonebook") },
                            onSchedules = { navController.navigate("schedules") },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("phonebook") {
                        PhonebookScreen(currentImei, repository, onBack = { navController.popBackStack() })
                    }

                    composable("schedules") {
                        ScheduleSettingsScreen(
                            currentImei,
                            modelCapabilities.firstOrNull {
                                it.value.equals(detailState?.deviceModel, ignoreCase = true)
                            } ?: modelCapabilities.firstOrNull { it.value == "0" },
                            repository, onBack = { navController.popBackStack() }
                        )
                    }

                    composable("device_list") {
                        DeviceListScreen(
                            devices = devices, currentTeid = currentImei, repository = repository,
                            onBack = { navController.popBackStack() },
                            onSelect = { device ->
                                currentImei = device.teid
                                mapFocusPoint = null
                                positionState = null
                                detailState = null
                                addressText = ""
                                loadDeviceData(device.teid)
                                navController.navigate("tracking")
                            }
                        )
                    }

                    composable("device_detail") {
                        DeviceDetailScreen(
                            detailState, positionState, addressText,
                            onBack = { navController.popBackStack() },
                            onEdit = {
                                navController.popBackStack()
                                detailState?.let { detail ->
                                    val raw = detail.rawFields
                                    deviceEditFields = listOf(
                                        detail.carNum, detail.sim, raw["strGroupName"].orEmpty(),
                                        raw["strOwnerName"].orEmpty(), raw["strOwnerTel"].orEmpty(),
                                        raw["strOwnerAddress"].orEmpty(), detail.remark
                                    )
                                    showDeviceEditor = true
                                }
                            }
                        )
                    }

                    composable("device_icon") {
                        DeviceIconScreen(
                            detailState, repository, onBack = { navController.popBackStack() },
                            onSaved = {
                                loadDeviceData(currentImei)
                                refreshDevices()
                                navController.popBackStack()
                            }
                        )
                    }

                    composable("public_device_info") {
                        PublicDeviceInfoScreen(
                            detailState, repository, onBack = { navController.popBackStack() },
                            onSaved = {
                                loadDeviceData(currentImei)
                                navController.popBackStack()
                            }
                        )
                    }

                    composable("monitoring") {
                        MonitoringScreen(
                            teid = currentImei,
                            position = positionState,
                            detail = detailState,
                            model = modelCapabilities.firstOrNull {
                                it.value.equals(detailState?.deviceModel, ignoreCase = true)
                            } ?: modelCapabilities.firstOrNull { it.value == "0" },
                            repository = repository,
                            onBack = { navController.popBackStack() },
                            onOpenMedia = { navController.navigate("media") },
                            onRefreshPosition = { loadDeviceData(currentImei) }
                        )
                    }

                    composable("sim_card") {
                        SimCardScreen(detailState, onBack = { navController.popBackStack() },
                            onWhitelist = { navController.navigate("sim_whitelist") })
                    }

                    composable("sim_whitelist") {
                        SimWhitelistScreen(detailState?.iccid.orEmpty(), onBack = { navController.popBackStack() })
                    }

                    composable("device_parameters") {
                        DeviceParametersScreen(
                            detailState, repository, onBack = { navController.popBackStack() },
                            onSaved = { loadDeviceData(currentImei) }
                        )
                    }

                    composable("wifi_marks") {
                        WifiMarksScreen(currentImei, positionState, repository, onBack = { navController.popBackStack() })
                    }

                    composable("membership") {
                        MembershipScreen(
                            currentImei, detailState,
                            modelCapabilities.firstOrNull {
                                it.value.equals(detailState?.deviceModel, ignoreCase = true)
                            } ?: modelCapabilities.firstOrNull { it.value == "0" },
                            repository, onBack = { navController.popBackStack() }
                        )
                    }
                }

                if (showFences) {
                    AlertDialog(
                        onDismissRequest = { showFences = false },
                        title = { Text(language.text("Geofence", "电子围栏")) },
                        text = {
                            Column {
                                if (fencesFailed) Text(language.text("FenceQueryFailed", "围栏查询失败，请刷新后重试"))
                                else if (fencesState.isEmpty()) Text(language.text("FenceNotSet", "当前设备没有查询到电子围栏"))
                                fencesState.forEach { fence ->
                                    Row(modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "${fence.name.ifBlank { fence.id }} · ${when (fence.state) {
                                                1 -> language.text("FenceEnterAlarm", "进入报警")
                                                2 -> language.text("FenceLeaveAlarm", "离开报警")
                                                else -> language.text("FenceEnterLeaveAlarm", "进入/离开报警")
                                            }}",
                                            modifier = androidx.compose.ui.Modifier.weight(1f).padding(vertical = 8.dp)
                                        )
                                        TextButton(onClick = {
                                            editingFence = fence
                                            fenceName = fence.name
                                            fenceType = fence.type
                                            fenceAlarmType = fence.alarmType
                                            fenceRadius = fence.distanceMeters.toString()
                                            fenceVertices = fence.geometry.split(';').mapNotNull { pair ->
                                                val coordinate = pair.split(',').mapNotNull { it.trim().toDoubleOrNull() }
                                                if (coordinate.size != 2) null else CoordinateTransform.wgs84ToGcj02(coordinate[1], coordinate[0])
                                            }
                                            selectedFencePoint = fenceVertices.firstOrNull()
                                            fenceCenter = selectedFencePoint?.let { "${it.longitude},${it.latitude}" }.orEmpty()
                                            showFenceEditor = true
                                        }) { Text(language.text("Edit", "编辑")) }
                                        TextButton(onClick = {
                                            scope.launch {
                                                val result = repository.unbindFenceFromDevice(currentImei, fence.name, fence.alarmType)
                                                if (result.getOrDefault(false)) loadDeviceData(currentImei)
                                                else Toast.makeText(this@MainActivity, result.exceptionOrNull()?.message ?: language.text("UnbindFailed", "解绑失败"), Toast.LENGTH_LONG).show()
                                            }
                                        }) { Text(language.text("Unbind", "解绑")) }
                                        TextButton(onClick = {
                                            scope.launch {
                                                val result = repository.deleteUserFence(currentImei, fence.name, fence.alarmType)
                                                if (result.getOrDefault(false)) loadDeviceData(currentImei)
                                                else Toast.makeText(this@MainActivity, result.exceptionOrNull()?.message ?: language.text("DeleteFailed", "删除失败"), Toast.LENGTH_LONG).show()
                                            }
                                        }) { Text(language.text("Del", "删除")) }
                                    }
                                }
                                TextButton(onClick = {
                                    editingFence = null
                                    fenceType = 1
                                    fenceAlarmType = 3
                                    fenceRadius = "200"
                                    fenceVertices = emptyList()
                                    fenceName = ""
                                    selectedFencePoint = positionState?.gcjLatLng
                                    fenceCenter = selectedFencePoint?.let { "${it.longitude},${it.latitude}" }.orEmpty()
                                    showFenceEditor = true
                                }) { Text(language.text("NewCircleFence", "新建圆形围栏")) }
                                TextButton(onClick = {
                                    editingFence = null
                                    fenceType = 3
                                    fenceAlarmType = 3
                                    fenceRadius = "200"
                                    fenceVertices = emptyList()
                                    fenceName = ""
                                    selectedFencePoint = positionState?.gcjLatLng
                                    showFenceEditor = true
                                }) { Text(language.text("NewPolygonFence", "新建多边形围栏")) }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showFences = false }) { Text(language.text("Close", "关闭")) }
                        }
                    )
                }
                if (showShareDialog) {
                    AlertDialog(
                        onDismissRequest = { showShareDialog = false },
                        title = { Text(language.text("SharePosition", "位置分享")) },
                        text = {
                            Column {
                                if (shareLoading) androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth())
                                shareError?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
                                Text(if (shareEnabled) language.text("SharingEnabled", "分享已开启") else language.text("SharingDisabled", "分享未开启"))
                                if (shareEnabled) {
                                    TextButton(onClick = {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, repository.sharePositionUrl(currentImei))
                                        }
                                        startActivity(Intent.createChooser(intent, language.text("ShareDevicePosition", "分享设备位置")))
                                    }) { Text(language.text("ShareLink", "分享链接")) }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                shareLoading = true
                                scope.launch {
                                    val result = repository.setShareEnabled(currentImei, !shareEnabled)
                                    if (result.getOrDefault(false)) shareEnabled = !shareEnabled
                                    else shareError = result.exceptionOrNull()?.message ?: language.text("OperationFailed", "操作失败")
                                    shareLoading = false
                                }
                            }, enabled = !shareLoading) { Text(if (shareEnabled) language.text("StopSharing", "停止分享") else language.text("StartSharing", "开启分享")) }
                        },
                        dismissButton = { TextButton(onClick = { showShareDialog = false }) { Text(language.text("Close", "关闭")) } }
                    )
                }
                if (showDeviceEditor) {
                    AlertDialog(
                        onDismissRequest = { showDeviceEditor = false },
                        title = { Text(language.text("EditDeviceProfile", "编辑设备资料")) },
                        text = {
                            Column(Modifier.verticalScroll(rememberScrollState())) {
                                listOf(language.text("DeviceName", "设备名称"), "SIM", language.text("Group", "分组"), language.text("Owner", "车主姓名"), language.text("OwnerPhone", "车主电话"), language.text("OwnerAddress", "车主地址"), language.text("Remark", "备注"))
                                    .forEachIndexed { index, label ->
                                        OutlinedTextField(
                                            value = deviceEditFields[index],
                                            onValueChange = { value ->
                                                deviceEditFields = deviceEditFields.toMutableList().also { it[index] = value }
                                            },
                                            label = { Text(label) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val detail = detailState ?: return@TextButton
                                scope.launch {
                                    val result = repository.modifyDevice(
                                        detail, deviceEditFields[0], deviceEditFields[1], deviceEditFields[2],
                                        deviceEditFields[3], deviceEditFields[4], deviceEditFields[5], deviceEditFields[6]
                                    )
                                    if (result.getOrDefault(false)) {
                                        showDeviceEditor = false
                                        loadDeviceData(currentImei)
                                    } else Toast.makeText(this@MainActivity, result.exceptionOrNull()?.message ?: language.text("SaveFailed", "保存失败"), Toast.LENGTH_LONG).show()
                                }
                            }) { Text(language.text("Save", "保存")) }
                        },
                        dismissButton = { TextButton(onClick = { showDeviceEditor = false }) { Text(language.text("Cancel", "取消")) } }
                    )
                }
                if (showFenceEditor) {
                    AlertDialog(
                        onDismissRequest = { showFenceEditor = false },
                        title = { Text("${if (editingFence == null) language.text("New", "新建") else language.text("Edit", "编辑")}${if (fenceType == 1) language.text("Circle", "圆形") else language.text("Polygon", "多边形")}${language.text("Fence", "围栏")}") },
                        text = {
                            Column(Modifier.verticalScroll(rememberScrollState())) {
                                OutlinedTextField(
                                    value = fenceName,
                                    onValueChange = { fenceName = it },
                                    label = { Text(language.text("FenceName", "围栏名称")) },
                                    singleLine = true
                                )
                                com.scluzlep.newmangps.ui.screens.map.AmapJsMap(
                                    points = listOfNotNull(if (fenceType == 3) fenceVertices.firstOrNull() ?: selectedFencePoint else selectedFencePoint),
                                    markerIndex = 0,
                                    onMapTap = { point ->
                                        if (fenceType == 1) {
                                            selectedFencePoint = point
                                            fenceCenter = "${point.longitude},${point.latitude}"
                                        } else fenceVertices = fenceVertices + point
                                    },
                                    circleRadiusMeters = if (fenceType == 1) fenceRadius.toIntOrNull() ?: 200 else 0,
                                    polygonPoints = if (fenceType == 3) fenceVertices else emptyList(),
                                    modifier = androidx.compose.ui.Modifier.fillMaxWidth().height(220.dp)
                                )
                                if (fenceType == 1) Text("${language.text("TapMapForCenter", "点击地图设置圆心")}：${fenceCenter.ifBlank { language.text("NotSelected", "未选择") }}")
                                else {
                                    Text("${language.text("SelectedVertices", "已选择")} ${fenceVertices.size} ${language.text("VerticesAtLeastThree", "个顶点，至少需要 3 个")}")
                                    TextButton(onClick = { fenceVertices = fenceVertices.dropLast(1) }, enabled = fenceVertices.isNotEmpty()) {
                                        Text(language.text("UndoVertex", "撤销顶点"))
                                    }
                                }
                                OutlinedTextField(
                                    value = fenceRadius,
                                    onValueChange = { fenceRadius = it.filter(Char::isDigit) },
                                    label = { Text(language.text("RadiusMetres", "半径（米）")) },
                                    singleLine = true
                                )
                                Row {
                                    listOf(1 to language.text("Enter", "进入"), 2 to language.text("Leave", "离开"), 3 to language.text("EnterAndLeave", "进入和离开")).forEach { (value, label) ->
                                        TextButton(onClick = { fenceAlarmType = value }) {
                                            Text(if (fenceAlarmType == value) "✓$label" else label)
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val vertices = if (fenceType == 1) listOfNotNull(selectedFencePoint) else fenceVertices
                                if (fenceName.isBlank() || vertices.size < if (fenceType == 1) 1 else 3) {
                                    Toast.makeText(this@MainActivity, language.text("FenceInputRequired", "请填写名称并在地图上选择有效位置"), Toast.LENGTH_SHORT).show()
                                } else {
                                    scope.launch {
                                        val radius = (fenceRadius.toIntOrNull() ?: 200).coerceIn(100, 2000)
                                        val geometry = vertices.joinToString(";") { vertex ->
                                            val point = CoordinateTransform.gcj02ToWgs84(vertex.latitude, vertex.longitude)
                                            String.format(java.util.Locale.US, "%.7f,%.7f", point.longitude, point.latitude)
                                        }
                                        val result = repository.saveUserFence(
                                            currentImei, fenceName, fenceType, radius, geometry, 0,
                                            fenceAlarmType, editingFence?.info.orEmpty(), editing = editingFence != null
                                        )
                                        if (result.getOrDefault(false)) {
                                            if (editingFence == null) {
                                                val bound = repository.bindFenceToDevice(currentImei, fenceName, fenceAlarmType)
                                                if (!bound.getOrDefault(false)) Toast.makeText(this@MainActivity, bound.exceptionOrNull()?.message ?: language.text("FenceBindFailed", "围栏已创建但绑定失败"), Toast.LENGTH_LONG).show()
                                            }
                                            showFenceEditor = false
                                            showFences = false
                                            fenceName = ""
                                            fenceCenter = ""
                                            selectedFencePoint = null
                                            loadDeviceData(currentImei)
                                        } else {
                                            Toast.makeText(this@MainActivity, result.exceptionOrNull()?.message ?: language.text("CreateFailed", "创建失败"), Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }) { Text(if (editingFence == null) language.text("CreateAndBind", "创建并绑定") else language.text("Save", "保存")) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showFenceEditor = false }) { Text(language.text("Cancel", "取消")) }
                        }
                    )
                }
                pendingCommand?.let { command ->
                    AlertDialog(
                        onDismissRequest = { pendingCommand = null },
                        title = { Text(if (command == "settings") language.text("DeviceSettings", "设备设置") else language.text("ConfirmDeviceCommand", "确认设备指令")) },
                        text = if (command == "settings") {
                            {
                                Column {
                                    listOf(
                                        "EmergencyLocate" to language.text("Command_EmergencyLocate", "立即定位"),
                                        "SetTrackInterval" to language.text("SetTrackInterval", "设置上报间隔"),
                                        "SetShakeSensitivity" to language.text("SetShakeSensitivity", "设置震动灵敏度"),
                                        "SetPowerSavingMode" to language.text("SetPowerSavingMode", "设置省电模式"),
                                        "CDeviceRestart" to language.text("RemoteRestart", "远程重启")
                                    ).filter { (action, _) -> canSendMapCommand(action) }.forEach { (action, label) ->
                                        TextButton(onClick = {
                                            commandParameter = if (action == "SetPowerSavingMode") "21" else ""
                                            pendingCommand = action
                                        }) { Text(label) }
                                    }
                                }
                            }
                        } else {
                            {
                                Column {
                                    Text(when (command) {
                                        "CDeviceRestart" -> language.text("ConfirmRestart", "确定要远程重启当前设备吗？")
                                        "EmergencyLocate" -> language.text("ConfirmLocate", "确定要让当前设备立即定位吗？")
                                        else -> language.text("EnterCommandParameter", "请输入设备设置参数")
                                    })
                                    if (command in setOf("SetTrackInterval", "SetShakeSensitivity", "SetPowerSavingMode")) {
                                        OutlinedTextField(
                                            value = commandParameter,
                                            onValueChange = { commandParameter = it.filter(Char::isDigit) },
                                            label = {
                                                Text(when (command) {
                                                    "SetTrackInterval" -> language.text("TrackIntervalSeconds", "上报间隔（秒）")
                                                    "SetShakeSensitivity" -> language.text("ShakeSensitivityRange", "灵敏度（1-5）")
                                                    else -> language.text("PowerModeCode", "省电模式代码（20/21/22）")
                                                })
                                            },
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            if (command != "settings") TextButton(onClick = {
                                pendingCommand = null
                                sendCommand(command, commandParameter)
                                commandParameter = ""
                            }) { Text(language.text("Send", "发送")) }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                pendingCommand = null
                                commandParameter = ""
                            }) { Text(language.text("Cancel", "取消")) }
                        }
                    )
                }
            }
            }
        }
    }
}

