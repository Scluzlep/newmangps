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
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.scluzlep.newmangps.data.model.DeviceDetail
import com.scluzlep.newmangps.data.model.DevicePosition
import com.scluzlep.newmangps.data.model.DeviceSummary
import com.scluzlep.newmangps.data.model.GeofenceItem
import com.scluzlep.newmangps.data.model.TrackPoint
import com.scluzlep.newmangps.data.repository.NewsmyRepository
import com.scluzlep.newmangps.data.push.AlarmPollingWorker
import com.scluzlep.newmangps.ui.screens.device.DeviceDashboardScreen
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
                var isLoginLoading by remember { mutableStateOf(false) }
                var loginError by remember { mutableStateOf<String?>(null) }
                var isRefreshing by remember { mutableStateOf(false) }
                var dataError by remember { mutableStateOf<String?>(null) }
                var showFences by remember { mutableStateOf(false) }
                var pendingCommand by remember { mutableStateOf<String?>(null) }
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
                        isRefreshing = false
                    }
                }

                fun sendCommand(command: String) {
                    if (currentImei.isBlank()) return
                    scope.launch {
                        val result = repository.sendDeviceCommand(currentImei, command)
                        val message = result.fold(
                            onSuccess = { "服务器已受理指令" },
                            onFailure = { it.message ?: "指令下发失败" }
                        )
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
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
                                            repository.currentUser.value ?: account
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
                            fences = fencesState,
                            fencesFailed = fencesFailed,
                            address = "",
                            isRefreshing = isRefreshing,
                            errorMessage = dataError,
                            onRefresh = { loadDeviceData(currentImei) },
                            onSelectDevice = { device ->
                                currentImei = device.teid
                                positionState = null
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
                            onDeviceCommand = { pendingCommand = it }
                        )
                    }

                    composable("tracking") {
                        LaunchedEffect(currentImei) {
                            val teid = currentImei
                            while (teid.isNotBlank()) {
                                delay(30_000)
                                repository.getLastPosition(teid).getOrNull()?.let {
                                    if (currentImei == teid) positionState = it
                                }
                            }
                        }
                        TrackingMapScreen(
                            position = positionState,
                            address = "",
                            onBack = { navController.popBackStack() },
                            onCommandClick = { pendingCommand = it }
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
                            Text(
                                if (fencesFailed) "围栏查询失败，请刷新后重试"
                                else if (fencesState.isEmpty()) "当前设备没有查询到电子围栏"
                                else fencesState.joinToString("\n") {
                                    "${it.name.ifBlank { it.id }} · ${when (it.state) {
                                        1 -> "进入报警"
                                        2 -> "离开报警"
                                        else -> "进入/离开报警"
                                    }}"
                                }
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { showFences = false }) { Text("关闭") }
                        }
                    )
                }
                pendingCommand?.let { command ->
                    AlertDialog(
                        onDismissRequest = { pendingCommand = null },
                        title = { Text("确认设备指令") },
                        text = {
                            Text(
                                if (command == "CDeviceRestart") "确定要远程重启当前设备吗？"
                                else "确定要让当前设备立即定位吗？"
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                pendingCommand = null
                                sendCommand(command)
                            }) { Text("发送") }
                        },
                        dismissButton = {
                            TextButton(onClick = { pendingCommand = null }) { Text("取消") }
                        }
                    )
                }
            }
        }
    }
}

