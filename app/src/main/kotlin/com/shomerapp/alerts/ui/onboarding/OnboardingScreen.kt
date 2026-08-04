package com.shomerapp.alerts.ui.onboarding

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shomerapp.alerts.R
import com.shomerapp.alerts.domain.OemInstruction
import com.shomerapp.alerts.ui.components.StatusIndicator
import com.shomerapp.alerts.ui.settings.areas.AreaPickerScreen
import com.shomerapp.alerts.ui.settings.sound.SoundSettingsScreen
import com.shomerapp.alerts.ui.settings.sound.SoundSettingsViewModel
import com.shomerapp.alerts.ui.theme.AmberPrimary
import com.shomerapp.alerts.ui.theme.Spacing
import com.shomerapp.alerts.ui.theme.StatusActiveGreen

@Composable
fun OnboardingScreen(modifier: Modifier = Modifier, onCompleted: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val step by viewModel.currentStep.collectAsStateWithLifecycle()

    Crossfade(targetState = step, animationSpec = tween(300), label = "onboarding-step") { current ->
        when (current) {
            OnboardingStep.WELCOME -> WelcomeStep(onNext = viewModel::next)
            OnboardingStep.AREAS -> AreaPickerScreen(onSaved = viewModel::next)
            OnboardingStep.NOTIFICATIONS -> NotificationsStep(onNext = viewModel::next)
            OnboardingStep.DND -> DndStep(onNext = viewModel::next)
            OnboardingStep.FULL_SCREEN_INTENT -> FullScreenIntentStep(onNext = viewModel::next)
            OnboardingStep.BATTERY -> BatteryStep(onNext = viewModel::next)
            OnboardingStep.OEM -> OemStep(instruction = viewModel.oemInstruction, onNext = viewModel::next)
            OnboardingStep.SOUND -> SoundStep(onNext = viewModel::next)
            OnboardingStep.FINISH -> FinishStep(onFinish = { viewModel.finish(); onCompleted() })
        }
    }
}

@Composable
private fun StepScaffold(
    title: String,
    explanation: String? = null,
    content: @Composable ColumnScope.() -> Unit = {},
    nextButton: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.screen),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
            Text(text = title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            explanation?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
            content()
        }
        nextButton?.invoke()
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.screen),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = AmberPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.onboarding_welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(text = stringResource(R.string.onboarding_welcome_disclaimer), style = MaterialTheme.typography.bodyLarge)
        }
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.onboarding_next_button)) }
    }
}

@Composable
private fun NotificationsStep(onNext: () -> Unit) {
    val context = LocalContext.current
    val granted by rememberPermissionState { PermissionChecks.notificationsGranted(context) }
    var attempted by remember { mutableStateOf(false) }
    val requestLauncher = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    } else null

    StepScaffold(
        title = stringResource(R.string.onboarding_notifications_title),
        explanation = stringResource(R.string.onboarding_notifications_explain),
        content = { PermissionStatusRow(granted) },
        nextButton = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.rowGap)) {
                if (!granted) {
                    Button(
                        onClick = {
                            attempted = true
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                requestLauncher?.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.onboarding_open_settings_button)) }
                }
                // Mandatory step (§7: "אסור לדלג") means the app always asks — it does NOT mean
                // trapping the user forever if the OS permission dialog gets permanently denied.
                // "Next" unlocks once granted OR once they've actually gone through the request
                // once (so silently tapping past it isn't possible, but a hard "denied" answer
                // isn't a dead end either).
                Button(onClick = onNext, modifier = Modifier.fillMaxWidth(), enabled = granted || attempted) {
                    Text(stringResource(R.string.onboarding_next_button))
                }
            }
        },
    )
}

