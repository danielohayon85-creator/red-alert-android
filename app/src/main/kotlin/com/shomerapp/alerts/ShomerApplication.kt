package com.shomerapp.alerts

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.shomerapp.alerts.audio.AlarmVolumeController
import com.shomerapp.alerts.work.ServiceWatchdogWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ShomerApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var alarmVolumeController: AlarmVolumeController

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // enqueueUniquePeriodicWork(..., KEEP, ...) makes this idempotent across process starts —
        // safe to call unconditionally rather than only reacting to BOOT_COMPLETED (§6).
        ServiceWatchdogWorker.schedule(this)

        // §2.2 crash recovery: if the process died mid-alert, the forced alarm volume was never
        // restored — fix it on the very next process start, not just on a clean alert end.
        CoroutineScope(Dispatchers.Default).launch {
            alarmVolumeController.restoreIfPending()
        }
    }
}
