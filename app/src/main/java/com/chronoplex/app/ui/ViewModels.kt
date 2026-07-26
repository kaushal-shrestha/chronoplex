package com.chronoplex.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.chronoplex.app.AppContainer
import com.chronoplex.app.data.AlarmZoneSource
import com.chronoplex.app.domain.Alarm
import com.chronoplex.app.domain.AlarmRepeatType
import com.chronoplex.app.domain.AppearanceMode
import com.chronoplex.app.domain.Clock
import com.chronoplex.app.domain.DayMask
import com.chronoplex.app.domain.Group
import com.chronoplex.app.domain.ThemePalette
import com.chronoplex.app.domain.Stopwatch
import com.chronoplex.app.domain.StopwatchLap
import com.chronoplex.app.domain.StopwatchState
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
import java.time.LocalDate

class ClocksViewModel(private val container: AppContainer) : ViewModel() {
    val clocks: StateFlow<List<Clock>> = container.clockRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val groups: StateFlow<List<Group>> = container.clockRepo.observeGroups()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val groupingEnabled: StateFlow<Boolean> = container.settings.clocksGroupingEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun delete(id: Long) = viewModelScope.launch { container.clockRepo.delete(id) }

    fun deleteAll() = viewModelScope.launch { container.clockRepo.deleteAll() }

    /** Wipe the list and seed with a small set of broadly-useful clocks. */
    fun resetToDefaults() = viewModelScope.launch {
        container.clockRepo.deleteAll()
        container.clockRepo.upsert(Clock(label = "Eastern Time", zoneId = "America/New_York"))
        container.clockRepo.upsert(Clock(label = "UTC Time", zoneId = "UTC"))
    }

    fun moveToGroup(clockId: Long, groupId: Long?) = viewModelScope.launch {
        container.clockRepo.assignToGroup(clockId, groupId)
    }
    fun createGroup(name: String) = viewModelScope.launch { container.clockRepo.createGroup(name) }
    fun renameGroup(id: Long, name: String) = viewModelScope.launch { container.clockRepo.renameGroup(id, name) }
    fun deleteGroup(id: Long) = viewModelScope.launch { container.clockRepo.deleteGroup(id) }
    fun toggleCollapsed(group: Group) = viewModelScope.launch {
        container.clockRepo.setCollapsed(group.id, !group.collapsed)
    }
    fun reorderItems(ids: List<Long>) = viewModelScope.launch { container.clockRepo.reorderItems(ids) }
    fun reorderGroups(ids: List<Long>) = viewModelScope.launch { container.clockRepo.reorderGroups(ids) }

    /** Create a new group AND immediately assign [itemId] to it. */
    fun createAndAssign(itemId: Long, name: String) = viewModelScope.launch {
        val newId = container.clockRepo.createGroup(name)
        container.clockRepo.assignToGroup(itemId, newId)
    }

    /** Re-insert a previously deleted clock (gets a new auto-id). */
    fun restore(clock: Clock) = viewModelScope.launch {
        container.clockRepo.upsert(clock.copy(id = 0))
    }
}

class AlarmsViewModel(private val container: AppContainer) : ViewModel() {
    val alarms: StateFlow<List<Alarm>> = container.alarmRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val groups: StateFlow<List<Group>> = container.alarmRepo.observeGroups()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val groupingEnabled: StateFlow<Boolean> = container.settings.alarmsGroupingEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

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

    fun deleteAll() = viewModelScope.launch {
        alarms.value.forEach { container.scheduler.cancel(it.id) }
        container.alarmRepo.deleteAll()
    }

