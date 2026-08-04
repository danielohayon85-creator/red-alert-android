package com.shomerapp.alerts.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.shomerapp.alerts.R
import com.shomerapp.alerts.domain.model.AlertKind
import com.shomerapp.alerts.ui.theme.Spacing

/**
 * Core of §10's Debug Panel: fake-alert injection, running through the real pipeline via
 * [com.shomerapp.alerts.domain.AlertSimulator]. Network-failure simulation, a live request log,
 * and JSON Replay mode are NOT implemented here — scope-trimmed given the size of the rest of the
 * spec; see README.md "שלב 6" for what's left.
 */
@Composable
private fun AlertKind.hebrewLabel(): String = stringResource(
    when (this) {
        AlertKind.IMMEDIATE -> R.string.debug_kind_immediate
        AlertKind.PREWARNING -> R.string.debug_kind_prewarning
        AlertKind.ALL_CLEAR -> R.string.debug_kind_all_clear
        AlertKind.INFO -> R.string.debug_kind_info
    },
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DebugPanelScreen(modifier: Modifier = Modifier, viewModel: DebugPanelViewModel = hiltViewModel()) {
    var kind by remember { mutableStateOf(AlertKind.IMMEDIATE) }
    var settlement by remember { mutableStateOf("") }
    var durationMinutes by remember { mutableStateOf("1") }

    Column(modifier = modifier.fillMaxWidth().padding(Spacing.cardInner), verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
        Text(text = stringResource(R.string.debug_panel_title), style = MaterialTheme.typography.titleLarge)

        Text(text = stringResource(R.string.debug_kind_label), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.rowGap)) {
            AlertKind.entries.forEach { candidate ->
                FilterChip(selected = kind == candidate, onClick = { kind = candidate }, label = { Text(candidate.hebrewLabel()) })
            }
        }

        OutlinedTextField(
            value = settlement,
            onValueChange = { settlement = it },
            label = { Text(stringResource(R.string.debug_settlement_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        OutlinedTextField(
            value = durationMinutes,
            onValueChange = { durationMinutes = it.filter(Char::isDigit) },
            label = { Text(stringResource(R.string.debug_duration_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Button(
            onClick = { viewModel.injectAlert(kind, settlement.trim(), durationMinutes.toIntOrNull() ?: 1) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.debug_inject_alert))
        }
    }
}
