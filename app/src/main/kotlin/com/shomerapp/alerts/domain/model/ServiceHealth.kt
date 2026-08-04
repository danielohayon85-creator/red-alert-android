package com.shomerapp.alerts.domain.model

/** §8 main-screen status card: 🟢 ACTIVE / 🟡 CONNECTING / 🔴 DISCONNECTED. */
enum class HealthStatus { ACTIVE, CONNECTING, DISCONNECTED }

data class ServiceHealth(
    val status: HealthStatus,
    val lastUpdateEpochMillis: Long?,
    val consecutiveFailures: Int,
)
