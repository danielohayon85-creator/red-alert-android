package com.shomerapp.alerts.ui.settings.sound

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shomerapp.alerts.R
import com.shomerapp.alerts.ui.theme.StatusActiveGreen
import com.shomerapp.alerts.ui.theme.StatusInactiveRed

@Composable
fun SoundSettingsScreen(modifier: Modifier = Modifier, viewModel: SoundSettingsViewModel = hiltViewModel()) {
    val immediateUri by viewModel.immediateSoundUri.collectAsStateWithLifecycle()
    val immediateConfirmed by viewModel.immediateSoundConfirmed.collectAsStateWithLifecycle()
    val prewarningUri by viewModel.prewarningSoundUri.collectAsStateWithLifecycle()
    val prewarningConfirmed by viewModel.prewarningSoundConfirmed.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SoundPickerCard(
            title = stringResource(R.string.sound_immediate_title),
            currentUri = immediateUri,
            confirmed = immediateConfirmed,
            onPicked = viewModel::onImmediateSoundPicked,
            onConfirmed = viewModel::confirmImmediateTested,
            onPreview = viewModel::previewSound,
            onStopPreview = viewModel::stopPreview,
        )
        SoundPickerCard(
            title = stringResource(R.string.sound_prewarning_title),
            currentUri = prewarningUri,
            confirmed = prewarningConfirmed,
            onPicked = viewModel::onPrewarningSoundPicked,
            onConfirmed = viewModel::confirmPrewarningTested,
            onPreview = viewModel::previewSound,
            onStopPreview = viewModel::stopPreview,
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(onClick = viewModel::simulateFullAlert, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.sound_simulate_button))
            }
            Text(
                text = stringResource(R.string.sound_simulate_disclaimer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SoundPickerCard(
    title: String,
    currentUri: String?,
    confirmed: Boolean,
    onPicked: (Uri) -> Unit,
    onConfirmed: () -> Unit,
    onPreview: (Uri) -> Unit,
    onStopPreview: () -> Unit,
) {
    val context = LocalContext.current

    val ringtoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        uri?.let(onPicked)
    }
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            // Required or the URI dies after a restart (§5) — ringtone picker URIs don't need
            // (or support) this, only SAF-obtained ones do.
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            onPicked(it)
        }
    }

    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Text(
                text = currentUri?.substringAfterLast('/') ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_NOTIFICATION)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                    }
                    ringtoneLauncher.launch(intent)
                }) {
                    Text(text = stringResource(R.string.sound_pick_system))
                }
                OutlinedButton(onClick = { fileLauncher.launch(arrayOf("audio/*")) }) {
                    Text(text = stringResource(R.string.sound_pick_file))
                }
            }

            if (currentUri != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onPreview(Uri.parse(currentUri)) }) {
                        Text(text = stringResource(R.string.sound_test_button))
                    }
                    OutlinedButton(onClick = onStopPreview) {
                        Text(text = stringResource(R.string.sound_stop_button))
                    }
                }

                if (confirmed) {
                    Text(text = stringResource(R.string.sound_tested_confirmation), color = StatusActiveGreen)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.sound_not_tested_warning),
                            color = StatusInactiveRed,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Button(onClick = onConfirmed, modifier = Modifier.fillMaxWidth()) {
                            Text(text = stringResource(R.string.sound_confirm_button))
                        }
                    }
                }
            }
        }
    }
}
