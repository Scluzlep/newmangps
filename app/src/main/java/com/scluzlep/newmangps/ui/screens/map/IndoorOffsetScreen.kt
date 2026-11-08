package com.scluzlep.newmangps.ui.screens.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scluzlep.newmangps.data.model.DevicePosition
import com.scluzlep.newmangps.data.model.IndoorOffset
import com.scluzlep.newmangps.data.repository.NewsmyRepository
import com.scluzlep.newmangps.data.util.CoordinateTransform
import com.scluzlep.newmangps.ui.LocalAppLanguage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndoorOffsetScreen(
    teid: String, position: DevicePosition?, repository: NewsmyRepository, onBack: () -> Unit
) {
    val language = LocalAppLanguage.current
    val scope = rememberCoroutineScope()
    var offsets by remember { mutableStateOf<List<IndoorOffset>>(emptyList()) }
    var selected by remember { mutableStateOf<IndoorOffset?>(null) }
    var target by remember { mutableStateOf<CoordinateTransform.LatLng?>(null) }
    var address by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val source = selected?.let { CoordinateTransform.wgs84ToGcj02(it.latitude, it.longitude) }
        ?: position?.gcjLatLng
    val destination = selected?.let { CoordinateTransform.wgs84ToGcj02(it.targetLatitude, it.targetLongitude) }
        ?: target

    fun refresh() {
        scope.launch {
            busy = true
            val result = repository.getIndoorOffsets(teid)
            offsets = result.getOrNull().orEmpty()
            error = result.exceptionOrNull()?.message
            busy = false
        }
    }
    LaunchedEffect(teid) { refresh() }

    Scaffold(topBar = {
        TopAppBar(title = { Text(language.text("PosOffset", "室内位置修正")) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, language.text("Back", "返回")) } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (source != null) AmapJsMap(
                points = listOfNotNull(source, destination), markerIndex = if (destination == null) 0 else 1,
                onMapTap = { selected = null; target = it }, modifier = Modifier.fillMaxWidth().weight(1f)
            ) else Box(Modifier.fillMaxWidth().weight(1f)) {
                Text(language.text("NoLocationForOffset", "设备尚无有效定位，无法设置修正点"), modifier = Modifier.padding(16.dp))
            }
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (selected != null) Text("${language.text("Viewing", "正在查看")} ${selected?.name}")
                else Text(if (target == null) language.text("SelectCorrectedLocation", "在地图上选择修正后的坐标") else
                    "${language.text("CorrectedCoordinates", "修正坐标")} ${"%.6f".format(target?.latitude)}, ${"%.6f".format(target?.longitude)}")
                OutlinedTextField(address, { address = it }, label = { Text(language.text("PositionDescription", "位置说明")) },
                    modifier = Modifier.fillMaxWidth(), enabled = !busy)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val sourcePosition = position ?: return@Button
                        val targetPoint = target ?: return@Button
                        val wgs = CoordinateTransform.gcj02ToWgs84(targetPoint.latitude, targetPoint.longitude)
                        scope.launch {
                            busy = true
                            val result = repository.addIndoorOffset(teid, sourcePosition,
                                wgs.longitude, wgs.latitude, address)
                            error = if (result.getOrDefault(false)) null else result.exceptionOrNull()?.message ?: language.text("SaveFailed", "保存失败")
                            if (result.getOrDefault(false)) { target = null; refresh() }
                            busy = false
                        }
                    }, enabled = !busy && selected == null && target != null && position != null) {
                        Text(language.text("SaveCorrection", "保存修正"))
                    }
                    if (selected != null) TextButton(onClick = { selected = null; target = null; address = "" }) {
                        Text(language.text("Add", "新增"))
                    }
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Text(language.text("SavedLocations", "已保存位置"), style = MaterialTheme.typography.titleMedium)
                LazyColumn(Modifier.heightIn(max = 180.dp)) {
                    items(offsets, key = IndoorOffset::name) { item ->
                        ListItem(headlineContent = { Text(item.address.ifBlank { item.name }) },
                            supportingContent = { Text(item.name) },
                            trailingContent = {
                                Row {
                                    TextButton(onClick = { selected = item; address = item.address }) { Text(language.text("View", "查看")) }
                                    IconButton(onClick = {
                                        scope.launch {
                                            busy = true
                                            val result = repository.deleteIndoorOffset(teid, item.name)
                                            error = if (result.getOrDefault(false)) null else result.exceptionOrNull()?.message ?: language.text("DeleteFailed", "删除失败")
                                            if (result.getOrDefault(false)) {
                                                if (selected?.name == item.name) selected = null
                                                refresh()
                                            }
                                            busy = false
                                        }
                                    }, enabled = !busy) { Icon(Icons.Default.Delete, language.text("DelCorrection", "删除修正")) }
                                }
                            })
                    }
                }
            }
        }
    }
}
