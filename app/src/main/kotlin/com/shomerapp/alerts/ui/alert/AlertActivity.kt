package com.shomerapp.alerts.ui.alert

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.shomerapp.alerts.ui.theme.AzakonTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Full-screen alert experience (lock-screen display, FSI, countdown ring, PREWARNING ->
 * IMMEDIATE -> ALL_CLEAR state machine per spec §4.1) is built out in Stage 5. This stub
 * only exists so the manifest-declared activity compiles and launches safely for now.
 */
@AndroidEntryPoint
class AlertActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AzakonTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {}
            }
        }
    }
}
