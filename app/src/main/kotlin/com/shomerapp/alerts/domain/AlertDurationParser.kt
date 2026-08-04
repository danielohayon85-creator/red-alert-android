package com.shomerapp.alerts.domain

/** §4 "מלכודות": duration = first number of minutes found in `desc`, times 60; default 600s if none. */
object AlertDurationParser {
    private val firstNumber = Regex("\\d+")
    private const val DEFAULT_SECONDS = 600

    fun extractSeconds(desc: String): Int =
        firstNumber.find(desc)?.value?.toIntOrNull()?.times(60) ?: DEFAULT_SECONDS
}
