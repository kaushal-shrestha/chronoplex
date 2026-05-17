package com.chronoplex.app.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.chronoplex.app.AppContainer
import com.chronoplex.app.data.AlarmZoneSource
import com.chronoplex.app.domain.Alarm
import com.chronoplex.app.domain.AppearanceMode
import com.chronoplex.app.domain.Clock
import com.chronoplex.app.domain.DayMask
import com.chronoplex.app.domain.ThemePalette
import com.chronoplex.app.domain.Timer
import com.chronoplex.app.domain.TimerFinishMode
import com.chronoplex.app.domain.TimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek

class ClocksViewModel(private val container: AppContainer) : ViewModel() {
    val clocks: StateFlow<List<Clock>> = container.clockRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun delete(id: Long) = viewModelScope.launch { container.clockRepo.delete(id) }

    fun deleteAll() = viewModelScope.launch { container.clockRepo.deleteAll() }

    /** Wipe the list and seed with a small set of broadly-useful clocks. */
    fun resetToDefaults() = viewModelScope.launch {
        container.clockRepo.deleteAll()
        container.clockRepo.upsert(Clock(label = "Eastern Time", zoneId = "America/New_York"))
        container.clockRepo.upsert(Clock(label = "UTC Time", zoneId = "UTC"))
    }
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
    /** Per-edit override; initialized from settings default. */
    val zoneSource: AlarmZoneSource = AlarmZoneSource.ALL_ZONES,
)

