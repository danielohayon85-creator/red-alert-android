package com.shomerapp.alerts.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single [AlertFetcher] bound in DI — delegates to the real endpoint or the mock source
 * based on a runtime-flippable flag, so the Stage 6 Debug Panel can toggle mock mode without
 * any repository/service code needing to know about it.
 */
@Singleton
class AlertFetcherSwitch @Inject constructor(
    private val real: OrefAlertFetcher,
    val mock: MockAlertFetcher,
) : AlertFetcher {
    private val _mockModeEnabled = MutableStateFlow(false)
    val mockModeEnabled: StateFlow<Boolean> = _mockModeEnabled.asStateFlow()

    fun setMockMode(enabled: Boolean) {
        _mockModeEnabled.value = enabled
    }

    override suspend fun fetchRaw(): String? =
        if (_mockModeEnabled.value) mock.fetchRaw() else real.fetchRaw()
}
