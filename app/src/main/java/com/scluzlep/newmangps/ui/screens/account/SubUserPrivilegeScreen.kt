package com.scluzlep.newmangps.ui.screens.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scluzlep.newmangps.data.model.SubUserPrivilege
import com.scluzlep.newmangps.data.repository.NewsmyRepository
import com.scluzlep.newmangps.ui.LocalAppLanguage
import kotlinx.coroutines.launch

private val privilegeFlags = listOf(
    64 to "个人资料", 32768 to "添加设备", 32 to "修改设备", 16 to "删除设备",
    16384 to "分配设备", 4 to "添加用户", 2 to "修改用户", 1 to "删除用户",
    4096 to "分配用户", 256 to "禁用设备命令"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubUserPrivilegeScreen(
    username: String, repository: NewsmyRepository, onBack: () -> Unit
) {
    val language = LocalAppLanguage.current
    val privilegeKeys = mapOf(64 to "UserInfo", 32768 to "AddCar", 32 to "EditCar",
        16 to "DelCar", 16384 to "AssignDevice", 4 to "AddUser", 2 to "ModUser",
        1 to "DelUser", 4096 to "AssignUser", 256 to "DisableDeviceCommands")
    val scope = rememberCoroutineScope()
    var parent by remember { mutableStateOf<SubUserPrivilege?>(null) }
    var target by remember { mutableStateOf<SubUserPrivilege?>(null) }
    var flags by remember { mutableIntStateOf(0) }
    var subLimit by remember { mutableStateOf("0") }
    var carLimit by remember { mutableStateOf("0") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(username) {
        loading = true
        val current = repository.currentUser.value.orEmpty()
        val own = repository.getUserPrivilege(current)
        val selected = repository.getUserPrivilege(username)
        parent = own.getOrNull()
        target = selected.getOrNull()
        flags = target?.flags ?: 0
        subLimit = target?.limitSubUsers?.toString() ?: "0"
        carLimit = target?.limitCars?.toString() ?: "0"
        error = own.exceptionOrNull()?.message ?: selected.exceptionOrNull()?.message
        loading = false
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("${language.text("UserPrivilege", "子账号权限")} · $username") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, language.text("Back", "返回")) } }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            val own = parent
            val selected = target
            if (own != null && selected != null) {
                val self = own.username == selected.username
                OutlinedTextField(
                    value = subLimit, onValueChange = { subLimit = it.filter(Char::isDigit) },
                    label = { Text(language.text("SubUserLimit", "可创建子账号数")) }, enabled = !self,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = carLimit, onValueChange = { carLimit = it.filter(Char::isDigit) },
                    label = { Text(language.text("DeviceLimit", "可管理设备数")) }, enabled = !self,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                privilegeFlags.forEach { (bit, label) ->
                    val allowed = !self && (bit == 256 || own.flags and bit != 0)
                    ListItem(
                        headlineContent = { Text(privilegeKeys[bit]?.let { language.text(it, label) } ?: label) },
                        trailingContent = {
                            Switch(
                                checked = flags and bit != 0,
                                onCheckedChange = { enabled ->
                                    flags = if (enabled) flags or bit else flags and bit.inv()
                                },
                                enabled = allowed
                            )
                        }
                    )
                    HorizontalDivider()
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        val update = selected.copy(
                            limitSubUsers = subLimit.toIntOrNull() ?: 0,
                            limitCars = carLimit.toIntOrNull() ?: 0,
                            flags = flags
                        )
                        scope.launch {
                            loading = true
                            val result = repository.saveUserPrivilege(update)
                            error = if (result.getOrDefault(false)) null else result.exceptionOrNull()?.message ?: language.text("SaveFailed", "保存失败")
                            loading = false
                            if (result.getOrDefault(false)) onBack()
                        }
                    },
                    enabled = !self && !loading && subLimit.isNotBlank() && carLimit.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(language.text("SavePrivileges", "保存权限")) }
            }
        }
    }
}
