package com.shomerapp.alerts.domain

import com.shomerapp.alerts.data.areas.AreaRepository
import com.shomerapp.alerts.data.local.AppPreferences
import com.shomerapp.alerts.data.repository.PollOutcome
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Closes the Stage 5 TODO: narrows an incoming alert down to only the settlements the user
 * actually selected, so [AlertSessionManager] never has to know about settlements the user
 * doesn't care about. Applied before every poll outcome reaches the session manager.
 */
@Singleton
class SettlementRelevanceFilter @Inject constructor(
    private val appPreferences: AppPreferences,
    private val areaRepository: AreaRepository,
) {
    suspend fun filterRelevant(outcome: PollOutcome.AlertUpdate): PollOutcome.AlertUpdate? {
        val selected = appPreferences.selectedSettlements.first()
        // Defensive: onboarding requires picking at least one settlement, but if this ever runs
        // before that's true (e.g. Debug Panel injection pre-onboarding), don't silently eat
        // every alert — treat "nothing configured" as "everything is relevant" instead.
        if (selected.isEmpty()) return outcome

        val normalizedSelected = selected.map(areaRepository::normalize).toSet()
        val relevantCities = outcome.alert.cities.filter { areaRepository.normalize(it) in normalizedSelected }
        if (relevantCities.isEmpty()) return null

        val relevantNewCities = outcome.newCities.filter { areaRepository.normalize(it) in normalizedSelected }
        if (relevantNewCities.isEmpty()) return null

        return outcome.copy(alert = outcome.alert.copy(cities = relevantCities), newCities = relevantNewCities)
    }
}
