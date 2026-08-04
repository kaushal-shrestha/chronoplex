package com.zoneanchor.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zoneanchor.app.AppContainer
import com.zoneanchor.app.data.AlarmZoneDisplayMode
import com.zoneanchor.app.data.AlarmZoneSource
import com.zoneanchor.app.domain.AppearanceMode
import com.zoneanchor.app.domain.ThemePalette
import java.time.DayOfWeek
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsState(
    val appearance: AppearanceMode = AppearanceMode.SYSTEM,
    val palette: ThemePalette = ThemePalette.Anchor,
    val alarmZoneSource: AlarmZoneSource = AlarmZoneSource.ALL_ZONES,
    val alarmZoneDisplay: AlarmZoneDisplayMode = AlarmZoneDisplayMode.CLOCK_LABEL,
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val clocksGrouping: Boolean = false,
    val alarmsGrouping: Boolean = false,
    val timersGrouping: Boolean = false,
    val stopwatchesGrouping: Boolean = false,
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus = _backupStatus.asStateFlow()

    private val baseState = combine(
        container.settings.appearance,
        container.settings.palette,
        container.settings.alarmZoneSource,
        container.settings.alarmZoneDisplay,
        container.settings.firstDayOfWeek,
    ) { appearance, palette, alarmZoneSource, alarmZoneDisplay, firstDayOfWeek ->
        SettingsState(
            appearance = appearance,
            palette = palette,
            alarmZoneSource = alarmZoneSource,
            alarmZoneDisplay = alarmZoneDisplay,
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
    fun setAlarmZoneDisplay(m: AlarmZoneDisplayMode) = viewModelScope.launch { container.settings.setAlarmZoneDisplay(m) }
    fun setFirstDayOfWeek(d: DayOfWeek) = viewModelScope.launch { container.settings.setFirstDayOfWeek(d) }
    fun setClocksGrouping(v: Boolean) = viewModelScope.launch { container.settings.setClocksGroupingEnabled(v) }
    fun setAlarmsGrouping(v: Boolean) = viewModelScope.launch { container.settings.setAlarmsGroupingEnabled(v) }
    fun setTimersGrouping(v: Boolean) = viewModelScope.launch { container.settings.setTimersGroupingEnabled(v) }
    fun setStopwatchesGrouping(v: Boolean) = viewModelScope.launch { container.settings.setStopwatchesGroupingEnabled(v) }

    fun exportBackup(onReady: (String) -> Unit) = viewModelScope.launch {
        runCatching { container.backupRepo.exportJson() }
            .onSuccess {
                _backupStatus.value = "Choose where to save the backup."
                onReady(it)
            }
            .onFailure { _backupStatus.value = it.message ?: "Could not create backup." }
    }

    fun importBackup(json: String) = viewModelScope.launch {
        runCatching {
            val result = container.backupRepo.importJson(json)
            container.alarmRepo.getAllEnabled().forEach { container.scheduler.schedule(it) }
            container.timerRepo.getAllRunning().forEach { container.timerScheduler.rescheduleExisting(it) }
            result
        }.onSuccess { result ->
            _backupStatus.value = "Imported ${result.clocks} clocks, ${result.alarms} alarms, " +
                "${result.timers} timers, ${result.stopwatches} stopwatches, ${result.groups} groups, ${result.settings} settings."
        }.onFailure {
            _backupStatus.value = it.message ?: "Could not import backup."
        }
    }

    fun onBackupSaved(saved: Boolean) {
        _backupStatus.value = if (saved) "Backup saved." else "Backup was not saved."
    }

    fun onBackupOpenFailed(message: String?) {
        _backupStatus.value = message ?: "Could not open backup file."
    }
}
