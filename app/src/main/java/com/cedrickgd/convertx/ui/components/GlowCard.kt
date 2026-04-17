package com.cedrickgd.convertx.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Card container used for feature blocks. When [glowing] is true the border pulses
 * with the brand purple.
 */
@Composable
fun GlowCard(
    modifier: Modifier = Modifier,
    glowing: Boolean = false,
    content: @Composable () -> Unit
) {
    val shape = MaterialTheme.shapes.large
    val outline = MaterialTheme.colorScheme.outline
    val primary = MaterialTheme.colorScheme.primary

    val transition = rememberInfiniteTransition(label = "glow-card-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow-card-pulse-alpha"
    )

    val borderColor = if (glowing) primary.copy(alpha = pulse) else outline
    val borderWidth = if (glowing) 1.5.dp else 1.dp

    ElevatedCard(
        modifier = modifier
            .border(width = borderWidth, color = borderColor, shape = shape),
        shape = shape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0512)
@Composable
private fun PreviewGlowCardIdle() {
    com.cedrickgd.convertx.ui.theme.ConvertXTheme(
        themeMode = com.cedrickgd.convertx.ui.theme.ThemeMode.DARK
    ) {
        GlowCard(modifier = Modifier.padding(24.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "Idle card", style = MaterialTheme.typography.titleMedium)
                Text(text = "Border matches the outline color.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0512)
@Composable
private fun PreviewGlowCardActive() {
    com.cedrickgd.convertx.ui.theme.ConvertXTheme(
        themeMode = com.cedrickgd.convertx.ui.theme.ThemeMode.DARK
    ) {
        GlowCard(modifier = Modifier.padding(24.dp), glowing = true) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "Active card", style = MaterialTheme.typography.titleMedium)
                Text(text = "Border pulses with the brand glow.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
