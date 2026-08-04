package com.shomerapp.alerts.domain

import com.shomerapp.alerts.data.areas.AreaRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class LocationSettlementMatcherTest {

    private val areaRepository = AreaRepository(File("src/main/assets/areas.json").readText())
    private val matcher = LocationSettlementMatcher(areaRepository)

    @Test
    fun `exact settlement name from geocoder matches directly`() {
        assertEquals("בני ברק", matcher.bestMatch(listOf("בני ברק")))
    }

    @Test
    fun `differently spaced or hyphenated candidate still matches`() {
        // Geocoder locality strings don't always match areas.json spelling exactly, same gap
        // AreaRepository.normalize already bridges for oref's own `data` field (§4).
        val result = matcher.bestMatch(listOf("תל אביב-מרכז העיר"))
        assertEquals("תל אביב - מרכז העיר", result)
    }

    @Test
    fun `falls back through candidates in priority order until one matches`() {
        val result = matcher.bestMatch(listOf("שם שלא קיים בשום מקום", "בני ברק", "עוד שם לא קיים"))
        assertEquals("בני ברק", result)
    }

    @Test
    fun `no candidate matches anything returns null, not a crash`() {
        assertNull(matcher.bestMatch(listOf("שם שלא קיים בשום מקום", "גם זה לא")))
    }

    @Test
    fun `blank and empty candidates are skipped without crashing`() {
        assertEquals("בני ברק", matcher.bestMatch(listOf("", "   ", "בני ברק")))
    }

    @Test
    fun `empty candidate list returns null`() {
        assertNull(matcher.bestMatch(emptyList()))
    }
}
