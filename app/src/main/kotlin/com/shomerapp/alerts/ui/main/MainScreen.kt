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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shomerapp.alerts.R
import com.shomerapp.alerts.ui.ads.BannerAdView
import com.shomerapp.alerts.ui.theme.StatusConnectingAmber

/**
 * Placeholder for the "האם אני מוגן?" status card (§8). Real health status wiring
 * (polling state, last-update timer, action-needed card) lands in Stage 2/3.
 */
@Composable
fun MainScreen(modifier: Modifier = Modifier) {
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
                Text(
                    text = stringResource(R.string.main_status_connecting),
                    style = MaterialTheme.typography.displayLarge,
                    color = StatusConnectingAmber,
                )
            }
        }

        BannerAdView(modifier = Modifier.padding(top = 8.dp))
    }
}
