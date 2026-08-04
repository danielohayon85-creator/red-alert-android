package com.shomerapp.alerts.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shomerapp.alerts.domain.ServiceHealthTracker
import com.shomerapp.alerts.domain.model.ServiceHealth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import javax.inject.Inject

data class MainUiState(val health: ServiceHealth) {
    val elapsedSeconds: Long?
        get() = health.lastUpdateEpochMillis?.let { (System.currentTimeMillis() - it) / 1000 }
}

@HiltViewModel
class MainViewModel @Inject constructor(
    healthTracker: ServiceHealthTracker,
) : ViewModel() {
    val uiState = combine(healthTracker.health, secondTicker()) { health, _ -> MainUiState(health) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = MainUiState(healthTracker.health.value),
        )
}

/** Only exists to force [MainUiState.elapsedSeconds] to recompute once a second while the
 *  status card is on screen and status stays ACTIVE — the underlying health value doesn't
 *  change between polls, but the "X seconds ago" text still needs to. */
private fun secondTicker(): Flow<Unit> = flow {
    while (true) {
        emit(Unit)
        delay(1_000)
    }
}
