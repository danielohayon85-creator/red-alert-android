package com.shomerapp.alerts.domain.model

/** §4 of the spec — four-way alert classification. Order matters for §4.1's state machine. */
enum class AlertKind {
    IMMEDIATE,
    PREWARNING,
    ALL_CLEAR,
    INFO,
}
