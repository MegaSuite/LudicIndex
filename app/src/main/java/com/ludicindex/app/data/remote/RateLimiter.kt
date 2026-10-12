package com.ludicindex.app.data.remote

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * IGDB allows 4 requests/second. Serialising callers through a token bucket keeps us at ~3.3/s
 * (300 ms spacing), which leaves headroom for the occasional retry.
 *
 * [clock] and [delay] are injectable so tests can run without real waiting.
 */
class RateLimiter(
    private val minIntervalMillis: Long = 300L,
    private val clock: () -> Long = System::currentTimeMillis,
    private val delayFor: suspend (Long) -> Unit = { kotlinx.coroutines.delay(it) },
) {

    private val mutex = Mutex()
    private var nextAllowedAtMillis = 0L

    suspend fun acquire() {
        mutex.withLock {
            val waitFor = nextAllowedAtMillis - clock()
            if (waitFor > 0) delayFor(waitFor)
            nextAllowedAtMillis = clock() + minIntervalMillis
        }
    }
}
