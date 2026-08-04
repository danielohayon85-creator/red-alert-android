package com.shomerapp.alerts.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shomerapp.alerts.R
import com.shomerapp.alerts.ui.components.StatusIndicator
import com.shomerapp.alerts.ui.onboarding.PermissionChecks
import com.shomerapp.alerts.ui.theme.Spacing

private data class DiagnosticRow(val labelRes: Int, val granted: Boolean, val ifDeniedRes: Int)

/** §7.1.H: reports honestly what's on/off and what breaks without it — never blocks the app. */
@Composable
fun DiagnosticsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val rows = listOf(
        DiagnosticRow(R.string.diagnostics_fsi_label, PermissionChecks.fullScreenIntentGranted(context), R.string.diagnostics_fsi_denied),
        DiagnosticRow(R.string.diagnostics_dnd_label, PermissionChecks.dndAccessGranted(context), R.string.diagnostics_dnd_denied),
        DiagnosticRow(R.string.diagnostics_battery_label, PermissionChecks.batteryOptimizationExempted(context), R.string.diagnostics_battery_denied),
        DiagnosticRow(R.string.diagnostics_notifications_label, PermissionChecks.notificationsGranted(context), R.string.diagnostics_notifications_denied),
    )

    Column(modifier = modifier.fillMaxWidth().padding(Spacing.cardInner), verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
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
