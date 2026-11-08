package com.scluzlep.newmangps.ui.screens.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.AdminPanelSettings
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
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.scluzlep.newmangps.data.model.SubUserInfo
import com.scluzlep.newmangps.data.model.DeviceSummary
import com.scluzlep.newmangps.data.model.UserRelation
import com.scluzlep.newmangps.ui.MainSectionBar
import com.scluzlep.newmangps.ui.WEB_LANGUAGE_CODES
import com.scluzlep.newmangps.ui.LocalAppLanguage
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountManagementScreen(
    users: List<SubUserInfo>,
    relations: List<UserRelation>,
    currentUsername: String,
    devices: List<DeviceSummary>,
    profile: SubUserInfo?,
    servers: List<Pair<String, String>>,
    currentServer: String,
    loginType: Int,
    loading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onMap: () -> Unit,
    onFeatures: () -> Unit,
    onRefresh: () -> Unit,
    onStats: () -> Unit,
    onMembership: () -> Unit,
    onSettings: () -> Unit,
    onSupport: () -> Unit,
    versionName: String,
    languageCode: String,
    onLanguageSelected: (String) -> Unit,
    onManageDevices: () -> Unit,
    onManagePrivileges: (String) -> Unit,
    onAssignUser: (String, String) -> Unit,
    onSaveProfile: (List<String>) -> Unit,
    onSaveSubUser: (List<String>, Boolean) -> Unit,
    onDeleteSubUser: (String) -> Unit,
    onAssignDevice: (String, String, Boolean) -> Unit,
    onSwitchServer: (String) -> Unit,
    onSwitchLoginType: () -> Unit,
    onChangePassword: (String, String) -> Unit,
    onLogout: () -> Unit
) {
    val language = LocalAppLanguage.current
    val accountKeys = mapOf(
        "账户管理" to "AccountManagement", "返回" to "Back", "刷新" to "Refresh",
        "系统设置" to "System", "关于" to "About", "语言" to "Language",
        "设备管理" to "CarManage", "修改密码" to "ModifyLoginPassword",
        "退出登录" to "LogoutAccount", "个人资料" to "UserInfo", "账户统计" to "Stats",
        "会员与续费" to "MembershipAndRenewal", "子账号" to "SubUser", "暂无子账号" to "NoSubuser",
        "添加" to "Add", "分配用户" to "AssignUser", "分配设备" to "AssignDevice",
        "关闭" to "Close", "账号" to "Account", "密码" to "Password",
        "姓名" to "Name", "电话" to "Tel", "公司" to "Company",
        "地址" to "Address", "邮箱" to "Email", "备注" to "Remark",
        "创建" to "Create", "保存" to "Save", "取消" to "Cancel",
        "新密码" to "NewPassword", "删除" to "Del", "退出" to "Exit",
        "隐私声明" to "Privacy", "售后服务" to "AfterSales",
        "选择语言" to "SelectLanguage", "搜索语言或代码" to "SearchLanguage",
        "当前密码" to "CurrentPassword", "删除子账号" to "DeleteSubUser"
    )
    fun tr(label: String) = accountKeys[label]?.let { language.text(it, label) } ?: label
    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<SubUserInfo?>(null) }
    var showPassword by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<String?>(null) }
    var serverMenu by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var confirmLogout by remember { mutableStateOf(false) }
    var languageMenu by remember { mutableStateOf(false) }
    var languageFilter by remember { mutableStateOf("") }
    var assignUserTarget by remember { mutableStateOf<String?>(null) }

    Scaffold(
        bottomBar = { MainSectionBar(2, onMap, onFeatures, {}) },
        topBar = {
            TopAppBar(
                title = { Text(tr("账户管理")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("返回"))
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = tr("刷新"))
                    }
                    androidx.compose.foundation.layout.Box {
                        IconButton(onClick = { moreMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = language.text("MoreAccountFeatures", "更多账户功能"))
                        }
                        DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                            DropdownMenuItem(text = { Text(tr("系统设置")) }, onClick = { moreMenu = false; onSettings() })
                            DropdownMenuItem(text = { Text(tr("售后服务")) }, onClick = { moreMenu = false; onSupport() })
                            DropdownMenuItem(text = { Text(tr("关于")) }, onClick = { moreMenu = false; showAbout = true })
                            DropdownMenuItem(text = { Text("${tr("语言")}：$languageCode") }, onClick = {
                                moreMenu = false; languageMenu = true
                            })
                            if (loginType == 0) DropdownMenuItem(text = { Text(tr("设备管理")) },
                                onClick = { moreMenu = false; onManageDevices() })
                            if (loginType != 1) DropdownMenuItem(
                                text = { Text(if (loginType == 2) language.text("SwitchToPlatformAccount", "切换为平台账号") else language.text("SwitchToImeiLogin", "切换为 IMEI 登录")) },
                                onClick = { moreMenu = false; onSwitchLoginType() })
                        }
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
                    Text(tr("修改密码"), Modifier.padding(start = 6.dp))
                }
                TextButton(onClick = { confirmLogout = true }) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Text(tr("退出登录"), Modifier.padding(start = 6.dp))
                }
            }
            TextButton(onClick = { serverMenu = true }) {
                Text("${language.text("Server", "服务器")}：${servers.firstOrNull { it.first == currentServer }?.second ?: currentServer}")
            }
            Row {
                if (loginType != 2) {
                    TextButton(onClick = { showProfile = true }) { Text(tr("个人资料")) }
                    TextButton(onClick = onStats) { Text(tr("账户统计")) }
                }
                TextButton(onClick = onMembership) { Text(tr("会员与续费")) }
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
                Text(language.text("SubusersUnsupported", "当前登录类型不支持子账号管理。"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(tr("子账号"), style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { editTarget = null; showAdd = true }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Text(tr("添加"))
                    }
                }
                if (loading) CircularProgressIndicator()
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (!loading && users.isEmpty() && errorMessage == null) {
                    Text(tr("暂无子账号"), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                        Icon(Icons.Default.Edit, contentDescription = language.text("EditSubUser", "编辑子账号"))
                                    }
                                    IconButton(onClick = { onManagePrivileges(user.username) }) {
                                        Icon(Icons.Default.AdminPanelSettings, contentDescription = language.text("SubUserPrivileges", "子账号权限"))
                                    }
                                    TextButton(onClick = { assignUserTarget = user.username }) { Text(tr("分配用户")) }
                                    IconButton(onClick = { deleteTarget = user.username }) {
                                        Icon(Icons.Default.Delete, contentDescription = tr("删除子账号"))
                                    }
                                }
                            }
                            if (devices.isNotEmpty()) {
                                Row {
                                    TextButton(onClick = { deviceMenu = true }) {
                                        Icon(Icons.Default.Devices, contentDescription = null)
                                        Text(tr("分配设备"), Modifier.padding(start = 6.dp))
                                    }
                                    DropdownMenu(expanded = deviceMenu, onDismissRequest = { deviceMenu = false }) {
                                        devices.forEach { device ->
                                            DropdownMenuItem(
                                                text = { Text("${language.text("Assign", "分配")} · ${device.name}") },
                                                onClick = {
                                                    deviceMenu = false
                                                    onAssignDevice(user.username, device.teid, true)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("${language.text("CancelAssignment", "取消分配")} · ${device.name}") },
                                                onClick = {
                                                    deviceMenu = false
                                                    onAssignDevice(user.username, device.teid, false)
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

    if (showAbout) AlertDialog(
        onDismissRequest = { showAbout = false },
        title = { Text("纽曼 GPS") },
        text = {
            Column {
                Text("${language.text("Version", "版本")} $versionName")
                Text("${language.text("Server", "服务器")}：${servers.firstOrNull { it.first == currentServer }?.second ?: currentServer}")
                TextButton(onClick = {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.newsmy-nmgps.com/HomePage/Privacy.html"))) }
                }) { Text(tr("隐私声明")) }
            }
        },
        confirmButton = { TextButton(onClick = { showAbout = false }) { Text(tr("关闭")) } }
    )

    assignUserTarget?.let { target ->
        val excluded = mutableSetOf(target)
        var changed: Boolean
        do {
            val before = excluded.size
            relations.filter { it.parent in excluded }.forEach { excluded.add(it.child) }
            changed = excluded.size != before
        } while (changed)
        val candidates = (listOf(currentUsername) + relations.map(UserRelation::child))
            .distinct().filter { it.isNotBlank() && it !in excluded }
        AlertDialog(
            onDismissRequest = { assignUserTarget = null },
            title = { Text("${tr("分配用户")} · $target") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    candidates.forEach { parent ->
                        TextButton(onClick = {
                            onAssignUser(target, parent)
                            assignUserTarget = null
                        }) { Text(parent) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { assignUserTarget = null }) { Text(tr("关闭")) } }
        )
    }

    if (languageMenu) AlertDialog(
        onDismissRequest = { languageMenu = false },
        title = { Text(tr("选择语言")) },
        text = {
            Column {
                OutlinedTextField(
                    languageFilter, { languageFilter = it },
                    label = { Text(tr("搜索语言或代码")) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    items(WEB_LANGUAGE_CODES.filter { code ->
                        val name = Locale.forLanguageTag(code).getDisplayName(Locale.forLanguageTag(code))
                        languageFilter.isBlank() || code.contains(languageFilter, true) || name.contains(languageFilter, true)
                    }) { code ->
                        val name = when (code) {
                            "zh-CN" -> "简体中文"
                            "zh-TW" -> "繁體中文"
                            else -> Locale.forLanguageTag(code).getDisplayName(Locale.forLanguageTag(code))
                        }
                        TextButton(onClick = {
                            languageMenu = false
                            onLanguageSelected(code)
                        }) { Text("$name · $code") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { languageMenu = false }) { Text(tr("关闭")) } }
    )

    if (showAdd || editTarget != null) {
        val user = editTarget
        val fields = remember(user) {
            mutableStateListOf(
                user?.username.orEmpty(), "", user?.displayName.orEmpty(), user?.phone.orEmpty(),
                user?.company.orEmpty(), user?.address.orEmpty(), user?.email.orEmpty(), user?.remark.orEmpty()
            )
        }
        val labels = listOf("账号", "密码", "姓名", "电话", "公司", "地址", "邮箱", "备注").map(::tr)
        AlertDialog(
            onDismissRequest = { showAdd = false; editTarget = null },
            title = { Text(if (user == null) language.text("AddSubUser", "添加子账号") else language.text("EditSubUser", "编辑子账号")) },
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
                    enabled = fields[0].isNotBlank() && (user != null || fields[1].isNotBlank())
                ) { Text(tr(if (user == null) "创建" else "保存")) }
            },
            dismissButton = { TextButton(onClick = { showAdd = false; editTarget = null }) { Text(tr("取消")) } }
        )
    }

    if (showPassword) {
        var oldPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPassword = false },
            title = { Text(tr("修改密码")) },
            text = {
                Column {
                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        label = { Text(tr("当前密码")) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text(tr("新密码")) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onChangePassword(oldPassword, newPassword); showPassword = false },
                    enabled = oldPassword.isNotBlank() && newPassword.isNotBlank()
                ) { Text(tr("保存")) }
            },
            dismissButton = { TextButton(onClick = { showPassword = false }) { Text(tr("取消")) } }
        )
    }

    if (showProfile && loginType != 2) {
        val fields = remember(profile) {
            mutableStateListOf(
                profile?.displayName.orEmpty(), profile?.phone.orEmpty(), profile?.company.orEmpty(),
                profile?.address.orEmpty(), profile?.email.orEmpty(), profile?.remark.orEmpty()
            )
        }
        val labels = listOf("姓名", "电话", "公司", "地址", "邮箱", "备注").map(::tr)
        AlertDialog(
            onDismissRequest = { showProfile = false },
            title = { Text(tr("个人资料")) },
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
                TextButton(onClick = { onSaveProfile(fields.toList()); showProfile = false }) { Text(tr("保存")) }
            },
            dismissButton = { TextButton(onClick = { showProfile = false }) { Text(tr("取消")) } }
        )
    }

    deleteTarget?.let { username ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(tr("删除子账号")) },
            text = { Text("${language.text("ConfirmDelete", "确定删除")} $username${language.text("QuestionSuffix", " 吗？")}") },
            confirmButton = {
                TextButton(onClick = { onDeleteSubUser(username); deleteTarget = null }) { Text(tr("删除")) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(tr("取消")) } }
        )
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text(tr("退出登录")) },
            text = { Text(language.text("ConfirmLogout", "退出当前账号？")) },
            confirmButton = {
                TextButton(onClick = { confirmLogout = false; onLogout() }) { Text(tr("退出")) }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text(tr("取消")) } }
        )
    }
}
