package com.cedrickgd.convertx.data

import android.content.Context
import com.cedrickgd.convertx.domain.ConversionEngine
import com.cedrickgd.convertx.engine.FFmpegEngine

/**
 * Manual DI container. Owns lifetime-scoped singletons for the app.
 */
class AppContainer(context: Context) {
    private val appContext: Context = context.applicationContext

    val settingsRepository: SettingsRepository = DataStoreSettingsRepository(appContext)

    val conversionEngine: ConversionEngine = FFmpegEngine(appContext)
}