@Composable
private fun DndStep(onNext: () -> Unit) {
    val context = LocalContext.current
    val granted by rememberPermissionState { PermissionChecks.dndAccessGranted(context) }

    StepScaffold(
        title = stringResource(R.string.onboarding_dnd_title),
        explanation = stringResource(R.string.onboarding_dnd_explain),
        content = { PermissionStatusRow(granted) },
        nextButton = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.rowGap)) {
                if (!granted) {
                    OutlinedButton(
                        onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.onboarding_open_settings_button)) }
                }
                Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.onboarding_next_button)) }
            }
        },
    )
}

@Composable
private fun FullScreenIntentStep(onNext: () -> Unit) {
    val context = LocalContext.current
    val granted by rememberPermissionState { PermissionChecks.fullScreenIntentGranted(context) }

    StepScaffold(
        title = stringResource(R.string.onboarding_fsi_title),
        explanation = stringResource(R.string.onboarding_fsi_explain),
        content = { PermissionStatusRow(granted) },
        nextButton = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.rowGap)) {
                if (!granted && Build.VERSION.SDK_INT >= 34) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}"))
                            runCatching { context.startActivity(intent) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.onboarding_open_settings_button)) }
                }
                // Not mandatory (§7.1.A: the app must work fully without it) — Next is always enabled.
                Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.onboarding_next_button)) }
            }
        },
    )
}

@Composable
private fun BatteryStep(onNext: () -> Unit) {
    val context = LocalContext.current
    val granted by rememberPermissionState { PermissionChecks.batteryOptimizationExempted(context) }
    var attempted by remember { mutableStateOf(false) }

    StepScaffold(
        title = stringResource(R.string.onboarding_battery_title),
        explanation = stringResource(R.string.onboarding_battery_explain),
        content = { PermissionStatusRow(granted) },
        nextButton = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.rowGap)) {
                if (!granted) {
                    Button(
                        onClick = {
                            attempted = true
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
                            runCatching { context.startActivity(intent) }
                                .onFailure { runCatching { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) } }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.onboarding_open_settings_button)) }
                }
                // See NotificationsStep — mandatory means "always ask," not "trap forever if denied."
                Button(onClick = onNext, modifier = Modifier.fillMaxWidth(), enabled = granted || attempted) {
                    Text(stringResource(R.string.onboarding_next_button))
                }
            }
        },
    )
}

@Composable
private fun OemStep(instruction: OemInstruction?, onNext: () -> Unit) {
    StepScaffold(
        title = stringResource(R.string.onboarding_oem_title, instruction?.manufacturerLabel.orEmpty()),
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.rowGap)) {
                instruction?.steps?.forEach { step ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.iconGap), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = StatusActiveGreen)
                        Text(text = step, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        nextButton = {
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.onboarding_next_button)) }
        },
    )
}

@Composable
private fun SoundStep(onNext: () -> Unit) {
    val viewModel: SoundSettingsViewModel = hiltViewModel()
    val immediateConfirmed by viewModel.immediateSoundConfirmed.collectAsStateWithLifecycle()
    val prewarningConfirmed by viewModel.prewarningSoundConfirmed.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f)) {
            SoundSettingsScreen(viewModel = viewModel)
        }
        // §2.5 "בדיקת חובה לפני הפעלה" — unlike the OS-permission steps, there's no external
        // grant that can be permanently denied here, so this can safely stay a hard gate rather
        // than needing an "attempted" escape hatch.
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().padding(Spacing.cardInner),
            enabled = immediateConfirmed && prewarningConfirmed,
        ) {
            Text(stringResource(R.string.onboarding_next_button))
        }
    }
}

@Composable
private fun FinishStep(onFinish: () -> Unit) {
    StepScaffold(
        title = stringResource(R.string.onboarding_finish_title),
        explanation = stringResource(R.string.disclaimer_short),
        nextButton = {
            Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.onboarding_finish_button)) }
        },
    )
}

@Composable
private fun PermissionStatusRow(granted: Boolean) {
    StatusIndicator(
        active = granted,
        activeLabel = stringResource(R.string.onboarding_permission_granted),
        inactiveLabel = stringResource(R.string.onboarding_permission_denied),
        style = MaterialTheme.typography.titleLarge,
    )
}
