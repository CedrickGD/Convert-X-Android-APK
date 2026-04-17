package com.cedrickgd.convertx.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cedrickgd.convertx.R
import com.cedrickgd.convertx.ui.theme.ConvertXTheme
import com.cedrickgd.convertx.ui.theme.ThemeMode

/**
 * Top bar for Convert-X: shows the brand wordmark with a subtle animated shimmer sweep
 * behind it, a placeholder menu button on the left and a theme-toggle on the right.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConvertXTopBar(
    themeMode: ThemeMode,
    onCycleTheme: () -> Unit,
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {}
) {
    CenterAlignedTopAppBar(
        modifier = modifier.fillMaxWidth(),
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
        title = {
            ShimmerWordmark()
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = stringResource(id = R.string.nav_home),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            IconButton(onClick = onCycleTheme) {
                Icon(
                    imageVector = themeIcon(themeMode),
                    contentDescription = stringResource(id = R.string.settings_theme),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    )
}

private fun themeIcon(mode: ThemeMode): ImageVector = when (mode) {
    ThemeMode.SYSTEM -> Icons.Filled.Brightness6
    ThemeMode.LIGHT -> Icons.Filled.LightMode
    ThemeMode.DARK -> Icons.Filled.DarkMode
}

@Composable
private fun ShimmerWordmark() {
    val transition = rememberInfiniteTransition(label = "wordmark-shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wordmark-shimmer-progress"
    )

    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .drawBehind {
                val sweepCenter = size.width * progress
                val band = size.width * 0.4f
                val brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        primary.copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    start = Offset(sweepCenter - band, 0f),
                    end = Offset(sweepCenter + band, size.height)
                )
                drawRect(brush = brush)
            }
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = onSurface)) { append("Convert-") }
                withStyle(SpanStyle(color = primary)) { append("X") }
            },
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0512)
@Composable
private fun PreviewConvertXTopBar() {
    ConvertXTheme(themeMode = ThemeMode.DARK) {
        ConvertXTopBar(themeMode = ThemeMode.DARK, onCycleTheme = {})
    }
}
