package com.scluzlep.newmangps.ui.screens.device

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scluzlep.newmangps.data.model.DeviceDetail
import com.scluzlep.newmangps.data.model.DeviceModelCapability
import com.scluzlep.newmangps.data.model.DevicePosition
import com.scluzlep.newmangps.data.repository.NewsmyRepository
import com.scluzlep.newmangps.ui.AppLanguage
import com.scluzlep.newmangps.ui.LocalAppLanguage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val monitorCards = listOf(
    "CPosCard" to "位置与状态",
    "CErrorCodeCard" to "故障码",
    "COBDCard" to "OBD",
    "CCANCard" to "CAN",
    "CRFIDCard" to "RFID",
    "CPhotoCard" to "照片"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoringScreen(
    teid: String, position: DevicePosition?, detail: DeviceDetail?, model: DeviceModelCapability?,
    repository: NewsmyRepository, onBack: () -> Unit, onOpenMedia: () -> Unit,
    onRefreshPosition: () -> Unit
) {
    val language = LocalAppLanguage.current
    val cardKeys = mapOf("CPosCard" to "Position", "CErrorCodeCard" to "FaultCode",
        "COBDCard" to "OBD", "CCANCard" to "CAN", "CRFIDCard" to "RFID",
        "CPhotoCard" to "Photos")
    val scope = rememberCoroutineScope()
    val expanded = remember(teid) { mutableStateMapOf("CPosCard" to true) }
    val loaded = remember(teid) { mutableStateMapOf<String, List<Map<String, String>>>() }
    val errors = remember(teid) { mutableStateMapOf<String, String>() }
    val busy = remember(teid) { mutableStateMapOf<String, Boolean>() }

    fun refresh(card: String) {
        if (card == "CPosCard") { onRefreshPosition(); return }
        scope.launch {
            busy[card] = true
            errors.remove(card)
            val result = when (card) {
                "CErrorCodeCard" -> repository.getMonitorErrorCodes(teid)
                "COBDCard" -> repository.getMonitorObd(teid)
                "CCANCard" -> repository.getMonitorCan(teid)
                "CRFIDCard" -> repository.getMonitorRfid(teid)
                else -> repository.getLastPhotos(teid).map { photos ->
                    photos.map { photo -> mapOf(
                        "nTime" to photo.timeSeconds.toString(),
                        "nPhotoType" to photo.type.toString(),
                        "nCameraID" to photo.cameraId.toString()
                    ) }
                }
            }
            loaded[card] = result.getOrNull().orEmpty()
            result.exceptionOrNull()?.message?.let { errors[card] = it }
            busy[card] = false
        }
    }

    val visible = monitorCards.filter { model == null || model.features.isEmpty() || it.first in model.features }
    Scaffold(topBar = {
        TopAppBar(title = { Text(language.text("Monitoring", "实时监控")) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, language.text("Back", "返回")) } })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
            visible.forEach { (key, label) ->
                item(key) {
                    ListItem(
                        headlineContent = { Text(cardKeys[key]?.let { language.text(it, label) } ?: label) },
                        supportingContent = { if (key == "CPosCard") Text(detail?.carNum?.ifBlank { teid } ?: teid) },
                        trailingContent = {
                            Row {
                                if (expanded[key] == true) IconButton(onClick = { refresh(key) }) {
                                    Icon(Icons.Default.Refresh, language.text("Refresh", "刷新") + label)
                                }
                                Icon(if (expanded[key] == true) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null)
                            }
                        },
                        modifier = Modifier.clickable {
                            val next = expanded[key] != true
                            expanded[key] = next
                            if (next && key != "CPosCard" && key !in loaded) refresh(key)
                        }
                    )
                    HorizontalDivider()
                }
                if (expanded[key] == true) {
                    if (key == "CPosCard") {
                        item {
                            val rows = listOf(
                                language.text("Time", "时间") to (position?.formattedTime ?: "--"),
                                language.text("State", "状态") to if (position?.isOnline == true) language.text("Online", "在线") else language.text("Offline", "离线"),
                                language.text("Speed", "速度") to position?.let { "${it.speed} ${language.text("KMH", "km/h")}" }.orEmpty(),
                                language.text("Mileage", "里程") to position?.let { "%.2f km".format(it.mileageKm) }.orEmpty(),
                                language.text("Electricity", "电量") to position?.let { "${it.batteryPercent}%" }.orEmpty(),
                                language.text("LonLat", "经纬度") to position?.let { "${it.rawLat}, ${it.rawLon}" }.orEmpty(),
                                "GSM / GPS" to position?.let { "${it.gsmSignal} / ${it.gpsSignal}" }.orEmpty(),
                                language.text("DeviceState", "设备状态") to position?.teState?.toString().orEmpty(),
                                language.text("VehicleState", "车辆状态") to position?.carState?.toString().orEmpty(),
                                language.text("AlarmState", "告警状态") to position?.alarmState?.toString().orEmpty()
                            )
                            Column(Modifier.padding(horizontal = 16.dp)) {
                                rows.forEach { (name, value) ->
                                    ListItem(headlineContent = { Text(name) },
                                        supportingContent = { Text(value.ifBlank { "--" }) })
                                }
                            }
                        }
                    } else {
                        if (busy[key] == true) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                        errors[key]?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)) } }
                        loaded[key]?.let { rows ->
                            if (rows.isEmpty() && busy[key] != true && key !in errors) item {
                                Text(language.text("NoData", "暂无数据"), modifier = Modifier.padding(16.dp))
                            }
                            items(rows) { row ->
                                val fields = monitorFields(key, row, language)
                                ListItem(
                                    headlineContent = { Text(fields.firstOrNull()?.second ?: label) },
                                    supportingContent = { Text(fields.drop(1).joinToString("\n") { (name, value) -> "$name: $value" }) }
                                )
                                HorizontalDivider()
                            }
                        }
                        if (key == "CPhotoCard") item {
                            TextButton(onClick = onOpenMedia, modifier = Modifier.padding(horizontal = 16.dp)) {
                                Text(language.text("ViewPhotos", "查看照片"))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun monitorFields(card: String, row: Map<String, String>, language: AppLanguage): List<Pair<String, String>> {
    val payloadKey = when (card) {
        "CErrorCodeCard" -> "strErrorCode"
        "COBDCard" -> "strOBD"
        "CCANCard" -> "strCAN"
        else -> ""
    }
    if (payloadKey.isNotEmpty()) {
        val data = row[payloadKey].orEmpty().ifBlank { row["strInfo"].orEmpty() }
        if (data.isNotBlank()) return data.split(';').filter(String::isNotBlank).mapIndexed { index, part ->
            val pair = part.split(':', limit = 2)
            if (pair.size == 2) pair[0] to pair[1] else "${language.text("Data", "数据")} ${index + 1}" to part
        }
    }
    if (card == "CPhotoCard") return listOf(
        language.text("PhotoTime", "拍摄时间") to formatMonitorTime(row["nTime"]),
        language.text("Type", "类型") to when (row["nPhotoType"]?.toIntOrNull()) {
            1 -> language.text("PlatformTakePhoto", "平台拍照")
            2 -> language.text("TimingUpload", "定时上传")
            3 -> language.text("AlarmUpload", "报警上传")
            else -> language.text("NormalPhoto", "普通照片")
        }, language.text("Camera", "摄像头") to row["nCameraID"].orEmpty()
    )
    return row.filterValues(String::isNotBlank).entries.map { it.key to it.value }
}

private fun formatMonitorTime(seconds: String?): String = seconds?.toLongOrNull()?.takeIf { it > 0 }?.let {
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(it * 1000))
} ?: "--"
