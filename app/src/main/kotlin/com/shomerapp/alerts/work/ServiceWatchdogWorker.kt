package com.shomerapp.alerts.work

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.shomerapp.alerts.data.local.AppPreferences
import com.shomerapp.alerts.service.AlertForegroundService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * §6 watchdog: every 15 minutes, unconditionally (re)request the service to start.
 * ContextCompat.startForegroundService on an already-running service is a harmless no-op
 * (just another onStartCommand call) — that's simpler and less failure-prone than tracking a
 * separate "is it alive" flag that can go stale.
 */
@HiltWorker
class ServiceWatchdogWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val appPreferences: AppPreferences,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (appPreferences.onboardingCompleted.first()) {
            ContextCompat.startForegroundService(applicationContext, Intent(applicationContext, AlertForegroundService::class.java))
        }
        return Result.success()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "alert_service_watchdog"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ServiceWatchdogWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
