package com.shomerapp.alerts.service.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.shomerapp.alerts.MainActivity
import com.shomerapp.alerts.R
import com.shomerapp.alerts.domain.ElapsedTimeFormatter

/**
 * Two channels, deliberately separate from each other AND from the alert-sound notification
 * channel that Stage 4/5 will add: this file's channels are ordinary system notifications (a
 * silent persistent status and a loud connectivity warning), never the life-safety alert sound
 * itself — that pipeline is manual MediaPlayer per the original prompt's §5, not
 * NotificationChannel.setSound().
 */
object ServiceNotifications {
    const val STATUS_CHANNEL_ID = "service_status"
    const val CONNECTION_LOST_CHANNEL_ID = "connection_lost"
    const val STATUS_NOTIFICATION_ID = 1001
    const val CONNECTION_LOST_NOTIFICATION_ID = 1002

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(
            NotificationChannel(
                STATUS_CHANNEL_ID,
                context.getString(R.string.notification_channel_status_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.notification_channel_status_description)
                setShowBadge(false)
            },
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CONNECTION_LOST_CHANNEL_ID,
                context.getString(R.string.notification_channel_connection_lost_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notification_channel_connection_lost_description)
            },
        )
    }

    fun buildStatusNotification(context: Context, elapsedSecondsSinceUpdate: Long?): Notification {
        val contentText = if (elapsedSecondsSinceUpdate != null) {
            context.getString(R.string.main_last_update, ElapsedTimeFormatter.format(elapsedSecondsSinceUpdate))
        } else {
            context.getString(R.string.main_status_connecting)
        }

        return NotificationCompat.Builder(context, STATUS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_status_title))
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppPendingIntent(context))
            .build()
    }

    fun buildConnectionLostNotification(context: Context): Notification =
        NotificationCompat.Builder(context, CONNECTION_LOST_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_connection_lost_title))
            .setContentText(context.getString(R.string.notification_connection_lost_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent(context))
            .build()

    /** POST_NOTIFICATIONS is a runtime permission from API 33 — without it, notify() calls are a
     *  silent no-op, but callers should check first rather than rely on that (also avoids a
     *  lint MissingPermission failure on release builds, which have abortOnError on by default). */
    fun hasNotificationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun openAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
