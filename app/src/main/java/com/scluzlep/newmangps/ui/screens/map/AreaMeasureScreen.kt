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
import com.scluzlep.newmangps.data.model.GeofenceItem
import com.scluzlep.newmangps.data.repository.NewsmyRepository
import com.scluzlep.newmangps.data.util.CoordinateTransform
import com.scluzlep.newmangps.ui.LocalAppLanguage
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreaMeasureScreen(
    teid: String, center: CoordinateTransform.LatLng?, repository: NewsmyRepository,
    onBack: () -> Unit
) {
    val language = LocalAppLanguage.current
    val scope = rememberCoroutineScope()
    var vertices by remember { mutableStateOf<List<CoordinateTransform.LatLng>>(emptyList()) }
    var name by remember { mutableStateOf("") }
    var shapes by remember { mutableStateOf<List<GeofenceItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            loading = true
            val result = repository.getUserShapes()
            shapes = result.getOrNull().orEmpty()
            error = result.exceptionOrNull()?.message
            loading = false
        }
    }
    LaunchedEffect(Unit) { refresh() }
    val area = polygonArea(vertices)

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(language.text("MeasureArea", "面积测量")) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, language.text("Back", "返回")) } }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            AmapJsMap(
                points = listOfNotNull(vertices.firstOrNull() ?: center),
                markerIndex = 0,
                onMapTap = { vertices = vertices + it },
                polygonPoints = vertices,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("${vertices.size} ${language.text("Vertex", "个顶点")} · ${"%.1f".format(area)} ${language.text("SquareMetres", "平方米")} · ${"%.3f".format(area * 0.0001)} ${language.text("hectare", "公顷")} · ${"%.3f".format(area * 0.0002471)} ${language.text("acre", "英亩")}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { vertices = vertices.dropLast(1) }, enabled = vertices.isNotEmpty()) { Text(language.text("Undo", "撤销")) }
                    TextButton(onClick = { vertices = emptyList(); name = "" }, enabled = vertices.isNotEmpty()) { Text(language.text("Clear", "清空")) }
                }
                OutlinedTextField(name, { name = it }, label = { Text(language.text("ShapeName", "形状名称")) }, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        val geometry = vertices.joinToString(";") { vertex ->
                            val point = CoordinateTransform.gcj02ToWgs84(vertex.latitude, vertex.longitude)
                            String.format(Locale.US, "%.7f,%.7f", point.longitude, point.latitude)
                        }
                        scope.launch {
                            val result = if (shapes.any { it.name == name }) {
                                repository.updateUserShape(name, geometry)
                            } else repository.addUserShape(name, geometry)
                            if (result.getOrDefault(false)) refresh()
                            else error = result.exceptionOrNull()?.message ?: language.text("SaveFailed", "保存失败")
                        }
                    },
                    enabled = vertices.size >= 3 && name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(language.text("SaveShape", "保存形状")) }
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Text(language.text("SavedShapes", "已保存形状"), style = MaterialTheme.typography.titleMedium)
                LazyColumn(Modifier.heightIn(max = 180.dp)) {
                    items(shapes) { shape ->
                        ListItem(
                            headlineContent = { Text(shape.name) },
                            supportingContent = { Text(language.text("TapToLoad", "点击加载")) },
                            trailingContent = {
                                Row {
                                    TextButton(onClick = {
                                        name = shape.name
                                        vertices = shape.geometry.split(';').mapNotNull { pair ->
                                            val coords = pair.split(',').mapNotNull(String::toDoubleOrNull)
                                            if (coords.size != 2) null else CoordinateTransform.wgs84ToGcj02(coords[1], coords[0])
                                        }
                                    }) { Text(language.text("View", "查看")) }
                                    IconButton(onClick = {
                                        scope.launch {
                                            val result = repository.deleteUserShape(shape.name)
                                            if (result.getOrDefault(false)) refresh()
                                            else error = result.exceptionOrNull()?.message ?: language.text("DeleteFailed", "删除失败")
                                        }
                                    }) { Icon(Icons.Default.Delete, language.text("DeleteShape", "删除形状")) }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun polygonArea(points: List<CoordinateTransform.LatLng>): Double {
    if (points.size < 3) return 0.0
    val radius = 6_378_137.0
    return abs(points.indices.sumOf { index ->
        val a = points[index]
        val b = points[(index + 1) % points.size]
        (Math.toRadians(b.longitude) - Math.toRadians(a.longitude)) *
            (2 + sin(Math.toRadians(a.latitude)) + sin(Math.toRadians(b.latitude)))
    }) * radius * radius / 2
}
