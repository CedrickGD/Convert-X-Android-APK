package com.cedrickgd.convertx.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cedrickgd.convertx.ui.theme.BrandTokens
import com.cedrickgd.convertx.ui.theme.ConvertXTheme
import com.cedrickgd.convertx.ui.theme.ThemeMode

/**
 * Primary brand CTA. Renders a gradient-filled button with a content slot, a soft purple
 * drop-shadow and a subtle press scale-down animation.
 */
@Composable
fun GradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "gradient-button-scale"
    )

    val shadowColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val disabledBackground: Brush = SolidColor(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
    val background: Brush = if (enabled) BrandTokens.brandGradient else disabledBackground
    val contentColor = if (enabled) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (enabled) 24.dp else 0.dp,
                shape = shape,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(shape)
            .background(brush = background, shape = shape)
            .defaultMinSize(minHeight = 48.dp)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(color = Color.White),
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            ProvideTextStyle(value = MaterialTheme.typography.labelLarge) {
                content()
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0512)
@Composable
private fun PreviewGradientButton() {
    ConvertXTheme(themeMode = ThemeMode.DARK) {
        Box(modifier = Modifier.padding(24.dp)) {
            GradientButton(onClick = {}) {
                Text(text = "Convert now")
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0512)
@Composable
private fun PreviewGradientButtonDisabled() {
    ConvertXTheme(themeMode = ThemeMode.DARK) {
        Box(modifier = Modifier.padding(24.dp)) {
            GradientButton(onClick = {}, enabled = false) {
                Text(text = "Convert now")
            }
        }
    }
}
