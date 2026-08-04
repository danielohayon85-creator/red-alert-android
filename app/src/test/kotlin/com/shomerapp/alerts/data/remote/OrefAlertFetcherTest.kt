package com.shomerapp.alerts.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class OrefAlertFetcherTest {

    @Test
    fun `strips a leading UTF-8 BOM before decoding`() {
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val bytes = bom + "{\"id\":\"1\"}".toByteArray(Charsets.UTF_8)
        assertEquals("{\"id\":\"1\"}", decodeStrippingUtf8Bom(bytes))
    }

    @Test
    fun `decodes normally when there is no BOM`() {
        val bytes = "{\"id\":\"1\"}".toByteArray(Charsets.UTF_8)
        assertEquals("{\"id\":\"1\"}", decodeStrippingUtf8Bom(bytes))
    }

    @Test
    fun `empty body decodes to an empty string, not a crash`() {
        assertEquals("", decodeStrippingUtf8Bom(ByteArray(0)))
    }
}
