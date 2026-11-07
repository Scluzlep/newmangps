package com.scluzlep.newmangps.ui.screens.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scluzlep.newmangps.data.model.DevicePosition
import com.scluzlep.newmangps.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingMapScreen(
    position: DevicePosition?,
    address: String,
    onBack: () -> Unit,
    onCommandClick: (String) -> Unit
) {
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
            // Simulated / Native Map Canvas placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE8ECEF)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(HyperPillShape)
                            .background(HyperBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = HyperBlue,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "高德地图 SDK 视图图层",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HyperTextPrimaryLight
                    )
                    Text(
                        text = if (position != null) {
                            "GCJ-02: ${String.format("%.6f", position.gcjLatLng.latitude)}, ${String.format("%.6f", position.gcjLatLng.longitude)}"
                        } else {
                            "等待定位坐标..."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = HyperTextSecondaryLight
                    )
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
                                text = "${position?.speed ?: 0}",
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
                            HyperBatteryPill(percent = position?.batteryPercent ?: 100)
                            HyperStatusPill(
                                text = if (position?.isSuperPowerSaving == true) "休眠" else "运动",
                                color = if (position?.isSuperPowerSaving == true) HyperBlue else HyperGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = address.ifEmpty { "暂无详细位置" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "更新时间: ${position?.formattedTime ?: "刚刚"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "GSM: ${position?.gsmSignal ?: 86}%",
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
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HyperBlue.copy(alpha = 0.15f), contentColor = HyperBlue)
                        ) {
                            Text("远程重启", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onCommandClick("CSetLocateMode") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HyperBlue.copy(alpha = 0.15f), contentColor = HyperBlue)
                        ) {
                            Text("定位模式", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

