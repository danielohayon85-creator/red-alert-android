package com.shomerapp.alerts.domain

import com.shomerapp.alerts.domain.model.AlertSessionState

/**
 * Pure state transition table for §4.1's event lifecycle. No Android/Room/coroutine dependency
 * on purpose — [AlertSessionManager] owns the side effects (audio, notifications, persistence),
 * this only decides what the next state is. Directly unit-tested against every §10 edge case.
 */
object AlertSessionReducer {

    fun reduce(state: AlertSessionState, event: AlertSessionEvent): AlertSessionState = when (event) {
        is AlertSessionEvent.PrewarningReceived -> onPrewarning(state, event)
        is AlertSessionEvent.ImmediateReceived -> onImmediate(state, event)
        is AlertSessionEvent.AllClearReceived -> onAllClear(state, event)
        AlertSessionEvent.LocalTimerElapsed -> onLocalTimerElapsed(state)
        AlertSessionEvent.PrewarningExpiryElapsed -> onPrewarningExpiry(state)
        AlertSessionEvent.UserConfirmedSafe -> onUserConfirmedSafe(state)
        AlertSessionEvent.Acknowledged -> onAcknowledged(state)
    }

    private fun onPrewarning(state: AlertSessionState, event: AlertSessionEvent.PrewarningReceived): AlertSessionState =
        when (state) {
            is AlertSessionState.Idle, is AlertSessionState.PrewarningExpired, is AlertSessionState.Cleared ->
                AlertSessionState.Prewarning(event.nowMillis, event.cities.distinct(), event.title, event.desc)

            is AlertSessionState.Prewarning ->
                // Accumulate settlements, don't reset the start time (§8: don't reset the timer
                // when new cities join an already-active session).
                state.copy(settlements = (state.settlements + event.cities).distinct())

            // A prewarning arriving mid-Immediate/WaitingForAllClear is a genuinely ambiguous
            // "second concurrent session" case the spec doesn't fully resolve — safest choice is
            // to not downgrade or interrupt an already-active full alarm. Ignored for now.
            is AlertSessionState.Immediate, is AlertSessionState.WaitingForAllClear -> state
        }

    private fun onImmediate(state: AlertSessionState, event: AlertSessionEvent.ImmediateReceived): AlertSessionState =
        when (state) {
            is AlertSessionState.Idle, is AlertSessionState.PrewarningExpired, is AlertSessionState.Cleared ->
                // No preceding prewarning (or the prior session had already fully concluded) —
                // a fresh alarm, re-triggered from scratch.
                AlertSessionState.Immediate(
                    startedAtEpochMillis = event.nowMillis,
                    prewarningStartedAtEpochMillis = null,
                    settlements = event.cities.distinct(),
                    title = event.title,
                    desc = event.desc,
                    durationSeconds = event.durationSeconds,
                    acknowledgedByUser = false,
                )

            is AlertSessionState.Prewarning ->
                // §4.1 "מעבר מקדימה -> מיידית": seamless, no overlap — carries the prewarning
                // start time forward for the "prewarning started X ago" display.
                AlertSessionState.Immediate(
                    startedAtEpochMillis = event.nowMillis,
                    prewarningStartedAtEpochMillis = state.startedAtEpochMillis,
                    settlements = (state.settlements + event.cities).distinct(),
                    title = event.title,
                    desc = event.desc,
                    durationSeconds = event.durationSeconds,
                    acknowledgedByUser = false,
                )

            is AlertSessionState.Immediate ->
                // Same ongoing session spreading to more cities — accumulate, never reset the
                // timer (§8), never lengthen it below what's already been promised, never
                // silently re-arm a session the user already confirmed as safe.
                state.copy(
                    settlements = (state.settlements + event.cities).distinct(),
                    durationSeconds = maxOf(state.durationSeconds, event.durationSeconds),
                )

            is AlertSessionState.WaitingForAllClear ->
                // §10 "גל שני": a new wave arrives after the local timer ran out but before the
                // official all-clear — treat as a fresh alarm, re-triggering sound/vibration.
                AlertSessionState.Immediate(
                    startedAtEpochMillis = event.nowMillis,
                    prewarningStartedAtEpochMillis = null,
                    settlements = (state.settlements + event.cities).distinct(),
                    title = event.title,
                    desc = event.desc,
                    durationSeconds = event.durationSeconds,
                    acknowledgedByUser = false,
                )
        }

    private fun onAllClear(state: AlertSessionState, event: AlertSessionEvent.AllClearReceived): AlertSessionState {
        val activeSettlements = when (state) {
            is AlertSessionState.Immediate -> state.settlements
            is AlertSessionState.WaitingForAllClear -> state.settlements
            else -> emptyList()
        }
        val overlaps = activeSettlements.any { it in event.cities }
        // §4.1: an all-clear for a settlement with no active alert on this device is ignored
        // silently — including while purely in Prewarning (no shelter instruction was ever
        // given, so there's nothing to tell the user they can leave).
        if (!overlaps) return state

        return AlertSessionState.Cleared(activeSettlements)
    }

    private fun onLocalTimerElapsed(state: AlertSessionState): AlertSessionState = when (state) {
        is AlertSessionState.Immediate -> AlertSessionState.WaitingForAllClear(state.startedAtEpochMillis, state.settlements, state.title)
        else -> state
    }

    private fun onPrewarningExpiry(state: AlertSessionState): AlertSessionState = when (state) {
        is AlertSessionState.Prewarning -> AlertSessionState.PrewarningExpired(state.settlements)
        else -> state
    }

    private fun onUserConfirmedSafe(state: AlertSessionState): AlertSessionState = when (state) {
        is AlertSessionState.Immediate -> state.copy(acknowledgedByUser = true)
        else -> state
    }

    private fun onAcknowledged(state: AlertSessionState): AlertSessionState = when (state) {
        is AlertSessionState.Cleared, is AlertSessionState.PrewarningExpired -> AlertSessionState.Idle
        else -> state
    }
}
