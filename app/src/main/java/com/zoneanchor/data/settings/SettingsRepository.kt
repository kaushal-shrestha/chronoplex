package com.zoneanchor.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zoneanchor.model.AppPalette
import com.zoneanchor.model.AppearanceMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.zoneAnchorDataStore by preferencesDataStore(name = "zoneanchor_settings")

class SettingsRepository(context: Context) {
    private val dataStore = context.applicationContext.zoneAnchorDataStore

    val appearanceFlow: Flow<AppearanceMode> = dataStore.data.map { prefs ->
        AppearanceMode.fromName(prefs[Keys.appearance])
    }

    val paletteFlow: Flow<AppPalette> = dataStore.data.map { prefs ->
        AppPalette.fromName(prefs[Keys.palette])
    }

    val legacyImportedFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.legacyImported] ?: false
    }

    suspend fun setAppearance(mode: AppearanceMode) {
        dataStore.edit { it[Keys.appearance] = mode.name }
    }

    suspend fun setPalette(palette: AppPalette) {
        dataStore.edit { it[Keys.palette] = palette.name }
    }

    suspend fun setLegacyImported(imported: Boolean) {
        dataStore.edit { it[Keys.legacyImported] = imported }
    }

    private object Keys {
        val palette = stringPreferencesKey("theme_palette")
        val appearance = stringPreferencesKey("appearance_mode")
        val legacyImported = booleanPreferencesKey("legacy_imported")
    }
}