    fun moveToGroup(alarmId: Long, groupId: Long?) = viewModelScope.launch {
        container.alarmRepo.assignToGroup(alarmId, groupId)
    }
    fun createGroup(name: String) = viewModelScope.launch { container.alarmRepo.createGroup(name) }
    fun renameGroup(id: Long, name: String) = viewModelScope.launch { container.alarmRepo.renameGroup(id, name) }
    fun deleteGroup(id: Long) = viewModelScope.launch { container.alarmRepo.deleteGroup(id) }
    fun toggleCollapsed(group: Group) = viewModelScope.launch {
        container.alarmRepo.setCollapsed(group.id, !group.collapsed)
    }
    fun reorderGroups(ids: List<Long>) = viewModelScope.launch { container.alarmRepo.reorderGroups(ids) }
    // Alarms use natural (time-of-day) ordering; we don't expose item reorder.

    fun createAndAssign(itemId: Long, name: String) = viewModelScope.launch {
        val newId = container.alarmRepo.createGroup(name)
        container.alarmRepo.assignToGroup(itemId, newId)
    }

    /** Re-insert a deleted alarm; if it was enabled at deletion, re-schedule it. */
    fun restore(alarm: Alarm) = viewModelScope.launch {
        val newId = container.alarmRepo.upsert(alarm.copy(id = 0, snoozeUntilMillis = null))
        if (alarm.enabled) {
            container.scheduler.schedule(alarm.copy(id = newId, snoozeUntilMillis = null))
        }
    }
}

data class ClockEditState(
    val id: Long = 0,
    val label: String = "",
    val zoneId: String = "",
    val groupId: Long? = null,
)

