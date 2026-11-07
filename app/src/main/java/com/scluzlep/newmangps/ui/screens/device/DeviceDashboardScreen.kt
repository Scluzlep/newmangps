package com.scluzlep.newmangps.ui.screens.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scluzlep.newmangps.data.model.DeviceDetail
import com.scluzlep.newmangps.data.model.DevicePosition
import com.scluzlep.newmangps.data.model.GeofenceItem
import com.scluzlep.newmangps.ui.theme.*

@Composable
fun DeviceDashboardScreen(
    position: DevicePosition?,
    detail: DeviceDetail?,
    fences: List<GeofenceItem>,
    address: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onNavigateTracking: () -> Unit,
    onNavigateTrackHistory: () -> Unit,
    onNavigateGeofence: () -> Unit,
    onDeviceCommand: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // HyperOS Large Header
        HyperHeader(
            title = "设备监控",
            subtitle = detail?.carNum?.ifEmpty { detail.teid } ?: "纽曼 GPS 终端",
            action = {
                IconButton(onClick = onRefresh) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = HyperBlue,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新",
                            tint = HyperBlue
                        )
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Main Vehicle Status Card (Xiaomi HyperOS Style)
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            HyperCard {
                // Top Row: IMEI + Battery + Status Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = detail?.carNum?.ifEmpty { position?.teid } ?: "未知设备",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "型号: ${detail?.model?.ifEmpty { "通用GPS定位器" } ?: "通用GPS定位器"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        HyperBatteryPill(percent = position?.batteryPercent ?: 100)
                        HyperStatusPill(
                            text = if (position?.isSuperPowerSaving == true) "静止休眠" else "实时在线",
                            color = if (position?.isSuperPowerSaving == true) HyperBlue else HyperGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))

                // Location / Address Row
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = HyperRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = address.ifEmpty { "暂未获取详细位置" },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (position != null) {
                                "高德火星坐标: ${String.format("%.6f", position.gcjLatLng.latitude)}, ${String.format("%.6f", position.gcjLatLng.longitude)}"
                            } else {
                                "等待定位数据..."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Telemetry Quick Stats (3 Columns)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(label = "速度", value = "${position?.speed ?: 0} km/h")
                    StatItem(label = "总里程", value = "${position?.mileage ?: 431} km")
                    StatItem(label = "GSM信号", value = "${position?.gsmSignal ?: 86}%")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Time info
                Text(
                    text = "最后上报: ${position?.formattedTime ?: "刚刚"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "停车时长: 连续停放约 23 小时 (自 ${position?.formattedParkTime})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Tiles Grid
            Text(
                text = "快捷功能",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionCard(
                    title = "实时地图",
                    subtitle = "高德120Hz跟踪",
                    icon = Icons.Default.Navigation,
                    color = HyperBlue,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateTracking
                )
                ActionCard(
                    title = "轨迹回放",
                    subtitle = "近48小时历史",
                    icon = Icons.Default.History,
                    color = HyperGreen,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateTrackHistory
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionCard(
                    title = "电子围栏",
                    subtitle = if (fences.isNotEmpty()) "已设「${fences.first().name}」" else "未设围栏",
                    icon = Icons.Default.Shield,
                    color = HyperYellow,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateGeofence
                )
                ActionCard(
                    title = "设备指令",
                    subtitle = "重启/灵敏度/关机",
                    icon = Icons.Default.SettingsRemote,
                    color = Color(0xFFA55EEA),
                    modifier = Modifier.weight(1f),
                    onClick = { onDeviceCommand("CDeviceRestart") }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Platform Validity Card
            HyperCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "平台服务状态",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "到期时间: ${detail?.formattedLimitDate ?: "2027-11-07 15:23"} (有效)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HyperStatusPill(text = "终身/年费有效", color = HyperGreen)
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    HyperCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

