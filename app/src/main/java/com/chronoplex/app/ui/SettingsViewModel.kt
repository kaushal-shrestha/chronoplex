package com.chronoplex.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronoplex.app.AppContainer
import com.chronoplex.app.data.AlarmZoneSource
import com.chronoplex.app.domain.AppearanceMode
import com.chronoplex.app.domain.ThemePalette
import java.time.DayOfWeek
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsState(
    val appearance: AppearanceMode = AppearanceMode.SYSTEM,
    val palette: ThemePalette = ThemePalette.Anchor,
    val alarmZoneSource: AlarmZoneSource = AlarmZoneSource.ALL_ZONES,
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val clocksGrouping: Boolean = false,
    val alarmsGrouping: Boolean = false,
    val timersGrouping: Boolean = false,
    val stopwatchesGrouping: Boolean = false,
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    private val baseState = combine(
        container.settings.appearance,
        container.settings.palette,
        container.settings.alarmZoneSource,
        container.settings.firstDayOfWeek,
    ) { appearance, palette, alarmZoneSource, firstDayOfWeek ->
        SettingsState(
            appearance = appearance,
            palette = palette,
            alarmZoneSource = alarmZoneSource,
            firstDayOfWeek = firstDayOfWeek,
        )
    }

    val state: StateFlow<SettingsState> = combine(
        baseState,
        container.settings.clocksGroupingEnabled,
        container.settings.alarmsGroupingEnabled,
        container.settings.timersGroupingEnabled,
        container.settings.stopwatchesGroupingEnabled,
    ) { base, clocksGrouping, alarmsGrouping, timersGrouping, stopwatchesGrouping ->
        base.copy(
            clocksGrouping = clocksGrouping,
            alarmsGrouping = alarmsGrouping,
            timersGrouping = timersGrouping,
            stopwatchesGrouping = stopwatchesGrouping,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsState())

    fun setAppearance(m: AppearanceMode) = viewModelScope.launch { container.settings.setAppearance(m) }
    fun setPalette(p: ThemePalette) = viewModelScope.launch { container.settings.setPalette(p) }
    fun setZoneSource(s: AlarmZoneSource) = viewModelScope.launch { container.settings.setAlarmZoneSource(s) }
    fun setFirstDayOfWeek(d: DayOfWeek) = viewModelScope.launch { container.settings.setFirstDayOfWeek(d) }
    fun setClocksGrouping(v: Boolean) = viewModelScope.launch { container.settings.setClocksGroupingEnabled(v) }
    fun setAlarmsGrouping(v: Boolean) = viewModelScope.launch { container.settings.setAlarmsGroupingEnabled(v) }
    fun setTimersGrouping(v: Boolean) = viewModelScope.launch { container.settings.setTimersGroupingEnabled(v) }
    fun setStopwatchesGrouping(v: Boolean) = viewModelScope.launch { container.settings.setStopwatchesGroupingEnabled(v) }
}
