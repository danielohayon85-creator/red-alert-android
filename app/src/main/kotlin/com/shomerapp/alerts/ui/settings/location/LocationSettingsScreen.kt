package com.shomerapp.alerts.ui.settings.location

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shomerapp.alerts.R
import com.shomerapp.alerts.ui.components.StatusIndicator
import com.shomerapp.alerts.ui.onboarding.PermissionChecks
import com.shomerapp.alerts.ui.onboarding.rememberPermissionState
import com.shomerapp.alerts.ui.theme.Spacing

/** §README "מיקום אוטומטי": opt-in, off by default, additive to manual area selection — never
 *  replaces it. The explanation below IS the "prominent in-app disclosure" Google Play requires
 *  before requesting ACCESS_BACKGROUND_LOCATION — it must stay visible before that request fires. */
@Composable
fun LocationSettingsScreen(modifier: Modifier = Modifier, viewModel: LocationSettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val enabled by viewModel.autoLocationEnabled.collectAsStateWithLifecycle()
    val currentSettlement by viewModel.autoDetectedSettlement.collectAsStateWithLifecycle()

    val foregroundGranted by rememberPermissionState { PermissionChecks.locationForegroundGranted(context) }
    val backgroundGranted by rememberPermissionState { PermissionChecks.locationBackgroundGranted(context) }

    val foregroundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val backgroundLauncher = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    } else null

    Column(modifier = modifier.fillMaxWidth().padding(Spacing.cardInner), verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
        Text(text = stringResource(R.string.location_title), style = MaterialTheme.typography.titleLarge)

        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(Spacing.cardInner), verticalArrangement = Arrangement.spacedBy(Spacing.rowGap)) {
                Text(text = stringResource(R.string.location_explain), style = MaterialTheme.typography.bodyMedium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = stringResource(R.string.location_toggle_label), style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = enabled,
                        onCheckedChange = { checked ->
                            viewModel.setEnabled(checked)
                            if (checked && !foregroundGranted) {
                                foregroundLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        },
                    )
                }

                if (currentSettlement != null) {
                    Text(
                        text = stringResource(R.string.location_current_settlement, currentSettlement!!),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (enabled) {
                    Text(
                        text = stringResource(R.string.location_none_detected),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (enabled) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(Spacing.cardInner), verticalArrangement = Arrangement.spacedBy(Spacing.rowGap)) {
                    Text(text = stringResource(R.string.location_foreground_label), style = MaterialTheme.typography.bodyLarge)
                    StatusIndicator(
                        active = foregroundGranted,
                        activeLabel = stringResource(R.string.status_active),
                        inactiveLabel = stringResource(R.string.status_inactive),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (!foregroundGranted) {
                        OutlinedButton(onClick = { foregroundLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) {
                            Text(text = stringResource(R.string.location_request_button))
                        }
                    }
                }
            }

            if (foregroundGranted && backgroundLauncher != null) {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(Spacing.cardInner), verticalArrangement = Arrangement.spacedBy(Spacing.rowGap)) {
                        Text(text = stringResource(R.string.location_background_label), style = MaterialTheme.typography.bodyLarge)
                        StatusIndicator(
                            active = backgroundGranted,
                            activeLabel = stringResource(R.string.status_active),
                            inactiveLabel = stringResource(R.string.status_inactive),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (!backgroundGranted) {
                            Text(text = stringResource(R.string.location_background_explain), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedButton(onClick = { backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION) }) {
                                Text(text = stringResource(R.string.location_request_button))
                            }
                        }
                    }
                }
            }
        }
    }
}
