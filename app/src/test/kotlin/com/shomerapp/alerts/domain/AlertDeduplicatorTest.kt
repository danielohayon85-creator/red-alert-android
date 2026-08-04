package com.shomerapp.alerts.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertDeduplicatorTest {

    @Test
    fun `first sighting of an id is a new event with all its cities as new`() = runTest {
        val dedup = AlertDeduplicator()
        val result = dedup.update("id1", listOf("עיר א", "עיר ב"))
        assertTrue(result.isNewEvent)
        assertEquals(listOf("עיר א", "עיר ב"), result.newCities)
    }

    @Test
    fun `same id with an expanded city list reports only the newly-added cities`() = runTest {
        val dedup = AlertDeduplicator()
        dedup.update("id1", listOf("עיר א"))
        val result = dedup.update("id1", listOf("עיר א", "עיר ב", "עיר ג"))
        assertEquals(false, result.isNewEvent)
        assertEquals(listOf("עיר ב", "עיר ג"), result.newCities)
    }

    @Test
    fun `repeating the same id and cities reports nothing new`() = runTest {
        val dedup = AlertDeduplicator()
        dedup.update("id1", listOf("עיר א"))
        val result = dedup.update("id1", listOf("עיר א"))
        assertTrue(result.newCities.isEmpty())
    }

    @Test
    fun `different ids are tracked independently`() = runTest {
        val dedup = AlertDeduplicator()
        dedup.update("id1", listOf("עיר א"))
        val result = dedup.update("id2", listOf("עיר א"))
        assertTrue("a new id must not be suppressed by another id's history", result.isNewEvent)
        assertEquals(listOf("עיר א"), result.newCities)
    }
}
