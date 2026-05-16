package com.zoneanchor.alarm.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zoneanchor.alarm.domain.AppearanceMode
import com.zoneanchor.alarm.domain.ThemePalette
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "zoneanchor_settings")

class SettingsRepository(private val context: Context) {
    private val store = context.settingsDataStore

    private val KEY_APPEARANCE = stringPreferencesKey("appearance_mode")
    private val KEY_PALETTE = stringPreferencesKey("theme_palette")
    private val KEY_ALARM_ZONE_SOURCE = stringPreferencesKey("alarm_zone_source")

    val appearance: Flow<AppearanceMode> = store.data.map { it.readAppearance() }
    val palette: Flow<ThemePalette> = store.data.map { it.readPalette() }
    val alarmZoneSource: Flow<AlarmZoneSource> = store.data.map { it.readZoneSource() }

    suspend fun setAppearance(mode: AppearanceMode) {
        store.edit { it[KEY_APPEARANCE] = mode.name }
    }

    suspend fun setPalette(palette: ThemePalette) {
        store.edit { it[KEY_PALETTE] = palette.name }
    }

    suspend fun setAlarmZoneSource(source: AlarmZoneSource) {
        store.edit { it[KEY_ALARM_ZONE_SOURCE] = source.name }
    }

    private fun Preferences.readAppearance(): AppearanceMode =
        this[KEY_APPEARANCE]?.let { runCatching { AppearanceMode.valueOf(it) }.getOrNull() } ?: AppearanceMode.SYSTEM

    private fun Preferences.readPalette(): ThemePalette =
        this[KEY_PALETTE]?.let { runCatching { ThemePalette.valueOf(it) }.getOrNull() } ?: ThemePalette.Anchor

    private fun Preferences.readZoneSource(): AlarmZoneSource =
        this[KEY_ALARM_ZONE_SOURCE]?.let { runCatching { AlarmZoneSource.valueOf(it) }.getOrNull() } ?: AlarmZoneSource.ALL_ZONES
}

enum class AlarmZoneSource { ADDED_CLOCKS, ALL_ZONES }
