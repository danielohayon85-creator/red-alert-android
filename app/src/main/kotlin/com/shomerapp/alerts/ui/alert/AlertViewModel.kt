package com.shomerapp.alerts.ui.alert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shomerapp.alerts.domain.AlertSessionManager
import com.shomerapp.alerts.domain.model.AlertSessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AlertUiState(val session: AlertSessionState, val nowMillis: Long)

@HiltViewModel
class AlertViewModel @Inject constructor(
    private val sessionManager: AlertSessionManager,
) : ViewModel() {

    val uiState = combine(sessionManager.state, secondTicker()) { session, now -> AlertUiState(session, now) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlertUiState(sessionManager.state.value, System.currentTimeMillis()))

    fun onConfirmedSafe() = sessionManager.onUserConfirmedSafe()

    fun onAcknowledge() = sessionManager.onAcknowledged()
}

private fun secondTicker(): Flow<Long> = flow {
    while (true) {
        emit(System.currentTimeMillis())
        delay(1_000)
    }
}
