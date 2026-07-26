package com.chronoplex.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronoplex.app.AppContainer
import com.chronoplex.app.data.AlarmZoneSource
import com.chronoplex.app.domain.Alarm
import com.chronoplex.app.domain.AlarmRepeatType
import com.chronoplex.app.domain.DayMask
import com.chronoplex.app.domain.Group
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

data class AlarmEditState(
    val id: Long = 0,
    val label: String = "",
    val zoneId: String = "",
    /** Defaults to 00:00 (12:00 AM) - matches Google Clock's new-alarm default. */
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
    fun setMonthlyOrdinal(ordinal: Int) =
        state.update { it.copy(monthlyOrdinal = if (ordinal == -1) -1 else ordinal.coerceIn(1, 4)) }
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
