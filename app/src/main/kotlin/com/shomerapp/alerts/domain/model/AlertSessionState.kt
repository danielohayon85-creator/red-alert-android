package com.shomerapp.alerts.domain.model

/**
 * The single active "local alert session" the AlertActivity renders (§4.1, §8). Correlated by
 * settlement name across separate oref messages, NOT by oref's `id` — PREWARNING, IMMEDIATE and
 * ALL_CLEAR for the same real-world event are three independent messages with three different
 * ids; only their overlapping settlement lists tie them together.
 */
sealed interface AlertSessionState {
    data object Idle : AlertSessionState

    data class Prewarning(
        val startedAtEpochMillis: Long,
        val settlements: List<String>,
        val title: String,
        val desc: String,
    ) : AlertSessionState

    /** A PREWARNING that got no follow-up IMMEDIATE within the 5-minute window — expected for
     *  roughly half of them per §4.1's real-world numbers. Shown briefly, then dismissed to Idle. */
    data class PrewarningExpired(val settlements: List<String>) : AlertSessionState

    data class Immediate(
        val startedAtEpochMillis: Long,
        /** Non-null when this followed a PREWARNING — lets the screen show "PREWARNING started
         *  X ago" per §4.1's "בונה אמון" requirement. */
        val prewarningStartedAtEpochMillis: Long?,
        val settlements: List<String>,
        val title: String,
        val desc: String,
        val durationSeconds: Int,
        /** True once the user pressed "אני במרחב המוגן" — stops the sound but must NOT stop the
         *  countdown or suppress new settlements being added to the list (§8). */
        val acknowledgedByUser: Boolean,
    ) : AlertSessionState

    /** Local duration timer elapsed but no official ALL_CLEAR arrived yet — §4.1's most
     *  important rule: never show the green "you can leave" screen in this state. */
    data class WaitingForAllClear(
        val startedAtEpochMillis: Long,
        val settlements: List<String>,
        val title: String,
    ) : AlertSessionState

    data class Cleared(val settlements: List<String>) : AlertSessionState
}
