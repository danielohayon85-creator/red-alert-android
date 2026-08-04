package com.shomerapp.alerts.domain

import android.content.Context
import com.shomerapp.alerts.R
import com.shomerapp.alerts.audio.AlarmAudioEngine
import com.shomerapp.alerts.audio.SoundResolver
import com.shomerapp.alerts.data.local.db.AlertHistoryDao
import com.shomerapp.alerts.data.local.db.AlertHistoryEntity
import com.shomerapp.alerts.data.local.db.AlertSessionDao
import com.shomerapp.alerts.data.local.db.AlertSessionEntity
import com.shomerapp.alerts.data.repository.PollOutcome
import com.shomerapp.alerts.domain.model.AlertKind
import com.shomerapp.alerts.domain.model.AlertSessionState
import com.shomerapp.alerts.service.notification.AlertNotifications
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val PREWARNING_EXPIRY_MS = 5 * 60 * 1000L

/**
 * The runtime side-effecting counterpart to [AlertSessionReducer]: owns the live state, persists
 * every transition to Room (§10 crash recovery), schedules the two real-time timers the reducer
 * itself can't own (local duration elapsing, prewarning 5-minute expiry), and triggers audio +
 * the full-screen notification as reactions to state changes rather than mixing that into the
 * pure transition logic.
 */
@Singleton
class AlertSessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioEngine: AlarmAudioEngine,
    private val soundResolver: SoundResolver,
    private val sessionDao: AlertSessionDao,
    private val historyDao: AlertHistoryDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow<AlertSessionState>(AlertSessionState.Idle)
    val state: StateFlow<AlertSessionState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var restored = false

    /** Called once at app/service startup, before any live poll outcomes arrive. */
    suspend fun restoreFromDatabase() {
        if (restored) return
        restored = true
        val saved = sessionDao.get() ?: return
        val restoredState = saved.toState()
        _state.value = restoredState
        rescheduleTimerFor(restoredState)
        // Deliberately does NOT re-trigger audio/notification on restore — a process restart
        // mid-alert should not literally regenerate the announcement/vibration burst, only put
        // the visible countdown/list back where they were. The sound stopping on process death
        // is an accepted gap; see README §5 known simplification.
    }

    /** [isDrill] is true when this outcome came from the Debug Panel / "simulate full alert"
     *  mock fetcher rather than the real oref endpoint — must reach the screen (§8). */
    fun onPollOutcome(outcome: PollOutcome.AlertUpdate, isDrill: Boolean = false) {
        val now = System.currentTimeMillis()
        val event = when (outcome.alert.kind) {
            AlertKind.PREWARNING -> AlertSessionEvent.PrewarningReceived(outcome.alert.cities, outcome.alert.title, outcome.alert.desc, now, isDrill)
            AlertKind.IMMEDIATE -> AlertSessionEvent.ImmediateReceived(outcome.alert.cities, outcome.alert.title, outcome.alert.desc, outcome.alert.durationSeconds, now, isDrill)
            AlertKind.ALL_CLEAR -> AlertSessionEvent.AllClearReceived(outcome.alert.cities, now)
            AlertKind.INFO -> return // plain notification, not part of the full-screen session
        }
        apply(event)
    }

    fun onUserConfirmedSafe() {
        apply(AlertSessionEvent.UserConfirmedSafe)
        audioEngine.stop() // stops sound only — timer/list are untouched by the reducer for this event
    }

    fun onAcknowledged() {
        apply(AlertSessionEvent.Acknowledged)
    }

    private fun apply(event: AlertSessionEvent) {
        val previous = _state.value
        val next = AlertSessionReducer.reduce(previous, event)
        if (next == previous) return
        _state.value = next
        scope.launch { persist(next) }
        rescheduleTimerFor(next)
        triggerSideEffects(previous, next)
    }

    private fun triggerSideEffects(previous: AlertSessionState, next: AlertSessionState) {
        when (next) {
            is AlertSessionState.Prewarning -> if (previous !is AlertSessionState.Prewarning) {
                scope.launch {
                    val uri = soundResolver.resolvePrewarningSoundUri()
                    audioEngine.playPrewarning(uri, context.getString(R.string.alert_tts_prewarning))
                }
                showFullScreenNotification(context.getString(R.string.alert_prewarning_title), next.settlements.joinToString())
            }

            is AlertSessionState.Immediate -> if (!next.acknowledgedByUser && (previous !is AlertSessionState.Immediate || previous.acknowledgedByUser)) {
                val city = next.settlements.lastOrNull().orEmpty()
                val announcement = context.getString(R.string.alert_tts_immediate, next.title, city)
                scope.launch {
                    val uri = soundResolver.resolveImmediateSoundUri()
                    audioEngine.playImmediate(uri, announcement)
                }
                showFullScreenNotification(next.title, next.settlements.joinToString())
            }

            is AlertSessionState.Cleared -> {
                audioEngine.playAllClear()
                AlertNotifications.dismiss(context)
                scope.launch { logHistory(previous) }
            }

            is AlertSessionState.PrewarningExpired -> {
                audioEngine.stop()
                AlertNotifications.dismiss(context)
                scope.launch { logHistory(previous) }
            }

            AlertSessionState.Idle -> AlertNotifications.dismiss(context)

            is AlertSessionState.WaitingForAllClear -> Unit // sound already stopped when the local timer elapsed, see rescheduleTimerFor
        }
    }

    private fun showFullScreenNotification(title: String, cities: String) {
        AlertNotifications.ensureChannel(context)
        AlertNotifications.show(context, title, cities)
    }

    /** Timers the reducer can't own itself since it's pure/synchronous: the Immediate session's
     *  local duration countdown, and the Prewarning 5-minute auto-expiry. Re-armed on every
     *  transition so a duration extension (merging cities with a longer duration) reschedules
     *  correctly instead of firing on the original, now-stale deadline. */
    private fun rescheduleTimerFor(state: AlertSessionState) {
        // Note: when this is called from inside the very timer job it's about to replace (the
        // Immediate/Prewarning branches below call apply(), which calls back into this function),
        // cancel() here is a harmless self-cancel — there's no further suspension point left in
        // that job's body, so it just finishes normally right after this line reassigns the field.
        timerJob?.cancel()
        timerJob = when (state) {
            is AlertSessionState.Immediate -> scope.launch {
                val remaining = (state.startedAtEpochMillis + state.durationSeconds * 1000L) - System.currentTimeMillis()
                if (remaining > 0) delay(remaining)
                audioEngine.stop() // the alarm itself must stop even while only "waiting" starts, per §4.1
                apply(AlertSessionEvent.LocalTimerElapsed)
            }

            is AlertSessionState.Prewarning -> scope.launch {
                val remaining = (state.startedAtEpochMillis + PREWARNING_EXPIRY_MS) - System.currentTimeMillis()
                if (remaining > 0) delay(remaining)
                apply(AlertSessionEvent.PrewarningExpiryElapsed)
            }

            else -> null
        }
    }

    private suspend fun persist(state: AlertSessionState) {
        if (state is AlertSessionState.Idle) {
            sessionDao.clear()
            return
        }
        sessionDao.save(state.toEntity())
    }

    private suspend fun logHistory(concludedState: AlertSessionState) {
        val (kind, title, settlements, startedAt) = when (concludedState) {
            is AlertSessionState.Immediate -> HistoryRow(AlertKind.IMMEDIATE, concludedState.title, concludedState.settlements, concludedState.startedAtEpochMillis)
            is AlertSessionState.WaitingForAllClear -> HistoryRow(AlertKind.IMMEDIATE, concludedState.title, concludedState.settlements, concludedState.startedAtEpochMillis)
            is AlertSessionState.Prewarning -> HistoryRow(AlertKind.PREWARNING, concludedState.title, concludedState.settlements, concludedState.startedAtEpochMillis)
            else -> return
        }
        historyDao.insert(
            AlertHistoryEntity(
                kind = kind.name,
                title = title,
                desc = "",
                settlementsCsv = settlements.joinToString(","),
                startedAtEpochMillis = startedAt,
                concludedAtEpochMillis = System.currentTimeMillis(),
            ),
        )
    }

    private data class HistoryRow(val kind: AlertKind, val title: String, val settlements: List<String>, val startedAt: Long)
}

