package com.scluzlep.newmangps

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.scluzlep.newmangps.data.model.DeviceDetail
import com.scluzlep.newmangps.data.model.DevicePosition
import com.scluzlep.newmangps.data.model.DeviceSummary
import com.scluzlep.newmangps.data.model.GeofenceItem
import com.scluzlep.newmangps.data.model.SubUserInfo
import com.scluzlep.newmangps.data.model.TrackPoint
import com.scluzlep.newmangps.data.api.AmapPlace
import com.scluzlep.newmangps.data.util.CoordinateTransform
import com.scluzlep.newmangps.data.repository.NewsmyRepository
import com.scluzlep.newmangps.data.push.AlarmPollingWorker
import com.scluzlep.newmangps.ui.screens.device.DeviceDashboardScreen
import com.scluzlep.newmangps.ui.screens.account.AccountManagementScreen
import com.scluzlep.newmangps.ui.screens.login.LoginScreen
import com.scluzlep.newmangps.ui.screens.map.TrackingMapScreen
import com.scluzlep.newmangps.ui.screens.track.TrackReplayScreen
import com.scluzlep.newmangps.ui.theme.HyperTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val repository = NewsmyRepository()
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HyperTheme {
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()

                var currentImei by remember { mutableStateOf("") }
                var devices by remember { mutableStateOf<List<DeviceSummary>>(emptyList()) }
                var positionState by remember { mutableStateOf<DevicePosition?>(null) }
                var detailState by remember { mutableStateOf<DeviceDetail?>(null) }
                var fencesState by remember { mutableStateOf<List<GeofenceItem>>(emptyList()) }
                var fencesFailed by remember { mutableStateOf(false) }
                var trackPointsState by remember { mutableStateOf<List<TrackPoint>>(emptyList()) }
                var accountUsers by remember { mutableStateOf<List<SubUserInfo>>(emptyList()) }
                var accountProfile by remember { mutableStateOf<SubUserInfo?>(null) }
                var accountLoading by remember { mutableStateOf(false) }
                var accountError by remember { mutableStateOf<String?>(null) }
                var addressText by remember { mutableStateOf("") }
                var mapPlaces by remember { mutableStateOf<List<AmapPlace>>(emptyList()) }
                var mapFocusPoint by remember { mutableStateOf<CoordinateTransform.LatLng?>(null) }
                var isLoginLoading by remember { mutableStateOf(false) }
                var loginError by remember { mutableStateOf<String?>(null) }
                var isRefreshing by remember { mutableStateOf(false) }
                var dataError by remember { mutableStateOf<String?>(null) }
                var showFences by remember { mutableStateOf(false) }
                var showFenceEditor by remember { mutableStateOf(false) }
                var fenceName by remember { mutableStateOf("") }
                var fenceRadius by remember { mutableStateOf("500") }
                var fenceCenter by remember { mutableStateOf("") }
                var selectedFencePoint by remember { mutableStateOf<com.scluzlep.newmangps.data.util.CoordinateTransform.LatLng?>(null) }
                var pendingCommand by remember { mutableStateOf<String?>(null) }
                var commandParameter by remember { mutableStateOf("") }
                var loadGeneration by remember { mutableIntStateOf(0) }

                fun loadDeviceData(imei: String) {
                    if (imei.isBlank()) return
                    val generation = ++loadGeneration
                    scope.launch {
                        isRefreshing = true
                        val posResult = repository.getLastPosition(imei)
                        val detailResult = repository.getDeviceDetail(imei)
                        val fenceResult = repository.getGeofences(imei)
                        val now = System.currentTimeMillis() / 1000
                        val past = now - 86400 * 2
                        val trackResult = repository.getTrackHistory(imei, past, now)
                        if (currentImei != imei || generation != loadGeneration) return@launch
                        val loadedPosition = posResult.getOrNull()
                        if (loadedPosition == null ||
                            loadedPosition.timeSeconds >= (positionState?.timeSeconds ?: 0)
                        ) positionState = loadedPosition
                        detailState = detailResult.getOrNull()
                        fencesState = fenceResult.getOrNull().orEmpty()
                        fencesFailed = fenceResult.isFailure
                        trackPointsState = trackResult.getOrNull().orEmpty()
                        dataError = listOfNotNull(
                            posResult.exceptionOrNull()?.message,
                            detailResult.exceptionOrNull()?.message,
                            fenceResult.exceptionOrNull()?.message,
                            trackResult.exceptionOrNull()?.message
                        )
                            .distinct()
                            .joinToString("；")
                            .ifBlank { null }
                        if (loadedPosition == null) {
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
                    scope.launch {
                        val result = repository.sendDeviceCommand(currentImei, command, parameter)
                        val message = result.fold(
                            onSuccess = { "服务器已受理指令" },
                            onFailure = { it.message ?: "指令下发失败" }
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
                        } else {
                            accountUsers = emptyList()
                            accountError = null
                        }
                        accountProfile = if (repository.loginType != 2) {
                            repository.getCurrentProfile().getOrNull()
                        } else null
                        accountLoading = false
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = "login"
                ) {
                    composable("login") {
                        LoginScreen(
                            onLoginClick = { account, password, isImei ->
                                scope.launch {
                                    isLoginLoading = true
                                    loginError = null
                                    if (account.isBlank() || password.isBlank()) {
                                        loginError = "请输入账号和密码"
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
                                        devices = available.getOrThrow()
                                        currentImei = devices.first().teid
                                        loadDeviceData(currentImei)
                                        AlarmPollingWorker.schedule(
                                            this@MainActivity,
                                            repository.currentUser.value ?: account,
                                            devices.map { it.teid }
                                        )
                                        if (Build.VERSION.SDK_INT >= 33 &&
                                            ContextCompat.checkSelfPermission(
                                                this@MainActivity, Manifest.permission.POST_NOTIFICATIONS
                                            ) != PackageManager.PERMISSION_GRANTED
                                        ) {
                                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                        navController.navigate("dashboard") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    } else {
                                        loginError = available.exceptionOrNull()?.message ?: "该账号下没有设备"
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
                                positionState = null
                                addressText = ""
                                detailState = null
                                fencesState = emptyList()
                                fencesFailed = false
                                trackPointsState = emptyList()
                                dataError = null
                                loadDeviceData(device.teid)
                            },
                            onNavigateTracking = { navController.navigate("tracking") },
                            onNavigateTrackHistory = { navController.navigate("replay") },
                            onNavigateGeofence = { showFences = true },
                            onManageAccount = { loadAccountUsers(); navController.navigate("account") },
                            onDeviceCommand = { pendingCommand = it }
                        )
                    }

                    composable("account") {
                        AccountManagementScreen(
                            users = accountUsers,
                            devices = devices,
                            profile = accountProfile,
                            servers = repository.availableServers,
                            currentServer = repository.currentServer,
                            loginType = repository.loginType,
                            loading = accountLoading,
                            errorMessage = accountError,
                            onBack = { navController.popBackStack() },
                            onRefresh = { loadAccountUsers() },
                            onSaveProfile = { fields ->
                                scope.launch {
                                    val result = repository.saveCurrentProfile(
                                        fields[0], fields[1], fields[2], fields[3], fields[4], fields[5]
                                    )
                                    Toast.makeText(
                                        this@MainActivity,
                                        if (result.getOrDefault(false)) "资料已保存" else result.exceptionOrNull()?.message ?: "保存失败",
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
                                    else Toast.makeText(this@MainActivity, result.exceptionOrNull()?.message ?: "创建失败", Toast.LENGTH_LONG).show()
                                }
                            },
                            onDeleteSubUser = { username ->
                                scope.launch {
                                    val result = repository.deleteSubUser(username)
                                    if (result.isSuccess) loadAccountUsers()
                                    else Toast.makeText(this@MainActivity, result.exceptionOrNull()?.message ?: "删除失败", Toast.LENGTH_LONG).show()
                                }
                            },
                            onAssignDevice = { username, teid, assigned ->
                                scope.launch {
                                    val result = repository.assignDeviceToSubUser(username, teid, assigned)
                                    Toast.makeText(
                                        this@MainActivity,
                                        if (result.getOrDefault(false)) "设备分配已更新" else result.exceptionOrNull()?.message ?: "分配失败",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            onSwitchServer = { server ->
                                scope.launch {
                                    val result = repository.switchServer(server)
                                    Toast.makeText(
                                        this@MainActivity,
                                        if (result.getOrDefault(false)) "服务器已切换" else result.exceptionOrNull()?.message ?: "切换失败",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    if (result.isSuccess) {
                                        loadDeviceData(currentImei)
                                        loadAccountUsers()
                                    }
                                }
                            },
                            onChangePassword = { oldPassword, newPassword ->
                                scope.launch {
                                    val result = repository.changePassword(oldPassword, newPassword)
                                    Toast.makeText(
                                        this@MainActivity,
                                        if (result.getOrDefault(false)) "密码已更新" else result.exceptionOrNull()?.message ?: "修改失败",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            onLogout = {
                                val user = repository.currentUser.value
                                if (user != null) AlarmPollingWorker.cancel(this@MainActivity, user)
                                repository.logout()
                                devices = emptyList()
                                currentImei = ""
                                accountUsers = emptyList()
                                navController.navigate("login") {
                                    popUpTo("dashboard") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("tracking") {
                        LaunchedEffect(currentImei) {
                            val teid = currentImei
                            while (teid.isNotBlank()) {
                                delay(30_000)
                                repository.getLastPosition(teid).getOrNull()?.let {
                                    if (currentImei == teid) {
                                        positionState = it
                                        repository.reverseGeocode(it).getOrNull()?.let { address ->
                                            if (currentImei == teid) addressText = address
                                        }
                                    }
                                }
                            }
                        }
                        TrackingMapScreen(
                            position = positionState,
                            address = addressText,
                            places = mapPlaces,
                            focusPoint = mapFocusPoint,
                            onBack = { navController.popBackStack() },
                            onCommandClick = { pendingCommand = it },
                            onSearchPlace = { keyword ->
                                scope.launch {
                                    val result = repository.searchPlaces(keyword, positionState?.gcjLatLng)
                                    mapPlaces = result.getOrNull().orEmpty()
                                    result.exceptionOrNull()?.let {
                                        Toast.makeText(this@MainActivity, it.message ?: "搜索失败", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            onSelectPlace = { place -> mapFocusPoint = place.point },
                            onSavePlace = { place ->
                                scope.launch {
                                    val result = repository.savePlace(place.point, place.name)
                                    Toast.makeText(
                                        this@MainActivity,
                                        if (result.getOrDefault(false)) "地点已收藏" else result.exceptionOrNull()?.message ?: "收藏失败",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        )
                    }

                    composable("replay") {
                        TrackReplayScreen(
                            trackPoints = trackPointsState,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                if (showFences) {
                    AlertDialog(
                        onDismissRequest = { showFences = false },
                        title = { Text("电子围栏") },
                        text = {
                            Column {
                                if (fencesFailed) Text("围栏查询失败，请刷新后重试")
                                else if (fencesState.isEmpty()) Text("当前设备没有查询到电子围栏")
                                fencesState.forEach { fence ->
                                    Row(modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "${fence.name.ifBlank { fence.id }} · ${when (fence.state) {
                                                1 -> "进入报警"
                                                2 -> "离开报警"
                                                else -> "进入/离开报警"
                                            }}",
                                            modifier = androidx.compose.ui.Modifier.weight(1f).padding(vertical = 8.dp)
                                        )
                                        TextButton(onClick = {
                                            scope.launch {
                                                val result = repository.unbindFenceFromDevice(currentImei, fence.name, fence.state)
                                                if (result.isSuccess) loadDeviceData(currentImei)
                                                else Toast.makeText(this@MainActivity, result.exceptionOrNull()?.message ?: "解绑失败", Toast.LENGTH_LONG).show()
                                            }
                                        }) { Text("解绑") }
                                        TextButton(onClick = {
                                            scope.launch {
                                                val result = repository.deleteUserFence(currentImei, fence.name, fence.state)
                                                if (result.isSuccess) loadDeviceData(currentImei)
                                                else Toast.makeText(this@MainActivity, result.exceptionOrNull()?.message ?: "删除失败", Toast.LENGTH_LONG).show()
                                            }
                                        }) { Text("删除") }
                                    }
                                }
                                TextButton(onClick = {
                                    fenceName = ""
                                    selectedFencePoint = positionState?.gcjLatLng
                                    fenceCenter = selectedFencePoint?.let { "${it.longitude},${it.latitude}" }.orEmpty()
                                    showFenceEditor = true
                                }) { Text("新建圆形围栏") }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showFences = false }) { Text("关闭") }
                        }
                    )
                }
                if (showFenceEditor) {
                    AlertDialog(
                        onDismissRequest = { showFenceEditor = false },
                        title = { Text("新建圆形围栏") },
                        text = {
                            Column {
                                OutlinedTextField(
                                    value = fenceName,
                                    onValueChange = { fenceName = it },
                                    label = { Text("围栏名称") },
                                    singleLine = true
                                )
                                com.scluzlep.newmangps.ui.screens.map.AmapJsMap(
                                    points = listOfNotNull(selectedFencePoint),
                                    markerIndex = 0,
                                    onMapTap = { point ->
                                        selectedFencePoint = point
                                        fenceCenter = "${point.longitude},${point.latitude}"
                                    },
                                    circleRadiusMeters = fenceRadius.toIntOrNull() ?: 500,
                                    modifier = androidx.compose.ui.Modifier.fillMaxWidth().height(220.dp)
                                )
                                Text("点击地图设置圆心：${fenceCenter.ifBlank { "未选择" }}")
                                OutlinedTextField(
                                    value = fenceRadius,
                                    onValueChange = { fenceRadius = it.filter(Char::isDigit) },
                                    label = { Text("半径（米）") },
                                    singleLine = true
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val center = fenceCenter.split(',').mapNotNull { it.trim().toDoubleOrNull() }
                                if (center.size != 2 || center[0] !in -180.0..180.0 || center[1] !in -85.0..85.0) {
                                    Toast.makeText(this@MainActivity, "请输入有效的经度,纬度", Toast.LENGTH_SHORT).show()
                                } else {
                                    scope.launch {
                                        val radius = fenceRadius.toDoubleOrNull() ?: 500.0
                                        val point = com.scluzlep.newmangps.data.util.CoordinateTransform.gcj02ToWgs84(center[1], center[0])
                                        val geometry = String.format(
                                            java.util.Locale.US,
                                            "%.7f,%.7f",
                                            point.longitude,
                                            point.latitude
                                        )
                                        val result = repository.saveUserFence(
                                            currentImei, fenceName, 1, radius.toInt(), geometry, 0, 3, ""
                                        )
                                        if (result.isSuccess) {
                                            val bound = repository.bindFenceToDevice(currentImei, fenceName, 3)
                                            if (bound.isFailure) Toast.makeText(this@MainActivity, bound.exceptionOrNull()?.message ?: "围栏已创建但绑定失败", Toast.LENGTH_LONG).show()
                                            showFenceEditor = false
                                            showFences = false
                                            fenceName = ""
                                            fenceCenter = ""
                                            selectedFencePoint = null
                                            loadDeviceData(currentImei)
                                        } else {
                                            Toast.makeText(this@MainActivity, result.exceptionOrNull()?.message ?: "创建失败", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }) { Text("创建并绑定") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showFenceEditor = false }) { Text("取消") }
                        }
                    )
                }
                pendingCommand?.let { command ->
                    AlertDialog(
                        onDismissRequest = { pendingCommand = null },
                        title = { Text(if (command == "settings") "设备设置" else "确认设备指令") },
                        text = if (command == "settings") {
                            {
                                Column {
                                    listOf(
                                        "EmergencyLocate" to "立即定位",
                                        "SetTrackInterval" to "设置上报间隔",
                                        "SetShakeSensitivity" to "设置震动灵敏度",
                                        "SetPowerSavingMode" to "设置省电模式",
                                        "CDeviceRestart" to "远程重启"
                                    ).forEach { (action, label) ->
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
                                        "CDeviceRestart" -> "确定要远程重启当前设备吗？"
                                        "EmergencyLocate" -> "确定要让当前设备立即定位吗？"
                                        else -> "请输入设备设置参数"
                                    })
                                    if (command in setOf("SetTrackInterval", "SetShakeSensitivity", "SetPowerSavingMode")) {
                                        OutlinedTextField(
                                            value = commandParameter,
                                            onValueChange = { commandParameter = it.filter(Char::isDigit) },
                                            label = {
                                                Text(when (command) {
                                                    "SetTrackInterval" -> "上报间隔（秒）"
                                                    "SetShakeSensitivity" -> "灵敏度（1-5）"
                                                    else -> "省电模式代码（20/21/22）"
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
                            }) { Text("发送") }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                pendingCommand = null
                                commandParameter = ""
                            }) { Text("取消") }
                        }
                    )
                }
            }
        }
    }
}

