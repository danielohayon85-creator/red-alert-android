package com.shomerapp.alerts.domain

import com.shomerapp.alerts.domain.model.AlertSessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** One test per §10 "מקרי קצה של מחזור חיי האירוע" bullet. */
class AlertSessionReducerTest {

    private fun reduce(state: AlertSessionState, event: AlertSessionEvent) = AlertSessionReducer.reduce(state, event)

    @Test
    fun `a drill alert is marked as a drill all the way through, and clearing it stays marked too`() {
        val immediate = reduce(
            AlertSessionState.Idle,
            AlertSessionEvent.ImmediateReceived(listOf("תל אביב"), "ירי רקטות וטילים", "d", 600, 0, isDrill = true),
        ) as AlertSessionState.Immediate
        assertTrue("a drill must never be indistinguishable from a real alert (§8)", immediate.isDrill)

        val cleared = reduce(immediate, AlertSessionEvent.AllClearReceived(listOf("תל אביב"), 1000))
        assertTrue(cleared is AlertSessionState.Cleared && cleared.isDrill)
    }

    @Test
    fun `a real alert is never accidentally marked as a drill`() {
        val immediate = reduce(
            AlertSessionState.Idle,
            AlertSessionEvent.ImmediateReceived(listOf("תל אביב"), "ירי רקטות וטילים", "d", 600, 0),
        ) as AlertSessionState.Immediate
        assertFalse(immediate.isDrill)
    }

    @Test
    fun `prewarning then immediate 2m09s later is a smooth transition that keeps the prewarning start time`() {
        val afterPrewarning = reduce(
            AlertSessionState.Idle,
            AlertSessionEvent.PrewarningReceived(listOf("תל אביב"), "בדקות הקרובות...", "...", nowMillis = 0),
        )
        val afterImmediate = reduce(
            afterPrewarning,
            AlertSessionEvent.ImmediateReceived(listOf("תל אביב"), "ירי רקטות וטילים", "...", 600, nowMillis = 129_000),
        ) as AlertSessionState.Immediate

        assertEquals(0L, afterImmediate.prewarningStartedAtEpochMillis)
        assertEquals(129_000L, afterImmediate.startedAtEpochMillis)
        assertEquals(listOf("תל אביב"), afterImmediate.settlements)
    }

    @Test
    fun `prewarning with no follow-up immediate expires cleanly after 5 minutes`() {
        val prewarning = AlertSessionState.Prewarning(0, listOf("תל אביב"), "t", "d")
        val expired = reduce(prewarning, AlertSessionEvent.PrewarningExpiryElapsed)
        assertTrue(expired is AlertSessionState.PrewarningExpired)

        val backToIdle = reduce(expired, AlertSessionEvent.Acknowledged)
        assertEquals(AlertSessionState.Idle, backToIdle)
    }

    @Test
    fun `closing the prewarning screen must not be modeled as suppressing a later immediate`() {
        // The reducer has no "user closed the screen" event at all for Prewarning — closing the
        // UI is purely a view-layer action; the session (and any later ImmediateReceived) is
        // completely unaffected by it. This test documents that guarantee.
        val prewarning = AlertSessionState.Prewarning(0, listOf("תל אביב"), "t", "d")
        val stillPrewarning = reduce(prewarning, AlertSessionEvent.UserConfirmedSafe) // only no-op events available pre-Immediate
        assertEquals(prewarning, stillPrewarning)

        val immediate = reduce(
            stillPrewarning,
            AlertSessionEvent.ImmediateReceived(listOf("תל אביב"), "ירי רקטות וטילים", "d", 600, 1000),
        )
        assertTrue("a later real alert must still fully fire", immediate is AlertSessionState.Immediate)
    }

    @Test
    fun `immediate with no preceding prewarning starts a session with a null prewarning time`() {
        val immediate = reduce(
            AlertSessionState.Idle,
            AlertSessionEvent.ImmediateReceived(listOf("שדרות"), "ירי רקטות וטילים", "d", 60, 0),
        ) as AlertSessionState.Immediate
        assertEquals(null, immediate.prewarningStartedAtEpochMillis)
    }

