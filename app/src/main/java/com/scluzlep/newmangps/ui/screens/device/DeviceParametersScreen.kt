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
import com.scluzlep.newmangps.ui.LocalAppLanguage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceParametersScreen(
    detail: DeviceDetail?, repository: NewsmyRepository, onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val language = LocalAppLanguage.current
    val scope = rememberCoroutineScope()
    val teid = detail?.teid.orEmpty()
    val raw = detail?.rawFields.orEmpty()
    var email1 by remember(detail) { mutableStateOf(raw["strNoticeEmail1"].orEmpty()) }
    var email2 by remember(detail) { mutableStateOf(raw["strNoticeEmail2"].orEmpty()) }
    var email3 by remember(detail) { mutableStateOf(raw["strNoticeEmail3"].orEmpty()) }
    var mileage by remember(detail) { mutableStateOf(raw["nMileageInit"].orEmpty()) }
    var tank by remember(detail) { mutableStateOf(raw["nFuelBoxSize"].orEmpty()) }
    var empty by remember(detail) { mutableStateOf(raw["strFuelScale"].orEmpty().split(';')
        .firstOrNull { it.startsWith("0,") }?.substringAfter(',').orEmpty()) }
    var full by remember(detail) { mutableStateOf(raw["strFuelScale"].orEmpty().split(';')
        .firstOrNull { it.startsWith("100,") }?.substringAfter(',').orEmpty()) }
    var lbsDisabled by remember(detail) {
        mutableStateOf((raw["nConfig"]?.toLongOrNull() ?: 0L) and 33_554_432L != 0L)
    }
    var working by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun save(action: suspend () -> Result<Boolean>) {
        scope.launch {
            working = true
            val result = action()
            message = if (result.getOrDefault(false)) language.text("Saved", "已保存")
                else result.exceptionOrNull()?.message ?: language.text("SaveFailed", "保存失败")
            working = false
            if (result.getOrDefault(false)) onSaved()
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(language.text("DeviceParameters", "设备参数")) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, language.text("Back", "返回")) } }
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (working) LinearProgressIndicator(Modifier.fillMaxWidth())
            message?.let { Text(it) }
            Text(language.text("AlarmNoticeEmail", "报警通知邮箱"), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(email1, { email1 = it }, label = { Text("${language.text("Email", "邮箱")} 1") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(email2, { email2 = it }, label = { Text("${language.text("Email", "邮箱")} 2") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(email3, { email3 = it }, label = { Text("${language.text("Email", "邮箱")} 3") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { save { repository.setAlarmEmails(teid, listOf(email1, email2, email3)) } },
                enabled = !working && teid.isNotBlank()) { Text(language.text("SaveEmails", "保存邮箱")) }
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Text(language.text("InitMileage", "初始里程"), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(mileage, { mileage = it.filter(Char::isDigit) }, label = { Text(language.text("KM", "公里")) }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { save { repository.setInitialMileage(teid, mileage.toIntOrNull() ?: -1) } },
                enabled = !working && mileage.toIntOrNull() != null) { Text(language.text("SaveMileage", "保存里程")) }
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Text(language.text("FuelCalibration", "油箱标定"), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(tank, { tank = it.filter(Char::isDigit) }, label = { Text(language.text("TankLitres", "油箱容量（升）")) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(empty, { empty = it.filter(Char::isDigit) }, label = { Text(language.text("EmptyTankReading", "空箱读数")) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(full, { full = it.filter(Char::isDigit) }, label = { Text(language.text("FullTankReading", "满箱读数")) }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                save { repository.setFuelCalibration(teid, tank.toInt(), empty.toInt(), full.toInt()) }
            }, enabled = !working && tank.toIntOrNull() != null && empty.toIntOrNull() != null && full.toIntOrNull() != null) {
                Text(language.text("SaveCalibration", "保存标定"))
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(language.text("DisableLbs", "禁用基站定位"), style = MaterialTheme.typography.titleMedium)
                Switch(checked = lbsDisabled, onCheckedChange = { lbsDisabled = it })
            }
            Button(onClick = { save { repository.setLbsDisabled(teid, lbsDisabled) } },
                enabled = !working && teid.isNotBlank()) { Text(language.text("SaveLocationSettings", "保存定位设置")) }
        }
    }
}
