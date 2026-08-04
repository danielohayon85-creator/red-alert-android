package com.shomerapp.alerts.ui.alert

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** §8: "טיימר ספירה לאחור ויזואלי... טבעת מתמלאת" — the ring fills UP as time elapses toward
 *  the duration (not down), which is why [progress] is elapsed/duration, not remaining/duration. */
@Composable
fun CountdownRing(
    progress: Float,
    ringColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.size(220.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.size(220.dp),
            strokeWidth = 10.dp,
            color = ringColor,
            trackColor = ringColor.copy(alpha = 0.2f),
        )
        content()
    }
}
