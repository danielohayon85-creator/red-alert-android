package com.shomerapp.alerts.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint

/**
 * Polling, persistent notification, health status and survivability (boot receiver,
 * WorkManager watchdog) are built out in Stage 3. This stub only exists so the
 * manifest-declared specialUse foreground service compiles; nothing starts it yet.
 */
@AndroidEntryPoint
class AlertForegroundService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
