package com.shomerapp.alerts.domain

import com.shomerapp.alerts.domain.model.HealthStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceHealthTrackerTest {

    @Test
    fun `starts in CONNECTING with no last update`() {
        val tracker = ServiceHealthTracker()
        assertEquals(HealthStatus.CONNECTING, tracker.health.value.status)
        assertEquals(null, tracker.health.value.lastUpdateEpochMillis)
    }

    @Test
    fun `a single failure does not flip status to DISCONNECTED`() {
        val tracker = ServiceHealthTracker()
        tracker.onPollFailure()
        assertEquals(HealthStatus.CONNECTING, tracker.health.value.status)
    }

    @Test
    fun `crossing the failure threshold flips status to DISCONNECTED exactly once`() {
        val tracker = ServiceHealthTracker()
        var crossings = 0
        repeat(ServiceHealthTracker.FAILURE_THRESHOLD + 5) {
            if (tracker.onPollFailure()) crossings++
        }
        assertEquals(1, crossings)
        assertEquals(HealthStatus.DISCONNECTED, tracker.health.value.status)
    }

    @Test
    fun `a success resets the failure count and marks ACTIVE`() {
        val tracker = ServiceHealthTracker()
        repeat(ServiceHealthTracker.FAILURE_THRESHOLD) { tracker.onPollFailure() }
        assertTrue(tracker.health.value.status == HealthStatus.DISCONNECTED)

        tracker.onPollSuccess()

        assertEquals(HealthStatus.ACTIVE, tracker.health.value.status)
        assertEquals(0, tracker.health.value.consecutiveFailures)
        assertFalse(tracker.health.value.lastUpdateEpochMillis == null)
    }
}
