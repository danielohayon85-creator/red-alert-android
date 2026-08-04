package com.shomerapp.alerts.domain

import com.shomerapp.alerts.data.remote.AlertFetcherSwitch
import com.shomerapp.alerts.domain.model.AlertKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Drives §8's "סימולציית התרעה מלאה" / the Debug Panel's fake-alert injection through the real
 * pipeline (Stage 2's mock fetcher -> real polling/classification/session logic), so a drill
 * exercises the actual code path instead of a separate mocked-up UI state.
 *
 * Safety-critical detail: mock mode is auto-reverted after a short window regardless of what the
 * caller does next. Forgetting to turn it off would mean the app silently stops seeing real
 * alerts for as long as it's left on — that must never depend on a UI screen staying open or a
 * button being pressed correctly.
 */
@Singleton
class AlertSimulator @Inject constructor(private val fetcherSwitch: AlertFetcherSwitch) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun simulate(kind: AlertKind, settlements: List<String>, durationMinutes: Int = 10) {
        scope.launch {
            fetcherSwitch.setMockMode(true)
            fetcherSwitch.mock.enqueue(buildFakeAlertJson(kind, settlements, durationMinutes))
            delay(AUTO_REVERT_DELAY_MS)
            fetcherSwitch.setMockMode(false)
        }
    }

    private fun buildFakeAlertJson(kind: AlertKind, settlements: List<String>, durationMinutes: Int): String {
        val title = when (kind) {
            AlertKind.IMMEDIATE -> "ירי רקטות וטילים"
            AlertKind.PREWARNING -> "בדקות הקרובות צפויות להתקבל התרעות באיזורים הבאים:"
            AlertKind.ALL_CLEAR -> "ירי רקטות וטילים - האירוע הסתיים"
            AlertKind.INFO -> "רעידת אדמה"
        }
        val desc = "היכנסו למרחב המוגן ושהו בו $durationMinutes דקות"
        val citiesJson = settlements.joinToString(",") { "\"${it.replace("\"", "")}\"" }
        // Must be unique per call, not just per (settlements, duration) — otherwise re-running
        // the same drill twice in a row would be silently swallowed by AlertDeduplicator as
        // "already seen this id before, nothing new".
        val id = "drill-${System.currentTimeMillis()}"
        return """{"id":"$id","cat":"1","title":"$title","data":[$citiesJson],"desc":"$desc"}"""
    }

    private companion object {
        // Long enough for at least one real poll cycle (default 2s interval) to consume the
        // enqueued fake alert before mock mode reverts.
        const val AUTO_REVERT_DELAY_MS = 10_000L
    }
}
