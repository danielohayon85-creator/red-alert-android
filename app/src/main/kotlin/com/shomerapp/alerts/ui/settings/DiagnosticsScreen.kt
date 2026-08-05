package com.shomerapp.alerts.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.shomerapp.alerts.R
import com.shomerapp.alerts.ShomerApplication
import com.shomerapp.alerts.ui.components.StatusIndicator
import com.shomerapp.alerts.ui.onboarding.PermissionChecks
import com.shomerapp.alerts.ui.theme.Spacing
import com.shomerapp.alerts.ui.theme.StatusInactiveRed
import java.io.File

private data class DiagnosticRow(val labelRes: Int, val granted: Boolean, val ifDeniedRes: Int)

private fun readCrashLog(filesDir: File): String? =
    File(filesDir, ShomerApplication.CRASH_LOG_FILE_NAME).takeIf { it.exists() }?.readText()

/** §7.1.H: reports honestly what's on/off and what breaks without it — never blocks the app. */
@Composable
fun DiagnosticsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var crashLog by remember { mutableStateOf(readCrashLog(context.filesDir)) }
    val rows = listOf(
        DiagnosticRow(R.string.diagnostics_fsi_label, PermissionChecks.fullScreenIntentGranted(context), R.string.diagnostics_fsi_denied),
        DiagnosticRow(R.string.diagnostics_dnd_label, PermissionChecks.dndAccessGranted(context), R.string.diagnostics_dnd_denied),
        DiagnosticRow(R.string.diagnostics_battery_label, PermissionChecks.batteryOptimizationExempted(context), R.string.diagnostics_battery_denied),
        DiagnosticRow(R.string.diagnostics_notifications_label, PermissionChecks.notificationsGranted(context), R.string.diagnostics_notifications_denied),
    )

    Column(modifier = modifier.fillMaxWidth().padding(Spacing.cardInner), verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
        crashLog?.let { log ->
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(Spacing.cardInner), verticalArrangement = Arrangement.spacedBy(Spacing.rowGap)) {
                    Text(text = stringResource(R.string.diagnostics_crash_log_title), style = MaterialTheme.typography.titleLarge, color = StatusInactiveRed)
                    Text(
                        text = log.take(1200),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.rowGap)) {
                        Button(onClick = { clipboard.setText(AnnotatedString(log)) }) {
                            Text(text = stringResource(R.string.diagnostics_crash_copy_button))
                        }
                        OutlinedButton(onClick = {
                            File(context.filesDir, ShomerApplication.CRASH_LOG_FILE_NAME).delete()
                            crashLog = null
                        }) {
                            Text(text = stringResource(R.string.diagnostics_crash_clear_button))
                        }
                    }
                }
            }
        }
        rows.forEach { row ->
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(Spacing.cardInner), verticalArrangement = Arrangement.spacedBy(Spacing.rowGap)) {
                    Text(text = stringResource(row.labelRes), style = MaterialTheme.typography.titleLarge)
                    StatusIndicator(
                        active = row.granted,
                        activeLabel = stringResource(R.string.status_active),
                        inactiveLabel = stringResource(R.string.status_inactive),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (!row.granted) {
                        Text(text = stringResource(row.ifDeniedRes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
