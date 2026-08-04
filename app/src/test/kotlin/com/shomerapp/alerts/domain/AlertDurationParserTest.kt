package com.shomerapp.alerts.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AlertDurationParserTest {

    @Test
    fun `extracts first number of minutes and converts to seconds`() {
        assertEquals(600, AlertDurationParser.extractSeconds("היכנסו למרחב המוגן ושהו בו 10 דקות"))
    }

    @Test
    fun `uses the first number when multiple appear`() {
        assertEquals(60, AlertDurationParser.extractSeconds("שהו 1 דקות, המרחק כ-15 ק״מ"))
    }

    @Test
    fun `defaults to 600 seconds when no number is present`() {
        assertEquals(600, AlertDurationParser.extractSeconds("היכנסו למרחב המוגן"))
    }
}
