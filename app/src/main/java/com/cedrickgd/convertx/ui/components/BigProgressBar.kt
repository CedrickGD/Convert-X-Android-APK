package com.cedrickgd.convertx.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cedrickgd.convertx.ui.theme.BrandTokens
import kotlin.math.roundToInt

/**
 * A tall pill-shaped progress bar that shows a shimmer traveling through the filled portion.
 * Used for the overall batch progress during conversions.
 */
@Composable
fun BigProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    val clamped = progress.coerceIn(0f, 1f)
    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val trackWidth = maxWidth
            val fillWidth = trackWidth * clamped

            val transition = rememberInfiniteTransition(label = "big-progress-shimmer")
            val shimmerX by transition.animateFloat(
                initialValue = -0.5f,
                targetValue = 1.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1600, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "shimmer-x"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped.coerceAtLeast(0.001f))
                    .height(18.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrandTokens.brandGradient)
            )

            if (clamped > 0.02f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(clamped)
                        .height(18.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.35f),
                                    Color.Transparent
                                ),
                                startX = fillWidth.value * shimmerX - 80f,
                                endX = fillWidth.value * shimmerX + 80f
                            )
                        )
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = label ?: "${(clamped * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
