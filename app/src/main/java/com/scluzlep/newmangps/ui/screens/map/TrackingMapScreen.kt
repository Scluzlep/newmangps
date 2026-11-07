package com.scluzlep.newmangps.ui.screens.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scluzlep.newmangps.data.model.DevicePosition
import com.scluzlep.newmangps.data.util.CoordinateTransform
import com.scluzlep.newmangps.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingMapScreen(
    position: DevicePosition?,
    address: String,
    onBack: () -> Unit,
    onCommandClick: (String) -> Unit
) {
    var nowSeconds by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            nowSeconds = System.currentTimeMillis() / 1000
        }
    }
    val online = position?.let { nowSeconds - it.timeSeconds in 0..899 } == true
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "实时位置跟踪",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = position?.teid ?: "定位设备",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (position != null &&
                position.rawLat in -85.0..85.0 && position.rawLon in -180.0..180.0 &&
                position.rawLat != 0.0 && position.rawLon != 0.0
            ) {
                OsmMap(
                    points = listOf(CoordinateTransform.LatLng(position.rawLat, position.rawLon)),
                    markerIndex = 0
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无有效定位坐标")
                }
            }

            // Bottom Floating Telemetry Card (Xiaomi HyperOS Style)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                HyperCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = position?.speed?.toString() ?: "--",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = HyperBlue
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "km/h",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            position?.let { HyperBatteryPill(percent = it.batteryPercent) }
                            HyperStatusPill(
                                text = when {
                                    position == null -> "状态未知"
                                    !online -> "离线"
                                    position.isSuperPowerSaving -> "休眠"
                                    position.speed > 0 -> "运动"
                                    else -> "静止"
                                },
                                color = if (online) HyperGreen else HyperRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = address.ifEmpty { "设备定位坐标" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    position?.let {
                        Text(
                            text = "WGS-84: ${String.format("%.6f", it.rawLat)}, ${String.format("%.6f", it.rawLon)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "更新时间: ${position?.formattedTime ?: "--"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "GSM: ${position?.gsmSignal ?: "--"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onCommandClick("CDeviceRestart") },
                            enabled = position != null,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HyperBlue.copy(alpha = 0.15f), contentColor = HyperBlue)
                        ) {
                            Text("远程重启", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onCommandClick("EmergencyLocate") },
                            enabled = position != null,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HyperBlue.copy(alpha = 0.15f), contentColor = HyperBlue)
                        ) {
                            Text("紧急定位", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

