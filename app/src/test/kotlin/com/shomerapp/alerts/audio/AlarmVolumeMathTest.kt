package com.shomerapp.alerts.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmVolumeMathTest {

    @Test
    fun `raises a quiet volume up to the minimum fraction of max`() {
        // max 15, 80% -> 12
        assertEquals(12, AlarmVolumeMath.targetVolume(currentVolume = 3, maxVolume = 15, minFraction = 0.8f))
    }

    @Test
    fun `never lowers a volume that is already above the minimum`() {
        assertEquals(14, AlarmVolumeMath.targetVolume(currentVolume = 14, maxVolume = 15, minFraction = 0.8f))
    }

    @Test
    fun `never exceeds max volume even with rounding`() {
        assertEquals(15, AlarmVolumeMath.targetVolume(currentVolume = 15, maxVolume = 15, minFraction = 1.0f))
    }

    @Test
    fun `at exactly the threshold stays unchanged`() {
        assertEquals(12, AlarmVolumeMath.targetVolume(currentVolume = 12, maxVolume = 15, minFraction = 0.8f))
    }
}
