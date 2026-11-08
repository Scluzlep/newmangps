package com.scluzlep.newmangps.ui.screens.report

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scluzlep.newmangps.data.model.MonthlyInstallCount
import com.scluzlep.newmangps.data.model.UserStats
import com.scluzlep.newmangps.data.repository.NewsmyRepository
import com.scluzlep.newmangps.ui.LocalAppLanguage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(repository: NewsmyRepository, onBack: () -> Unit) {
    val language = LocalAppLanguage.current
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<UserStats?>(null) }
    var installs by remember { mutableStateOf<List<MonthlyInstallCount>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            loading = true
            val summary = repository.getUserStats()
            val monthly = repository.getInstallStats()
            stats = summary.getOrNull()
            installs = monthly.getOrNull().orEmpty()
            error = summary.exceptionOrNull()?.message ?: monthly.exceptionOrNull()?.message
            loading = false
        }
    }
    LaunchedEffect(Unit) { refresh() }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(language.text("Stats", "账户统计")) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, language.text("Back", "返回")) } },
            actions = { IconButton(onClick = { refresh() }, enabled = !loading) { Icon(Icons.Default.Refresh, language.text("Refresh", "刷新")) } }
        )
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp)) {
            if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            stats?.let { value ->
                item { Text(language.text("Device", "设备"), style = MaterialTheme.typography.titleLarge) }
                val rows = listOf(
                    language.text("Total", "总数") to value.carTotal,
                    language.text("Online", "在线") to value.onlineTotal,
                    language.text("Offline", "离线") to value.offlineTotal,
                    language.text("NotActive", "未激活") to value.unactivatedTotal,
                    language.text("HaveExpired", "已到期") to value.timeoutTotal,
                    language.text("Alarm", "报警") to value.alarmTotal,
                    language.text("SubUser", "子账号") to value.subUserTotal,
                    language.text("DayOnline", "今天在线") to value.dayOnlineTotal,
                    language.text("WeekOnline", "本周在线") to value.weekOnlineTotal,
                    language.text("MonthOnline", "本月在线") to value.monthOnlineTotal,
                    language.text("QuarterlyOnline", "本季度在线") to value.quarterOnlineTotal,
                    language.text("YearOnline", "本年在线") to value.yearOnlineTotal
                )
                items(rows) { (label, count) ->
                    ListItem(headlineContent = { Text(label) }, trailingContent = { Text(count.toString()) })
                    HorizontalDivider()
                }
            }
            if (installs.isNotEmpty()) {
                item { Spacer(Modifier.height(16.dp)); Text(language.text("InstallCount", "装机量"), style = MaterialTheme.typography.titleLarge) }
                items(installs) { month ->
                    val label = if (month.monthSeconds > 0) {
                        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(month.monthSeconds * 1000))
                    } else "--"
                    ListItem(headlineContent = { Text(label) }, trailingContent = { Text("${month.count} 台") })
                    HorizontalDivider()
                }
            }
        }
    }
}
