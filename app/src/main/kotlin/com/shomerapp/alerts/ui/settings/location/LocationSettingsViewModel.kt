package com.shomerapp.alerts.ui.settings.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shomerapp.alerts.data.local.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationSettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
) : ViewModel() {

    val autoLocationEnabled = appPreferences.autoLocationEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val autoDetectedSettlement = appPreferences.autoDetectedSettlement.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setEnabled(enabled: Boolean) = viewModelScope.launch { appPreferences.setAutoLocationEnabled(enabled) }
}
