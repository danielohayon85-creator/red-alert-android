package com.shomerapp.alerts.domain.model

/** oref alerts.json payload after BOM stripping + `data` array-or-CSV normalization, pre-classification. */
data class RawOrefAlert(
    val id: String,
    val cat: String,
    val title: String,
    val cities: List<String>,
    val desc: String,
)
