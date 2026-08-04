package com.shomerapp.alerts.data.areas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AreaRepositoryTest {

    private val realRepo = AreaRepository(File("src/main/assets/areas.json").readText())

    @Test
    fun `real areas asset has around 30 areas`() {
        assertTrue(realRepo.allAreas().size in 20..40)
    }

    @Test
    fun `settlement lookup normalizes dashes, parens and spacing like oref's data field does`() {
        // "תל אביב - מרכז העיר" (areas.json) must match "תל אביב-מרכז העיר" as oref sometimes sends it
        val areasForDashed = realRepo.areasForSettlement("תל אביב-מרכז העיר")
        assertTrue("expected a match for a differently-spaced variant", areasForDashed.isNotEmpty())
    }

    @Test
    fun `unknown settlement returns an empty list, not a crash`() {
        assertEquals(emptyList<String>(), realRepo.areasForSettlement("שם שלא קיים בשום מקום"))
    }

    @Test
    fun `settlement belonging to multiple areas returns all of them, not just one`() {
        // Synthetic fixture — real data currently has zero overlaps, but the lookup contract
        // must not silently collapse to a single area if/when the source data does have one.
        val fixture = AreaRepository(
            """{"אזור א": ["ישוב משותף"], "אזור ב": ["ישוב משותף", "ישוב אחר"]}""",
        )
        val areas = fixture.areasForSettlement("ישוב משותף")
        assertEquals(setOf("אזור א", "אזור ב"), areas.toSet())
    }
}
