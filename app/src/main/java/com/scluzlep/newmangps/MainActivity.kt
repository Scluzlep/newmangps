package com.scluzlep.newmangps

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.scluzlep.newmangps.data.model.DeviceDetail
import com.scluzlep.newmangps.data.model.DevicePosition
import com.scluzlep.newmangps.data.model.GeofenceItem
import com.scluzlep.newmangps.data.model.TrackPoint
import com.scluzlep.newmangps.data.repository.NewsmyRepository
import com.scluzlep.newmangps.ui.screens.device.DeviceDashboardScreen
import com.scluzlep.newmangps.ui.screens.login.LoginScreen
import com.scluzlep.newmangps.ui.screens.map.TrackingMapScreen
import com.scluzlep.newmangps.ui.screens.track.TrackReplayScreen
import com.scluzlep.newmangps.ui.theme.HyperTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val repository = NewsmyRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HyperTheme {
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()

                var currentImei by remember { mutableStateOf("") }
                var positionState by remember { mutableStateOf<DevicePosition?>(null) }
                var detailState by remember { mutableStateOf<DeviceDetail?>(null) }
                var fencesState by remember { mutableStateOf<List<GeofenceItem>>(emptyList()) }
                var trackPointsState by remember { mutableStateOf<List<TrackPoint>>(emptyList()) }
                var addressText by remember { mutableStateOf("") }

                var isLoginLoading by remember { mutableStateOf(false) }
                var loginError by remember { mutableStateOf<String?>(null) }
                var isRefreshing by remember { mutableStateOf(false) }

                fun loadDeviceData(imei: String) {
                    scope.launch {
                        isRefreshing = true
                        val posResult = repository.getLastPosition(imei)
                        posResult.getOrNull()?.let { positionState = it }

                        val detailResult = repository.getDeviceDetail(imei)
                        detailResult.getOrNull()?.let { detailState = it }

                        val fenceResult = repository.getGeofences(imei)
                        fenceResult.getOrNull()?.let { fencesState = it }

                        // Load track for the last 48 hours
                        val now = System.currentTimeMillis() / 1000
                        val past = now - 86400 * 2
                        val trackResult = repository.getTrackHistory(imei, past, now, 500)
                        trackResult.getOrNull()?.let { trackPointsState = it }

                        isRefreshing = false
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = "login"
                ) {
                    composable("login") {
                        LoginScreen(
                            onLoginSuccess = { imei ->
                                currentImei = imei
                                loadDeviceData(imei)
                                navController.navigate("dashboard") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onLoginClick = { account, password, isImei ->
                                scope.launch {
                                    isLoginLoading = true
                                    loginError = null
                                    val result = if (isImei) {
                                        repository.loginIMEI(account, password)
                                    } else {
                                        repository.loginUser(account, password)
                                    }
                                    isLoginLoading = false

                                    result.onSuccess {
                                        currentImei = account
                                        loadDeviceData(account)
                                        navController.navigate("dashboard") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }.onFailure { err ->
                                        loginError = err.message ?: "登录失败，请检查账号密码"
                                    }
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
                            fences = fencesState,
                            address = addressText,
                            isRefreshing = isRefreshing,
                            onRefresh = { loadDeviceData(currentImei) },
                            onNavigateTracking = { navController.navigate("tracking") },
                            onNavigateTrackHistory = { navController.navigate("replay") },
                            onNavigateGeofence = {
                                Toast.makeText(this@MainActivity, "围栏「家」正在实时布防监控中", Toast.LENGTH_SHORT).show()
                            },
                            onDeviceCommand = { cmd ->
                                Toast.makeText(this@MainActivity, "已向定位器下发指令: $cmd", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    composable("tracking") {
                        TrackingMapScreen(
                            position = positionState,
                            address = addressText,
                            onBack = { navController.popBackStack() },
                            onCommandClick = { cmd ->
                                Toast.makeText(this@MainActivity, "指令已下发: $cmd", Toast.LENGTH_SHORT).show()
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
            }
        }
    }
}

