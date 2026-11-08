package com.scluzlep.newmangps.ui.screens.account

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scluzlep.newmangps.data.AppSettingsStore
import com.scluzlep.newmangps.ui.LocalAppLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemSettingsScreen(store: AppSettingsStore, onBack: () -> Unit, onAlarmChanged: (Boolean) -> Unit = {}) {
    val settings by store.state.collectAsState()
    val language = LocalAppLanguage.current
    Scaffold(topBar = {
        TopAppBar(title = { Text(language.text("System", "系统设置")) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(language.text("RefreshTrackInterval", "定位刷新间隔"), style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(30, 60, 180, 600).forEach { seconds ->
                    FilterChip(selected = settings.refreshSeconds == seconds,
                        onClick = { store.update(settings.copy(refreshSeconds = seconds)) },
                        label = { Text(if (seconds < 60) "${seconds}秒" else "${seconds / 60}分") })
                }
            }
            HorizontalDivider()
            SettingSwitch(language.text("HideAddress", "隐藏地址"), settings.hideAddress) {
                store.update(settings.copy(hideAddress = it))
            }
            SettingSwitch(language.text("CenterTrackingDeviceMap", "地图自动居中"), settings.autoCenterMap) {
                store.update(settings.copy(autoCenterMap = it))
            }
            SettingSwitch(language.text("FastLoadDevice", "快速加载设备"), settings.quickLoadDevices) {
                store.update(settings.copy(quickLoadDevices = it))
            }
            SettingSwitch(language.text("PushAlarm", "报警推送"), settings.pushAlarm) {
                store.update(settings.copy(pushAlarm = it))
                onAlarmChanged(it)
            }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
