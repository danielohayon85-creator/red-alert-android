package com.shomerapp.alerts.domain

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

data class DedupResult(val isNewEvent: Boolean, val newCities: List<String>)

/**
 * Tracks Map<id, Set<city>> per §4: the same alert id can recur with a growing city list as an
 * event spreads — only newly-added cities should trigger a fresh notification, not a whole new
 * alert from scratch. In-memory only for now; surviving process death is handled once Room-backed
 * event persistence lands in Stage 5.
 *
 * @Singleton is required here, not just a nicety: this must be the SAME instance the polling
 * repository uses across its lifetime, or dedup state resets on every injection and every
 * alert looks "new" again.
 */
@Singleton
class AlertDeduplicator @Inject constructor() {
    private val mutex = Mutex()
    private val seenCitiesByAlertId = mutableMapOf<String, MutableSet<String>>()

    suspend fun update(id: String, cities: List<String>): DedupResult = mutex.withLock {
        val existing = seenCitiesByAlertId[id]
        val isNewEvent = existing == null
        val tracked = existing ?: mutableSetOf<String>().also { seenCitiesByAlertId[id] = it }
        val newCities = cities.filter { it !in tracked }
        tracked.addAll(cities)
        DedupResult(isNewEvent, newCities)
    }
}
