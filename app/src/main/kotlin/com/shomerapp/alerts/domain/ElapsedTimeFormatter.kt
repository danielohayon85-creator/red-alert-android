package com.shomerapp.alerts.domain

/** Formats "how long ago" for the status card / persistent notification's
 *  "עדכון אחרון: לפני X שניות" (§8). Pure function, kept out of UI code so it's unit-testable. */
object ElapsedTimeFormatter {
    fun format(elapsedSeconds: Long): String = when {
        elapsedSeconds < 60 -> "$elapsedSeconds שניות"
        elapsedSeconds < 3600 -> "${elapsedSeconds / 60} דקות"
        else -> "${elapsedSeconds / 3600} שעות"
    }
}
