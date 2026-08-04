package com.shomerapp.alerts.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class OemGuidanceTest {
    @Test
    fun `known aggressive-killer manufacturers get instructions`() {
        assertNotNull(OemGuidance.instructionsFor("Xiaomi"))
        assertNotNull(OemGuidance.instructionsFor("samsung"))
        assertNotNull(OemGuidance.instructionsFor("HUAWEI"))
        assertNotNull(OemGuidance.instructionsFor("OnePlus"))
        assertNotNull(OemGuidance.instructionsFor("OPPO"))
    }

    @Test
    fun `matching is case-insensitive and substring-based for device model variants`() {
        assertEquals("Xiaomi", OemGuidance.instructionsFor("Xiaomi/Redmi Note 12")?.manufacturerLabel)
    }

    @Test
    fun `unrelated manufacturers get no instructions so the onboarding step can skip itself`() {
        assertNull(OemGuidance.instructionsFor("Google"))
        assertNull(OemGuidance.instructionsFor("Motorola"))
    }
}
