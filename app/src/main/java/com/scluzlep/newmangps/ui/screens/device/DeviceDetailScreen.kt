package com.scluzlep.newmangps.ui.screens.device

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scluzlep.newmangps.data.model.DeviceDetail
import com.scluzlep.newmangps.data.model.DevicePosition
import com.scluzlep.newmangps.ui.LocalAppLanguage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    detail: DeviceDetail?, position: DevicePosition?, address: String,
    onBack: () -> Unit, onEdit: () -> Unit
) {
    val language = LocalAppLanguage.current
    val keys = mapOf("设备编号" to "DeviceID", "设备名称" to "CarNum",
        "设备型号" to "DeviceModel", "分组" to "Group", "车主" to "Owner",
        "车主电话" to "OwnerPhone", "车主地址" to "OwnerAddress",
        "备注" to "Remark", "创建时间" to "CreatedTime", "激活时间" to "ActiveTime",
        "到期时间" to "ExpiryDate", "剩余天数" to "RemainingDays",
        "位置时间" to "PositionTime", "连接状态" to "ConnectionStatus",
        "位置" to "Position", "速度" to "Speed", "电量" to "Electricity",
        "总里程" to "TotalMileage", "GSM 信号" to "GSMSignal",
        "GPS 信号" to "GPSSignal", "录音剩余" to "RecordingRemaining")
    fun tr(label: String) = keys[label]?.let { language.text(it, label) } ?: label
    val raw = detail?.rawFields.orEmpty()
    val rows = listOf(
        "设备编号" to detail?.teid.orEmpty(),
        "设备名称" to detail?.carNum.orEmpty(),
        "设备型号" to detail?.deviceModel.orEmpty(),
        "IMEI" to raw["strIMEI"].orEmpty(),
        "SIM" to detail?.sim.orEmpty(),
        "ICCID" to detail?.iccid.orEmpty(),
        "分组" to raw["strGroupName"].orEmpty(),
        "车主" to raw["strOwnerName"].orEmpty(),
        "车主电话" to raw["strOwnerTel"].orEmpty(),
        "车主地址" to raw["strOwnerAddress"].orEmpty(),
        "备注" to detail?.remark.orEmpty(),
        "创建时间" to formatDeviceDate(detail?.addTimeSeconds ?: 0),
        "激活时间" to formatDeviceDate(detail?.activeTimeSeconds ?: 0),
        "到期时间" to formatDeviceDate(detail?.limitTimeSeconds ?: 0),
        "剩余天数" to detail?.limitTimeSeconds?.let {
            if (it > 0) ((it - System.currentTimeMillis() / 1000) / 86400).coerceAtLeast(0).toString() else "--"
        }.orEmpty(),
        "位置时间" to (position?.formattedTime ?: "--"),
        "连接状态" to if (position?.isOnline == true) language.text("Online", "在线") else language.text("Offline", "离线"),
        "位置" to address,
        "速度" to position?.let { "${it.speed} km/h" }.orEmpty(),
        "电量" to position?.let { "${it.batteryPercent}%" }.orEmpty(),
        "总里程" to position?.let { "%.2f km".format(it.mileageKm) }.orEmpty(),
        "GSM 信号" to position?.gsmSignal?.toString().orEmpty(),
        "GPS 信号" to position?.gpsSignal?.toString().orEmpty(),
        "录音剩余" to detail?.let { "${it.soundRemainSec} ${language.text("Second", "秒")}" }.orEmpty()
    )
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(language.text("CarData", "设备详情")) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, language.text("Back", "返回")) } },
            actions = { IconButton(onClick = onEdit, enabled = detail != null) { Icon(Icons.Default.Edit, language.text("Edit", "编辑资料")) } }
        )
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp)) {
            items(rows) { (label, value) ->
                ListItem(
                    headlineContent = { Text(tr(label)) },
                    supportingContent = { Text(value.ifBlank { "--" }) }
                )
                HorizontalDivider()
            }
        }
    }
}

private fun formatDeviceDate(seconds: Long): String = if (seconds <= 0) "--" else
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(seconds * 1000))
