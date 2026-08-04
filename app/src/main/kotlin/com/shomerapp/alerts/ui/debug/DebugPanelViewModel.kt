package com.shomerapp.alerts.ui.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shomerapp.alerts.data.local.AppPreferences
import com.shomerapp.alerts.domain.AlertSimulator
import com.shomerapp.alerts.domain.model.AlertKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebugPanelViewModel @Inject constructor(
    private val alertSimulator: AlertSimulator,
    private val appPreferences: AppPreferences,
) : ViewModel() {

    fun injectAlert(kind: AlertKind, settlement: String, durationMinutes: Int) {
        viewModelScope.launch {
            val settlements = if (settlement.isNotBlank()) {
                listOf(settlement)
            } else {
                appPreferences.selectedSettlements.first().toList().ifEmpty { listOf("תל אביב") }
            }
            alertSimulator.simulate(kind, settlements, durationMinutes)
        }
    }
}
