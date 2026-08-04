package com.shomerapp.alerts.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject

/** Fetches from the real oref endpoint. Headers and BOM handling per spec §4 — without the
 *  headers below, the endpoint returns 403/404. */
class OrefAlertFetcher @Inject constructor(private val client: OkHttpClient) : AlertFetcher {

    override suspend fun fetchRaw(): String? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(ENDPOINT)
            .header("User-Agent", USER_AGENT)
            .header("Referer", REFERER)
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Content-Type", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bytes = response.body?.bytes() ?: return@withContext null
                decodeStrippingUtf8Bom(bytes)
            }
        } catch (e: IOException) {
            null
        }
    }

    private companion object {
        const val ENDPOINT = "https://www.oref.org.il/WarningMessages/alert/alerts.json"
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3"
        const val REFERER = "https://www.oref.org.il/"
    }
}

/** The response is encoded utf-8-sig — a leading BOM must be stripped before UTF-8 decoding,
 *  otherwise JSON parsing fails on the stray ﻿ (§4 "מלכודות"). */
fun decodeStrippingUtf8Bom(bytes: ByteArray): String {
    val hasBom = bytes.size >= 3 &&
        bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()
    val offset = if (hasBom) 3 else 0
    return String(bytes, offset, bytes.size - offset, Charsets.UTF_8)
}
