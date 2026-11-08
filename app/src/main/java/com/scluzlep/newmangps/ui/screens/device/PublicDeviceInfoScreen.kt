package com.scluzlep.newmangps.ui.screens.device

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scluzlep.newmangps.data.model.DeviceDetail
import com.scluzlep.newmangps.data.repository.NewsmyRepository
import com.scluzlep.newmangps.data.repository.PublicDeviceInfo
import com.scluzlep.newmangps.ui.LocalAppLanguage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicDeviceInfoScreen(
    detail: DeviceDetail?, repository: NewsmyRepository, onBack: () -> Unit, onSaved: () -> Unit
) {
    val language = LocalAppLanguage.current
    val scope = rememberCoroutineScope()
    var value by remember(detail) { mutableStateOf(detail?.let(repository::publicDeviceInfo)
        ?: PublicDeviceInfo("", "", "", "", "", "", false)) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val enabled = detail != null && !detail.isExpired && !busy
    Scaffold(topBar = {
        TopAppBar(title = { Text(language.text("PublicDeviceInfo", "找设备公开信息")) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, language.text("Back", "返回")) } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${language.text("Device", "设备")} ${detail?.teid ?: "--"}")
            if (detail?.isExpired == true) Text(language.text("DeviceServiceExpired", "设备服务已到期"), color = MaterialTheme.colorScheme.error)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(language.text("ShowPublicDeviceInfo", "展示公开信息"))
                Switch(value.visible, onCheckedChange = { value = value.copy(visible = it) }, enabled = enabled)
            }
            if (value.visible) {
                OutlinedTextField(value.name, { value = value.copy(name = it.take(23)) },
                    label = { Text(language.text("CarNum", "设备名称")) }, modifier = Modifier.fillMaxWidth(), enabled = enabled)
                OutlinedTextField(value.owner, { value = value.copy(owner = it.take(512)) },
                    label = { Text(language.text("OwnerName", "车主姓名")) }, modifier = Modifier.fillMaxWidth(), enabled = enabled)
                OutlinedTextField(value.phone, { value = value.copy(phone = it.take(512)) },
                    label = { Text(language.text("OwnerTel", "车主电话")) }, modifier = Modifier.fillMaxWidth(), enabled = enabled)
                OutlinedTextField(value.address, { value = value.copy(address = it.take(512)) },
                    label = { Text(language.text("OwnerAddress", "车主地址")) }, modifier = Modifier.fillMaxWidth(), enabled = enabled)
                OutlinedTextField(value.detail, { value = value.copy(detail = it.take(512)) },
                    label = { Text(language.text("Detail", "详情")) }, modifier = Modifier.fillMaxWidth(), enabled = enabled)
                OutlinedTextField(value.message, { value = value.copy(message = it.take(512)) },
                    label = { Text(language.text("LeaveMessage", "留言")) }, modifier = Modifier.fillMaxWidth(), enabled = enabled)
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = {
                val teid = detail?.teid ?: return@Button
                scope.launch {
                    busy = true
                    val result = repository.savePublicDeviceInfo(teid, value)
                    busy = false
                    if (result.getOrDefault(false)) onSaved()
                    else error = result.exceptionOrNull()?.message ?: language.text("SaveFailed", "保存失败")
                }
            }, enabled = enabled && (!value.visible || value.name.isNotBlank()), modifier = Modifier.fillMaxWidth()) {
                Text(language.text("SavePublicDeviceInfo", "保存公开信息"))
            }
        }
    }
}
