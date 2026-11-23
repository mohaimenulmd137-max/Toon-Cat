package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.ExportResolution
import com.example.domain.model.PreviewQuality
import com.example.domain.model.StudioSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.studioDataStore: DataStore<Preferences> by preferencesDataStore(name = "toonmotion_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val PREVIEW_QUALITY = stringPreferencesKey("preview_quality")
        val HIGH_QUALITY_ANIM = booleanPreferencesKey("high_quality_anim")
        val DEFAULT_FPS = intPreferencesKey("default_fps")
        val DEFAULT_RESOLUTION = stringPreferencesKey("default_resolution")
        val MASTER_VOLUME = floatPreferencesKey("master_volume")
        val AUTOSAVE = booleanPreferencesKey("autosave")
        val PERFORMANCE_MODE = booleanPreferencesKey("performance_mode")
        val LANGUAGE = stringPreferencesKey("language")
    }

    val settingsFlow: Flow<StudioSettings> = context.studioDataStore.data.map { prefs ->
        StudioSettings(
            darkMode = prefs[Keys.DARK_MODE] ?: true,
            previewQuality = prefs[Keys.PREVIEW_QUALITY]?.let { name ->
                PreviewQuality.entries.find { it.name == name }
            } ?: PreviewQuality.HIGH,
            highQualityAnimation = prefs[Keys.HIGH_QUALITY_ANIM] ?: true,
            defaultFps = prefs[Keys.DEFAULT_FPS] ?: 24,
            defaultResolution = prefs[Keys.DEFAULT_RESOLUTION]?.let { name ->
                ExportResolution.entries.find { it.name == name }
            } ?: ExportResolution.P720,
            masterVolume = prefs[Keys.MASTER_VOLUME] ?: 0.9f,
            autosaveEnabled = prefs[Keys.AUTOSAVE] ?: true,
            performanceMode = prefs[Keys.PERFORMANCE_MODE] ?: false,
            language = prefs[Keys.LANGUAGE] ?: "বাংলা"
        )
    }

    suspend fun updateSettings(transform: (StudioSettings) -> StudioSettings) {
        context.studioDataStore.edit { prefs ->
            val current = StudioSettings(
                darkMode = prefs[Keys.DARK_MODE] ?: true,
                previewQuality = prefs[Keys.PREVIEW_QUALITY]?.let { n -> PreviewQuality.entries.find { it.name == n } } ?: PreviewQuality.HIGH,
                highQualityAnimation = prefs[Keys.HIGH_QUALITY_ANIM] ?: true,
                defaultFps = prefs[Keys.DEFAULT_FPS] ?: 24,
                defaultResolution = prefs[Keys.DEFAULT_RESOLUTION]?.let { n -> ExportResolution.entries.find { it.name == n } } ?: ExportResolution.P720,
                masterVolume = prefs[Keys.MASTER_VOLUME] ?: 0.9f,
                autosaveEnabled = prefs[Keys.AUTOSAVE] ?: true,
                performanceMode = prefs[Keys.PERFORMANCE_MODE] ?: false,
                language = prefs[Keys.LANGUAGE] ?: "বাংলা"
            )
            val next = transform(current)
            prefs[Keys.DARK_MODE] = next.darkMode
            prefs[Keys.PREVIEW_QUALITY] = next.previewQuality.name
            prefs[Keys.HIGH_QUALITY_ANIM] = next.highQualityAnimation
            prefs[Keys.DEFAULT_FPS] = next.defaultFps
            prefs[Keys.DEFAULT_RESOLUTION] = next.defaultResolution.name
            prefs[Keys.MASTER_VOLUME] = next.masterVolume
            prefs[Keys.AUTOSAVE] = next.autosaveEnabled
            prefs[Keys.PERFORMANCE_MODE] = next.performanceMode
            prefs[Keys.LANGUAGE] = next.language
        }
    }
}
