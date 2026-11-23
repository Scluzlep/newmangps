package com.scluzlep.newmangps.ui.screens.track

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scluzlep.newmangps.data.MapSource
import com.scluzlep.newmangps.data.model.TrackPoint
import com.scluzlep.newmangps.data.repository.NewsmyRepository
import com.scluzlep.newmangps.data.util.CoordinateTransform
import com.scluzlep.newmangps.ui.LocalAppLanguage
import com.scluzlep.newmangps.ui.screens.map.AmapJsMap
import com.scluzlep.newmangps.ui.screens.map.MapLayerDialog
import com.scluzlep.newmangps.ui.theme.*
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackReplayScreen(
    teid: String,
    repository: NewsmyRepository,
    mapSource: MapSource = MapSource.AMAP,
    satellite: Boolean = false,
    traffic: Boolean = false,
    fallbackCenter: CoordinateTransform.LatLng? = null,
    onSelectMapSource: (MapSource) -> Unit = {},
    onToggleSatellite: (Boolean) -> Unit = {},
    onToggleTraffic: (Boolean) -> Unit = {},
    onBack: () -> Unit
) {
    val language = LocalAppLanguage.current
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var trackPoints by remember { mutableStateOf<List<TrackPoint>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var speedMultiplier by remember { mutableIntStateOf(60) }
    var showLayerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(teid, selectedDate) {
        loading = true
        error = null
        val zone = ZoneId.systemDefault()
        val start = selectedDate.atStartOfDay(zone).toEpochSecond()
        val end = selectedDate.plusDays(1).atStartOfDay(zone).toEpochSecond() - 1
        val result = repository.getTrackHistory(teid, start, end)
        trackPoints = result.getOrNull().orEmpty()
        error = result.exceptionOrNull()?.message
        loading = false
    }

    LaunchedEffect(trackPoints) {
        currentIndex = 0
        isPlaying = false
    }
    LaunchedEffect(isPlaying, speedMultiplier, trackPoints) {
        if (isPlaying && trackPoints.size > 1) {
            while (isPlaying && currentIndex < trackPoints.size - 1) {
                val gap = (trackPoints[currentIndex + 1].timeSeconds -
                    trackPoints[currentIndex].timeSeconds).coerceAtLeast(1)
                delay((gap * 1000 / speedMultiplier).coerceIn(100, 5000))
                currentIndex++
            }
            if (currentIndex >= trackPoints.size - 1) {
                isPlaying = false
            }
        }
    }

    val currentPoint = trackPoints.getOrNull(currentIndex)
    val mapPoints = remember(trackPoints, fallbackCenter) {
        if (trackPoints.isNotEmpty()) {
            trackPoints.map { CoordinateTransform.LatLng(it.rawLat, it.rawLon) }
        } else {
            listOfNotNull(fallbackCenter)
        }
    }
    val routeSpeeds = remember(trackPoints) { trackPoints.map { it.speed } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "${language.text("HistoryTrack", "历史轨迹")} · ${language.text("ReplayTrack", "回放")}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${language.text("TrackPointCount", "历史轨迹点")}：${trackPoints.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = language.text("Back", "返回"))
                    }
                },
                actions = {
                    IconButton(onClick = { showLayerDialog = true }) {
                        Icon(Icons.Default.Layers, contentDescription = language.text("MapSource", "地图源"))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AmapJsMap(
                points = mapPoints,
                markerIndex = if (trackPoints.isNotEmpty()) currentIndex else 0,
                mapSource = mapSource,
                satellite = satellite,
                traffic = traffic,
                markerDirection = if (trackPoints.isNotEmpty()) currentPoint?.direction ?: 0 else 0,
                followMarker = trackPoints.isNotEmpty(),
                routeSpeeds = routeSpeeds,
                modifier = Modifier.fillMaxSize()
            )

            if (!loading && trackPoints.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = language.text("NoValidTrackPoints", "此时间段没有有效轨迹点"),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Bottom Replay Control Card (Xiaomi HyperOS Style)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                HyperCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { selectedDate = selectedDate.minusDays(1) }) { Text(language.text("PreviousDay", "前一天")) }
                        Text(selectedDate.toString(), modifier = Modifier.padding(top = 12.dp))
                        TextButton(onClick = { selectedDate = selectedDate.plusDays(1) },
                            enabled = selectedDate.isBefore(LocalDate.now())) { Text(language.text("NextDay", "后一天")) }
                    }
                    if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentPoint?.formattedTime ?: "--",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentPoint?.let {
                                "${language.text("Speed", "速度")}: ${it.speed} ${language.text("KMH", "km/h")} · ${language.text("Mileage", "里程")}: ${String.format(Locale.US, "%.2f", it.mileageKm)} km"
                            } ?: language.text("NoTrackData", "暂无轨迹数据"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Slider(
                        value = currentIndex.toFloat(),
                        onValueChange = {
                            currentIndex = it.toInt()
                            isPlaying = false
                        },
                        enabled = trackPoints.size > 1,
                        valueRange = 0f..(if (trackPoints.isEmpty()) 1f else (trackPoints.size - 1).toFloat()),
                        colors = SliderDefaults.colors(
                            thumbColor = HyperBlue,
                            activeTrackColor = HyperBlue
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Speed selector
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(60, 120, 240).forEach { spd ->
                                FilterChip(
                                    selected = speedMultiplier == spd,
                                    onClick = { speedMultiplier = spd },
                                    label = { Text("${spd}x") }
                                )
                            }
                        }

                        // Play / Pause Button
                        Button(
                            onClick = {
                                if (currentIndex >= trackPoints.size - 1) {
                                    currentIndex = 0
                                }
                                isPlaying = !isPlaying
                            },
                            enabled = trackPoints.size > 1,
                            colors = ButtonDefaults.buttonColors(containerColor = HyperBlue),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) language.text("Pause", "暂停") else language.text("Play", "播放")
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLayerDialog) {
        MapLayerDialog(
            currentSource = mapSource,
            satellite = satellite,
            traffic = traffic,
            onSelectSource = onSelectMapSource,
            onToggleSatellite = onToggleSatellite,
            onToggleTraffic = onToggleTraffic,
            onDismiss = { showLayerDialog = false }
        )
    }
}

