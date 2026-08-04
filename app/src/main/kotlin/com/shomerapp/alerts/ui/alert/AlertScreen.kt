package com.shomerapp.alerts.ui.alert

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shomerapp.alerts.R
import com.shomerapp.alerts.domain.MmSsFormatter
import com.shomerapp.alerts.domain.model.AlertSessionState
import com.shomerapp.alerts.ui.theme.AlertClearGreen
import com.shomerapp.alerts.ui.theme.AlertImmediateRed
import com.shomerapp.alerts.ui.theme.AlertPrewarningAmber
import kotlinx.coroutines.delay

private const val PREWARNING_EXPIRED_AUTO_DISMISS_MS = 6_000L
private const val CLEARED_AUTO_DISMISS_MS = 15_000L

@Composable
fun AlertScreen(uiState: AlertUiState, onConfirmSafe: () -> Unit, onAcknowledge: () -> Unit) {
    // Keyed on the phase (class), not the whole state — a growing settlement list or the
    // per-second countdown tick must update in place, not re-trigger a crossfade every time.
    Crossfade(targetState = uiState.session::class, label = "alert-screen-phase", animationSpec = tween(300)) {
        when (val session = uiState.session) {
            is AlertSessionState.Prewarning -> PrewarningScreen(session, uiState.nowMillis)
            is AlertSessionState.PrewarningExpired -> PrewarningExpiredScreen(session, onAcknowledge)
            is AlertSessionState.Immediate -> ImmediateScreen(session, uiState.nowMillis, onConfirmSafe)
            is AlertSessionState.WaitingForAllClear -> WaitingForAllClearScreen(session)
            is AlertSessionState.Cleared -> ClearedScreen(session, onAcknowledge)
            AlertSessionState.Idle -> Unit // AlertActivity finishes itself when it observes Idle
        }
    }
}

@Composable
private fun FullBleedColumn(background: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(PaddingValues(horizontal = 24.dp, vertical = 40.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        content = content,
    )
}

@Composable
private fun PrewarningScreen(session: AlertSessionState.Prewarning, nowMillis: Long) {
    val elapsedSeconds = ((nowMillis - session.startedAtEpochMillis) / 1000).coerceAtLeast(0)
    FullBleedColumn(AlertPrewarningAmber) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (session.isDrill) DrillBanner()
            Text(
                text = stringResource(R.string.alert_prewarning_title),
                style = MaterialTheme.typography.headlineLarge,
                color = Color.Black,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.alert_prewarning_subtitle),
                style = MaterialTheme.typography.titleLarge,
                color = Color.Black,
                textAlign = TextAlign.Center,
            )
            SettlementList(session.settlements, Color.Black)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.alert_prewarning_elapsed, MmSsFormatter.format(elapsedSeconds)),
                style = MaterialTheme.typography.titleLarge,
                color = Color.Black,
            )
            Text(
                text = stringResource(R.string.alert_prewarning_explain),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PrewarningExpiredScreen(session: AlertSessionState.PrewarningExpired, onAcknowledge: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(PREWARNING_EXPIRED_AUTO_DISMISS_MS)
        onAcknowledge()
    }
    FullBleedColumn(AlertPrewarningAmber) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.alert_prewarning_expired_title),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Black,
                textAlign = TextAlign.Center,
            )
        }
        SettlementList(session.settlements, Color.Black)
    }
}

@Composable
private fun ImmediateScreen(session: AlertSessionState.Immediate, nowMillis: Long, onConfirmSafe: () -> Unit) {
    val elapsedSeconds = ((nowMillis - session.startedAtEpochMillis) / 1000).coerceAtLeast(0)
    val remainingSeconds = (session.durationSeconds - elapsedSeconds).coerceAtLeast(0)
    val progress = if (session.durationSeconds > 0) (elapsedSeconds.toFloat() / session.durationSeconds).coerceIn(0f, 1f) else 0f

    FullBleedColumn(AlertImmediateRed) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (session.isDrill) DrillBanner()
            Text(
                text = session.settlements.lastOrNull().orEmpty(),
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Text(text = session.title, style = MaterialTheme.typography.titleLarge, color = Color.White, textAlign = TextAlign.Center)
            Text(text = session.desc, style = MaterialTheme.typography.bodyLarge, color = Color.White, textAlign = TextAlign.Center)
            if (session.prewarningStartedAtEpochMillis != null) {
                val sincePrewarning = ((nowMillis - session.prewarningStartedAtEpochMillis) / 1000).coerceAtLeast(0)
                Text(
                    text = stringResource(R.string.alert_immediate_prewarning_note, MmSsFormatter.format(sincePrewarning)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
            if (session.settlements.size > 1) {
                SettlementList(session.settlements, Color.White)
            }
        }

        CountdownRing(progress = progress, ringColor = Color.White) {
            Text(text = MmSsFormatter.format(remainingSeconds), style = MaterialTheme.typography.headlineMedium, color = Color.White)
        }

        Button(
            onClick = onConfirmSafe,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = AlertImmediateRed),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.alert_confirm_safe_button), style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun WaitingForAllClearScreen(session: AlertSessionState.WaitingForAllClear) {
    FullBleedColumn(AlertPrewarningAmber) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (session.isDrill) DrillBanner()
            Text(
                text = stringResource(R.string.alert_waiting_for_all_clear_title),
                style = MaterialTheme.typography.headlineLarge,
                color = Color.Black,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.alert_waiting_for_all_clear_text),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black,
                textAlign = TextAlign.Center,
            )
            SettlementList(session.settlements, Color.Black)
        }
        Box(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ClearedScreen(session: AlertSessionState.Cleared, onAcknowledge: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(CLEARED_AUTO_DISMISS_MS)
        onAcknowledge()
    }
    FullBleedColumn(AlertClearGreen) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (session.isDrill) DrillBanner()
            Text(
                text = stringResource(R.string.alert_cleared_title),
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.alert_cleared_text),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            SettlementList(session.settlements, Color.White)
        }
        Button(onClick = onAcknowledge, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = AlertClearGreen)) {
            Text(text = stringResource(R.string.alert_dismiss_button))
        }
    }
}

/** §8: a drill must be unmistakable everywhere it's shown — never rely on subtlety here. */
@Composable
private fun DrillBanner() {
    Text(
        text = stringResource(R.string.drill_disclaimer),
        style = MaterialTheme.typography.titleLarge,
        color = Color.Black,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(8.dp),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun SettlementList(settlements: List<String>, color: Color) {
    Text(
        text = settlements.joinToString(" • "),
        style = MaterialTheme.typography.bodyMedium,
        color = color.copy(alpha = 0.9f),
        textAlign = TextAlign.Center,
    )
}