    @Test
    fun `all-clear before the local timer elapses is authoritative immediately`() {
        val immediate = AlertSessionState.Immediate(0, null, listOf("אשקלון"), "t", "d", 600, acknowledgedByUser = false)
        val cleared = reduce(immediate, AlertSessionEvent.AllClearReceived(listOf("אשקלון"), nowMillis = 5_000))
        assertTrue(cleared is AlertSessionState.Cleared)
    }

    @Test
    fun `local timer elapsing without an official all-clear shows waiting, never green`() {
        val immediate = AlertSessionState.Immediate(0, null, listOf("אשקלון"), "t", "d", 600, acknowledgedByUser = false)
        val waiting = reduce(immediate, AlertSessionEvent.LocalTimerElapsed)
        assertTrue(waiting is AlertSessionState.WaitingForAllClear)
        assertFalse("must never be Cleared without an explicit AllClearReceived", waiting is AlertSessionState.Cleared)
    }

    @Test
    fun `second wave after the timer elapsed but before all-clear re-arms a fresh alarm`() {
        val waiting = AlertSessionState.WaitingForAllClear(0, listOf("אשקלון"), "t")
        val secondWave = reduce(
            waiting,
            AlertSessionEvent.ImmediateReceived(listOf("אשקלון", "שדרות"), "ירי רקטות וטילים", "d", 600, nowMillis = 700_000),
        ) as AlertSessionState.Immediate

        assertFalse("a second wave must not silently inherit the acknowledged flag", secondWave.acknowledgedByUser)
        assertEquals(setOf("אשקלון", "שדרות"), secondWave.settlements.toSet())
    }

    @Test
    fun `all-clear for a settlement with nothing active on this device is ignored silently`() {
        val idle = AlertSessionState.Idle
        assertEquals(idle, reduce(idle, AlertSessionEvent.AllClearReceived(listOf("עיר כלשהי"), 0)))

        val immediateElsewhere = AlertSessionState.Immediate(0, null, listOf("חיפה"), "t", "d", 600, false)
        val unaffected = reduce(immediateElsewhere, AlertSessionEvent.AllClearReceived(listOf("אילת"), 1000))
        assertEquals(immediateElsewhere, unaffected)
    }

    @Test
    fun `phone restart mid-alert is just state restored as-is, timers recomputed from stored start time`() {
        // AlertSessionManager restores this exact state from Room; the reducer itself has no
        // special-case for "restarted" because there isn't one needed — startedAtEpochMillis is
        // wall-clock, so elapsed/remaining time recomputes correctly from any restore point.
        val restored = AlertSessionState.Immediate(
            startedAtEpochMillis = 1_000_000L,
            prewarningStartedAtEpochMillis = null,
            settlements = listOf("נתניה"),
            title = "t",
            desc = "d",
            durationSeconds = 600,
            acknowledgedByUser = false,
        )
        val stillTicking = reduce(
            restored,
            AlertSessionEvent.ImmediateReceived(listOf("נתניה", "רעננה"), "ירי רקטות וטילים", "d", 600, nowMillis = 1_100_000L),
        ) as AlertSessionState.Immediate
        assertEquals(1_000_000L, stillTicking.startedAtEpochMillis) // unaffected by the restart/merge
        assertEquals(setOf("נתניה", "רעננה"), stillTicking.settlements.toSet())
    }

    @Test
    fun `confirming safe stops later re-triggering but keeps the list and timer alive`() {
        val immediate = AlertSessionState.Immediate(0, null, listOf("לוד"), "t", "d", 600, acknowledgedByUser = false)
        val confirmed = reduce(immediate, AlertSessionEvent.UserConfirmedSafe) as AlertSessionState.Immediate
        assertTrue(confirmed.acknowledgedByUser)

        val moreCities = reduce(
            confirmed,
            AlertSessionEvent.ImmediateReceived(listOf("לוד", "רמלה"), "t", "d", 600, 5_000),
        ) as AlertSessionState.Immediate
        assertEquals(setOf("לוד", "רמלה"), moreCities.settlements.toSet())
        assertEquals(0L, moreCities.startedAtEpochMillis) // timer never reset
    }
}
