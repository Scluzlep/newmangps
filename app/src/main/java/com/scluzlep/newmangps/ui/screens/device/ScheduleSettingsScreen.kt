package com.scluzlep.newmangps.ui.screens.device

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scluzlep.newmangps.data.model.DeviceModelCapability
import com.scluzlep.newmangps.data.repository.NewsmyRepository
import com.scluzlep.newmangps.ui.LocalAppLanguage
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private data class ScheduleEntry(
    val enabled: Boolean = false, val start: String = "00:00", val end: String = "00:00",
    val weekdays: Set<Int> = emptySet()
)

private fun formatScheduleTime(seconds: String?): String {
    val value = seconds?.toLongOrNull()?.takeIf { it > 0 } ?: return "00:00"
    return runCatching {
        Instant.ofEpochSecond(value).atZone(ZoneId.systemDefault()).toLocalTime()
            .format(DateTimeFormatter.ofPattern("HH:mm"))
    }.getOrDefault("00:00")
}

private fun parseSchedules(value: String, count: Int, classMode: Boolean): List<ScheduleEntry> =
    (0 until count).map { index ->
        val parts = value.split('&').getOrNull(index)?.split(',').orEmpty()
        ScheduleEntry(
            enabled = parts.getOrNull(0) == "1",
            start = formatScheduleTime(parts.getOrNull(1)),
            end = if (classMode) formatScheduleTime(parts.getOrNull(2)) else "00:00",
            weekdays = parts.getOrNull(if (classMode) 3 else 2).orEmpty().mapNotNull(Char::digitToIntOrNull)
                .filter { it in 1..7 }.toSet()
        )
    }

private fun scheduleEpoch(value: String): Long? = runCatching {
    LocalDate.now().atTime(LocalTime.parse(value.trim())).atZone(ZoneId.systemDefault()).toEpochSecond()
}.getOrNull()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleSettingsScreen(
    teid: String, model: DeviceModelCapability?, repository: NewsmyRepository, onBack: () -> Unit
) {
    val language = LocalAppLanguage.current
    val weekdayKeys = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val weekdayFallback = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val scope = rememberCoroutineScope()
    val allowAlarm = model == null || "CSetAlarmClock" in model.commands
    val allowClass = model == null || "CSetInClassDisable" in model.commands
    var classMode by remember(teid) { mutableStateOf(!allowAlarm && allowClass) }
    var entries by remember { mutableStateOf<List<ScheduleEntry>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val key = if (classMode) "SET_IN_CLASS_DISABLE" else "SET_ALARM_CLOCK"
    val action = if (classMode) "ClassDisable" else "SetAlarmClock"

    LaunchedEffect(teid, classMode) {
        loaded = false
        busy = true
        val result = repository.getDeviceParameter(teid, key)
        if (result.isSuccess) {
            entries = parseSchedules(result.getOrThrow(), if (classMode) 4 else 3, classMode)
            loaded = true
            message = null
        } else message = result.exceptionOrNull()?.message ?: language.text("ReadParametersFailed", "参数读取失败")
        busy = false
    }

    fun update(index: Int, change: (ScheduleEntry) -> ScheduleEntry) {
        entries = entries.toMutableList().also { it[index] = change(it[index]) }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text(language.text("ScheduleSettings", "定时设置")) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, language.text("Back", "返回")) } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = if (!allowAlarm) 0 else if (classMode) 1 else 0) {
                if (allowAlarm) Tab(selected = !classMode, onClick = { classMode = false }, text = { Text(language.text("SetAlarmClock", "闹钟")) })
                if (allowClass) Tab(selected = classMode, onClick = { classMode = true }, text = { Text(language.text("InClassDisable", "上课禁用")) })
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            message?.let { Text(it, modifier = Modifier.padding(16.dp),
                color = if (loaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (loaded) entries.forEachIndexed { index, entry ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${if (classMode) language.text("TimePeriod", "时段") else language.text("SetAlarmClock", "闹钟")} ${index + 1}", style = MaterialTheme.typography.titleMedium)
                        Switch(entry.enabled, onCheckedChange = { enabled -> update(index) { it.copy(enabled = enabled) } })
                    }
                    OutlinedTextField(entry.start, { value -> update(index) { it.copy(start = value) } },
                        label = { Text("${if (classMode) language.text("Start", "开始") else language.text("Ring", "响铃")} HH:mm") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (classMode) OutlinedTextField(entry.end,
                        { value -> update(index) { it.copy(end = value) } },
                        label = { Text("${language.text("End", "结束")} HH:mm") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..7).forEach { day ->
                            FilterChip(selected = day in entry.weekdays,
                                onClick = { update(index) {
                                    it.copy(weekdays = if (day in it.weekdays) it.weekdays - day else it.weekdays + day)
                                } }, label = { Text(language.text(weekdayKeys[day - 1], weekdayFallback[day - 1])) })
                        }
                    }
                    HorizontalDivider()
                }
            }
            Button(onClick = {
                val value = entries.joinToString("&") { entry ->
                    val start = scheduleEpoch(entry.start) ?: 0
                    val end = scheduleEpoch(entry.end) ?: 0
                    val days = entry.weekdays.sorted().joinToString("")
                    if (classMode) "${if (entry.enabled) 1 else 0},$start,$end,$days"
                    else "${if (entry.enabled) 1 else 0},$start,$days"
                }
                scope.launch {
                    busy = true
                    val result = repository.saveScheduledSetting(teid, key, action, value)
                    message = if (result.getOrDefault(false)) language.text("Saved", "已保存") else
                        result.exceptionOrNull()?.message ?: language.text("SaveFailed", "保存失败")
                    busy = false
                }
            }, enabled = loaded && !busy && entries.all { entry ->
                !entry.enabled || (scheduleEpoch(entry.start) != null &&
                    (!classMode || scheduleEpoch(entry.end) != null) && entry.weekdays.isNotEmpty())
            }, modifier = Modifier.fillMaxWidth().padding(16.dp)) { Text(language.text("SaveSettings", "保存设置")) }
        }
    }
}
