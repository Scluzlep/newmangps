package com.scluzlep.newmangps.ui.screens.track

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scluzlep.newmangps.data.model.TrackPoint
import com.scluzlep.newmangps.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackReplayScreen(
    trackPoints: List<TrackPoint>,
    onBack: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var speedMultiplier by remember { mutableIntStateOf(1) } // 1x, 2x, 4x

    // Playback loop
    LaunchedEffect(isPlaying, speedMultiplier) {
        if (isPlaying && trackPoints.isNotEmpty()) {
            while (isPlaying && currentIndex < trackPoints.size - 1) {
                delay(1000L / speedMultiplier)
                currentIndex++
            }
            if (currentIndex >= trackPoints.size - 1) {
                isPlaying = false
            }
        }
    }

    val currentPoint = trackPoints.getOrNull(currentIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "历史轨迹回放",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "共 ${trackPoints.size} 个历史轨迹点",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
            // Simulated Map Route Canvas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE8ECEF)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = null,
                        tint = HyperGreen,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "正在回放第 ${currentIndex + 1} / ${trackPoints.size} 点",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HyperTextPrimaryLight
                    )
                    Text(
                        text = "时间: ${currentPoint?.formattedTime ?: "--"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HyperTextSecondaryLight
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
                            text = "速度: ${currentPoint?.speed ?: 0} km/h · 里程: ${currentPoint?.mileage ?: 0} km",
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
                            listOf(1, 2, 4).forEach { spd ->
                                FilterChip(
                                    selected = speedMultiplier == spd,
                                    onClick = { speedMultiplier = spd },
                                    label = { Text("${spd}x") }
                                )
                            }
                        }

                        // Play / Pause Button
                        FloatingActionButton(
                            onClick = {
                                if (currentIndex >= trackPoints.size - 1) {
                                    currentIndex = 0
                                }
                                isPlaying = !isPlaying
                            },
                            containerColor = HyperBlue,
                            contentColor = Color.White,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "暂停" else "播放"
                            )
                        }
                    }
                }
            }
        }
    }
}

