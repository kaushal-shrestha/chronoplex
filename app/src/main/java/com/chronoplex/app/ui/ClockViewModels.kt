package com.chronoplex.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chronoplex.app.AppContainer
import com.chronoplex.app.domain.Clock
import com.chronoplex.app.domain.Group
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
