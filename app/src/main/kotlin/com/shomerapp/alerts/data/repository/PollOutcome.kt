package com.shomerapp.alerts.data.repository

import com.shomerapp.alerts.domain.model.ClassifiedAlert

sealed interface PollOutcome {
    /** oref returned a blank body — no active alert. */
    data object Empty : PollOutcome

    /** New alert, or an existing alert whose city list grew — carries only the newly-added cities. */
    data class AlertUpdate(
        val alert: ClassifiedAlert,
        val newCities: List<String>,
        val isNewEvent: Boolean,
    ) : PollOutcome

    /** Same alert id, no cities added since last poll — nothing new to surface. */
    data object NoChange : PollOutcome

    /** Fetch failed (timeout, non-2xx, no connectivity). */
    data object NetworkError : PollOutcome

    /** Non-blank body that didn't parse as the expected shape. */
    data class MalformedResponse(val raw: String) : PollOutcome
}
