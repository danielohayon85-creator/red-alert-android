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
import androidx.compose.ui.unit.dp
import com.shomerapp.alerts.ui.onboarding.PermissionChecks
import com.shomerapp.alerts.ui.theme.StatusActiveGreen
import com.shomerapp.alerts.ui.theme.StatusInactiveRed

private data class DiagnosticRow(val label: String, val granted: Boolean, val ifDenied: String)

/** §7.1.H: reports honestly what's on/off and what breaks without it — never blocks the app. */
@Composable
fun DiagnosticsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val rows = listOf(
        DiagnosticRow("הרשאת מסך מלא (FSI)", PermissionChecks.fullScreenIntentGranted(context), "המסך לא ייפתח אוטומטית על מסך נעול — אבל הצליל המלא + heads-up עדיין יעבדו"),
        DiagnosticRow("גישה ל\"נא לא להפריע\"", PermissionChecks.dndAccessGranted(context), "הצליל המלא עדיין יעבוד, אבל לא יעקוף את מצב \"נא לא להפריע\""),
        DiagnosticRow("פטור מאופטימיזציית סוללה", PermissionChecks.batteryOptimizationExempted(context), "הכול עובד בטווח קצר, אבל השירות עלול להיהרג אחרי כמה שעות"),
        DiagnosticRow("הרשאת התראות", PermissionChecks.notificationsGranted(context), "אין התראה ויזואלית ואין שירות רקע תקין"),
    )

    Column(modifier = modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = row.label, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = if (row.granted) "✅ פעיל" else "❌ לא פעיל",
                        color = if (row.granted) StatusActiveGreen else StatusInactiveRed,
                    )
                    if (!row.granted) {
                        Text(text = row.ifDenied, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
