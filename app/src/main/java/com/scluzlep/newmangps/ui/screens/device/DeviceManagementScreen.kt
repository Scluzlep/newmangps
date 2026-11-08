package com.scluzlep.newmangps.ui.screens.device

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.scluzlep.newmangps.data.model.DeviceSummary
import com.scluzlep.newmangps.ui.LocalAppLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceManagementScreen(
    devices: List<DeviceSummary>, loading: Boolean, error: String?,
    onBack: () -> Unit, onRefresh: () -> Unit,
    onBulkImport: () -> Unit,
    onAdd: (String, String, String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    val language = LocalAppLanguage.current
    var adding by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<DeviceSummary?>(null) }
    var teid by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var sim by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("123456") }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(language.text("CarManage", "设备管理")) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, language.text("Back", "返回")) } },
            actions = {
                IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, language.text("Refresh", "刷新")) }
                IconButton(onClick = { adding = true }) { Icon(Icons.Default.Add, language.text("AddCar", "添加设备")) }
                IconButton(onClick = onBulkImport) { Icon(Icons.Default.FileUpload, language.text("BatchDeviceImport", "批量导入")) }
            }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                items(devices, key = DeviceSummary::teid) { device ->
                    ListItem(
                        headlineContent = { Text(device.name) },
                        supportingContent = { Text(device.teid) },
                        trailingContent = {
                            IconButton(onClick = { deleteTarget = device }) {
                                Icon(Icons.Default.Delete, language.text("DelCar", "删除设备"))
                            }
                        }
                    )
                    HorizontalDivider()
                }
                if (devices.isEmpty() && !loading && error == null) item { Text(language.text("NoDevices", "暂无设备")) }
            }
        }
    }

    if (adding) AlertDialog(
        onDismissRequest = { adding = false },
        title = { Text(language.text("AddCar", "添加设备")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(teid, { teid = it }, label = { Text(language.text("DeviceID", "设备编号")) }, singleLine = true)
                OutlinedTextField(name, { name = it }, label = { Text(language.text("CarNum", "设备名称")) }, singleLine = true)
                OutlinedTextField(sim, { sim = it }, label = { Text("SIM") }, singleLine = true)
                OutlinedTextField(password, { password = it }, label = { Text(language.text("InitialPassword", "初始密码")) }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation())
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(teid, name, sim, password); adding = false }, enabled = teid.isNotBlank() && name.isNotBlank()) {
                Text(language.text("Add", "添加"))
            }
        },
        dismissButton = { TextButton(onClick = { adding = false }) { Text(language.text("Cancel", "取消")) } }
    )
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(language.text("DelCar", "删除设备")) },
            text = { Text("${target.name}（${target.teid}） · ${language.text("RemoveDeviceQuestion", "确定从账户移除？")}") },
            confirmButton = { TextButton(onClick = { onDelete(target.teid); deleteTarget = null }) { Text(language.text("Del", "删除")) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(language.text("Cancel", "取消")) } }
        )
    }
}
