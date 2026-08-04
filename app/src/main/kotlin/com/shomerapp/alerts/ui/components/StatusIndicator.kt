package com.shomerapp.alerts.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.shomerapp.alerts.ui.theme.Spacing
import com.shomerapp.alerts.ui.theme.StatusActiveGreen
import com.shomerapp.alerts.ui.theme.StatusInactiveRed

/** App-wide granted/denied status row — icon instead of raw emoji, works regardless of the device's emoji font. */
@Composable
fun StatusIndicator(
    active: Boolean,
    activeLabel: String,
    inactiveLabel: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val color = if (active) StatusActiveGreen else StatusInactiveRed
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.iconGap),
    ) {
        Icon(
            imageVector = if (active) Icons.Filled.Check else Icons.Filled.Close,
            contentDescription = null,
            tint = color,
        )
        Text(text = if (active) activeLabel else inactiveLabel, color = color, style = style)
    }
}
