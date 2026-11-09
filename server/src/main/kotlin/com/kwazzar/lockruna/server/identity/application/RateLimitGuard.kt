package com.kwazzar.lockruna.server.identity.application

import com.kwazzar.lockruna.server.identity.domain.RateLimitDecision
import com.kwazzar.lockruna.server.identity.domain.RateLimiter
import com.kwazzar.lockruna.server.platform.error.Problem
import com.kwazzar.lockruna.server.platform.error.ProblemException
import java.time.Duration

/**
 * Turns a blocked rate limit decision into a 429 problem that carries `Retry-After`.
 */
suspend fun RateLimiter.requireAllowed(scope: String, key: String, limit: Int, window: Duration) {
    val decision = check(scope, key, limit, window)
    if (decision is RateLimitDecision.Blocked) {
        throw ProblemException(
            problem = Problem(status = 429, code = "rate_limited", title = "Too many requests"),
            headers = mapOf("Retry-After" to decision.retryAfterSeconds.toString()),
        )
    }
}
