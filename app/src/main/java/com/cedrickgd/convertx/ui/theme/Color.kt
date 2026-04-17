package com.cedrickgd.convertx.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Dark palette
private val DarkBackground = Color(0xFF0B0512)
private val DarkSurface = Color(0xFF140825)
private val DarkSurfaceVariant = Color(0xFF1F0E37)
private val DarkOnSurface = Color(0xFFF3ECFF)
private val DarkOnSurfaceVariant = Color(0xFFBCA8D8)
private val DarkPrimary = Color(0xFFA855F7)
private val DarkPrimaryContainer = Color(0xFF6B21A8)
private val DarkOnPrimary = Color(0xFFFFFFFF)
private val DarkSecondary = Color(0xFFC084FC)
private val DarkOutline = Color(0xFF3B1E5E)

// Light palette
private val LightBackground = Color(0xFFFAFAFE)
private val LightSurface = Color(0xFFFFFFFF)
private val LightSurfaceVariant = Color(0xFFF1EAFA)
private val LightOnSurface = Color(0xFF140825)
private val LightPrimary = Color(0xFFA855F7)
private val LightPrimaryContainer = Color(0xFFEADCFF)
private val LightOutline = Color(0xFFD7C7EB)
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightSecondary = Color(0xFF7C3AED)
private val LightOnSurfaceVariant = Color(0xFF4B2E73)

val ConvertXDarkColors: ColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimary,
    secondary = DarkSecondary,
    onSecondary = DarkOnPrimary,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutline
)

val ConvertXLightColors: ColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnSurface,
    secondary = LightSecondary,
    onSecondary = LightOnPrimary,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutline
)

/**
 * Brand-specific tokens that live alongside the Material color scheme but are not
 * covered by it: glow color and the signature purple gradient brush.
 */
object BrandTokens {
    val AccentGlow: Color = Color(0xFFA855F7)

    val brandGradient: Brush = Brush.linearGradient(
        colors = listOf(DarkPrimary, DarkSecondary)
    )
}
