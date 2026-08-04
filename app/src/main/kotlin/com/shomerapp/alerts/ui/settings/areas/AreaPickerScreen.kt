package com.shomerapp.alerts.ui.settings.areas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shomerapp.alerts.R
import com.shomerapp.alerts.ui.theme.StatusInactiveRed

@Composable
fun AreaPickerScreen(modifier: Modifier = Modifier, onSaved: () -> Unit, viewModel: AreaPickerViewModel = hiltViewModel()) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()

    LaunchedEffect(saved) { if (saved) onSaved() }

    Column(modifier = modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = stringResource(R.string.areas_title), style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            label = { Text(stringResource(R.string.areas_search_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Text(text = stringResource(R.string.areas_selected_count, selected.size), style = MaterialTheme.typography.bodyMedium)

        if (selected.size > MANY_SETTLEMENTS_WARNING_THRESHOLD) {
            Text(
                text = stringResource(R.string.areas_many_selected_warning),
                style = MaterialTheme.typography.bodyMedium,
                color = StatusInactiveRed,
            )
        }

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (area in viewModel.allAreas) {
                val settlementsInArea = viewModel.settlementsFor(area).filter { viewModel.matchesQuery(it) }
                if (settlementsInArea.isEmpty() && !viewModel.matchesQuery(area)) continue

                item(key = "area_$area") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = viewModel.isAreaFullySelected(area), onCheckedChange = { viewModel.toggleArea(area) })
                        Text(text = area, style = MaterialTheme.typography.titleLarge)
                    }
                }
                items(settlementsInArea, key = { "settlement_$it" }) { settlement ->
                    Row(modifier = Modifier.padding(start = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = settlement in selected, onCheckedChange = { viewModel.toggleSettlement(settlement) })
                        Text(text = settlement, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        Button(onClick = viewModel::save, enabled = selected.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.areas_save_button))
        }
        if (selected.isEmpty()) {
            Text(text = stringResource(R.string.areas_none_selected_error), color = StatusInactiveRed, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
