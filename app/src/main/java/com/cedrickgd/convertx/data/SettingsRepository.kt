package com.cedrickgd.convertx.data

import android.net.Uri
import com.cedrickgd.convertx.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    val outputTreeUri: Flow<Uri?>
    val useAccentGlow: Flow<Boolean>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setOutputTreeUri(uri: Uri?)
    suspend fun setAccentGlow(enabled: Boolean)
}
