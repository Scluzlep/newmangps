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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scluzlep.newmangps.data.model.DeviceDetail
import com.scluzlep.newmangps.data.model.DevicePosition
import com.scluzlep.newmangps.data.model.DeviceSummary
import com.scluzlep.newmangps.data.model.GeofenceItem
import com.scluzlep.newmangps.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun DeviceDashboardScreen(
    position: DevicePosition?,
    detail: DeviceDetail?,
    devices: List<DeviceSummary>,
    selectedTeid: String,
    canManageAccount: Boolean,
    fences: List<GeofenceItem>,
    fencesFailed: Boolean,
    address: String,
    isRefreshing: Boolean,
    errorMessage: String?,
    onRefresh: () -> Unit,
    onSelectDevice: (DeviceSummary) -> Unit,
    onNavigateTracking: () -> Unit,
    onNavigateTrackHistory: () -> Unit,
    onNavigateAlarms: () -> Unit,
    onNavigateGeofence: () -> Unit,
    onManageAccount: () -> Unit,
    onDeviceCommand: (String) -> Unit
) {
    var nowSeconds by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            nowSeconds = System.currentTimeMillis() / 1000
        }
    }
    val online = position?.let { nowSeconds - it.timeSeconds in 0..899 } == true
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
            subtitle = detail?.carNum?.takeIf { it.isNotBlank() } ?: selectedTeid,
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

        if (devices.size > 1) {
            var menuExpanded by remember {
                androidx.compose.runtime.mutableStateOf(false)
            }
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                TextButton(onClick = { menuExpanded = true }) {
                    Text("切换设备：${devices.firstOrNull { it.teid == selectedTeid }?.name ?: selectedTeid}")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    devices.forEach { device ->
                        DropdownMenuItem(
                            text = { Text(device.name) },
                            onClick = {
                                menuExpanded = false
                                onSelectDevice(device)
                            }
                        )
                    }
                }
            }
        }
        if (errorMessage != null) {
            Text(
                text = "部分数据加载失败：$errorMessage",
                color = HyperRed,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
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
                            text = detail?.carNum?.takeIf { it.isNotBlank() }
                                ?: position?.teid?.takeIf { it.isNotBlank() }
                                ?: selectedTeid.ifBlank { "未知设备" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "型号: ${detail?.deviceModel?.takeIf { it.isNotBlank() } ?: "--"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        position?.let { HyperBatteryPill(percent = it.batteryPercent) }
                        HyperStatusPill(
                            text = when {
                                position == null -> "状态未知"
                                !online -> "离线"
                                position.isSuperPowerSaving -> "静止休眠"
                                else -> "在线"
                            },
                            color = if (online) HyperGreen else HyperRed
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
                            text = address.ifEmpty { "设备定位坐标" },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (position != null) {
                                "WGS-84: ${String.format("%.6f", position.rawLat)}, ${String.format("%.6f", position.rawLon)}"
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
                    StatItem(label = "速度", value = position?.let { "${it.speed} km/h" } ?: "--")
                    StatItem(label = "设备里程", value = position?.let { String.format("%.2f km", it.mileageKm) } ?: "--")
                    StatItem(label = "GSM信号", value = position?.let { it.gsmSignal.toString() } ?: "--")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Time info
                Text(
                    text = "最后上报: ${position?.formattedTime ?: "--"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "停车时长: ${position?.parkDuration ?: "--"}",
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
                    subtitle = "查看设备位置",
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
                    subtitle = when {
                        fencesFailed -> "查询失败"
                        fences.isNotEmpty() -> "已设「${fences.first().name}」"
                        else -> "未设围栏"
                    },
                    icon = Icons.Default.Shield,
                    color = HyperYellow,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateGeofence
                )
                ActionCard(
                    title = "设备指令",
                    subtitle = "定位与设备设置",
                    icon = Icons.Default.SettingsRemote,
                    color = Color(0xFFA55EEA),
                    modifier = Modifier.weight(1f),
                    onClick = { onDeviceCommand("settings") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            ActionCard(
                title = "历史告警",
                subtitle = "查看设备报警记录",
                icon = Icons.Default.NotificationsActive,
                color = HyperRed,
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateAlarms
            )

            if (canManageAccount) {
                Spacer(modifier = Modifier.height(12.dp))
                ActionCard(
                    title = "账户管理",
                    subtitle = "子账号与密码",
                    icon = Icons.Default.AccountCircle,
                    color = HyperBlue,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onManageAccount
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
                            text = "到期时间: ${detail?.formattedLimitDate ?: "--"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HyperStatusPill(
                        text = when {
                            detail == null || detail.limitTimeSeconds <= 0 -> "状态未知"
                            detail.isExpired -> "已到期"
                            else -> "有效"
                        },
                        color = if (detail != null && !detail.isExpired && detail.limitTimeSeconds > 0) HyperGreen else HyperRed
                    )
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

