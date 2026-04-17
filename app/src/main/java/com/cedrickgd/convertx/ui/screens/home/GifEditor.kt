package com.cedrickgd.convertx.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cedrickgd.convertx.ui.components.GlowCard
import com.cedrickgd.convertx.ui.components.SectionHeader
import com.cedrickgd.convertx.util.formatDuration
import kotlin.math.roundToInt

@Composable
fun GifEditor(
    duration: Double,
    trimStart: Double?,
    trimEnd: Double?,
    onTrimChange: (start: Double, end: Double) -> Unit,
    stripAudio: Boolean,
    onStripAudioChange: (Boolean) -> Unit,
    gifColors: Int,
    onGifColorsChange: (Int) -> Unit,
    gifFps: Int,
    onGifFpsChange: (Int) -> Unit,
    gifWidth: Int,
    onGifWidthChange: (Int) -> Unit,
    gifTargetSizeMb: Int,
    onGifTargetSizeMbChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val startValue = (trimStart ?: 0.0).toFloat()
    val endValue = (trimEnd ?: duration).toFloat().coerceAtLeast(startValue + 0.1f)
    val maxValue = duration.toFloat().coerceAtLeast(endValue)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionHeader(text = "GIF Editor")
        GlowCard(modifier = Modifier.fillMaxWidth(), glowing = true) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (duration > 0.0) {
                    Text(
                        text = "Trim",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    RangeSlider(
                        value = startValue..endValue,
                        onValueChange = { range ->
                            onTrimChange(range.start.toDouble(), range.endInclusive.toDouble())
                        },
                        valueRange = 0f..maxValue,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            text = formatDuration(startValue.toDouble()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = formatDuration(endValue.toDouble()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Strip audio",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Always on for GIF",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = stripAudio,
                        onCheckedChange = onStripAudioChange,
                        enabled = false,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                Spacer(Modifier.height(2.dp))
                SliderRow(
                    label = "Colors",
                    valueLabel = gifColors.toString(),
                    value = gifColors.toFloat(),
                    onChange = { onGifColorsChange(roundToStep(it, 32).coerceIn(32, 256)) },
                    range = 32f..256f,
                    steps = 6
                )
                SliderRow(
                    label = "FPS",
                    valueLabel = "$gifFps fps",
                    value = gifFps.toFloat(),
                    onChange = { onGifFpsChange(it.roundToInt().coerceIn(8, 30)) },
                    range = 8f..30f,
                    steps = 21
                )
                SliderRow(
                    label = "Max width",
                    valueLabel = "${gifWidth}px",
                    value = gifWidth.toFloat(),
                    onChange = { onGifWidthChange(roundToStep(it, 60).coerceIn(240, 720)) },
                    range = 240f..720f,
                    steps = 7
                )
                SliderRow(
                    label = "Target size",
                    valueLabel = if (gifTargetSizeMb == 0) "Unlimited" else "$gifTargetSizeMb MB",
                    value = gifTargetSizeMb.toFloat(),
                    onChange = { onGifTargetSizeMbChange(it.roundToInt().coerceIn(0, 10)) },
                    range = 0f..10f,
                    steps = 9
                )
            }
        }
    }
}

private fun roundToStep(value: Float, step: Int): Int {
    val v = value.roundToInt()
    return ((v + step / 2) / step) * step
}

@Composable
private fun SliderRow(
    label: String,
    valueLabel: String,
    value: Float,
    onChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    steps: Int
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            steps = steps
        )
    }
}
