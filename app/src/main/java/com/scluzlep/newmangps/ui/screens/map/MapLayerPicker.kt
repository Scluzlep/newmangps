package com.scluzlep.newmangps.ui.screens.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import com.scluzlep.newmangps.ui.theme.AppIcons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scluzlep.newmangps.data.MapCoordSystem
import com.scluzlep.newmangps.data.MapSource
import com.scluzlep.newmangps.ui.LocalAppLanguage
import com.scluzlep.newmangps.ui.theme.HyperBlue
import com.scluzlep.newmangps.ui.theme.HyperCardShape
import com.scluzlep.newmangps.ui.theme.HyperChipShape
import com.scluzlep.newmangps.ui.theme.HyperGreen
import com.scluzlep.newmangps.ui.theme.HyperPillShape
import com.scluzlep.newmangps.ui.theme.HyperYellow

@Composable
fun MapLayerDialog(
    currentSource: MapSource,
    satellite: Boolean,
    traffic: Boolean,
    onSelectSource: (MapSource) -> Unit,
    onToggleSatellite: (Boolean) -> Unit,
    onToggleTraffic: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val language = LocalAppLanguage.current
    val satelliteSelected = satellite && currentSource.supportsSatellite

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = HyperCardShape,
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(
                imageVector = AppIcons.Layers,
                contentDescription = null,
                tint = HyperBlue
            )
        },
        title = {
            Text(
                text = language.text("MapSource", "地图源"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = language.text("MapStyle", "地图样式"),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !satelliteSelected,
                        onClick = { onToggleSatellite(false) },
                        modifier = Modifier.weight(1f),
                        shape = HyperChipShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HyperBlue.copy(alpha = 0.14f),
                            selectedLabelColor = HyperBlue,
                            selectedLeadingIconColor = HyperBlue
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = AppIcons.Map,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        label = {
                            Text(
                                text = language.text("StandardMap", "标准"),
                                maxLines = 1
                            )
                        }
                    )
                    FilterChip(
                        selected = satelliteSelected,
                        onClick = { onToggleSatellite(true) },
                        enabled = currentSource.supportsSatellite,
                        modifier = Modifier.weight(1f),
                        shape = HyperChipShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HyperBlue.copy(alpha = 0.14f),
                            selectedLabelColor = HyperBlue,
                            selectedLeadingIconColor = HyperBlue
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = AppIcons.SatelliteAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        label = {
                            Text(
                                text = language.text("Satellite", "卫星"),
                                maxLines = 1
                            )
                        }
                    )
                }

                if (currentSource.supportsTraffic) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = HyperChipShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(
                            width = 0.8.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = AppIcons.Traffic,
                                    contentDescription = null,
                                    tint = HyperBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = language.text("RoadTraffic", "交通路况"),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Switch(
                                checked = traffic,
                                onCheckedChange = onToggleTraffic,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = HyperBlue
                                )
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MapSource.entries.forEach { source ->
                        MapSourceRow(
                            source = source,
                            label = language.text(source.labelKey, source.fallbackLabel),
                            selected = source == currentSource,
                            onClick = { onSelectSource(source) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = language.text("Close", "关闭"),
                    color = HyperBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
private fun MapSourceRow(
    source: MapSource,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val badgeColor = when (source.coordSystem) {
        MapCoordSystem.GCJ02 -> HyperYellow
        MapCoordSystem.WGS84 -> HyperGreen
    }
    val badgeText = when (source.coordSystem) {
        MapCoordSystem.GCJ02 -> "GCJ-02"
        MapCoordSystem.WGS84 -> "WGS-84"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HyperChipShape)
            .clickable(onClick = onClick),
        shape = HyperChipShape,
        color = if (selected) {
            HyperBlue.copy(alpha = 0.10f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
        border = BorderStroke(
            width = if (selected) 1.2.dp else 0.8.dp,
            color = if (selected) {
                HyperBlue
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) HyperBlue else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = HyperPillShape,
                color = badgeColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            if (selected) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = HyperBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
