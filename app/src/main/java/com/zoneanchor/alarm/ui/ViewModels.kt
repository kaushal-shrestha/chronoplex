package com.zoneanchor.alarm.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.zoneanchor.alarm.AppContainer
import com.zoneanchor.alarm.data.AlarmZoneSource
import com.zoneanchor.alarm.domain.Alarm
import com.zoneanchor.alarm.domain.AppearanceMode
import com.zoneanchor.alarm.domain.Clock
import com.zoneanchor.alarm.domain.DayMask
import com.zoneanchor.alarm.domain.ThemePalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek

class ClocksViewModel(private val container: AppContainer) : ViewModel() {
    val clocks: StateFlow<List<Clock>> = container.clockRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun delete(id: Long) = viewModelScope.launch { container.clockRepo.delete(id) }
}

class AlarmsViewModel(private val container: AppContainer) : ViewModel() {
    val alarms: StateFlow<List<Alarm>> = container.alarmRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun toggleEnabled(alarm: Alarm) = viewModelScope.launch {
        val newEnabled = !alarm.enabled
        container.alarmRepo.setEnabled(alarm.id, newEnabled)
        if (newEnabled) {
            container.scheduler.schedule(alarm.copy(enabled = true))
        } else {
            container.scheduler.cancel(alarm.id)
        }
    }

    fun delete(alarm: Alarm) = viewModelScope.launch {
        container.scheduler.cancel(alarm.id)
        container.alarmRepo.delete(alarm.id)
    }
}

data class ClockEditState(
    val id: Long = 0,
    val label: String = "",
    val zoneId: String = "",
)

class ClockEditViewModel(
    private val container: AppContainer,
    private val handle: SavedStateHandle,
) : ViewModel() {
    val state = MutableStateFlow(ClockEditState())

    init {
        val id = handle.get<Long>("id") ?: 0L
        if (id > 0L) viewModelScope.launch {
            container.clockRepo.getAll().firstOrNull { it.id == id }?.let {
                state.value = ClockEditState(id = it.id, label = it.label, zoneId = it.zoneId)
            }
        }
    }

    fun setLabel(v: String) = state.update { it.copy(label = v) }
    fun setZone(v: String) = state.update { it.copy(zoneId = v) }

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val s = state.value
        if (s.zoneId.isBlank()) return@launch
        val finalLabel = s.label.ifBlank { defaultLabelFor(s.zoneId) }
        container.clockRepo.upsert(
            Clock(id = s.id, label = finalLabel, zoneId = s.zoneId)
        )
        onDone()
    }

    private fun defaultLabelFor(zoneId: String): String =
        zoneId.substringAfterLast('/').replace('_', ' ')
}

data class AlarmEditState(
    val id: Long = 0,
    val label: String = "",
    val zoneId: String = "",
    val hour: Int = 7,
    val minute: Int = 0,
    val daysMask: Int = 0,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val enabled: Boolean = true,
)

class AlarmEditViewModel(
    private val container: AppContainer,
    private val handle: SavedStateHandle,
) : ViewModel() {
    val state = MutableStateFlow(AlarmEditState())

    val zoneSource: StateFlow<AlarmZoneSource> = container.settings.alarmZoneSource
        .stateIn(viewModelScope, SharingStarted.Eagerly, AlarmZoneSource.ALL_ZONES)

    init {
        val id = handle.get<Long>("id") ?: 0L
        if (id > 0L) viewModelScope.launch {
            container.alarmRepo.getById(id)?.let { a ->
                state.value = AlarmEditState(
                    id = a.id,
                    label = a.label,
                    zoneId = a.zoneId,
                    hour = a.hour,
                    minute = a.minute,
                    daysMask = a.daysMask,
                    soundEnabled = a.soundEnabled,
                    vibrationEnabled = a.vibrationEnabled,
                    enabled = a.enabled,
                )
            }
        } else {
            // Default the zone to the device's current zone for new alarms.
            state.update { it.copy(zoneId = java.time.ZoneId.systemDefault().id) }
        }
    }

    fun setLabel(v: String) = state.update { it.copy(label = v) }
    fun setZone(v: String) = state.update { it.copy(zoneId = v) }
    fun setTime(h: Int, m: Int) = state.update { it.copy(hour = h, minute = m) }
    fun toggleDay(d: DayOfWeek) = state.update {
        val bit = 1 shl (d.value - 1)
        it.copy(daysMask = it.daysMask xor bit)
    }
    fun setDaysMask(mask: Int) = state.update { it.copy(daysMask = mask) }
    fun setSound(v: Boolean) = state.update { it.copy(soundEnabled = v) }
    fun setVibration(v: Boolean) = state.update { it.copy(vibrationEnabled = v) }

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val s = state.value
        if (s.zoneId.isBlank()) return@launch
        val alarm = Alarm(
            id = s.id,
            label = s.label,
            zoneId = s.zoneId,
            hour = s.hour,
            minute = s.minute,
            daysMask = s.daysMask,
            soundEnabled = s.soundEnabled,
            vibrationEnabled = s.vibrationEnabled,
            enabled = true,
        )
        val newId = container.alarmRepo.upsert(alarm)
        val saved = alarm.copy(id = if (alarm.id == 0L) newId else alarm.id)
        container.scheduler.schedule(saved)
        onDone()
    }
}

data class SettingsState(
    val appearance: AppearanceMode = AppearanceMode.SYSTEM,
    val palette: ThemePalette = ThemePalette.Anchor,
    val alarmZoneSource: AlarmZoneSource = AlarmZoneSource.ALL_ZONES,
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    val state: StateFlow<SettingsState> = combine(
        container.settings.appearance,
        container.settings.palette,
        container.settings.alarmZoneSource,
    ) { a, p, z -> SettingsState(a, p, z) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsState())

    fun setAppearance(m: AppearanceMode) = viewModelScope.launch { container.settings.setAppearance(m) }
    fun setPalette(p: ThemePalette) = viewModelScope.launch { container.settings.setPalette(p) }
    fun setZoneSource(s: AlarmZoneSource) = viewModelScope.launch { container.settings.setAlarmZoneSource(s) }
}

/** Single factory routes every ViewModel through the AppContainer. */
class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val handle = extras.createSavedStateHandle()
        @Suppress("UNCHECKED_CAST")
        return when (modelClass) {
            ClocksViewModel::class.java -> ClocksViewModel(container)
            AlarmsViewModel::class.java -> AlarmsViewModel(container)
            ClockEditViewModel::class.java -> ClockEditViewModel(container, handle)
            AlarmEditViewModel::class.java -> AlarmEditViewModel(container, handle)
            SettingsViewModel::class.java -> SettingsViewModel(container)
            else -> error("Unknown VM: $modelClass")
        } as T
    }
}
