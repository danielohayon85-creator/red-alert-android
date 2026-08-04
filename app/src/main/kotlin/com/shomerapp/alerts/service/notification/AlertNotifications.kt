package com.shomerapp.alerts.service.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.shomerapp.alerts.R
import com.shomerapp.alerts.ui.alert.AlertActivity

/**
 * §5/§7.1.A: this channel exists ONLY to trigger the full-screen visual experience — it is
 * silent and non-vibrating on purpose (`setSound(null, null)`, `enableVibration(false)`) because
 * [com.shomerapp.alerts.audio.AlarmAudioEngine] plays the actual sound/vibration manually and
 * completely independently. That independence is exactly what makes the "works even if the user
 * never granted the Full-Screen Intent permission" requirement true: this notification's content
 * can be silently downgraded to a heads-up by the OS on API 34+ without canUseFullScreenIntent(),
 * and the alarm still fires in full because it was never wired to this notification's success.
 */
object AlertNotifications {
    const val CHANNEL_ID = "life_safety_alert"
    const val NOTIFICATION_ID = 2001

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel_alert_name), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.notification_channel_alert_description)
                setSound(null, null)
                enableVibration(false)
                setBypassDnd(true)
            },
        )
    }

    fun buildFullScreenNotification(context: Context, title: String, text: String): Notification {
        val intent = Intent(context, AlertActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    fun show(context: Context, title: String, text: String) {
        if (!ServiceNotifications.hasNotificationPermission(context)) return
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, buildFullScreenNotification(context, title, text))
    }

    fun dismiss(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
