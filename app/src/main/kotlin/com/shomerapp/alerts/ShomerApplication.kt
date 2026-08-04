package com.shomerapp.alerts

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.shomerapp.alerts.work.ServiceWatchdogWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ShomerApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // enqueueUniquePeriodicWork(..., KEEP, ...) makes this idempotent across process starts —
        // safe to call unconditionally rather than only reacting to BOOT_COMPLETED (§6).
        ServiceWatchdogWorker.schedule(this)
    }
}
