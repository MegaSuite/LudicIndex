package com.ludicindex.app.data.remote

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicLong

class RateLimiterTest {

    @Test
    fun firstCallDoesNotWait() = runTest {
        val waits = mutableListOf<Long>()
        val limiter = RateLimiter(minIntervalMillis = 300, clock = { 0L }, delayFor = { waits += it })

        limiter.acquire()

        assertTrue(waits.isEmpty())
    }

    @Test
    fun spacesSubsequentCallsByTheMinimumInterval() = runTest {
        val now = AtomicLong(0)
        val waits = mutableListOf<Long>()
        val limiter = RateLimiter(
            minIntervalMillis = 300,
            clock = { now.get() },
            delayFor = { waits += it; now.addAndGet(it) },
        )

        repeat(3) { limiter.acquire() }

        assertEquals(listOf(300L, 300L), waits)
    }

    @Test
    fun doesNotWaitWhenEnoughTimeHasPassedOnItsOwn() = runTest {
        val now = AtomicLong(0)
        val waits = mutableListOf<Long>()
        val limiter = RateLimiter(
            minIntervalMillis = 300,
            clock = { now.get() },
            delayFor = { waits += it; now.addAndGet(it) },
        )

        limiter.acquire()
        now.addAndGet(1_000)
        limiter.acquire()

        assertTrue(waits.isEmpty())
    }
}
