package com.shomerapp.alerts.data.remote

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * Fake alert source for the Debug Panel (full UI lands in Stage 6) and for tests. Consumers of
 * this class enqueue a raw alerts.json body via [enqueue]; the next [fetchRaw] call returns and
 * removes it. An empty queue behaves like the real endpoint's "no alert" response (blank body),
 * so a queued alert naturally "clears" on the following poll unless enqueued again.
 */
class MockAlertFetcher @Inject constructor() : AlertFetcher {
    private val mutex = Mutex()
    private val queue = ArrayDeque<String>()

    suspend fun enqueue(rawJsonBody: String) = mutex.withLock { queue.addLast(rawJsonBody) }

    suspend fun clear() = mutex.withLock { queue.clear() }

    override suspend fun fetchRaw(): String? = mutex.withLock {
        if (queue.isEmpty()) "" else queue.removeFirst()
    }
}