class ClockEditViewModel(
    private val container: AppContainer,
) : ViewModel() {
    val state = MutableStateFlow(ClockEditState())

    val groups: StateFlow<List<Group>> = container.clockRepo.observeGroups()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val groupingEnabled: StateFlow<Boolean> = container.settings.clocksGroupingEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun load(id: Long) = viewModelScope.launch {
        if (id <= 0L) {
            state.value = ClockEditState()
            return@launch
        }
        container.clockRepo.getAll().firstOrNull { it.id == id }?.let {
            state.value = ClockEditState(id = it.id, label = it.label, zoneId = it.zoneId, groupId = it.groupId)
        }
    }

    fun setLabel(v: String) = state.update { it.copy(label = v) }
    fun setZone(v: String) = state.update { it.copy(zoneId = v) }
    fun setGroupId(g: Long?) = state.update { it.copy(groupId = g) }

    /** Create a new group and select it for the in-progress edit. */
    fun createAndSelectGroup(name: String) = viewModelScope.launch {
        val newId = container.clockRepo.createGroup(name)
        state.update { it.copy(groupId = newId) }
    }

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        val s = state.value
        if (s.zoneId.isBlank()) return@launch
        val finalLabel = s.label.ifBlank { defaultLabelFor(s.zoneId) }
        container.clockRepo.upsert(
            Clock(id = s.id, label = finalLabel, zoneId = s.zoneId, groupId = s.groupId)
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
    /** Defaults to 00:00 (12:00 AM) — matches Google Clock's new-alarm default. */
    val hour: Int = 0,
    val minute: Int = 0,
    val daysMask: Int = 0,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val enabled: Boolean = true,
    /** Per-edit override; initialized from settings default. */
    val zoneSource: AlarmZoneSource = AlarmZoneSource.ALL_ZONES,
    val groupId: Long? = null,
    val repeatType: AlarmRepeatType = AlarmRepeatType.WEEKLY,
    val repeatInterval: Int = 1,
    val repeatStartDate: String = LocalDate.now().toString(),
    val monthlyDay: Int = 1,
    val monthlyOrdinal: Int = 1,
    val monthlyWeekday: Int = DayOfWeek.MONDAY.value,
)

class AlarmEditViewModel(
    private val container: AppContainer,
) : ViewModel() {
    val state = MutableStateFlow(AlarmEditState())

    val groups: StateFlow<List<Group>> = container.alarmRepo.observeGroups()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val groupingEnabled: StateFlow<Boolean> = container.settings.alarmsGroupingEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun load(id: Long) = viewModelScope.launch {
        val defaultSource = container.settings.alarmZoneSource.first()
        if (id <= 0L) {
            state.value = AlarmEditState(
                zoneId = java.time.ZoneId.systemDefault().id,
                zoneSource = defaultSource,
            )
            return@launch
        }
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
                zoneSource = defaultSource,
                groupId = a.groupId,
                repeatType = a.effectiveRepeatType,
                repeatInterval = a.repeatInterval,
                repeatStartDate = a.repeatStartDate.ifBlank { LocalDate.now().toString() },
                monthlyDay = a.monthlyDay,
                monthlyOrdinal = a.monthlyOrdinal,
                monthlyWeekday = a.monthlyWeekday,
            )
        }
    }

    fun setZoneSource(source: AlarmZoneSource) = state.update { it.copy(zoneSource = source) }
    fun setGroupId(g: Long?) = state.update { it.copy(groupId = g) }

    fun createAndSelectGroup(name: String) = viewModelScope.launch {
        val newId = container.alarmRepo.createGroup(name)
        state.update { it.copy(groupId = newId) }
    }

    fun setLabel(v: String) = state.update { it.copy(label = v) }
    fun setZone(v: String) = state.update { it.copy(zoneId = v) }
    fun setTime(h: Int, m: Int) = state.update { it.copy(hour = h, minute = m) }
    fun toggleDay(d: DayOfWeek) = state.update {
        if (it.repeatType == AlarmRepeatType.MONTHLY_WEEKDAY) {
            return@update it.copy(monthlyWeekday = d.value)
        }
        val bit = 1 shl (d.value - 1)
        val next = it.daysMask xor bit
        it.copy(daysMask = next, repeatType = if (next == 0) AlarmRepeatType.ONCE else AlarmRepeatType.WEEKLY)
    }
    fun setDaysMask(mask: Int) = state.update {
        it.copy(
            daysMask = mask,
            repeatType = if (mask == 0) AlarmRepeatType.ONCE else AlarmRepeatType.WEEKLY,
            repeatInterval = 1,
            repeatStartDate = LocalDate.now().toString(),
        )
    }
    fun setRepeatType(type: AlarmRepeatType) = state.update {
        it.copy(
            repeatType = type,
            daysMask = if (type == AlarmRepeatType.ONCE) 0 else it.daysMask.takeIf { mask -> mask != 0 } ?: DayMask.EVERY_DAY,
            repeatStartDate = it.repeatStartDate.ifBlank { LocalDate.now().toString() },
        )
    }
    fun setRepeatInterval(interval: Int) = state.update { it.copy(repeatInterval = interval.coerceIn(1, 99)) }
    fun setRepeatStartDate(value: String) = state.update { it.copy(repeatStartDate = value) }
    fun setMonthlyDay(day: Int) = state.update { it.copy(monthlyDay = day.coerceIn(1, 31)) }
    fun setMonthlyOrdinal(ordinal: Int) = state.update { it.copy(monthlyOrdinal = if (ordinal == -1) -1 else ordinal.coerceIn(1, 4)) }
    fun setMonthlyWeekday(day: DayOfWeek) = state.update { it.copy(monthlyWeekday = day.value) }
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
            groupId = s.groupId,
            repeatType = s.repeatType,
            repeatInterval = s.repeatInterval,
            repeatStartDate = s.repeatStartDate,
            monthlyDay = s.monthlyDay,
            monthlyOrdinal = s.monthlyOrdinal,
            monthlyWeekday = s.monthlyWeekday,
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
    val clocksGrouping: Boolean = false,
    val alarmsGrouping: Boolean = false,
    val timersGrouping: Boolean = false,
    val stopwatchesGrouping: Boolean = false,
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    val state: StateFlow<SettingsState> = kotlinx.coroutines.flow.combine(
        listOf(
            container.settings.appearance,
            container.settings.palette,
            container.settings.alarmZoneSource,
            container.settings.firstDayOfWeek,
            container.settings.clocksGroupingEnabled,
            container.settings.alarmsGroupingEnabled,
            container.settings.timersGroupingEnabled,
            container.settings.stopwatchesGroupingEnabled,
        )
    ) { values ->
        SettingsState(
            appearance = values[0] as AppearanceMode,
            palette = values[1] as ThemePalette,
            alarmZoneSource = values[2] as AlarmZoneSource,
            firstDayOfWeek = values[3] as DayOfWeek,
            clocksGrouping = values[4] as Boolean,
            alarmsGrouping = values[5] as Boolean,
            timersGrouping = values[6] as Boolean,
            stopwatchesGrouping = values[7] as Boolean,
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

class TimersViewModel(private val container: AppContainer) : ViewModel() {
    val timers: StateFlow<List<Timer>> = container.timerRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val groups: StateFlow<List<Group>> = container.timerRepo.observeGroups()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val groupingEnabled: StateFlow<Boolean> = container.settings.timersGroupingEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun start(timer: Timer) = viewModelScope.launch { container.timerScheduler.start(timer) }
    fun pause(timer: Timer) = viewModelScope.launch { container.timerScheduler.pause(timer) }
    fun reset(timer: Timer) = viewModelScope.launch { container.timerScheduler.reset(timer) }
    fun dismiss(timer: Timer) = viewModelScope.launch { container.timerScheduler.dismiss(timer) }
    fun addMinute(timer: Timer) = viewModelScope.launch { container.timerScheduler.addMinute(timer) }

    fun delete(timer: Timer) = viewModelScope.launch {
        container.timerScheduler.reset(timer)
        container.timerRepo.delete(timer.id)
    }

    fun deleteAll() = viewModelScope.launch {
        timers.value.forEach { container.timerScheduler.reset(it) }
        container.timerRepo.deleteAll()
    }

    fun moveToGroup(timerId: Long, groupId: Long?) = viewModelScope.launch {
        container.timerRepo.assignToGroup(timerId, groupId)
    }
    fun createGroup(name: String) = viewModelScope.launch { container.timerRepo.createGroup(name) }
    fun renameGroup(id: Long, name: String) = viewModelScope.launch { container.timerRepo.renameGroup(id, name) }
    fun deleteGroup(id: Long) = viewModelScope.launch { container.timerRepo.deleteGroup(id) }
    fun toggleCollapsed(group: Group) = viewModelScope.launch {
        container.timerRepo.setCollapsed(group.id, !group.collapsed)
    }
    fun reorderItems(ids: List<Long>) = viewModelScope.launch { container.timerRepo.reorderItems(ids) }
    fun reorderGroups(ids: List<Long>) = viewModelScope.launch { container.timerRepo.reorderGroups(ids) }

    fun createAndAssign(itemId: Long, name: String) = viewModelScope.launch {
        val newId = container.timerRepo.createGroup(name)
        container.timerRepo.assignToGroup(itemId, newId)
    }

    /** Re-insert a deleted timer in IDLE state (we don't try to resume a partial run). */
    fun restore(timer: Timer) = viewModelScope.launch {
        container.timerRepo.upsert(
            timer.copy(
                id = 0,
                state = TimerState.IDLE,
                endsAtMillis = null,
                pausedRemainingMillis = null,
            )
        )
    }
}

data class TimerEditState(
    val id: Long = 0,
    val label: String = "",
    /** 6-digit HHMMSS buffer; new digits shift in from the right. Range 0..999999. */
    val digits: Int = 0,
    val groupId: Long? = null,
) {
    val seconds: Int get() = digits % 100
    val minutes: Int get() = (digits / 100) % 100
    val hours: Int get() = digits / 10000
    val totalMillis: Long
        get() = (hours.toLong() * 3600 + minutes.toLong() * 60 + seconds.toLong()) * 1000L
    val isValid: Boolean get() = totalMillis > 0L
}

class TimerEditViewModel(
    private val container: AppContainer,
) : ViewModel() {
    val state = MutableStateFlow(TimerEditState())

    val groups: StateFlow<List<Group>> = container.timerRepo.observeGroups()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val groupingEnabled: StateFlow<Boolean> = container.settings.timersGroupingEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun load(id: Long) = viewModelScope.launch {
        if (id <= 0L) {
            state.value = TimerEditState()
            return@launch
        }
        container.timerRepo.getById(id)?.let { t ->
            state.value = TimerEditState(
                id = t.id,
                label = t.label,
                digits = millisToDigits(t.durationMillis),
                groupId = t.groupId,
            )
        }
    }

    fun setLabel(v: String) = state.update { it.copy(label = v) }
    fun setGroupId(g: Long?) = state.update { it.copy(groupId = g) }

    fun createAndSelectGroup(name: String) = viewModelScope.launch {
        val newId = container.timerRepo.createGroup(name)
        state.update { it.copy(groupId = newId) }
    }

    /** Shift digits left and append. Caps at 6 digits (99h 99m 99s). */
    fun typeDigit(digit: Int) {
        if (digit !in 0..9) return
        state.update { s ->
            val next = s.digits * 10 + digit
            if (next > 999_999) s else s.copy(digits = next)
        }
    }

    fun backspace() = state.update { it.copy(digits = it.digits / 10) }

    fun clearField() = state.update { it.copy(digits = 0) }

    fun setPresetMillis(millis: Long) = state.update {
        it.copy(digits = millisToDigits(millis))
    }

    fun save(autoStart: Boolean = false, onDone: () -> Unit) = viewModelScope.launch {
        val s = state.value
        if (!s.isValid) return@launch
        val timer = Timer(
            id = s.id,
            label = s.label,
            durationMillis = s.totalMillis,
            state = TimerState.IDLE,
            endsAtMillis = null,
            pausedRemainingMillis = null,
            sortOrder = System.currentTimeMillis(),
            groupId = s.groupId,
        )
        val newId = container.timerRepo.upsert(timer)
        if (autoStart) {
            val saved = timer.copy(id = if (timer.id == 0L) newId else timer.id)
            container.timerScheduler.start(saved)
        }
        onDone()
    }

    private fun millisToDigits(millis: Long): Int {
        val total = millis / 1000L
        val h = (total / 3600).toInt().coerceAtMost(99)
        val m = ((total % 3600) / 60).toInt()
        val sec = (total % 60).toInt()
        return h * 10_000 + m * 100 + sec
    }
}

class StopwatchesViewModel(private val container: AppContainer) : ViewModel() {
    val stopwatches: StateFlow<List<Stopwatch>> = container.stopwatchRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val groups: StateFlow<List<Group>> = container.stopwatchRepo.observeGroups()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val groupingEnabled: StateFlow<Boolean> = container.settings.stopwatchesGroupingEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun observeLaps(stopwatchId: Long) = container.stopwatchRepo.observeLaps(stopwatchId)

    fun moveToGroup(stopwatchId: Long, groupId: Long?) = viewModelScope.launch {
        container.stopwatchRepo.assignToGroup(stopwatchId, groupId)
    }
    fun createGroup(name: String) = viewModelScope.launch { container.stopwatchRepo.createGroup(name) }
    fun renameGroup(id: Long, name: String) = viewModelScope.launch { container.stopwatchRepo.renameGroup(id, name) }
    fun deleteGroup(id: Long) = viewModelScope.launch { container.stopwatchRepo.deleteGroup(id) }
    fun toggleCollapsed(group: Group) = viewModelScope.launch {
        container.stopwatchRepo.setCollapsed(group.id, !group.collapsed)
    }
    fun reorderItems(ids: List<Long>) = viewModelScope.launch { container.stopwatchRepo.reorderItems(ids) }
    fun reorderGroups(ids: List<Long>) = viewModelScope.launch { container.stopwatchRepo.reorderGroups(ids) }

    fun createAndAssign(itemId: Long, name: String) = viewModelScope.launch {
        val newId = container.stopwatchRepo.createGroup(name)
        container.stopwatchRepo.assignToGroup(itemId, newId)
    }

    /** Re-insert a deleted stopwatch along with its lap history. */
    fun restore(stopwatch: Stopwatch, laps: List<StopwatchLap>) = viewModelScope.launch {
        val newId = container.stopwatchRepo.upsert(
            stopwatch.copy(
                id = 0,
                state = StopwatchState.IDLE,
                startedAtMillis = null,
                accumulatedMillis = stopwatch.elapsedMillis(),
            )
        )
        laps.sortedBy { it.lapNumber }.forEach { lap ->
            container.stopwatchRepo.addLap(newId, lap.totalElapsedMillis)
        }
    }

    fun addStopwatch() = viewModelScope.launch {
        // Number it sequentially based on what's already there for a friendly default label.
        val existing = stopwatches.value.size
        container.stopwatchRepo.upsert(
            Stopwatch(label = "Stopwatch ${existing + 1}", state = StopwatchState.IDLE)
        )
    }

    fun start(stopwatch: Stopwatch) = viewModelScope.launch {
        when (stopwatch.state) {
            StopwatchState.IDLE -> container.stopwatchRepo.updateState(
                id = stopwatch.id,
                state = StopwatchState.RUNNING,
                startedAtMillis = System.currentTimeMillis(),
                accumulatedMillis = 0L,
            )
            StopwatchState.PAUSED -> container.stopwatchRepo.updateState(
                id = stopwatch.id,
                state = StopwatchState.RUNNING,
                startedAtMillis = System.currentTimeMillis(),
                accumulatedMillis = stopwatch.accumulatedMillis,
            )
            StopwatchState.RUNNING -> Unit
        }
    }

    fun pause(stopwatch: Stopwatch) = viewModelScope.launch {
        if (stopwatch.state != StopwatchState.RUNNING) return@launch
        val elapsed = stopwatch.elapsedMillis(System.currentTimeMillis())
        container.stopwatchRepo.updateState(
            id = stopwatch.id,
            state = StopwatchState.PAUSED,
            startedAtMillis = null,
            accumulatedMillis = elapsed,
        )
    }

    fun reset(stopwatch: Stopwatch) = viewModelScope.launch {
        container.stopwatchRepo.updateState(
            id = stopwatch.id,
            state = StopwatchState.IDLE,
            startedAtMillis = null,
            accumulatedMillis = 0L,
        )
        container.stopwatchRepo.clearLaps(stopwatch.id)
    }

    fun lap(stopwatch: Stopwatch) = viewModelScope.launch {
        if (stopwatch.state != StopwatchState.RUNNING) return@launch
        val total = stopwatch.elapsedMillis(System.currentTimeMillis())
        container.stopwatchRepo.addLap(stopwatch.id, total)
    }

    fun rename(stopwatch: Stopwatch, label: String) = viewModelScope.launch {
        container.stopwatchRepo.rename(stopwatch.id, label)
    }

    fun delete(stopwatch: Stopwatch) = viewModelScope.launch {
        container.stopwatchRepo.delete(stopwatch.id)
    }

    fun deleteAll() = viewModelScope.launch {
        container.stopwatchRepo.deleteAll()
    }
}

/** Single factory routes every ViewModel through the AppContainer. */
class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        @Suppress("UNCHECKED_CAST")
        return when (modelClass) {
            ClocksViewModel::class.java -> ClocksViewModel(container)
            AlarmsViewModel::class.java -> AlarmsViewModel(container)
            TimersViewModel::class.java -> TimersViewModel(container)
            StopwatchesViewModel::class.java -> StopwatchesViewModel(container)
            ClockEditViewModel::class.java -> ClockEditViewModel(container)
            AlarmEditViewModel::class.java -> AlarmEditViewModel(container)
            TimerEditViewModel::class.java -> TimerEditViewModel(container)
            SettingsViewModel::class.java -> SettingsViewModel(container)
            else -> error("Unknown VM: $modelClass")
        } as T
    }
}
