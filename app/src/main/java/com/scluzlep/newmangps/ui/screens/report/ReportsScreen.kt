package com.scluzlep.newmangps.ui.screens.report

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.scluzlep.newmangps.data.model.ReportSample
import com.scluzlep.newmangps.data.model.DeviceDetail
import com.scluzlep.newmangps.data.model.fuelReading
import com.scluzlep.newmangps.data.model.parkSegments
import com.scluzlep.newmangps.data.model.runSegments
import com.scluzlep.newmangps.data.model.temperatureReading
import com.scluzlep.newmangps.data.repository.NewsmyRepository
import com.scluzlep.newmangps.ui.LocalAppLanguage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

private val reportKinds = listOf(
    "run" to "行驶", "park" to "停车", "mileage" to "每日里程",
    "fuel" to "油量", "temperature" to "温度"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(teid: String, detail: DeviceDetail?, repository: NewsmyRepository, onBack: () -> Unit) {
    val language = LocalAppLanguage.current
    val reportKeys = mapOf("run" to "Travel", "park" to "Park", "mileage" to "DayMileageReport",
        "fuel" to "Fuel", "temperature" to "Temperature")
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }
    var kind by remember { mutableStateOf("run") }
    var temperatureChannel by remember { mutableIntStateOf(0) }
    var startDate by remember { mutableStateOf(today.toString()) }
    var endDate by remember { mutableStateOf(today.toString()) }
    var samples by remember { mutableStateOf<List<ReportSample>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var requestGeneration by remember { mutableIntStateOf(0) }

    fun refresh() {
        val start = runCatching { LocalDate.parse(startDate) }.getOrNull()
        val end = runCatching { LocalDate.parse(endDate) }.getOrNull()
        if (start == null || end == null || start.isBefore(LocalDate.of(1970, 1, 1)) ||
            end.isAfter(LocalDate.of(2100, 12, 30)) || end.isBefore(start)) {
            error = "请输入有效日期，格式为 YYYY-MM-DD，结束日期不得早于开始日期"
            return
        }
        val zone = ZoneId.systemDefault()
        val generation = ++requestGeneration
        val requestedKind = kind
        scope.launch {
            loading = true
            error = null
            val from = start.atStartOfDay(zone).toEpochSecond()
            val until = end.plusDays(1).atStartOfDay(zone).toEpochSecond() - 1
            val result = repository.getReport(teid, requestedKind, from, until)
            if (generation != requestGeneration) return@launch
            samples = result.getOrNull().orEmpty()
            error = result.exceptionOrNull()?.message
            loading = false
        }
    }
    LaunchedEffect(teid, kind) { refresh() }

    val runs = remember(samples) { runSegments(samples) }
    val parks = remember(samples) { parkSegments(samples) }
    val values = remember(samples, kind, detail, temperatureChannel) {
        when (kind) {
            "mileage" -> samples.map { it.mileageKm }
            "fuel" -> samples.mapNotNull { fuelReading(it, detail)?.first }
            "temperature" -> samples.mapNotNull { temperatureReading(it, detail, temperatureChannel) }
            else -> emptyList()
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(language.text("Report", "设备报表")) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, language.text("Back", "返回")) } },
            actions = { IconButton(onClick = { refresh() }, enabled = !loading) { Icon(Icons.Default.Refresh, language.text("Refresh", "刷新")) } }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = reportKinds.indexOfFirst { it.first == kind }) {
                reportKinds.forEach { (value, label) ->
                    Tab(selected = kind == value, onClick = { kind = value },
                        text = { Text(reportKeys[value]?.let { language.text(it, label) } ?: label) })
                }
            }
            if (kind == "temperature") {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0..4).forEach { channel ->
                        FilterChip(selected = temperatureChannel == channel,
                            onClick = { temperatureChannel = channel },
                            label = { Text(if (channel == 0) "主温度" else "TEMP$channel") })
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 7, 30).forEach { days ->
                    val begin = today.minusDays(days.toLong() - 1).toString()
                    FilterChip(
                        selected = startDate == begin && endDate == today.toString(),
                        onClick = { startDate = begin; endDate = today.toString(); refresh() },
                        label = { Text(if (days == 1) language.text("Today", "今天") else "近${days}天") }
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(startDate, { startDate = it }, label = { Text(language.text("StartDate", "开始日期")) },
                    singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(endDate, { endDate = it }, label = { Text(language.text("EndDate", "结束日期")) },
                    singleLine = true, modifier = Modifier.weight(1f))
            }
            Button(onClick = { refresh() }, enabled = !loading, modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(language.text("Search", "查询"))
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
            val count = when (kind) {
                "run" -> runs.size
                "park" -> parks.size
                else -> samples.size
            }
            if (count > 0) {
                Text(
                    text = when (kind) {
                        "run" -> "${language.text("Travel", "行驶")} ${runs.size} · ${"%.1f".format(runs.sumOf { it.distanceKm })} km"
                        "park" -> "${language.text("Park", "停车")} ${parks.size} · ${parks.sumOf { it.durationSeconds } / 60} ${language.text("Minute", "分钟")}"
                        "mileage" -> "${samples.size} 天里程记录"
                        "fuel" -> "${language.text("Fuel", "油量")} ${values.size} · ${fuelReading(samples.first(), detail)?.second ?: "原始读数"}"
                        else -> if (values.isEmpty()) "暂无有效温度读数" else
                            "${language.text("Temperature", "温度")} ${values.size} · ${"%.1f".format(values.min())} - ${"%.1f".format(values.max())} °C"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
                if (values.isNotEmpty()) {
                    if (kind == "mileage") ReportBarChart(values, Modifier.fillMaxWidth().height(160.dp).padding(horizontal = 16.dp))
                    else ReportLineChart(values, Modifier.fillMaxWidth().height(160.dp).padding(horizontal = 16.dp))
                }
            }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (kind) {
                    "run" -> items(runs) { run ->
                        ListItem(
                            headlineContent = { Text("${formatReportTime(run.startSeconds)} - ${formatReportTime(run.endSeconds)}") },
                            supportingContent = { Text("${run.durationSeconds / 60} ${language.text("Minute", "分钟")} · ${"%.1f".format(run.distanceKm)} km · ${language.text("AverageSpeed", "均速")} ${run.averageSpeed.toInt()} · ${language.text("MaxSpeed", "最高")} ${run.maximumSpeed.toInt()} ${language.text("KMH", "km/h")}") }
                        )
                        HorizontalDivider()
                    }
                    "park" -> items(parks) { park ->
                        ListItem(
                            headlineContent = { Text("${formatReportTime(park.startSeconds)} - ${formatReportTime(park.endSeconds)}") },
                            supportingContent = { Text("${language.text("Park", "停车")} ${park.durationSeconds / 60} ${language.text("Minute", "分钟")} · ${"%.5f".format(park.latitude)}, ${"%.5f".format(park.longitude)}") }
                        )
                        HorizontalDivider()
                    }
                    else -> items(samples) { sample ->
                        ListItem(
                            headlineContent = { Text(formatReportTime(sample.timeSeconds)) },
                            supportingContent = { Text(when (kind) {
                                "mileage" -> "${"%.2f".format(sample.mileageKm)} km"
                                "fuel" -> fuelReading(sample, detail)?.let { (value, unit) -> "${"%.1f".format(value)} $unit" } ?: "--"
                                else -> temperatureReading(sample, detail, temperatureChannel)?.let { "${"%.1f".format(it)} °C" } ?: "--"
                            }) }
                        )
                        HorizontalDivider()
                    }
                }
                if (count == 0 && !loading && error == null) item { Text(language.text("NoReportDataInPeriod", "此时间段暂无报表数据")) }
            }
        }
    }
}

@Composable
private fun ReportLineChart(values: List<Double>, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val minimum = values.min()
        val span = (values.max() - minimum).coerceAtLeast(1.0)
        values.zipWithNext().forEachIndexed { index, (first, second) ->
            drawLine(
                color = color,
                start = Offset(size.width * index / (values.size - 1), size.height * (1 - (first - minimum) / span).toFloat()),
                end = Offset(size.width * (index + 1) / (values.size - 1), size.height * (1 - (second - minimum) / span).toFloat()),
                strokeWidth = 3.dp.toPx()
            )
        }
    }
}

@Composable
private fun ReportBarChart(values: List<Double>, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        val maximum = values.max().coerceAtLeast(1.0)
        val width = size.width / values.size
        values.forEachIndexed { index, value ->
            drawLine(color, Offset(width * (index + 0.5f), size.height),
                Offset(width * (index + 0.5f), size.height * (1 - value / maximum).toFloat()),
                strokeWidth = (width * 0.7f).coerceAtMost(24.dp.toPx()))
        }
    }
}

private fun formatReportTime(seconds: Long): String =
    if (seconds <= 0) "--" else SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(seconds * 1000))
