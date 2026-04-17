package com.cedrickgd.convertx.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cedrickgd.convertx.data.AppContainer
import com.cedrickgd.convertx.ui.components.ConvertXBottomBar
import com.cedrickgd.convertx.ui.components.ConvertXTopBar
import com.cedrickgd.convertx.ui.screens.home.HomeScreen
import com.cedrickgd.convertx.ui.screens.resize.ResizeScreen
import com.cedrickgd.convertx.ui.screens.settings.SettingsScreen
import com.cedrickgd.convertx.ui.theme.ThemeMode
import kotlinx.coroutines.launch

/**
 * Root composable for the app. Hosts the top/bottom bars and the nav graph.
 */
@Composable
fun ConvertXNavHost(
    appContainer: AppContainer,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val settingsRepo = appContainer.settingsRepository
    val scope = rememberCoroutineScope()
    val themeMode by settingsRepo.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            ConvertXTopBar(
                themeMode = themeMode,
                onCycleTheme = {
                    val next = when (themeMode) {
                        ThemeMode.SYSTEM -> ThemeMode.LIGHT
                        ThemeMode.LIGHT -> ThemeMode.DARK
                        ThemeMode.DARK -> ThemeMode.SYSTEM
                    }
                    scope.launch { settingsRepo.setThemeMode(next) }
                }
            )
        },
        bottomBar = {
            ConvertXBottomBar(
                currentRoute = currentRoute,
                onDestinationSelected = { destination ->
                    if (currentRoute != destination.route) {
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Destination.Home.route) {
                HomeScreen(appContainer = appContainer)
            }
            composable(Destination.Resize.route) {
                ResizeScreen(appContainer = appContainer)
            }
            composable(Destination.History.route) {
                HistoryScreen()
            }
            composable(Destination.Settings.route) {
                SettingsScreen(appContainer = appContainer)
            }
        }
    }
}

/**
 * Placeholder history screen used until a real history feature lands.
 */
@Composable
private fun HistoryScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = "History coming soon",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}
