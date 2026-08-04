package com.shomerapp.alerts.domain

/** "0:47" / "2:09" style — used by the alert screen's countdown and elapsed-time displays,
 *  distinct from [ElapsedTimeFormatter]'s "5 שניות" prose style used for the status card. */
object MmSsFormatter {
    fun format(totalSeconds: Long): String {
        val clamped = totalSeconds.coerceAtLeast(0)
        val minutes = clamped / 60
        val seconds = clamped % 60
        return "%d:%02d".format(minutes, seconds)
    }
}
