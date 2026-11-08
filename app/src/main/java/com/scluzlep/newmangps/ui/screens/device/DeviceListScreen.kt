package com.scluzlep.newmangps.ui.screens.device

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scluzlep.newmangps.data.model.DevicePosition
import com.scluzlep.newmangps.data.model.DeviceSummary
import com.scluzlep.newmangps.data.repository.NewsmyRepository
import com.scluzlep.newmangps.ui.LocalAppLanguage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(
    devices: List<DeviceSummary>, currentTeid: String, repository: NewsmyRepository,
    onBack: () -> Unit, onSelect: (DeviceSummary) -> Unit
) {
    val language = LocalAppLanguage.current
    val scope = rememberCoroutineScope()
    var positions by remember { mutableStateOf<Map<String, DevicePosition>>(emptyMap()) }
    var keyword by remember { mutableStateOf("") }
    var filter by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            loading = true
            val result = repository.getPositionsForDevices(devices.map { it.teid }, currentTeid)
            positions = result.getOrNull().orEmpty().associateBy(DevicePosition::teid)
            error = result.exceptionOrNull()?.message
            loading = false
        }
    }
    LaunchedEffect(currentTeid) { refresh() }
    val filtered = devices.filter { device ->
        val position = positions[device.teid]
        (keyword.isBlank() || device.name.contains(keyword, true) || device.teid.contains(keyword, true)) &&
            when (filter) {
                1 -> position?.isOnline == true
                2 -> position?.isOnline != true
                3 -> position?.alarmState?.let { it != 0L } == true
                else -> true
            }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(language.text("CarList", "设备列表")) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, language.text("Back", "返回")) } },
            actions = { IconButton(onClick = { refresh() }) { Icon(Icons.Default.Refresh, language.text("Refresh", "刷新")) } }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                keyword, { keyword = it }, label = { Text(language.text("SearchDeviceNameOrId", "搜索设备名称或编号")) },
                singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All" to "全部", "Online" to "在线", "Offline" to "离线", "Alarm" to "报警").forEachIndexed { index, (key, label) ->
                    FilterChip(selected = filter == index, onClick = { filter = index }, label = { Text(language.text(key, label)) })
                }
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                items(filtered, key = DeviceSummary::teid) { device ->
                    val position = positions[device.teid]
                    ListItem(
                        headlineContent = { Text(device.name) },
                        supportingContent = { Text("${device.teid} · ${if (position?.isOnline == true) language.text("Online", "在线") else language.text("Offline", "离线")}${if (position?.alarmState?.let { it != 0L } == true) " · ${language.text("Alarm", "报警")}" else ""}") },
                        trailingContent = { TextButton(onClick = { onSelect(device) }) { Text(language.text("View", "查看")) } }
                    )
                    HorizontalDivider()
                }
                if (filtered.isEmpty() && !loading) item { Text(language.text("NoMatchingDevices", "没有符合条件的设备")) }
            }
        }
    }
}
