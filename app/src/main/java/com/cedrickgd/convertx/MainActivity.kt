package com.cedrickgd.convertx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cedrickgd.convertx.ui.navigation.ConvertXNavHost
import com.cedrickgd.convertx.ui.theme.ConvertXTheme
import com.cedrickgd.convertx.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { false }

        val app = application as ConvertXApplication
        val settingsRepo = app.container.settingsRepository

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.Transparent.toArgb(), Color.Transparent.toArgb()),
            navigationBarStyle = SystemBarStyle.auto(Color.Transparent.toArgb(), Color.Transparent.toArgb())
        )

        setContent {
            val mode by settingsRepo.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            ConvertXTheme(themeMode = mode) {
                Surface(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
                    ConvertXNavHost(appContainer = app.container)
                }
            }
        }
    }
}
