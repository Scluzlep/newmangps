package com.scluzlep.newmangps.ui.screens.alarm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scluzlep.newmangps.data.model.AlarmItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmHistoryScreen(
    alarms: List<AlarmItem>,
    periodHours: Int,
    loading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onPeriodSelected: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("历史告警") },
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
    ) { inset ->
        Column(Modifier.fillMaxSize().padding(inset).padding(horizontal = 16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                listOf(24, 48).forEach { hours ->
                    FilterChip(
                        selected = periodHours == hours,
                        onClick = { onPeriodSelected(hours) },
                        label = { Text("近 ${hours} 小时") }
                    )
                }
            }
            if (loading) CircularProgressIndicator()
            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (!loading && alarms.isEmpty() && errorMessage == null) {
                Text("所选时间内没有告警", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LazyColumn {
                items(alarms, key = AlarmItem::id) { alarm ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        Text(alarm.content, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                        Text("${alarm.teid} · ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(alarm.timeSeconds * 1000))}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
