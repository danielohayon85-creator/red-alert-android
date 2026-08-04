package com.shomerapp.alerts.audio

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import com.shomerapp.alerts.data.local.SoundPreferences
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decides what sound actually plays for an alert. Falls back to the system default whenever the
 * user's choice isn't safe to trust: never picked one, picked one but never pressed "test" (§2.5
 * mandatory test-before-use), or the file was deleted since (§5, checked via [SoundUriValidator]).
 */
@Singleton
class SoundResolver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val soundPreferences: SoundPreferences,
    private val validator: SoundUriValidator,
) {
    suspend fun resolveImmediateSoundUri(): Uri =
        resolve(soundPreferences.immediateSoundUri.first(), soundPreferences.immediateSoundConfirmed.first())
            ?: defaultAlarmUri()

    suspend fun resolvePrewarningSoundUri(): Uri =
        resolve(soundPreferences.prewarningSoundUri.first(), soundPreferences.prewarningSoundConfirmed.first())
            ?: defaultNotificationUri()

    private fun resolve(savedUri: String?, confirmed: Boolean): Uri? {
        if (savedUri == null || !confirmed || !validator.isValid(savedUri)) return null
        return Uri.parse(savedUri)
    }

    private fun defaultAlarmUri(): Uri =
        RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

    private fun defaultNotificationUri(): Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
}
