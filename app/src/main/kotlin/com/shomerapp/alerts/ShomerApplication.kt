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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltAndroidApp
class ShomerApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var alarmVolumeController: AlarmVolumeController

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        installCrashLogger()
        // enqueueUniquePeriodicWork(..., KEEP, ...) makes this idempotent across process starts —
        // safe to call unconditionally rather than only reacting to BOOT_COMPLETED (§6).
        ServiceWatchdogWorker.schedule(this)

        // §2.2 crash recovery: if the process died mid-alert, the forced alarm volume was never
        // restored — fix it on the very next process start, not just on a clean alert end.
        CoroutineScope(Dispatchers.Default).launch {
            alarmVolumeController.restoreIfPending()
        }
    }

    /** Temporary diagnostic aid (not a crash-reporting SDK — nothing leaves the device): writes
     *  the full stack trace of any uncaught crash to a local file, readable from the Diagnostics
     *  screen ("שגיאת הקריסה האחרונה"). Without this there is no way to see what actually failed
     *  on a real device from outside it. */
    private fun installCrashLogger() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                File(filesDir, CRASH_LOG_FILE_NAME).writeText(
                    "Time: $timestamp\nThread: ${thread.name}\n\n${throwable.stackTraceToString()}",
                )
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        const val CRASH_LOG_FILE_NAME = "last_crash.txt"
    }
}