private fun AlertSessionState.toEntity(): AlertSessionEntity = when (this) {
    is AlertSessionState.Prewarning -> AlertSessionEntity(
        phase = "PREWARNING", startedAtEpochMillis = startedAtEpochMillis, prewarningStartedAtEpochMillis = null,
        settlementsCsv = settlements.joinToString(","), title = title, desc = desc, durationSeconds = 0, acknowledgedByUser = false, isDrill = isDrill,
    )
    is AlertSessionState.Immediate -> AlertSessionEntity(
        phase = "IMMEDIATE", startedAtEpochMillis = startedAtEpochMillis, prewarningStartedAtEpochMillis = prewarningStartedAtEpochMillis,
        settlementsCsv = settlements.joinToString(","), title = title, desc = desc, durationSeconds = durationSeconds, acknowledgedByUser = acknowledgedByUser, isDrill = isDrill,
    )
    is AlertSessionState.WaitingForAllClear -> AlertSessionEntity(
        phase = "WAITING_FOR_ALL_CLEAR", startedAtEpochMillis = startedAtEpochMillis, prewarningStartedAtEpochMillis = null,
        settlementsCsv = settlements.joinToString(","), title = title, desc = "", durationSeconds = 0, acknowledgedByUser = false, isDrill = isDrill,
    )
    is AlertSessionState.Cleared -> AlertSessionEntity(
        phase = "CLEARED", startedAtEpochMillis = 0, prewarningStartedAtEpochMillis = null,
        settlementsCsv = settlements.joinToString(","), title = "", desc = "", durationSeconds = 0, acknowledgedByUser = false, isDrill = isDrill,
    )
    is AlertSessionState.PrewarningExpired -> AlertSessionEntity(
        phase = "PREWARNING_EXPIRED", startedAtEpochMillis = 0, prewarningStartedAtEpochMillis = null,
        settlementsCsv = settlements.joinToString(","), title = "", desc = "", durationSeconds = 0, acknowledgedByUser = false,
    )
    AlertSessionState.Idle -> AlertSessionEntity(
        phase = "IDLE", startedAtEpochMillis = 0, prewarningStartedAtEpochMillis = null,
        settlementsCsv = "", title = "", desc = "", durationSeconds = 0, acknowledgedByUser = false,
    )
}

private fun AlertSessionEntity.toState(): AlertSessionState {
    val settlements = settlementsCsv.split(",").filter { it.isNotBlank() }
    return when (phase) {
        "PREWARNING" -> AlertSessionState.Prewarning(startedAtEpochMillis, settlements, title, desc, isDrill)
        "IMMEDIATE" -> AlertSessionState.Immediate(startedAtEpochMillis, prewarningStartedAtEpochMillis, settlements, title, desc, durationSeconds, acknowledgedByUser, isDrill)
        "WAITING_FOR_ALL_CLEAR" -> AlertSessionState.WaitingForAllClear(startedAtEpochMillis, settlements, title, isDrill)
        "CLEARED" -> AlertSessionState.Cleared(settlements, isDrill)
        "PREWARNING_EXPIRED" -> AlertSessionState.PrewarningExpired(settlements)
        else -> AlertSessionState.Idle
    }
}
