package com.shomerapp.alerts.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MmSsFormatterTest {
    @Test
    fun `formats seconds under a minute with a leading zero`() {
        assertEquals("0:47", MmSsFormatter.format(47))
    }

    @Test
    fun `formats minutes and seconds`() {
        assertEquals("2:09", MmSsFormatter.format(129))
    }

    @Test
    fun `never goes negative`() {
        assertEquals("0:00", MmSsFormatter.format(-5))
    }
}
