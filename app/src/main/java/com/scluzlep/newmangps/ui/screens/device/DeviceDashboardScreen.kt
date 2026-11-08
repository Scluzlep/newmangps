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
import com.scluzlep.newmangps.data.model.DeviceModelCapability
import com.scluzlep.newmangps.ui.theme.*
import com.scluzlep.newmangps.ui.MainSectionBar
import com.scluzlep.newmangps.ui.LocalAppLanguage
import kotlinx.coroutines.delay

@Composable
fun DeviceDashboardScreen(
    position: DevicePosition?,
    detail: DeviceDetail?,
    model: DeviceModelCapability?,
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
    onNavigateMedia: () -> Unit,
    onNavigateReports: () -> Unit,
    onNavigateHealth: () -> Unit,
    onNavigateArea: () -> Unit,
    onNavigateDeviceList: () -> Unit,
    onNavigateMonitoring: () -> Unit,
    onOpenRadar: () -> Unit,
    onVoiceCall: () -> Unit,
    onSharePosition: () -> Unit,
    onEditDevice: () -> Unit,
    onDeviceDetail: () -> Unit,
    onDeviceIcon: () -> Unit,
    onPublicDeviceInfo: () -> Unit,
    onIndoorOffset: () -> Unit,
    onSimCard: () -> Unit,
    onDeviceParameters: () -> Unit,
    onWifiMarks: () -> Unit,
    onManageAccount: () -> Unit,
    onDeviceCommand: (String) -> Unit
) {
    val language = LocalAppLanguage.current
    val dashboardKeys = mapOf(
        "功能" to "Function", "刷新" to "Refresh", "型号" to "DeviceModel",
        "离线" to "Offline", "在线" to "Online", "速度" to "Speed",
        "设备里程" to "Mileage", "GSM信号" to "GSMSignal",
        "设备列表" to "CarList", "实时地图" to "RealtimeLocation",
        "轨迹回放" to "HistoryTrack", "电子围栏" to "Geofence",
        "设备指令" to "DeviceCmd", "历史告警" to "Alarm",
        "设备报表" to "Report", "健康" to "Health", "面积测量" to "MeasureArea",
        "语音通话" to "VoiceCall", "位置分享" to "SharePos",
        "流量卡" to "TrafficCard", "设备详情" to "CarData",
        "室内位置修正" to "PosOffset", "账户管理" to "AccountManagement",
        "已到期" to "HaveExpired", "有效" to "Valid",
        "到期时间" to "ExpiryDate"
    )
    fun tr(label: String) = dashboardKeys[label]?.let { language.text(it, label) } ?: label
    var nowSeconds by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            nowSeconds = System.currentTimeMillis() / 1000
        }
    }
    val online = position?.let { nowSeconds - it.timeSeconds in 0..899 } == true
    Box(Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 96.dp)
    ) {
        // HyperOS Large Header
        HyperHeader(
            title = tr("功能"),
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
                            contentDescription = tr("刷新"),
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
                    Text("${language.text("SwitchDevice", "切换设备")}：${devices.firstOrNull { it.teid == selectedTeid }?.name ?: selectedTeid}")
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
                text = "${language.text("PartialLoadFailed", "部分数据加载失败")}：$errorMessage",
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
                                ?: selectedTeid.ifBlank { language.text("UnknownDevice", "未知设备") },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${tr("型号")}: ${detail?.deviceModel?.takeIf { it.isNotBlank() } ?: "--"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        position?.let { HyperBatteryPill(percent = it.batteryPercent) }
                        HyperStatusPill(
                            text = when {
                                position == null -> language.text("StatusUnknown", "状态未知")
                                !online -> tr("离线")
                                position.isSuperPowerSaving -> language.text("SleepStill", "静止休眠")
                                else -> tr("在线")
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
                            text = address.ifEmpty { language.text("DeviceCoordinates", "设备定位坐标") },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (position != null) {
                                "WGS-84: ${String.format("%.6f", position.rawLat)}, ${String.format("%.6f", position.rawLon)}"
                            } else {
                                language.text("WaitingLocation", "等待定位数据...")
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
                    StatItem(label = tr("速度"), value = position?.let { "${it.speed} ${language.text("KMH", "km/h")}" } ?: "--")
                    StatItem(label = tr("设备里程"), value = position?.let { String.format("%.2f km", it.mileageKm) } ?: "--")
                    StatItem(label = tr("GSM信号"), value = position?.let { it.gsmSignal.toString() } ?: "--")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Time info
                Text(
                    text = "${language.text("LastReport", "最后上报")}: ${position?.formattedTime ?: "--"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${language.text("ParkDuration", "停车时长")}: ${position?.parkDuration ?: "--"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Tiles Grid
            Text(
                text = language.text("QuickFeatures", "快捷功能"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            ActionCard(
                title = tr("设备列表"),
                subtitle = language.text("FindFilterDevices", "查找与筛选账户设备"),
                icon = Icons.Default.Devices,
                color = HyperBlue,
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateDeviceList
            )

            Spacer(modifier = Modifier.height(12.dp))
            ActionCard(
                title = language.text("RealtimeMonitoring", "实时监控"),
                subtitle = language.text("DeviceDiagnostics", "位置、诊断与设备数据"),
                icon = Icons.Default.MonitorHeart,
                color = HyperGreen,
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateMonitoring
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionCard(
                    title = tr("实时地图"),
                    subtitle = language.text("ViewDeviceLocation", "查看设备位置"),
                    icon = Icons.Default.Navigation,
                    color = HyperBlue,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateTracking
                )
                ActionCard(
                    title = tr("轨迹回放"),
                    subtitle = language.text("Recent48Hours", "近48小时历史"),
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
                    title = tr("电子围栏"),
                    subtitle = when {
                        fencesFailed -> language.text("QueryFailed", "查询失败")
                        fences.isNotEmpty() -> "已设「${fences.first().name}」"
                        else -> language.text("FenceNotSet", "未设围栏")
                    },
                    icon = Icons.Default.Shield,
                    color = HyperYellow,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateGeofence
                )
                ActionCard(
                    title = tr("设备指令"),
                    subtitle = language.text("LocationDeviceSettings", "定位与设备设置"),
                    icon = Icons.Default.SettingsRemote,
                    color = Color(0xFFA55EEA),
                    modifier = Modifier.weight(1f),
                    onClick = { onDeviceCommand("settings") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            ActionCard(
                title = tr("历史告警"),
                subtitle = language.text("ViewAlarmHistory", "查看设备报警记录"),
                icon = Icons.Default.NotificationsActive,
                color = HyperRed,
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateAlarms
            )

            Spacer(modifier = Modifier.height(12.dp))
            ActionCard(
                title = language.text("PhotosRecordings", "照片与录音"),
                subtitle = language.text("ViewDeviceMedia", "查看设备上传的媒体"),
                icon = Icons.Default.PhotoLibrary,
                color = HyperGreen,
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateMedia
            )

            Spacer(modifier = Modifier.height(12.dp))
            ActionCard(
                title = tr("设备报表"),
                subtitle = language.text("ReportSummary", "行驶、停车、里程、油量与温度"),
                icon = Icons.Default.BarChart,
                color = HyperBlue,
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateReports
            )

            if (model == null || "CHealthCard" in model.features) {
                Spacer(modifier = Modifier.height(12.dp))
                ActionCard(
                    title = tr("健康"),
                    subtitle = language.text("HealthSummary", "步数、心率与设备健康记录"),
                    icon = Icons.Default.Favorite,
                    color = HyperRed,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateHealth
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            ActionCard(
                title = tr("面积测量"),
                subtitle = language.text("AreaSummary", "绘制、保存和查看形状"),
                icon = Icons.Default.SquareFoot,
                color = HyperGreen,
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateArea
            )

            Spacer(modifier = Modifier.height(12.dp))
            ActionCard(
                title = language.text("RadarFindDevice", "雷达找设备"),
                subtitle = language.text("RadarSummary", "通过微信蓝牙查找设备"),
                icon = Icons.Default.Radar,
                color = HyperGreen,
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenRadar
            )

            Spacer(modifier = Modifier.height(12.dp))
            ActionCard(
                title = tr("语音通话"),
                subtitle = language.text("VoiceCallSummary", "通过微信小程序呼叫设备"),
                icon = Icons.Default.Call,
                color = HyperBlue,
                modifier = Modifier.fillMaxWidth(),
                onClick = onVoiceCall
            )

            Spacer(modifier = Modifier.height(12.dp))
            ActionCard(
                title = tr("位置分享"),
                subtitle = language.text("SharePositionSummary", "开启或停止临时位置分享"),
                icon = Icons.Default.Share,
                color = HyperGreen,
                modifier = Modifier.fillMaxWidth(),
                onClick = onSharePosition
            )

            if (detail != null) {
                if (model == null || "CSimCard" in model.features) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ActionCard(
                        title = tr("流量卡"),
                        subtitle = language.text("SimProviderSummary", "查询 SIM 与运营商服务"),
                        icon = Icons.Default.SimCard,
                        color = HyperBlue,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onSimCard
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                ActionCard(
                    title = tr("设备详情"),
                    subtitle = language.text("DeviceDetailSummary", "型号、服务时间与定位状态"),
                    icon = Icons.Default.Info,
                    color = HyperGreen,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDeviceDetail
                )
                Spacer(modifier = Modifier.height(12.dp))
                ActionCard(
                    title = language.text("DeviceIcon", "设备图标"),
                    subtitle = language.text("DeviceIconSummary", "更换地图和列表标记"),
                    icon = Icons.Default.Image,
                    color = HyperGreen,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDeviceIcon
                )
                if (model == null || "CScanCodeDisplayInfo" in model.commands) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ActionCard(
                        title = language.text("PublicDeviceInfo", "找设备公开信息"),
                        subtitle = language.text("PublicDeviceSummary", "编辑扫码展示的联系人和留言"),
                        icon = Icons.Default.ContactPage,
                        color = HyperBlue,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onPublicDeviceInfo
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                ActionCard(
                    title = language.text("DeviceParameters", "设备参数"),
                    subtitle = language.text("DeviceParametersSummary", "邮箱、初始里程与油箱标定"),
                    icon = Icons.Default.Tune,
                    color = HyperBlue,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDeviceParameters
                )
                if (model == null || "CIndoorPosOffset" in model.commands) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ActionCard(
                        title = tr("室内位置修正"),
                        subtitle = language.text("IndoorOffsetSummary", "将室内误差位置映射到实际地点"),
                        icon = Icons.Default.LocationSearching,
                        color = HyperGreen,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onIndoorOffset
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                ActionCard(
                    title = language.text("WifiMarks", "Wi-Fi 标记"),
                    subtitle = language.text("WifiMarksSummary", "扫描、保存和管理定位 Wi-Fi"),
                    icon = Icons.Default.Wifi,
                    color = HyperGreen,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onWifiMarks
                )
                Spacer(modifier = Modifier.height(12.dp))
                ActionCard(
                    title = language.text("DeviceProfile", "设备资料"),
                    subtitle = language.text("DeviceProfileSummary", "名称、SIM 与车主信息"),
                    icon = Icons.Default.Edit,
                    color = HyperBlue,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onEditDevice
                )
            }

            if (canManageAccount) {
                Spacer(modifier = Modifier.height(12.dp))
                ActionCard(
                    title = tr("账户管理"),
                    subtitle = language.text("AccountSummary", "子账号与密码"),
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
                            text = language.text("PlatformStatus", "平台服务状态"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${tr("到期时间")}: ${detail?.formattedLimitDate ?: "--"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HyperStatusPill(
                        text = when {
                            detail == null || detail.limitTimeSeconds <= 0 -> language.text("StatusUnknown", "状态未知")
                            detail.isExpired -> tr("已到期")
                            else -> tr("有效")
                        },
                        color = if (detail != null && !detail.isExpired && detail.limitTimeSeconds > 0) HyperGreen else HyperRed
                    )
                }
            }
        }
    }
    MainSectionBar(
        selected = 1,
        onMap = onNavigateTracking,
        onFeatures = {},
        onAccount = onManageAccount,
        modifier = Modifier.align(Alignment.BottomCenter)
    )
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

