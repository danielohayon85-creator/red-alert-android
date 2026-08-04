package com.shomerapp.alerts.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint

/**
 * Restarts AlertForegroundService after BOOT_COMPLETED / MY_PACKAGE_REPLACED.
 * Implemented in Stage 3 alongside the service it restarts.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Unit
}
