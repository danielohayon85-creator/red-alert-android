package com.shomerapp.alerts.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight looping preview used only by the sound-picker screen's "בדוק" button — deliberately
 * NOT [AlarmAudioEngine]: no forced volume, no audio focus takeover, no vibration. Just "let the
 * user hear the file they're about to pick," at whatever volume the phone is already at.
 */
@Singleton
class SoundPreviewPlayer @Inject constructor(@ApplicationContext private val context: Context) {
    private var player: MediaPlayer? = null

    fun play(uri: Uri) {
        stop()
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            runCatching {
                setDataSource(context, uri)
                isLooping = true
                setOnPreparedListener { it.start() }
                setOnErrorListener { _, _, _ -> true }
                prepareAsync()
            }
        }
    }

    fun stop() {
        player?.let { runCatching { if (it.isPlaying) it.stop() }; it.release() }
        player = null
    }
}
