package com.zoneanchor.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zoneanchor.app.AppContainer
import com.zoneanchor.app.domain.Group
import com.zoneanchor.app.domain.Stopwatch
import com.zoneanchor.app.domain.StopwatchLap
import com.zoneanchor.app.domain.StopwatchState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
