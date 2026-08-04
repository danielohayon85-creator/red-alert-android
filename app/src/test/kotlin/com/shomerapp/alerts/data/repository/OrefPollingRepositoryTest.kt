package com.shomerapp.alerts.data.repository

import com.shomerapp.alerts.data.remote.AlertFetcher
import com.shomerapp.alerts.domain.AlertClassifier
import com.shomerapp.alerts.domain.AlertDeduplicator
import com.shomerapp.alerts.domain.model.AlertKind
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** End-to-end poll behavior against a scripted fetcher — covers the §10 edge case
 *  "אותה התרעה שמתפשטת לעוד ערים (אותו id)". */
class OrefPollingRepositoryTest {

    private class ScriptedFetcher(private val responses: MutableList<String?>) : AlertFetcher {
        override suspend fun fetchRaw(): String? =
            if (responses.isEmpty()) "" else responses.removeAt(0)
    }

    private fun repositoryWith(vararg responses: String?): OrefPollingRepository {
        val rulesJson = File("src/main/assets/alert_rules.json").readText()
        return OrefPollingRepository(
            fetcher = ScriptedFetcher(responses.toMutableList()),
            classifier = AlertClassifier(rulesJson),
            deduplicator = AlertDeduplicator(),
            json = Json { ignoreUnknownKeys = true },
        )
    }

    @Test
    fun `blank body yields Empty`() = runTest {
        val repo = repositoryWith("")
        assertEquals(PollOutcome.Empty, repo.pollOnce())
    }

    @Test
    fun `null fetch result yields NetworkError`() = runTest {
        val repo = repositoryWith(null)
        assertEquals(PollOutcome.NetworkError, repo.pollOnce())
    }

    @Test
    fun `first alert is a new event and classifies correctly`() = runTest {
        val body = """{"id":"1","cat":"1","title":"ירי רקטות וטילים","data":["תל אביב"],"desc":"היכנסו למרחב המוגן ושהו בו 10 דקות"}"""
        val repo = repositoryWith(body)
        val outcome = repo.pollOnce() as PollOutcome.AlertUpdate
        assertTrue(outcome.isNewEvent)
        assertEquals(AlertKind.IMMEDIATE, outcome.alert.kind)
        assertEquals(listOf("תל אביב"), outcome.newCities)
        assertEquals(600, outcome.alert.durationSeconds)
    }

    @Test
    fun `same id expanding to more cities reports only the new ones and is not a new event`() = runTest {
        val first = """{"id":"1","cat":"1","title":"ירי רקטות וטילים","data":["תל אביב"],"desc":"..."}"""
        val second = """{"id":"1","cat":"1","title":"ירי רקטות וטילים","data":["תל אביב","רמת גן"],"desc":"..."}"""
        val repo = repositoryWith(first, second)

        repo.pollOnce()
        val outcome = repo.pollOnce() as PollOutcome.AlertUpdate
        assertEquals(false, outcome.isNewEvent)
        assertEquals(listOf("רמת גן"), outcome.newCities)
    }

    @Test
    fun `repeating the exact same alert yields NoChange, not a re-trigger`() = runTest {
        val body = """{"id":"1","cat":"1","title":"ירי רקטות וטילים","data":["תל אביב"],"desc":"..."}"""
        val repo = repositoryWith(body, body)

        repo.pollOnce()
        assertEquals(PollOutcome.NoChange, repo.pollOnce())
    }

    @Test
    fun `event end never carries an alarm sound kind`() = runTest {
        val body = """{"id":"1","cat":"1","title":"ירי רקטות וטילים - האירוע הסתיים","data":["תל אביב"],"desc":"האירוע הסתיים"}"""
        val repo = repositoryWith(body)
        val outcome = repo.pollOnce() as PollOutcome.AlertUpdate
        assertEquals(AlertKind.ALL_CLEAR, outcome.alert.kind)
    }
}
