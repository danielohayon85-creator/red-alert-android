package com.shomerapp.alerts.domain

import com.shomerapp.alerts.data.areas.AreaRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maps free-text place names (from Android's Geocoder — locality/subLocality/subAdminArea, in
 * that preference order) to a real settlement name from areas.json. Pure/no Android dependency
 * beyond [AreaRepository] itself, so it's directly unit-testable without a device/emulator.
 *
 * Geocoder spelling rarely matches areas.json exactly (transliteration, hyphenation, "אזור
 * תעשייה" prefixes) — same normalization gap [AreaRepository.normalize] already exists to bridge
 * for oref's own `data` field (§4).
 */
@Singleton
class LocationSettlementMatcher @Inject constructor(private val areaRepository: AreaRepository) {

    fun bestMatch(candidates: List<String>): String? {
        val allSettlements = areaRepository.allSettlements()
        val normalizedCandidates = candidates.filter { it.isNotBlank() }.map(areaRepository::normalize).filter { it.isNotBlank() }
        if (normalizedCandidates.isEmpty()) return null

        // Exact normalized match first (across all candidates), then substring containment —
        // preferring a precise hit over a loose one even if the loose one came from an earlier,
        // higher-priority candidate field.
        for (normalized in normalizedCandidates) {
            val exact = allSettlements.firstOrNull { areaRepository.normalize(it) == normalized }
            if (exact != null) return exact
        }
        for (normalized in normalizedCandidates) {
            val partial = allSettlements.firstOrNull { settlement ->
                val n = areaRepository.normalize(settlement)
                n.contains(normalized) || normalized.contains(n)
            }
            if (partial != null) return partial
        }
        return null
    }
}
