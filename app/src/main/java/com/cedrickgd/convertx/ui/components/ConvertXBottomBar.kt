package com.cedrickgd.convertx.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cedrickgd.convertx.ui.navigation.Destination
import com.cedrickgd.convertx.ui.theme.ConvertXTheme
import com.cedrickgd.convertx.ui.theme.ThemeMode

/**
 * Material 3 bottom nav with a translucent surface and a 1dp top divider drawn in the
 * theme's outline color.
 */
@Composable
fun ConvertXBottomBar(
    currentRoute: String?,
    onDestinationSelected: (Destination) -> Unit,
    modifier: Modifier = Modifier
) {
    val outline = MaterialTheme.colorScheme.outline
    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawLine(
                    color = outline,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = stroke
                )
            },
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        tonalElevation = 0.dp
    ) {
        Destination.topLevel.forEach { destination ->
            val selected = currentRoute == destination.route
            NavigationBarItem(
                selected = selected,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = null
                    )
                },
                label = { Text(text = stringResource(id = destination.labelRes)) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0512)
@Composable
private fun PreviewConvertXBottomBar() {
    ConvertXTheme(themeMode = ThemeMode.DARK) {
        ConvertXBottomBar(
            currentRoute = Destination.Home.route,
            onDestinationSelected = {},
            modifier = Modifier.height(80.dp)
        )
    }
}
