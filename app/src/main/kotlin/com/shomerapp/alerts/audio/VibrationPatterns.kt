package com.shomerapp.alerts.audio

/**
 * Fixed, user-unchangeable vibration patterns (§2.4: vibration is the identity anchor when the
 * sound itself is a personal choice — it must always feel the same). Arrays are
 * [delay, vibrate, sleep, vibrate, sleep, ...] ms, as [android.os.VibrationEffect.createWaveform] expects.
 */
object VibrationPatterns {
    val IMMEDIATE = longArrayOf(0, 800, 400, 800, 400, 800, 400)
    val PREWARNING = longArrayOf(0, 200, 150, 200, 150, 200)
}
