package com.shomerapp.alerts.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlertRawJsonTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `blank body means no alert`() {
        assertNull(parseOrefResponse("", json))
        assertNull(parseOrefResponse("   \n  ", json))
    }

    @Test
    fun `parses data as a JSON array of settlement names`() {
        val body = """{"id":"1","cat":"1","title":"ירי רקטות וטילים","data":["תל אביב","רמת גן"],"desc":"היכנסו למרחב המוגן"}"""
        val alert = parseOrefResponse(body, json)!!
        assertEquals(listOf("תל אביב", "רמת גן"), alert.cities)
        assertEquals("1", alert.cat) // cat must stay a String, not be coerced to a number
    }

    @Test
    fun `parses data as a comma-separated string, matching an alternate response shape`() {
        val body = """{"id":"1","cat":"1","title":"ירי רקטות וטילים","data":"תל אביב, רמת גן","desc":"..."}"""
        val alert = parseOrefResponse(body, json)!!
        assertEquals(listOf("תל אביב", "רמת גן"), alert.cities)
    }
}
