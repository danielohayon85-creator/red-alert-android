package com.shomerapp.alerts.service

import android.app.NotificationManager
import android.content.Context
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.shomerapp.alerts.data.connectivity.ConnectivityObserver
import com.shomerapp.alerts.data.remote.AlertFetcherSwitch
import com.shomerapp.alerts.data.repository.OrefPollingRepository
import com.shomerapp.alerts.data.repository.PollOutcome
import com.shomerapp.alerts.domain.AlertSessionManager
import com.shomerapp.alerts.domain.ServiceHealthTracker
import com.shomerapp.alerts.domain.SettlementRelevanceFilter
import com.shomerapp.alerts.service.notification.ServiceNotifications
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Owns the always-on polling loop's lifecycle: persistent low-importance notification, wake
 * lock, connectivity-aware start/stop of [OrefPollingRepository.pollLoop], and health status
 * updates. What happens to an actual incoming alert (sound, full-screen UI) is Stage 4/5's job —
 * this stage only wires the *reliability* half of the spec (§6).
 */
@AndroidEntryPoint
class AlertForegroundService : LifecycleService() {

    @Inject lateinit var pollingRepository: OrefPollingRepository
    @Inject lateinit var connectivityObserver: ConnectivityObserver
    @Inject lateinit var healthTracker: ServiceHealthTracker
    @Inject lateinit var sessionManager: AlertSessionManager
    @Inject lateinit var relevanceFilter: SettlementRelevanceFilter
    @Inject lateinit var fetcherSwitch: AlertFetcherSwitch

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        ServiceNotifications.ensureChannels(this)
        startForeground(ServiceNotifications.STATUS_NOTIFICATION_ID, ServiceNotifications.buildStatusNotification(this, null))
        acquireWakeLock()
        lifecycleScope.launch { sessionManager.restoreFromDatabase() } // §10 crash-recovery
        observeConnectivityAndPoll()
    }

    override fun onDestroy() {
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }

    /** §6: stop polling entirely while there's no network, resume at full pace the instant it's
     *  back — collectLatest cancels the in-flight pollLoop() collection on every connectivity
     *  change and starts a fresh one, which also resets backoff to the base interval. */
    private fun observeConnectivityAndPoll() {
        lifecycleScope.launch {
            connectivityObserver.isConnected.collectLatest { connected ->
                if (!connected) {
                    healthTracker.markNoNetwork()
                    updateStatusNotification()
                    return@collectLatest
                }
                healthTracker.markConnecting()
                pollingRepository.pollLoop().collect { outcome -> handlePollOutcome(outcome) }
            }
        }
    }

    private suspend fun handlePollOutcome(outcome: PollOutcome) {
        // Renews the bounded wake lock on every tick instead of holding it unboundedly (§6):
        // WakeLock.acquire(timeout) resets the timer when called again while already held, so as
        // long as polling keeps ticking the lock never lapses, but a stuck/dead poll loop lets it
        // expire on its own after WAKE_LOCK_TIMEOUT_MS rather than draining the battery forever.
        wakeLock?.acquire(WAKE_LOCK_TIMEOUT_MS)
        when (outcome) {
            is PollOutcome.Empty, is PollOutcome.NoChange -> healthTracker.onPollSuccess()
            is PollOutcome.AlertUpdate -> {
                healthTracker.onPollSuccess()
                // Snapshotted here, not inside the filter/session manager — this is the moment
                // the outcome was actually fetched via the mock source, which is what "is this a
                // drill" needs to reflect (§8: a drill must always be visibly marked as one).
                val isDrill = fetcherSwitch.mockModeEnabled.value
                relevanceFilter.filterRelevant(outcome)?.let { sessionManager.onPollOutcome(it, isDrill) }
            }
            is PollOutcome.NetworkError, is PollOutcome.MalformedResponse -> {
                if (healthTracker.onPollFailure() && ServiceNotifications.hasNotificationPermission(this)) {
                    NotificationManagerCompat.from(this).notify(
                        ServiceNotifications.CONNECTION_LOST_NOTIFICATION_ID,
                        ServiceNotifications.buildConnectionLostNotification(this),
                    )
                }
            }
        }
        updateStatusNotification()
    }

    private fun updateStatusNotification() {
        if (!ServiceNotifications.hasNotificationPermission(this)) return
        val health = healthTracker.health.value
        val elapsedSeconds = health.lastUpdateEpochMillis?.let { (System.currentTimeMillis() - it) / 1000 }
        val notification = ServiceNotifications.buildStatusNotification(this, elapsedSeconds)
        getSystemService(NotificationManager::class.java).notify(ServiceNotifications.STATUS_NOTIFICATION_ID, notification)
    }

    /** Bounded + periodically renewed rather than held indefinitely (§6: "לא WakeLock אינסופי
     *  בלי הצדקה") — if the service is ever killed without onDestroy running, the lock still
     *  auto-expires instead of draining the battery forever. */
    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:alert-polling").apply {
            setReferenceCounted(false)
            acquire(WAKE_LOCK_TIMEOUT_MS)
        }
    }

    private companion object {
        const val WAKE_LOCK_TIMEOUT_MS = 15 * 60 * 1000L // renewed on every poll tick; only lapses if polling itself has stopped ticking
    }
}
