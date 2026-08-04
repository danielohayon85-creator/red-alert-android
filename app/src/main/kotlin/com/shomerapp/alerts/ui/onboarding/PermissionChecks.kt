package com.shomerapp.alerts.ui.onboarding

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat

object PermissionChecks {
    fun notificationsGranted(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true // permission didn't exist before API 33 — notifications were always allowed
        }

    fun dndAccessGranted(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted

    fun fullScreenIntentGranted(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 34) {
            context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        } else {
            true // auto-granted below API 34
        }

    fun batteryOptimizationExempted(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
}
