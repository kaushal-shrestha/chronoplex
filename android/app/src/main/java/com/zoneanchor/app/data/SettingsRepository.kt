package com.zoneanchor.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zoneanchor.app.domain.AppearanceMode
import com.zoneanchor.app.domain.ThemePalette
import com.zoneanchor.app.domain.TimerFinishMode
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "zoneanchor_settings")

class SettingsRepository(private val context: Context) {
    private val store = context.settingsDataStore

    private val KEY_APPEARANCE = stringPreferencesKey("appearance_mode")
    private val KEY_PALETTE = stringPreferencesKey("theme_palette")
    private val KEY_ALARM_ZONE_SOURCE = stringPreferencesKey("alarm_zone_source")
    private val KEY_ALARM_ZONE_DISPLAY = stringPreferencesKey("alarm_zone_display")
    private val KEY_FIRST_DAY_OF_WEEK = stringPreferencesKey("first_day_of_week")
    private val KEY_TIMER_FINISH_MODE = stringPreferencesKey("timer_finish_mode")
    private val KEY_CLOCKS_GROUPING = booleanPreferencesKey("clocks_grouping_enabled")
    private val KEY_ALARMS_GROUPING = booleanPreferencesKey("alarms_grouping_enabled")
    private val KEY_TIMERS_GROUPING = booleanPreferencesKey("timers_grouping_enabled")
    private val KEY_STOPWATCHES_GROUPING = booleanPreferencesKey("stopwatches_grouping_enabled")

    val appearance: Flow<AppearanceMode> = store.data.map { it.readAppearance() }
    val palette: Flow<ThemePalette> = store.data.map { it.readPalette() }
    val alarmZoneSource: Flow<AlarmZoneSource> = store.data.map { it.readZoneSource() }
    val alarmZoneDisplay: Flow<AlarmZoneDisplayMode> = store.data.map { it.readAlarmZoneDisplay() }
    val firstDayOfWeek: Flow<DayOfWeek> = store.data.map { it.readFirstDayOfWeek() }
    val timerFinishMode: Flow<TimerFinishMode> = store.data.map { it.readTimerFinishMode() }
    val clocksGroupingEnabled: Flow<Boolean> = store.data.map { it[KEY_CLOCKS_GROUPING] ?: false }
    val alarmsGroupingEnabled: Flow<Boolean> = store.data.map { it[KEY_ALARMS_GROUPING] ?: false }
    val timersGroupingEnabled: Flow<Boolean> = store.data.map { it[KEY_TIMERS_GROUPING] ?: false }
    val stopwatchesGroupingEnabled: Flow<Boolean> = store.data.map { it[KEY_STOPWATCHES_GROUPING] ?: false }

    suspend fun setAppearance(mode: AppearanceMode) {
        store.edit { it[KEY_APPEARANCE] = mode.name }
    }

    suspend fun setPalette(palette: ThemePalette) {
        store.edit { it[KEY_PALETTE] = palette.name }
    }

    suspend fun setAlarmZoneSource(source: AlarmZoneSource) {
        store.edit { it[KEY_ALARM_ZONE_SOURCE] = source.name }
    }

    suspend fun setAlarmZoneDisplay(mode: AlarmZoneDisplayMode) {
        store.edit { it[KEY_ALARM_ZONE_DISPLAY] = mode.name }
    }

    suspend fun setFirstDayOfWeek(day: DayOfWeek) {
        store.edit { it[KEY_FIRST_DAY_OF_WEEK] = day.name }
    }

    suspend fun setTimerFinishMode(mode: TimerFinishMode) {
        store.edit { it[KEY_TIMER_FINISH_MODE] = mode.name }
    }

    suspend fun setClocksGroupingEnabled(enabled: Boolean) {
        store.edit { it[KEY_CLOCKS_GROUPING] = enabled }
    }

    suspend fun setAlarmsGroupingEnabled(enabled: Boolean) {
        store.edit { it[KEY_ALARMS_GROUPING] = enabled }
    }

    suspend fun setTimersGroupingEnabled(enabled: Boolean) {
        store.edit { it[KEY_TIMERS_GROUPING] = enabled }
    }

    suspend fun setStopwatchesGroupingEnabled(enabled: Boolean) {
        store.edit { it[KEY_STOPWATCHES_GROUPING] = enabled }
    }

    private fun Preferences.readAppearance(): AppearanceMode =
        this[KEY_APPEARANCE]?.let { runCatching { AppearanceMode.valueOf(it) }.getOrNull() } ?: AppearanceMode.SYSTEM

    private fun Preferences.readPalette(): ThemePalette =
        this[KEY_PALETTE]?.let { runCatching { ThemePalette.valueOf(it) }.getOrNull() } ?: ThemePalette.Anchor

    private fun Preferences.readZoneSource(): AlarmZoneSource =
        this[KEY_ALARM_ZONE_SOURCE]?.let { runCatching { AlarmZoneSource.valueOf(it) }.getOrNull() } ?: AlarmZoneSource.ALL_ZONES

    private fun Preferences.readAlarmZoneDisplay(): AlarmZoneDisplayMode =
        this[KEY_ALARM_ZONE_DISPLAY]?.let {
            runCatching { AlarmZoneDisplayMode.valueOf(it) }.getOrNull()
        } ?: AlarmZoneDisplayMode.CLOCK_LABEL

    private fun Preferences.readFirstDayOfWeek(): DayOfWeek =
        this[KEY_FIRST_DAY_OF_WEEK]?.let { runCatching { DayOfWeek.valueOf(it) }.getOrNull() } ?: DayOfWeek.MONDAY

    private fun Preferences.readTimerFinishMode(): TimerFinishMode =
        this[KEY_TIMER_FINISH_MODE]?.let { runCatching { TimerFinishMode.valueOf(it) }.getOrNull() } ?: TimerFinishMode.NOTIFICATION
}

enum class AlarmZoneSource { ADDED_CLOCKS, ALL_ZONES }
enum class AlarmZoneDisplayMode { CLOCK_LABEL, ZONE_ID }
