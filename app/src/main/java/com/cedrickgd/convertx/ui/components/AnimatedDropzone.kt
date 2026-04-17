package com.cedrickgd.convertx.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cedrickgd.convertx.ui.theme.ConvertXTheme
import com.cedrickgd.convertx.ui.theme.ThemeMode

/**
 * Large dashed dropzone card with a floating upload icon and a shimmering background.
 */
@Composable
fun AnimatedDropzone(
    onPickFiles: () -> Unit,
    title: String,
    subtitle: String,
    buttonLabel: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    val outline = MaterialTheme.colorScheme.outline
    val primary = MaterialTheme.colorScheme.primary

    val transition = rememberInfiniteTransition(label = "dropzone")
    val shimmer by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dropzone-shimmer"
    )
    val float by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dropzone-float"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 220.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), shape)
            .drawBehind {
                val sweepCenter = size.width * shimmer
                val band = size.width * 0.45f
                val shimmerBrush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        primary.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    start = Offset(sweepCenter - band, 0f),
                    end = Offset(sweepCenter + band, size.height)
                )
                drawRoundRect(
                    brush = shimmerBrush,
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
                )
                val stroke = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                )
                drawRoundRect(
                    color = outline,
                    topLeft = Offset(stroke.width / 2f, stroke.width / 2f),
                    size = Size(
                        size.width - stroke.width,
                        size.height - stroke.width
                    ),
                    cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                    style = stroke
                )
            }
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.CloudUpload,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer { translationY = float }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        GradientButton(onClick = onPickFiles) {
            Text(text = buttonLabel)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0512)
@Composable
private fun PreviewAnimatedDropzone() {
    ConvertXTheme(themeMode = ThemeMode.DARK) {
        AnimatedDropzone(
            onPickFiles = {},
            title = "Drop files here",
            subtitle = "Video / Image / Audio / Batch supported",
            buttonLabel = "Pick files",
            modifier = Modifier.padding(24.dp)
        )
    }
}
