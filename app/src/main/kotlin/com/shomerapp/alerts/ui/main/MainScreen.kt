package com.shomerapp.alerts.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shomerapp.alerts.R
import com.shomerapp.alerts.domain.ElapsedTimeFormatter
import com.shomerapp.alerts.domain.model.HealthStatus
import com.shomerapp.alerts.ui.ads.BannerAdView
import com.shomerapp.alerts.ui.theme.StatusActiveGreen
import com.shomerapp.alerts.ui.theme.StatusConnectingAmber
import com.shomerapp.alerts.ui.theme.StatusInactiveRed

@Composable
fun MainScreen(modifier: Modifier = Modifier, viewModel: MainViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.main_status_title), style = MaterialTheme.typography.titleLarge)

                val health = uiState.health
                val (statusLabelRes, statusColor) = when (health.status) {
                    HealthStatus.ACTIVE -> R.string.main_status_active to StatusActiveGreen
                    HealthStatus.CONNECTING -> R.string.main_status_connecting to StatusConnectingAmber
                    HealthStatus.DISCONNECTED -> R.string.main_status_inactive to StatusInactiveRed
                }
                Text(
                    text = stringResource(statusLabelRes),
                    style = MaterialTheme.typography.displayLarge,
                    color = statusColor,
                )

                uiState.elapsedSeconds?.let { seconds ->
                    Text(
                        text = stringResource(R.string.main_last_update, ElapsedTimeFormatter.format(seconds)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        BannerAdView(modifier = Modifier.padding(top = 8.dp))
    }
}
