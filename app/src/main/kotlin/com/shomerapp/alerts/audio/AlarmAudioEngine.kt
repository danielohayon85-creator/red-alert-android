package com.shomerapp.alerts.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The actual "core of the product" per the original prompt's §5: everything is played manually
 * (MediaPlayer/TTS/Vibrator), NEVER via NotificationChannel.setSound() — that API burns the
 * sound into the channel forever on API 26+ with no way to change it later. The visual
 * notification (Stage 3's channels, Stage 5's full-screen alert) is completely separate from
 * this class; they never share a sound pipeline.
 *
 * Owns a long-lived scope independent of any caller's lifecycle (an Activity can be destroyed
 * mid-alert on rotation/backgrounding; the sound must keep playing regardless — that's the whole
 * point of it being a MediaPlayer under app control, not tied to a Compose composition).
 *
 * Known simplification: MediaPlayer callbacks (TTS ducking, prepared-listener) can land on a
 * binder/TTS thread rather than the thread that created the player. This works in practice for
 * the simple volume/start calls used here, but isn't fully thread-confined — worth revisiting in
 * Stage 7 polish if real-device testing surfaces a race.
 */
@Singleton
class AlarmAudioEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val volumeController: AlarmVolumeController,
    private val ttsAnnouncer: TtsAnnouncer,
) {
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var activeJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null

    /** §2.7: plays until [stop] is called explicitly (duration timer or user confirmation in the
     *  Stage 5 alert screen) — never auto-stops itself after a few seconds. */
    fun playImmediate(soundUri: Uri, announcementText: String) {
        activeJob?.cancel()
        activeJob = engineScope.launch {
            stopPlaybackKeepingVolume()
            volumeController.forceMinimumVolume(IMMEDIATE_MIN_VOLUME_FRACTION)
            requestAudioFocus()
            vibrate(VibrationPatterns.IMMEDIATE, repeatFromIndex = 0)
            startLoopingSound(soundUri)
            announceOverSound(announcementText)
        }
    }

    /** §4.1: separate sound, fixed 15s non-looping play with a 4s fade-in, lower volume floor
     *  than IMMEDIATE — deliberately less alarming since ~half of these never lead to a real alert. */
    fun playPrewarning(soundUri: Uri, announcementText: String) {
        activeJob?.cancel()
        activeJob = engineScope.launch {
            stopPlaybackKeepingVolume()
            volumeController.forceMinimumVolume(PREWARNING_MIN_VOLUME_FRACTION)
            requestAudioFocus()
            vibrate(VibrationPatterns.PREWARNING, repeatFromIndex = -1)
            startFadeInSound(soundUri, fadeInMs = PREWARNING_FADE_IN_MS)
            announceOverSound(announcementText)
            delay(PREWARNING_TOTAL_DURATION_MS)
            stop()
        }
    }

    /** §4.1: never the alarm sound — just silence (stops whatever was playing). A short pleasant
     *  chime asset can be added here later; none is bundled yet (no CC0 asset in this repo). */
    fun playAllClear() {
        stop()
    }

    /** Stops everything and restores the pre-alert volume — the normal end-of-alert path
     *  (duration elapsed or user pressed "אני במרחב המוגן"). */
    fun stop() {
        activeJob?.cancel()
        engineScope.launch {
            stopPlaybackKeepingVolume()
            volumeController.restore()
        }
    }

    private fun stopPlaybackKeepingVolume() {
        mediaPlayer?.let { player ->
            runCatching { if (player.isPlaying) player.stop() }
            player.release()
        }
        mediaPlayer = null
        cancelVibration()
        abandonAudioFocus()
        ttsAnnouncer.stop()
    }

    private fun announceOverSound(text: String) {
        ttsAnnouncer.announce(
            text = text,
            onStart = { mediaPlayer?.setVolume(DUCKED_VOLUME, DUCKED_VOLUME) },
            onDone = { mediaPlayer?.setVolume(FULL_VOLUME, FULL_VOLUME) },
        )
    }

    private fun startLoopingSound(uri: Uri) {
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(alarmAudioAttributes())
            setDataSource(context, uri)
            isLooping = true
            setVolume(FULL_VOLUME, FULL_VOLUME)
            setOnPreparedListener { it.start() }
            setOnErrorListener { _, _, _ -> true }
            prepareAsync()
        }
    }

    private fun startFadeInSound(uri: Uri, fadeInMs: Long) {
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(alarmAudioAttributes())
            setDataSource(context, uri)
            isLooping = false
            setVolume(0f, 0f)
            setOnPreparedListener { player ->
                player.start()
                fadeIn(player, fadeInMs)
            }
            setOnErrorListener { _, _, _ -> true }
            prepareAsync()
        }
    }

    private fun fadeIn(player: MediaPlayer, durationMs: Long) {
        engineScope.launch {
            val steps = 20
            val stepDelay = durationMs / steps
            for (step in 1..steps) {
                if (mediaPlayer !== player) return@launch // superseded by a newer playback
                val level = FULL_VOLUME * step / steps
                runCatching { player.setVolume(level, level) }
                delay(stepDelay)
            }
        }
    }

    private fun alarmAudioAttributes(): AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

    private fun requestAudioFocus() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
            .setAudioAttributes(alarmAudioAttributes())
            .build()
        focusRequest = request
        audioManager.requestAudioFocus(request)
    }

    private fun abandonAudioFocus() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    private fun vibrator(): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    private fun vibrate(pattern: LongArray, repeatFromIndex: Int) {
        val effect = VibrationEffect.createWaveform(pattern, repeatFromIndex)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val attributes = VibrationAttributes.Builder().setUsage(VibrationAttributes.USAGE_ALARM).build()
            vibrator().vibrate(effect, attributes)
        } else {
            vibrator().vibrate(effect)
        }
    }

    private fun cancelVibration() {
        vibrator().cancel()
    }

    private companion object {
        const val IMMEDIATE_MIN_VOLUME_FRACTION = 0.8f
        const val PREWARNING_MIN_VOLUME_FRACTION = 0.6f
        const val PREWARNING_FADE_IN_MS = 4_000L
        const val PREWARNING_TOTAL_DURATION_MS = 15_000L
        const val DUCKED_VOLUME = 0.3f
        const val FULL_VOLUME = 1f
    }
}
