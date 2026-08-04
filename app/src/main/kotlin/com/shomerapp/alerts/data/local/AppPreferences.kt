package com.shomerapp.alerts.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "app_prefs")

/** BootReceiver/the watchdog check [onboardingCompleted] before starting the service — a fresh
 *  install with no areas picked yet has nothing to protect and no notification permission
 *  granted. [selectedSettlements] is the raw (un-normalized) settlement names the user picked in
 *  the Stage 6 area picker, as they appear in areas.json.
 *
 *  [autoLocationEnabled]/[autoDetectedSettlement]: opt-in location-based auto-detection (off by
 *  default). [autoDetectedSettlement] holds only the single most-recently-resolved settlement —
 *  it's overwritten, not accumulated, so it naturally "follows" the user as they move without
 *  ever needing to explicitly remove a stale entry. [SettlementRelevanceFilter] treats it as an
 *  addition to [selectedSettlements], never a replacement — manual picks are never touched. */
@Singleton
class AppPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val SELECTED_SETTLEMENTS = stringSetPreferencesKey("selected_settlements")
        val AUTO_LOCATION_ENABLED = booleanPreferencesKey("auto_location_enabled")
        val AUTO_DETECTED_SETTLEMENT = stringPreferencesKey("auto_detected_settlement")
    }

    val onboardingCompleted: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.ONBOARDING_COMPLETED] ?: false }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    val selectedSettlements: Flow<Set<String>> =
        context.dataStore.data.map { it[Keys.SELECTED_SETTLEMENTS] ?: emptySet() }

    suspend fun setSelectedSettlements(settlements: Set<String>) {
        context.dataStore.edit { it[Keys.SELECTED_SETTLEMENTS] = settlements }
    }

    val autoLocationEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.AUTO_LOCATION_ENABLED] ?: false }

    suspend fun setAutoLocationEnabled(enabled: Boolean) {
        context.dataStore.edit {
            it[Keys.AUTO_LOCATION_ENABLED] = enabled
            if (!enabled) it.remove(Keys.AUTO_DETECTED_SETTLEMENT) // don't keep alerting for a stale location after opting out
        }
    }

    val autoDetectedSettlement: Flow<String?> =
        context.dataStore.data.map { it[Keys.AUTO_DETECTED_SETTLEMENT] }

    suspend fun setAutoDetectedSettlement(settlement: String?) {
        context.dataStore.edit {
            if (settlement == null) it.remove(Keys.AUTO_DETECTED_SETTLEMENT) else it[Keys.AUTO_DETECTED_SETTLEMENT] = settlement
        }
    }
}
