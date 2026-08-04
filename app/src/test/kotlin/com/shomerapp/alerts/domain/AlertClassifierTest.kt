package com.shomerapp.alerts.domain

import com.shomerapp.alerts.domain.model.AlertKind
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** One test per row of the §4 classification table, run against the actual shipped
 *  alert_rules.json (not a fixture copy) so the test catches drift from the real asset. */
class AlertClassifierTest {

    private val classifier = AlertClassifier(File("src/main/assets/alert_rules.json").readText())

    @Test
    fun `event end phrase classifies as ALL_CLEAR`() {
        assertEquals(AlertKind.ALL_CLEAR, classifier.classify("ירי רקטות וטילים - האירוע הסתיים", "1"))
    }

    @Test
    fun `prewarning phrase classifies as PREWARNING`() {
        assertEquals(
            AlertKind.PREWARNING,
            classifier.classify("בדקות הקרובות צפויות להתקבל התרעות באיזורים הבאים:", "1"),
        )
    }

    @Test
    fun `rocket fire classifies as IMMEDIATE`() {
        assertEquals(AlertKind.IMMEDIATE, classifier.classify("ירי רקטות וטילים", "1"))
    }

    @Test
    fun `hostile aircraft intrusion classifies as IMMEDIATE`() {
        assertEquals(AlertKind.IMMEDIATE, classifier.classify("חדירת כלי טיס עוין", "3"))
    }

    @Test
    fun `earthquake classifies as INFO`() {
        assertEquals(AlertKind.INFO, classifier.classify("רעידת אדמה", "20"))
    }

    @Test
    fun `tsunami classifies as INFO`() {
        assertEquals(AlertKind.INFO, classifier.classify("צונאמי", "21"))
    }

    @Test
    fun `unrecognized title defaults to IMMEDIATE, never silently dropped`() {
        assertEquals(AlertKind.IMMEDIATE, classifier.classify("כותרת לא מוכרת שלא נראתה מעולם", "99"))
    }

    @Test
    fun `ALL_CLEAR phrase always wins even with an unrelated cat`() {
        assertEquals(AlertKind.ALL_CLEAR, classifier.classify("חדירת כלי טיס עוין - האירוע הסתיים", "3"))
    }

    @Test
    fun `matching is whitespace-normalized`() {
        assertEquals(AlertKind.IMMEDIATE, classifier.classify("ירי   רקטות   וטילים", "1"))
    }
}
