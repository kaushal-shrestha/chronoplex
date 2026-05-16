package com.zoneanchor.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zoneanchor.ZoneAnchorApp
import com.zoneanchor.data.settings.SettingsRepository
import com.zoneanchor.model.AppPalette
import com.zoneanchor.model.AppearanceMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val appearance: StateFlow<AppearanceMode> = repository.appearanceFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppearanceMode.SYSTEM,
    )
    val palette: StateFlow<AppPalette> = repository.paletteFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppPalette.DAYBREAK,
    )

    fun setAppearance(mode: AppearanceMode) {
        viewModelScope.launch { repository.setAppearance(mode) }
    }

    fun setPalette(next: AppPalette) {
        viewModelScope.launch { repository.setPalette(next) }
    }

    companion object {
        fun factory(app: ZoneAnchorApp) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(app.settingsRepository) as T
            }
        }
    }
}
