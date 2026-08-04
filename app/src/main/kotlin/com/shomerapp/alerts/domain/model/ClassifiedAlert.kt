package com.shomerapp.alerts.domain.model

/** A [RawOrefAlert] after classification and duration extraction — what the rest of the app consumes. */
data class ClassifiedAlert(
    val id: String,
    val kind: AlertKind,
    val title: String,
    val desc: String,
    val cities: List<String>,
    val durationSeconds: Int,
)
