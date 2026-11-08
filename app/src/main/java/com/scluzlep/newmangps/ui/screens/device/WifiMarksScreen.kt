package com.scluzlep.newmangps.ui.screens.device

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scluzlep.newmangps.data.model.DevicePosition
import com.scluzlep.newmangps.data.model.WifiMark
import com.scluzlep.newmangps.data.repository.NewsmyRepository
import com.scluzlep.newmangps.ui.LocalAppLanguage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiMarksScreen(
    teid: String, position: DevicePosition?, repository: NewsmyRepository, onBack: () -> Unit
) {
    val language = LocalAppLanguage.current
    val scope = rememberCoroutineScope()
    var marks by remember { mutableStateOf<List<WifiMark>>(emptyList()) }
    var editing by remember { mutableStateOf<WifiMark?>(null) }
    var deleteTarget by remember { mutableStateOf<WifiMark?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var dialogError by remember { mutableStateOf<String?>(null) }
    var scanRequested by remember { mutableStateOf(false) }

    fun refresh(scanned: Boolean = false) {
        scope.launch {
            loading = true
            val result = repository.getWifiMarks(teid, scanned)
            marks = result.getOrNull().orEmpty()
            error = result.exceptionOrNull()?.message
            loading = false
        }
    }
    LaunchedEffect(teid) { refresh() }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(language.text("WifiMarks", "Wi-Fi 标记")) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, language.text("Back", "返回")) } },
            actions = {
                IconButton(onClick = { refresh() }) { Icon(Icons.Default.Refresh, language.text("Refresh", "刷新")) }
                IconButton(onClick = {
                    dialogError = null
                    editing = WifiMark(teid, System.currentTimeMillis() / 1000,
                        position?.rawLon ?: 0.0, position?.rawLat ?: 0.0, "", "", "", "")
                }) { Icon(Icons.Default.Add, language.text("AddWifiBeacon", "添加标记")) }
            }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Button(onClick = {
                scope.launch {
                    val result = repository.sendDeviceCommand(teid, "ScanWifi")
                    if (result.isSuccess) {
                        scanRequested = true
                        delay(5_000)
                        refresh(scanned = true)
                    } else error = result.exceptionOrNull()?.message
                }
            }, modifier = Modifier.padding(16.dp)) { Text(language.text("ScanNearbyWifi", "扫描附近 Wi-Fi")) }
            if (scanRequested) Text(language.text("WifiScanResultHint", "设备扫描结果可用于新增标记"), modifier = Modifier.padding(horizontal = 16.dp))
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                items(marks) { mark ->
                    ListItem(
                        headlineContent = { Text(mark.name.ifBlank { mark.wifi }) },
                        supportingContent = { Text(mark.address.ifBlank { mark.wifi }) },
                        trailingContent = {
                            Row {
                                TextButton(onClick = { dialogError = null; editing = mark }) { Text(language.text("Edit", "编辑")) }
                                IconButton(onClick = { dialogError = null; deleteTarget = mark }) {
                                    Icon(Icons.Default.Delete, language.text("DeleteWifiBeacon", "删除 Wi-Fi 标记"))
                                }
                            }
                        }
                    )
                    HorizontalDivider()
                }
                if (marks.isEmpty() && !loading) item { Text(language.text("NoWifiMarks", "暂无 Wi-Fi 标记")) }
            }
        }
    }

    editing?.let { original ->
        var name by remember(original) { mutableStateOf(original.name) }
        var wifi by remember(original) { mutableStateOf(original.wifi) }
        var near by remember(original) { mutableStateOf(original.near) }
        var address by remember(original) { mutableStateOf(original.address) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(language.text("WifiMarks", "Wi-Fi 标记")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text(language.text("Name", "名称")) })
                    OutlinedTextField(wifi, { wifi = it }, label = { Text("Wi-Fi 标识 / MAC") })
                    OutlinedTextField(near, { near = it }, label = { Text(language.text("NearbyWifi", "附近 Wi-Fi")) })
                    OutlinedTextField(address, { address = it }, label = { Text(language.text("Address", "地址")) })
                    dialogError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val result = repository.saveWifiMark(original.copy(
                            name = name, wifi = wifi, near = near, address = address
                        ))
                        if (result.getOrDefault(false)) { editing = null; refresh() }
                        else dialogError = result.exceptionOrNull()?.message ?: language.text("SaveFailed", "保存失败")
                    }
                }, enabled = name.isNotBlank() && wifi.isNotBlank()) { Text(language.text("Save", "保存")) }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text(language.text("Cancel", "取消")) } }
        )
    }
    deleteTarget?.let { mark ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(language.text("DeleteWifiBeacon", "删除 Wi-Fi 标记")) },
            text = { Column {
                Text("${language.text("DeleteWifiQuestion", "确定删除")} ${mark.name.ifBlank { mark.wifi }}？")
                dialogError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val result = repository.deleteWifiMark(mark)
                        if (result.getOrDefault(false)) { deleteTarget = null; refresh() }
                        else dialogError = result.exceptionOrNull()?.message ?: language.text("DeleteFailed", "删除失败")
                    }
                }) { Text(language.text("Del", "删除")) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(language.text("Cancel", "取消")) } }
        )
    }
}
