package com.cedrickgd.convertx.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.cedrickgd.convertx.domain.Format
import com.cedrickgd.convertx.ui.components.GlowCard
import kotlin.math.roundToInt

private data class ResolutionOption(val label: String, val value: String?)

private val ResolutionOptions = listOf(
    ResolutionOption("Original", null),
    ResolutionOption("480p", "854:480"),
    ResolutionOption("720p", "1280:720"),
    ResolutionOption("1080p", "1920:1080"),
    ResolutionOption("1440p", "2560:1440"),
    ResolutionOption("2160p", "3840:2160")
)

private data class BitrateOption(val label: String, val value: String?)

private val BitrateOptions = listOf(
    BitrateOption("Auto", null),
    BitrateOption("1 Mbps", "1M"),
    BitrateOption("2 Mbps", "2M"),
    BitrateOption("5 Mbps", "5M"),
    BitrateOption("10 Mbps", "10M")
)

private val PresetOptions = listOf("ultrafast", "fast", "medium", "slow")

@Composable
fun AdvancedSettings(
    format: Format,
    resolution: String?,
    onResolutionChange: (String?) -> Unit,
    fps: Int?,
    onFpsChange: (Int?) -> Unit,
    bitrate: String?,
    onBitrateChange: (String?) -> Unit,
    preset: String?,
    onPresetChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val supportsPreset = format == Format.MP4 || format == Format.MKV || format == Format.MOV
    var expanded by remember { mutableStateOf(false) }
    GlowCard(modifier = modifier.fillMaxWidth(), glowing = false) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Advanced",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LabeledDropdown(
                    label = "Resolution",
                    currentLabel = ResolutionOptions.firstOrNull { it.value == resolution }?.label
                        ?: "Original",
                    options = ResolutionOptions.map { it.label },
                    onSelect = { idx ->
                        onResolutionChange(ResolutionOptions[idx].value)
                    }
                )
                FpsSliderRow(fps = fps, onFpsChange = onFpsChange)
                LabeledDropdown(
                    label = "Bitrate",
                    currentLabel = BitrateOptions.firstOrNull { it.value == bitrate }?.label
                        ?: "Auto",
                    options = BitrateOptions.map { it.label },
                    onSelect = { idx ->
                        onBitrateChange(BitrateOptions[idx].value)
                    }
                )
                if (supportsPreset) {
                    LabeledDropdown(
                        label = "Preset",
                        currentLabel = preset ?: "medium",
                        options = PresetOptions,
                        onSelect = { idx -> onPresetChange(PresetOptions[idx]) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FpsSliderRow(fps: Int?, onFpsChange: (Int?) -> Unit) {
    val stops = listOf<Int?>(null, 24, 30, 60)
    val currentIdx = stops.indexOf(fps).coerceAtLeast(0)
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "FPS",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stops[currentIdx]?.let { "$it fps" } ?: "Original",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = currentIdx.toFloat(),
            onValueChange = { v -> onFpsChange(stops[v.roundToInt().coerceIn(0, stops.lastIndex)]) },
            valueRange = 0f..(stops.size - 1).toFloat(),
            steps = stops.size - 2
        )
    }
}

@Composable
private fun LabeledDropdown(
    label: String,
    currentLabel: String,
    options: List<String>,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Box {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { expanded = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEachIndexed { index, optLabel ->
                    DropdownMenuItem(
                        text = { Text(optLabel) },
                        onClick = {
                            onSelect(index)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
