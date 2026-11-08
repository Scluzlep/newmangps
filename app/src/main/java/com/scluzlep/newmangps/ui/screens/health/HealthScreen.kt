package com.scluzlep.newmangps.ui.screens.health

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scluzlep.newmangps.data.model.HealthSample
import com.scluzlep.newmangps.data.repository.NewsmyRepository
import com.scluzlep.newmangps.ui.LocalAppLanguage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

private val metrics = listOf(
    "nJump" to "跳绳次数", "nHR" to "心率", "nTemp" to "体温",
    "nHum" to "湿度", "nSPO2" to "血氧", "nSBP" to "收缩压",
    "nDBP" to "舒张压", "nPP" to "脉压", "nFlip" to "翻转次数"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(teid: String, repository: NewsmyRepository, onBack: () -> Unit) {
    val language = LocalAppLanguage.current
    val metricLanguageKeys = mapOf("nJump" to "JumpRopeCount", "nHR" to "HeartRate",
        "nTemp" to "BodyTemp", "nHum" to "Humidity", "nSPO2" to "BloodOxygenSaturation",
        "nSBP" to "SBP", "nDBP" to "DBP")
    val scope = rememberCoroutineScope()
    var samples by remember { mutableStateOf<List<HealthSample>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var actionResult by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            loading = true
            val result = repository.getHealth(teid)
            samples = result.getOrNull().orEmpty()
            error = result.exceptionOrNull()?.message
            loading = false
        }
    }
    LaunchedEffect(teid) {
        while (true) {
            refresh()
            delay(60_000)
        }
    }
    val today = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis / 1000
    val steps = samples.filter { it.name == "nStep" && it.timeSeconds >= today }
    val todaySteps = ((steps.lastOrNull()?.value ?: 0.0) - (steps.firstOrNull()?.value ?: 0.0)).coerceAtLeast(0.0).toInt()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(language.text("Health", "健康")) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, language.text("Back", "返回")) } },
            actions = { IconButton(onClick = { refresh() }) { Icon(Icons.Default.Refresh, language.text("Refresh", "刷新")) } }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            actionResult?.let { Text(it) }
            Text(language.text("TodaySteps", "今日步数"), style = MaterialTheme.typography.titleMedium)
            Text(todaySteps.toString(), style = MaterialTheme.typography.headlineLarge)
            Text("${language.text("EstimateCalories", "消耗约")} ${todaySteps / 20} ${language.text("Kilocalorie", "千卡")}")
            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            metrics.forEach { (key, label) ->
                val latest = samples.lastOrNull { it.name == key }
                ListItem(
                    headlineContent = { Text(metricLanguageKeys[key]?.let { language.text(it, label) } ?: label) },
                    trailingContent = { Text(latest?.value?.let { "%.1f".format(it) } ?: "--") }
                )
                HorizontalDivider()
            }
            Text(language.text("JumpRope", "跳绳"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("开始" to "1", "停止" to "0", "重置" to "2").forEach { (label, value) ->
                    TextButton(onClick = {
                        scope.launch {
                            val command = repository.sendDeviceCommand(teid, "JumpRope", value)
                            val saved = if (value == "2" && command.isSuccess) repository.resetJumpRope(teid) else null
                            actionResult = command.exceptionOrNull()?.message ?: saved?.exceptionOrNull()?.message ?: "$label 指令已发送"
                            refresh()
                        }
                    }) { Text(language.text(when (value) { "1" -> "Start"; "0" -> "Stop"; else -> "Reset" }, label)) }
                }
            }
        }
    }
}
