package com.cedrickgd.convertx.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cedrickgd.convertx.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "convertx_prefs")

private object PrefKeys {
    val ThemeMode = stringPreferencesKey("theme_mode")
    val OutputTreeUri = stringPreferencesKey("output_tree_uri")
    val UseAccentGlow = booleanPreferencesKey("use_accent_glow")
}

/**
 * DataStore-backed implementation of [SettingsRepository].
 * Reads and writes user preferences as a single "convertx_prefs" file.
 */
class DataStoreSettingsRepository(context: Context) : SettingsRepository {

    private val store: DataStore<Preferences> = context.applicationContext.dataStore

    override val themeMode: Flow<ThemeMode> = store.data.map { prefs ->
        val raw = prefs[PrefKeys.ThemeMode]
        parseThemeMode(raw)
    }

    override val outputTreeUri: Flow<Uri?> = store.data.map { prefs ->
        prefs[PrefKeys.OutputTreeUri]?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
    }

    override val useAccentGlow: Flow<Boolean> = store.data.map { prefs ->
        prefs[PrefKeys.UseAccentGlow] ?: true
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { prefs ->
            prefs[PrefKeys.ThemeMode] = mode.name
        }
    }

    override suspend fun setOutputTreeUri(uri: Uri?) {
        store.edit { prefs ->
            val value = uri?.toString()
            if (value.isNullOrBlank()) {
                prefs.remove(PrefKeys.OutputTreeUri)
            } else {
                prefs[PrefKeys.OutputTreeUri] = value
            }
        }
    }

    override suspend fun setAccentGlow(enabled: Boolean) {
        store.edit { prefs ->
            prefs[PrefKeys.UseAccentGlow] = enabled
        }
    }

    private fun parseThemeMode(raw: String?): ThemeMode {
        if (raw.isNullOrBlank()) return ThemeMode.SYSTEM
        return runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.SYSTEM)
    }
}
