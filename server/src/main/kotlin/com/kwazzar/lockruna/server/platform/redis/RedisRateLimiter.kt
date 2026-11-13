package com.kwazzar.lockruna.server.platform.redis

import com.kwazzar.lockruna.server.identity.domain.RateLimitDecision
import com.kwazzar.lockruna.server.identity.domain.RateLimiter
import io.lettuce.core.RedisClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration

/**
 * Rate limiting counters in Redis: one key per scope, the window is the key TTL.
 */
class RedisRateLimiter(redisUrl: String) :
    RateLimiter,
    AutoCloseable {
    private val client = RedisClient.create(redisUrl)
    private val connection by lazy { client.connect() }

    override suspend fun check(scope: String, key: String, limit: Int, window: Duration): RateLimitDecision =
        withContext(Dispatchers.IO) {
            val redisKey = "rl:$scope:$key"
            val commands = connection.sync()
            val count = commands.incr(redisKey)
            if (count == 1L) commands.expire(redisKey, window.seconds)
            if (count > limit) {
                RateLimitDecision.Blocked(commands.ttl(redisKey).coerceAtLeast(1L).toInt())
            } else {
                RateLimitDecision.Allowed
            }
        }

    override suspend fun resetForUser(userId: String) = withContext(Dispatchers.IO) {
        val commands = connection.sync()
        val keys = commands.keys("rl:*:*$userId*")
        keys.forEach { commands.del(it) }
    }

    override fun close() {
        client.shutdown()
    }
}
