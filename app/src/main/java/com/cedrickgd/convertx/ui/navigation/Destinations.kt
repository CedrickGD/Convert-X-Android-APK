package com.cedrickgd.convertx.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.cedrickgd.convertx.R

/**
 * A top-level tab destination in Convert-X. Displayed by the bottom nav and routed by the
 * [ConvertXNavHost].
 */
sealed class Destination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    data object Home : Destination(
        route = "home",
        labelRes = R.string.nav_home,
        icon = Icons.Rounded.Home
    )

    data object Resize : Destination(
        route = "resize",
        labelRes = R.string.nav_resize,
        icon = Icons.Filled.AspectRatio
    )

    data object History : Destination(
        route = "history",
        labelRes = R.string.nav_history,
        icon = Icons.Filled.History
    )

    data object Settings : Destination(
        route = "settings",
        labelRes = R.string.nav_settings,
        icon = Icons.Rounded.Settings
    )

    companion object {
        val topLevel: List<Destination> = listOf(Home, Resize, History, Settings)
    }
}
