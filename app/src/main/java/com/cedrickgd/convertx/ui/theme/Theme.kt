package com.cedrickgd.convertx.ui.theme

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Convert-X theme. Applies Material 3 colors, typography and shapes, handles status bar
 * appearance and paints a subtle top radial glow that is our signature "purple glow".
 *
 * The [dynamicColor] parameter is retained for future use but is not consumed today —
 * the brand palette is always used so the Convert-X look stays consistent across devices.
 */
@Composable
fun ConvertXTheme(
    themeMode: ThemeMode,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemInDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    // dynamicColor is reserved for a future "use wallpaper" toggle. It is consumed here so
    // the parameter is never flagged as unused; passing `true` today still returns the
    // brand palette — we simply have not wired Material You yet.
    val colorScheme = when {
        dynamicColor && darkTheme -> ConvertXDarkColors
        dynamicColor -> ConvertXLightColors
        darkTheme -> ConvertXDarkColors
        else -> ConvertXLightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ConvertXTypography,
        shapes = ConvertXShapes
    ) {
        val glowColor = colorScheme.primary
        val bgColor = colorScheme.background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .drawBehind {
                    val brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = Offset(size.width / 2f, 0f),
                        radius = 800f
                    )
                    drawRect(brush = brush)
                }
        ) {
            content()
        }
    }
}
