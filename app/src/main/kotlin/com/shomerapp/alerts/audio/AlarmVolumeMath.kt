package com.shomerapp.alerts.audio

import kotlin.math.roundToInt

/** Pure arithmetic pulled out of [AlarmVolumeController] so it's unit-testable without an
 *  AudioManager. Never lowers an already-louder volume, never exceeds max. */
object AlarmVolumeMath {
    fun targetVolume(currentVolume: Int, maxVolume: Int, minFraction: Float): Int {
        val proportional = (maxVolume * minFraction).roundToInt()
        return proportional.coerceIn(0, maxVolume).coerceAtLeast(currentVolume)
    }
}
