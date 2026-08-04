package com.shomerapp.alerts.ui.onboarding

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shomerapp.alerts.R
import com.shomerapp.alerts.domain.OemInstruction
import com.shomerapp.alerts.ui.components.StatusIndicator
import com.shomerapp.alerts.ui.settings.areas.AreaPickerScreen
import com.shomerapp.alerts.ui.settings.sound.SoundSettingsScreen
import com.shomerapp.alerts.ui.settings.sound.SoundSettingsViewModel
import com.shomerapp.alerts.ui.theme.AmberPrimary
import com.shomerapp.alerts.ui.theme.BackgroundDark
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
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit = {},
    nextButton: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding() // enableEdgeToEdge() draws content under system bars — without
            // this, the bottom button can crowd or sit under the gesture/nav bar (real bug, seen
            // on device).
            .padding(Spacing.screen),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
            Text(text = title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            explanation?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
            content()
        }
        // weight(1f) both pins the button to the bottom (like the old SpaceBetween did) AND gives
        // a short step (e.g. just a title + one line + status) a real place to put an icon instead
        // of a large dead gap between the content and the button.
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(104.dp),
                    tint = AmberPrimary.copy(alpha = 0.3f),
                )
            }
        }
        nextButton?.invoke()
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    // "Pop in" entrance for the icon/name, matching the native splash's scale+fade icon animation
    // (ic_splash_animated.xml) so the transition from splash into this screen feels continuous
    // rather than two unrelated static frames.
    val intro = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        intro.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(Spacing.screen),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.itemGap),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Spacer(Modifier.height(Spacing.itemGap))
            WelcomeBadgeIcon(
                modifier = Modifier
                    .size(88.dp)
                    .scale(0.5f + 0.5f * intro.value)
                    .alpha(intro.value),
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = AmberPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().alpha(intro.value),
            )
            Text(
                text = stringResource(R.string.onboarding_welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().alpha(intro.value),
            )
            Text(
                text = stringResource(R.string.onboarding_welcome_disclaimer),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.alpha(intro.value),
            )
        }
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.onboarding_next_button)) }
    }
}

/** Same badge design as the app/splash icon (amber circle + exclamation + sound-wave arcs),
 *  redrawn via Canvas instead of loaded as a drawable resource so [WelcomeStep] can scale/fade it
 *  as part of the entrance animation above. */
@Composable
private fun WelcomeBadgeIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val unit = size.width / 108f

        drawCircle(color = AmberPrimary, radius = 22f * unit, center = Offset(cx, cy))
        drawRect(
            color = BackgroundDark,
            topLeft = Offset(cx - 3.2f * unit, cy - 14f * unit),
            size = Size(6.4f * unit, 17f * unit),
        )
        drawCircle(color = BackgroundDark, radius = 3.4f * unit, center = Offset(cx, cy + 9f * unit))

        val arcSize = Size(60f * unit, 60f * unit)
        val arcStroke = Stroke(width = 5f * unit, cap = StrokeCap.Round)
        drawArc(
            color = AmberPrimary,
            startAngle = -30f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = Offset(cx - 30f * unit, cy - 30f * unit),
            size = arcSize,
            style = arcStroke,
        )
        drawArc(
            color = AmberPrimary,
            startAngle = 150f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = Offset(cx - 30f * unit, cy - 30f * unit),
            size = arcSize,
            style = arcStroke,
        )
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
        icon = Icons.Filled.Notifications,
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
        icon = Icons.Filled.DoNotDisturbOn,
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
        icon = Icons.Filled.Fullscreen,
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
        icon = Icons.Filled.BatteryChargingFull,
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

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
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
        icon = Icons.Filled.VerifiedUser,
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
