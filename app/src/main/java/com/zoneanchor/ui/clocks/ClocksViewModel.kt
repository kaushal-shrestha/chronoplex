package com.zoneanchor.ui.clocks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zoneanchor.ZoneAnchorApp
import com.zoneanchor.data.clocks.ClockRepository
import com.zoneanchor.model.ClockEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClocksViewModel(private val repository: ClockRepository) : ViewModel() {
    val clocks: StateFlow<List<ClockEntry>> = repository.clocks.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    fun save(oldZoneId: String?, newZoneId: String, label: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.addOrUpdate(oldZoneId, newZoneId, label)
            onDone()
        }
    }

    fun delete(zoneId: String) {
        viewModelScope.launch { repository.delete(zoneId) }
    }

    companion object {
        fun factory(app: ZoneAnchorApp) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ClocksViewModel(app.clockRepository) as T
            }
        }
    }
}
