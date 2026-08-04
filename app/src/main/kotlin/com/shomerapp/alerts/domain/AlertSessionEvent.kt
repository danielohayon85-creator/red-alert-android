package com.shomerapp.alerts.domain

sealed interface AlertSessionEvent {
    data class PrewarningReceived(
        val cities: List<String>,
        val title: String,
        val desc: String,
        val nowMillis: Long,
        val isDrill: Boolean = false,
    ) : AlertSessionEvent

    data class ImmediateReceived(
        val cities: List<String>,
        val title: String,
        val desc: String,
        val durationSeconds: Int,
        val nowMillis: Long,
        val isDrill: Boolean = false,
    ) : AlertSessionEvent

    data class AllClearReceived(val cities: List<String>, val nowMillis: Long) : AlertSessionEvent

    /** The Immediate session's local duration timer has run out. */
    data object LocalTimerElapsed : AlertSessionEvent

    /** 5 minutes passed since a Prewarning started with no follow-up Immediate. */
    data object PrewarningExpiryElapsed : AlertSessionEvent

    /** User pressed "אני במרחב המוגן" — stops the sound, does not touch the timer or the list. */
    data object UserConfirmedSafe : AlertSessionEvent

    /** User dismissed a Cleared/PrewarningExpired screen (or the app did, automatically). */
    data object Acknowledged : AlertSessionEvent
}
