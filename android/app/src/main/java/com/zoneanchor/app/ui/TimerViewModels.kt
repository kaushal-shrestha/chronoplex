package com.zoneanchor.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zoneanchor.app.AppContainer
import com.zoneanchor.app.domain.Group
import com.zoneanchor.app.domain.Timer
import com.zoneanchor.app.domain.TimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
