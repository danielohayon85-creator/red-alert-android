package com.shomerapp.alerts.ui.settings.areas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import com.shomerapp.alerts.ui.theme.Spacing
import com.shomerapp.alerts.ui.theme.StatusAdvisoryAmber
import com.shomerapp.alerts.ui.theme.StatusInactiveRed

@Composable
fun AreaPickerScreen(modifier: Modifier = Modifier, onSaved: () -> Unit, viewModel: AreaPickerViewModel = hiltViewModel()) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()

    LaunchedEffect(saved) { if (saved) onSaved() }

    val currentSelection = selected
    if (currentSelection == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(modifier = modifier.fillMaxWidth().padding(Spacing.cardInner), verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
        Text(text = stringResource(R.string.areas_title), style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            label = { Text(stringResource(R.string.areas_search_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Text(text = stringResource(R.string.areas_selected_count, currentSelection.size), style = MaterialTheme.typography.bodyMedium)

        if (currentSelection.size > MANY_SETTLEMENTS_WARNING_THRESHOLD) {
            Text(
                text = stringResource(R.string.areas_many_selected_warning),
                style = MaterialTheme.typography.bodyMedium,
                color = StatusAdvisoryAmber,
            )
        }

        val visibleAreas = viewModel.allAreas.filter { area ->
            viewModel.visibleSettlementsFor(area).isNotEmpty() || viewModel.matchesQuery(area)
        }

        if (visibleAreas.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.areas_no_results),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.rowGap)) {
                for (area in visibleAreas) {
                    val settlementsInArea = viewModel.visibleSettlementsFor(area)

                    item(key = "area_$area") {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleArea(area) },
                        ) {
                            Row(
                                modifier = Modifier.padding(Spacing.cardInner),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.itemGap),
                            ) {
                                Checkbox(checked = viewModel.isAreaFullySelected(area), onCheckedChange = { viewModel.toggleArea(area) })
                                Text(text = area, style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                    items(settlementsInArea, key = { "settlement_$it" }) { settlement ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth().padding(start = 32.dp).clickable { viewModel.toggleSettlement(settlement) },
                        ) {
                            Row(
                                modifier = Modifier.padding(Spacing.cardInner),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.itemGap),
                            ) {
                                Checkbox(checked = settlement in currentSelection, onCheckedChange = { viewModel.toggleSettlement(settlement) })
                                Text(text = settlement, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }

        Button(onClick = viewModel::save, enabled = currentSelection.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.areas_save_button))
        }
        if (currentSelection.isEmpty()) {
            Text(text = stringResource(R.string.areas_none_selected_error), color = StatusInactiveRed, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
