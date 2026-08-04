package com.shomerapp.alerts.domain

import com.shomerapp.alerts.domain.model.HealthStatus
import com.shomerapp.alerts.domain.model.ServiceHealth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for "is the app actually protecting me right now" (§6, §8) — updated
 * by [com.shomerapp.alerts.service.AlertForegroundService], observed by the UI. A single
 * failed poll must NOT flip the status to disconnected (network hiccups are routine); only
 * [FAILURE_THRESHOLD] *consecutive* failures does, per §6's "מעל 10 כשלים".
 */
@Singleton
class ServiceHealthTracker @Inject constructor() {
    private val _health = MutableStateFlow(
        ServiceHealth(status = HealthStatus.CONNECTING, lastUpdateEpochMillis = null, consecutiveFailures = 0),
    )
    val health: StateFlow<ServiceHealth> = _health.asStateFlow()

    fun markConnecting() {
        _health.update { it.copy(status = HealthStatus.CONNECTING) }
    }

    fun markNoNetwork() {
        _health.update { it.copy(status = HealthStatus.DISCONNECTED) }
    }

    fun onPollSuccess() {
        _health.update {
            it.copy(status = HealthStatus.ACTIVE, lastUpdateEpochMillis = System.currentTimeMillis(), consecutiveFailures = 0)
        }
    }

    /** Returns true exactly once, the poll where the failure count *crosses* the threshold —
     *  callers use that edge to fire a one-shot loud notification instead of one per failure. */
    fun onPollFailure(): Boolean {
        var crossedThreshold = false
        _health.update { current ->
            val failures = current.consecutiveFailures + 1
            crossedThreshold = failures == FAILURE_THRESHOLD
            val status = if (failures >= FAILURE_THRESHOLD) HealthStatus.DISCONNECTED else current.status
            current.copy(consecutiveFailures = failures, status = status)
        }
        return crossedThreshold
    }

    companion object {
        const val FAILURE_THRESHOLD = 10
    }
}
