package com.zoneanchor.ui.alarms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zoneanchor.ZoneAnchorApp
import com.zoneanchor.alarm.AlarmScheduler
import com.zoneanchor.data.alarms.AlarmRepository
import com.zoneanchor.data.clocks.ClockRepository
import com.zoneanchor.model.ClockEntry
import com.zoneanchor.model.ZonedAlarm
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmsViewModel(
    private val alarmsRepository: AlarmRepository,
    private val clocksRepository: ClockRepository,
    private val scheduler: AlarmScheduler,
) : ViewModel() {
    val alarms: StateFlow<List<ZonedAlarm>> = alarmsRepository.alarms.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val clocks: StateFlow<List<ClockEntry>> = clocksRepository.clocks.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    var lastAction = kotlinx.coroutines.flow.MutableStateFlow("")
        private set

    fun save(
        id: Int?,
        label: String,
        zoneId: String,
        hour: Int,
        minute: Int,
        daysMask: Int,
        soundMode: String,
        vibrate: Boolean,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            if ((daysMask and ZonedAlarm.ALL_DAYS) == 0) {
                lastAction.value = "Pick at least one day."
                return@launch
            }
            val alarm = ZonedAlarm(
                id = id ?: alarmsRepository.nextId(),
                label = label,
                zoneId = zoneId,
                hour = hour,
                minute = minute,
                daysOfWeekMask = daysMask,
                soundMode = soundMode,
                vibrate = vibrate,
                enabled = true,
            )
            val result = scheduler.scheduleNext(alarm)
            lastAction.value = result.message
            if (result.scheduled) onDone()
        }
    }

    fun cancel(id: Int) {
        viewModelScope.launch {
            scheduler.cancel(id)
            lastAction.value = "Alarm canceled."
        }
    }

    companion object {
        fun factory(app: ZoneAnchorApp) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AlarmsViewModel(app.alarmRepository, app.clockRepository, app.alarmScheduler) as T
            }
        }
    }
}
