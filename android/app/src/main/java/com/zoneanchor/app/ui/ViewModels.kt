package com.zoneanchor.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.zoneanchor.app.AppContainer

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
