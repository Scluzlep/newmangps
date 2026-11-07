package com.scluzlep.newmangps.ui.screens.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.scluzlep.newmangps.data.model.SubUserInfo
import com.scluzlep.newmangps.data.model.DeviceSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManagementScreen(
    users: List<SubUserInfo>,
    devices: List<DeviceSummary>,
    profile: SubUserInfo?,
    servers: List<Pair<String, String>>,
    currentServer: String,
    loginType: Int,
    loading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSaveProfile: (List<String>) -> Unit,
    onSaveSubUser: (List<String>, Boolean) -> Unit,
    onDeleteSubUser: (String) -> Unit,
    onAssignDevice: (String, String, Boolean) -> Unit,
    onSwitchServer: (String) -> Unit,
    onChangePassword: (String, String) -> Unit,
    onLogout: () -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<SubUserInfo?>(null) }
    var showPassword by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<String?>(null) }
    var serverMenu by remember { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("账户管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        }
    ) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { showPassword = true }) {
                    Icon(Icons.Default.LockReset, contentDescription = null)
                    Text("修改密码", Modifier.padding(start = 6.dp))
                }
                TextButton(onClick = { confirmLogout = true }) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Text("退出登录", Modifier.padding(start = 6.dp))
                }
            }
            TextButton(onClick = { serverMenu = true }) {
                Text("服务器：${servers.firstOrNull { it.first == currentServer }?.second ?: currentServer}")
            }
            if (loginType != 2) {
                TextButton(onClick = { showProfile = true }) { Text("个人资料") }
            }
            DropdownMenu(expanded = serverMenu, onDismissRequest = { serverMenu = false }) {
                servers.forEach { (url, name) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = { serverMenu = false; onSwitchServer(url) }
                    )
                }
            }
            if (loginType != 0) {
                Text("当前登录类型不支持子账号管理。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("子账号", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { editTarget = null; showAdd = true }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Text("添加")
                    }
                }
                if (loading) CircularProgressIndicator()
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (!loading && users.isEmpty() && errorMessage == null) {
                    Text("暂无子账号", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(users, key = SubUserInfo::username) { user ->
                        var deviceMenu by remember(user.username) { mutableStateOf(false) }
                        Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text(user.displayName.ifBlank { user.username }, style = MaterialTheme.typography.titleMedium)
                                    Text(user.username, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (user.phone.isNotBlank()) Text(user.phone, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row {
                                    IconButton(onClick = { editTarget = user }) {
                                        Icon(Icons.Default.Edit, contentDescription = "编辑子账号")
                                    }
                                    IconButton(onClick = { deleteTarget = user.username }) {
                                        Icon(Icons.Default.Delete, contentDescription = "删除子账号")
                                    }
                                }
                            }
                            if (devices.isNotEmpty()) {
                                Row {
                                    TextButton(onClick = { deviceMenu = true }) {
                                        Icon(Icons.Default.Devices, contentDescription = null)
                                        Text("分配设备", Modifier.padding(start = 6.dp))
                                    }
                                    DropdownMenu(expanded = deviceMenu, onDismissRequest = { deviceMenu = false }) {
                                        devices.forEach { device ->
                                            DropdownMenuItem(
                                                text = { Text(device.name) },
                                                onClick = {
                                                    deviceMenu = false
                                                    onAssignDevice(user.username, device.teid, true)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showAdd || editTarget != null) {
        val user = editTarget
        val fields = remember(user) {
            mutableStateListOf(
                user?.username.orEmpty(), "", user?.displayName.orEmpty(), user?.phone.orEmpty(),
                user?.company.orEmpty(), user?.address.orEmpty(), user?.email.orEmpty(), user?.remark.orEmpty()
            )
        }
        val labels = listOf("账号", "密码", "姓名", "电话", "公司", "地址", "邮箱", "备注")
        AlertDialog(
            onDismissRequest = { showAdd = false; editTarget = null },
            title = { Text(if (user == null) "添加子账号" else "编辑子账号") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    fields.indices.forEach { index ->
                        OutlinedTextField(
                            value = fields[index],
                            onValueChange = { fields[index] = it },
                            label = { Text(labels[index]) },
                            singleLine = index < 4,
                            readOnly = user != null && index == 0,
                            visualTransformation = if (index == 1) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onSaveSubUser(fields.toList(), user != null); showAdd = false; editTarget = null },
                    enabled = fields[0].isNotBlank() && fields[1].isNotBlank()
                ) { Text(if (user == null) "创建" else "保存") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false; editTarget = null }) { Text("取消") } }
        )
    }

    if (showPassword) {
        var oldPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPassword = false },
            title = { Text("修改密码") },
            text = {
                Column {
                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        label = { Text("当前密码") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("新密码") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onChangePassword(oldPassword, newPassword); showPassword = false },
                    enabled = oldPassword.isNotBlank() && newPassword.isNotBlank()
                ) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showPassword = false }) { Text("取消") } }
        )
    }

    if (showProfile && loginType != 2) {
        val fields = remember(profile) {
            mutableStateListOf(
                profile?.displayName.orEmpty(), profile?.phone.orEmpty(), profile?.company.orEmpty(),
                profile?.address.orEmpty(), profile?.email.orEmpty(), profile?.remark.orEmpty()
            )
        }
        val labels = listOf("姓名", "电话", "公司", "地址", "邮箱", "备注")
        AlertDialog(
            onDismissRequest = { showProfile = false },
            title = { Text("个人资料") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    fields.indices.forEach { index ->
                        OutlinedTextField(
                            value = fields[index],
                            onValueChange = { fields[index] = it },
                            label = { Text(labels[index]) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onSaveProfile(fields.toList()); showProfile = false }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showProfile = false }) { Text("取消") } }
        )
    }

    deleteTarget?.let { username ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除子账号") },
            text = { Text("确定删除 $username 吗？") },
            confirmButton = {
                TextButton(onClick = { onDeleteSubUser(username); deleteTarget = null }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }
        )
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("退出登录") },
            text = { Text("退出当前账号？") },
            confirmButton = {
                TextButton(onClick = { confirmLogout = false; onLogout() }) { Text("退出") }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("取消") } }
        )
    }
}