class AlarmEditViewModel(
    private val container: AppContainer,
    private val handle: SavedStateHandle,
) : ViewModel() {
    val state = MutableStateFlow(AlarmEditState())

    init {
        val id = handle.get<Long>("id") ?: 0L
        // Always seed zoneSource from the user's saved default; per-alarm override happens in the UI.
        viewModelScope.launch {
            val defaultSource = container.settings.alarmZoneSource.first()
            state.update { it.copy(zoneSource = defaultSource) }
        }
        if (id > 0L) viewModelScope.launch {
            container.alarmRepo.getById(id)?.let { a ->
                state.update {
                    it.copy(
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
            }
        } else {
            state.update { it.copy(zoneId = java.time.ZoneId.systemDefault().id) }
        }
    }

    fun setZoneSource(source: AlarmZoneSource) = state.update { it.copy(zoneSource = source) }

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
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val timerFinishMode: TimerFinishMode = TimerFinishMode.NOTIFICATION,
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    val state: StateFlow<SettingsState> = combine(
        container.settings.appearance,
        container.settings.palette,
        container.settings.alarmZoneSource,
        container.settings.firstDayOfWeek,
        container.settings.timerFinishMode,
    ) { a, p, z, d, t -> SettingsState(a, p, z, d, t) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsState())

    fun setAppearance(m: AppearanceMode) = viewModelScope.launch { container.settings.setAppearance(m) }
    fun setPalette(p: ThemePalette) = viewModelScope.launch { container.settings.setPalette(p) }
    fun setZoneSource(s: AlarmZoneSource) = viewModelScope.launch { container.settings.setAlarmZoneSource(s) }
    fun setFirstDayOfWeek(d: DayOfWeek) = viewModelScope.launch { container.settings.setFirstDayOfWeek(d) }
    fun setTimerFinishMode(m: TimerFinishMode) = viewModelScope.launch { container.settings.setTimerFinishMode(m) }
}

class TimersViewModel(private val container: AppContainer) : ViewModel() {
    val timers: StateFlow<List<Timer>> = container.timerRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun start(timer: Timer) = viewModelScope.launch { container.timerScheduler.start(timer) }
    fun pause(timer: Timer) = viewModelScope.launch { container.timerScheduler.pause(timer) }
    fun reset(timer: Timer) = viewModelScope.launch { container.timerScheduler.reset(timer) }
    fun dismiss(timer: Timer) = viewModelScope.launch { container.timerScheduler.dismiss(timer) }
    fun addMinute(timer: Timer) = viewModelScope.launch { container.timerScheduler.addMinute(timer) }

    fun delete(timer: Timer) = viewModelScope.launch {
        container.timerScheduler.reset(timer)
        container.timerRepo.delete(timer.id)
    }
}

data class TimerEditState(
    val id: Long = 0,
    val label: String = "",
    val hours: Int = 0,
    val minutes: Int = 5,
    val seconds: Int = 0,
    val finishMode: TimerFinishMode = TimerFinishMode.NOTIFICATION,
    val focusedField: DurationField = DurationField.MINUTES,
) {
    val totalMillis: Long
        get() = (hours.toLong() * 3600 + minutes.toLong() * 60 + seconds.toLong()) * 1000L
    val isValid: Boolean get() = totalMillis > 0L
}

enum class DurationField { HOURS, MINUTES, SECONDS }

class TimerEditViewModel(
    private val container: AppContainer,
    private val handle: SavedStateHandle,
) : ViewModel() {
    val state = MutableStateFlow(TimerEditState())

    init {
        viewModelScope.launch {
            val defaultMode = container.settings.timerFinishMode.first()
            state.update { it.copy(finishMode = defaultMode) }
        }
        val id = handle.get<Long>("id") ?: 0L
        if (id > 0L) viewModelScope.launch {
            container.timerRepo.getById(id)?.let { t ->
                val total = t.durationMillis / 1000L
                state.update {
                    it.copy(
                        id = t.id,
                        label = t.label,
                        hours = (total / 3600).toInt(),
                        minutes = ((total % 3600) / 60).toInt(),
                        seconds = (total % 60).toInt(),
                        finishMode = t.finishMode,
                    )
                }
            }
        }
    }

    fun setLabel(v: String) = state.update { it.copy(label = v) }
    fun setFinishMode(m: TimerFinishMode) = state.update { it.copy(finishMode = m) }
    fun setFocus(f: DurationField) = state.update { it.copy(focusedField = f) }

    /** Append a digit to the currently-focused field (shift-left within 2 digits). */
    fun typeDigit(digit: Int) {
        if (digit !in 0..9) return
        state.update { s ->
            val newValue = ((current(s) % 10) * 10 + digit).coerceIn(0, 99)
            val maxed = newValue >= 10
            val next = s.copy(
                hours = if (s.focusedField == DurationField.HOURS) newValue else s.hours,
                minutes = if (s.focusedField == DurationField.MINUTES) newValue else s.minutes,
                seconds = if (s.focusedField == DurationField.SECONDS) newValue else s.seconds,
            )
            // Auto-advance focus once a field has been filled to 2 digits.
            if (maxed) next.copy(focusedField = advance(s.focusedField)) else next
        }
    }

    fun backspace() = state.update { s ->
        val newValue = current(s) / 10
        s.copy(
            hours = if (s.focusedField == DurationField.HOURS) newValue else s.hours,
            minutes = if (s.focusedField == DurationField.MINUTES) newValue else s.minutes,
            seconds = if (s.focusedField == DurationField.SECONDS) newValue else s.seconds,
        )
    }

    fun clearField() = state.update { s ->
        s.copy(
            hours = if (s.focusedField == DurationField.HOURS) 0 else s.hours,
            minutes = if (s.focusedField == DurationField.MINUTES) 0 else s.minutes,
            seconds = if (s.focusedField == DurationField.SECONDS) 0 else s.seconds,
        )
    }

    fun setPresetMillis(millis: Long) = state.update { s ->
        val total = millis / 1000L
        s.copy(
            hours = (total / 3600).toInt().coerceAtMost(99),
            minutes = ((total % 3600) / 60).toInt(),
            seconds = (total % 60).toInt(),
        )
    }

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val s = state.value
        if (!s.isValid) return@launch
        val timer = Timer(
            id = s.id,
            label = s.label,
            durationMillis = s.totalMillis,
            state = TimerState.IDLE,
            endsAtMillis = null,
            pausedRemainingMillis = null,
            finishMode = s.finishMode,
            sortOrder = System.currentTimeMillis(),
        )
        container.timerRepo.upsert(timer)
        onDone()
    }

    private fun current(s: TimerEditState): Int = when (s.focusedField) {
        DurationField.HOURS -> s.hours
        DurationField.MINUTES -> s.minutes
        DurationField.SECONDS -> s.seconds
    }

    private fun advance(f: DurationField): DurationField = when (f) {
        DurationField.HOURS -> DurationField.MINUTES
        DurationField.MINUTES -> DurationField.SECONDS
        DurationField.SECONDS -> DurationField.SECONDS
    }
}

/** Single factory routes every ViewModel through the AppContainer. */
class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val handle = extras.createSavedStateHandle()
        @Suppress("UNCHECKED_CAST")
        return when (modelClass) {
            ClocksViewModel::class.java -> ClocksViewModel(container)
            AlarmsViewModel::class.java -> AlarmsViewModel(container)
            TimersViewModel::class.java -> TimersViewModel(container)
            ClockEditViewModel::class.java -> ClockEditViewModel(container, handle)
            AlarmEditViewModel::class.java -> AlarmEditViewModel(container, handle)
            TimerEditViewModel::class.java -> TimerEditViewModel(container, handle)
            SettingsViewModel::class.java -> SettingsViewModel(container)
            else -> error("Unknown VM: $modelClass")
        } as T
    }
}
