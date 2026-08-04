package com.shomerapp.alerts.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shomerapp.alerts.R
import com.shomerapp.alerts.data.local.db.AlertHistoryEntity
import com.shomerapp.alerts.ui.ads.BannerAdView
import com.shomerapp.alerts.ui.theme.Spacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(modifier: Modifier = Modifier, viewModel: HistoryViewModel = hiltViewModel()) {
    val history by viewModel.history.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        if (history.isEmpty()) {
            // weight(1f), not fillMaxSize() — fillMaxSize() here claimed all remaining height and
            // pushed BannerAdView fully off-screen, since this Column has no other flexible child
            // to share space with in the empty state (real bug, not just polish).
            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(Spacing.screen), contentAlignment = Alignment.Center) {
                Text(text = stringResource(R.string.history_placeholder), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = Spacing.screen, vertical = Spacing.itemGap),
                verticalArrangement = Arrangement.spacedBy(Spacing.itemGap),
            ) {
                items(history, key = { it.id }) { entry -> HistoryRow(entry) }
            }
        }
        BannerAdView(modifier = Modifier.padding(Spacing.screen))
    }
}

private val timeFormatter = DateTimeFormatter.ofPattern("dd/MM HH:mm")

@Composable
private fun HistoryRow(entry: AlertHistoryEntity) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(Spacing.cardInner), verticalArrangement = Arrangement.spacedBy(Spacing.iconGap)) {
            Text(text = entry.title, style = MaterialTheme.typography.titleLarge)
            Text(
                text = entry.settlementsCsv.replace(",", " • "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val time = Instant.ofEpochMilli(entry.startedAtEpochMillis).atZone(ZoneId.systemDefault()).format(timeFormatter)
            Text(text = time, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
