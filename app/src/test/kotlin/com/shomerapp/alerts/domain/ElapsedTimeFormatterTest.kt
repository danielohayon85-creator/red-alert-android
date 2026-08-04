package com.shomerapp.alerts.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ElapsedTimeFormatterTest {

    @Test
    fun `formats under a minute as seconds`() {
        assertEquals("5 שניות", ElapsedTimeFormatter.format(5))
    }

    @Test
    fun `formats under an hour as minutes`() {
        assertEquals("2 דקות", ElapsedTimeFormatter.format(150))
    }

    @Test
    fun `formats an hour or more as hours`() {
        assertEquals("2 שעות", ElapsedTimeFormatter.format(7200))
    }
}
