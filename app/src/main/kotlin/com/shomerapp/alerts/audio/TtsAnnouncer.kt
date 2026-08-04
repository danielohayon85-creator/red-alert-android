package com.shomerapp.alerts.audio

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mandatory Hebrew TTS announcement, spoken on STREAM_ALARM (§2.1, §5). [onStart]/[onDone] exist
 * so [AlarmAudioEngine] can duck the looping alarm sound to 30% while the announcement plays and
 * restore it after — "מעל" the sound, not a replacement for it.
 */
@Singleton
class TtsAnnouncer @Inject constructor(@ApplicationContext private val context: Context) {
    private var tts: TextToSpeech? = null

    fun announce(text: String, onStart: () -> Unit = {}, onDone: () -> Unit = {}) {
        val engine = tts
        if (engine == null) {
            initialize { announce(text, onStart, onDone) }
            return
        }
        speak(engine, text, onStart, onDone)
    }

    private fun initialize(onReady: () -> Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("he", "IL")
                onReady()
            }
        }
    }

    private fun speak(engine: TextToSpeech, text: String, onStart: () -> Unit, onDone: () -> Unit) {
        engine.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        engine.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = onStart()
                override fun onDone(utteranceId: String?) = onDone()

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) = onDone()
            },
        )
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
    }

    private companion object {
        const val UTTERANCE_ID = "alert_announcement"
    }
}
