package com.shomerapp.alerts.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.shomerapp.alerts.data.local.AppPreferences
import com.shomerapp.alerts.service.AlertForegroundService
import com.shomerapp.alerts.work.ServiceWatchdogWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * §6 survivability: restarts the service after BOOT_COMPLETED / MY_PACKAGE_REPLACED, and
 * (re)schedules the watchdog. Only starts the service if onboarding actually finished — a fresh
 * install with no areas picked yet has nothing to protect and no notification permission granted.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var appPreferences: AppPreferences

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        ServiceWatchdogWorker.schedule(context)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                if (appPreferences.onboardingCompleted.first()) {
                    ContextCompat.startForegroundService(context, Intent(context, AlertForegroundService::class.java))
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
