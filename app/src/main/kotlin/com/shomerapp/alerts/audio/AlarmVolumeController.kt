package com.shomerapp.alerts.audio

import android.content.Context
import android.media.AudioManager
import dagger.hilt.android.qualifiers.ApplicationContext
import com.shomerapp.alerts.data.local.SoundPreferences
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * §2.2: forces STREAM_ALARM to at least [minFraction] during an alert, remembers the pre-alert
 * volume in DataStore so it can be restored — including after a process death mid-alert, since
 * [restoreIfPending] is called on every app/service startup, not just on a clean [restore].
 */
@Singleton
class AlarmVolumeController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val soundPreferences: SoundPreferences,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    suspend fun forceMinimumVolume(minFraction: Float) {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        soundPreferences.saveVolumeToRestoreIfAbsent(current)
        val target = AlarmVolumeMath.targetVolume(current, max, minFraction)
        if (target != current) {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, target, 0)
        }
    }

    /** Called when an alert ends normally (duration elapsed or user confirmed). */
    suspend fun restore() {
        val saved = soundPreferences.volumeToRestore.first() ?: return
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, saved, 0)
        soundPreferences.clearVolumeToRestore()
    }

    /** Called once at app/service startup: if a saved value is still there, the process must
     *  have died mid-alert without a clean [restore] — fix the volume now. */
    suspend fun restoreIfPending() = restore()
}
