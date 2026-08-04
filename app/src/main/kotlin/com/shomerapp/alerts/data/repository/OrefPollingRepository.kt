package com.shomerapp.alerts.data.repository

import com.shomerapp.alerts.data.remote.AlertFetcher
import com.shomerapp.alerts.data.remote.parseOrefResponse
import com.shomerapp.alerts.domain.AlertClassifier
import com.shomerapp.alerts.domain.AlertDeduplicator
import com.shomerapp.alerts.domain.AlertDurationParser
import com.shomerapp.alerts.domain.model.ClassifiedAlert
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Combines fetch -> BOM/parse -> dedup -> classify into a single poll, and exposes a
 * self-pacing poll loop. Connectivity-aware pause/resume, the persistent notification, and the
 * consecutive-failure health status surfaced to the UI are the Foreground Service's job
 * (Stage 3) — this class only owns the HTTP/parsing/backoff mechanics.
 */
@Singleton
class OrefPollingRepository @Inject constructor(
    private val fetcher: AlertFetcher,
    private val classifier: AlertClassifier,
    private val deduplicator: AlertDeduplicator,
    private val json: Json,
) {
    suspend fun pollOnce(): PollOutcome {
        val raw = fetcher.fetchRaw() ?: return PollOutcome.NetworkError

        val rawAlert = try {
            parseOrefResponse(raw, json) ?: return PollOutcome.Empty
        } catch (e: SerializationException) {
            return PollOutcome.MalformedResponse(raw)
        }

        val dedupResult = deduplicator.update(rawAlert.id, rawAlert.cities)
        if (dedupResult.newCities.isEmpty()) return PollOutcome.NoChange

        val classified = ClassifiedAlert(
            id = rawAlert.id,
            kind = classifier.classify(rawAlert.title, rawAlert.cat),
            title = rawAlert.title,
            desc = rawAlert.desc,
            cities = rawAlert.cities,
            durationSeconds = AlertDurationParser.extractSeconds(rawAlert.desc),
        )
        return PollOutcome.AlertUpdate(classified, dedupResult.newCities, dedupResult.isNewEvent)
    }

    /**
     * Self-pacing loop: exponential backoff on consecutive network failures (base -> 2x -> 4x ...
     * capped at [MAX_BACKOFF_MS]), instant reset to [intervalMillis] on the first success after a
     * failure (§6). Cancellation is handled by the collecting coroutine via the suspending [delay].
     */
    fun pollLoop(intervalMillis: Long = DEFAULT_INTERVAL_MS): Flow<PollOutcome> = flow {
        var currentDelay = intervalMillis
        while (true) {
            val outcome = pollOnce()
            emit(outcome)
            currentDelay = if (outcome is PollOutcome.NetworkError) {
                (currentDelay * 2).coerceAtMost(MAX_BACKOFF_MS)
            } else {
                intervalMillis
            }
            delay(currentDelay)
        }
    }

    companion object {
        const val DEFAULT_INTERVAL_MS = 2_000L
        const val MIN_INTERVAL_MS = 1_000L
        const val MAX_INTERVAL_MS = 5_000L
        const val MAX_BACKOFF_MS = 30_000L
    }
}
