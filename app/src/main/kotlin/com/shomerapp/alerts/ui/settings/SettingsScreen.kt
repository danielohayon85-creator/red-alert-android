package com.shomerapp.alerts.ui.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shomerapp.alerts.R
import com.shomerapp.alerts.ui.ads.BannerAdView
import com.shomerapp.alerts.ui.debug.DebugPanelScreen
import com.shomerapp.alerts.ui.settings.areas.AreaPickerScreen
import com.shomerapp.alerts.ui.settings.sound.SoundSettingsScreen
import com.shomerapp.alerts.ui.theme.Spacing

private const val DEBUG_PANEL_TAP_THRESHOLD = 5

private enum class SettingsDestination { MENU, AREAS, SOUND, DIAGNOSTICS, DEBUG }

/** §10: the Debug Panel is reached via 5 taps on the app name/logo — hidden from ordinary use. */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    var destination by remember { mutableStateOf(SettingsDestination.MENU) }
    var logoTapCount by remember { mutableIntStateOf(0) }
    var debugUnlocked by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current

    BackHandler(enabled = destination != SettingsDestination.MENU) { destination = SettingsDestination.MENU }

    Crossfade(targetState = destination, animationSpec = tween(300), label = "settings-destination") { current ->
        when (current) {
            SettingsDestination.MENU -> Column(modifier = modifier.fillMaxSize().padding(Spacing.screen), verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.clickable {
                        logoTapCount++
                        if (logoTapCount >= DEBUG_PANEL_TAP_THRESHOLD) {
                            debugUnlocked = true
                            logoTapCount = 0
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            Toast.makeText(context, context.getString(R.string.debug_panel_unlocked), Toast.LENGTH_SHORT).show()
                        }
                    },
                )

                SettingsMenuItem(stringResource(R.string.settings_areas_item)) { destination = SettingsDestination.AREAS }
                SettingsMenuItem(stringResource(R.string.settings_sound_item)) { destination = SettingsDestination.SOUND }
                SettingsMenuItem(stringResource(R.string.settings_diagnostics_item)) { destination = SettingsDestination.DIAGNOSTICS }
                if (debugUnlocked) {
                    SettingsMenuItem(stringResource(R.string.debug_panel_title)) { destination = SettingsDestination.DEBUG }
                }

                BannerAdView()
            }

            SettingsDestination.AREAS -> WithBackButton(onBack = { destination = SettingsDestination.MENU }) {
                AreaPickerScreen(onSaved = { destination = SettingsDestination.MENU })
            }

            SettingsDestination.SOUND -> WithBackButton(onBack = { destination = SettingsDestination.MENU }) {
                SoundSettingsScreen()
            }

            SettingsDestination.DIAGNOSTICS -> WithBackButton(onBack = { destination = SettingsDestination.MENU }) {
                DiagnosticsScreen()
            }

            SettingsDestination.DEBUG -> WithBackButton(onBack = { destination = SettingsDestination.MENU }) {
                DebugPanelScreen()
            }
        }
    }
}

@Composable
private fun WithBackButton(onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        TextButton(onClick = onBack) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.iconGap)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Text(text = stringResource(R.string.settings_back))
            }
        }
        content()
    }
}

@Composable
private fun SettingsMenuItem(label: String, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Text(text = label, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(Spacing.cardInner))
    }
}
