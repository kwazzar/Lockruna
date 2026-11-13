package com.kwazzar.lockruna.server.support

import com.kwazzar.lockruna.server.identity.domain.RateLimitDecision
import com.kwazzar.lockruna.server.identity.domain.RateLimiter
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Rate limiter fake for tests: counters in memory, one instance per application.
 */
class InMemoryRateLimiter(private val clock: Clock = Clock.systemUTC()) : RateLimiter {
    private val counters = ConcurrentHashMap<String, Counter>()

    override suspend fun check(scope: String, key: String, limit: Int, window: Duration): RateLimitDecision {
        val now = clock.instant()
        val counter = counters.compute("$scope:$key") { _, existing ->
            existing?.takeIf { it.expiresAt > now } ?: Counter(now, now.plus(window))
        }!!
        counter.count += 1
        return if (counter.count <= limit) {
            RateLimitDecision.Allowed
        } else {
            RateLimitDecision.Blocked(remainingSeconds(counter.expiresAt, now))
        }
    }

    override suspend fun resetForUser(userId: String) {
        counters.keys.filter { it.contains(userId) }.forEach { counters.remove(it) }
    }

    private fun remainingSeconds(expiresAt: Instant, now: Instant): Int =
        ((expiresAt.epochSecond - now.epochSecond).coerceAtLeast(1)).toInt()

    private class Counter(val createdAt: Instant, val expiresAt: Instant) {
        var count: Int = 0
    }
}
