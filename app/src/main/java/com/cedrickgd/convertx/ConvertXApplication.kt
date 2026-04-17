package com.cedrickgd.convertx

import android.app.Application
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.Level
import com.cedrickgd.convertx.data.AppContainer

class ConvertXApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        FFmpegKitConfig.setLogLevel(if (BuildConfig.DEBUG) Level.AV_LOG_INFO else Level.AV_LOG_WARNING)
    }
}
