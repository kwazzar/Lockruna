package com.kwazzar.lockruna.server.identity.domain

import java.time.Duration

/**
 * Rate limiting counters. Implementations live in the infrastructure layer; tests use an in-memory fake.
 */
interface RateLimiter {
    suspend fun check(scope: String, key: String, limit: Int, window: Duration): RateLimitDecision

    /** Removes all rate-limit keys for the given user (best-effort cleanup on account deletion). */
    suspend fun resetForUser(userId: String)
}

sealed interface RateLimitDecision {
    data object Allowed : RateLimitDecision

    data class Blocked(val retryAfterSeconds: Int) : RateLimitDecision
}
