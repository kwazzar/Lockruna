package com.kwazzar.lockruna.server.platform.redis

import com.kwazzar.lockruna.server.identity.domain.DeviceDenylist
import io.lettuce.core.RedisClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration

/**
 * Revoked devices in Redis: the entry lives as long as the access tokens of the device.
 */
class RedisDeviceDenylist(redisUrl: String) :
    DeviceDenylist,
    AutoCloseable {
    private val client = RedisClient.create(redisUrl)
    private val connection by lazy { client.connect() }

    override suspend fun isRevoked(deviceId: String): Boolean = withContext(Dispatchers.IO) {
        connection.sync().exists(key(deviceId)) > 0
    }

    override suspend fun revoke(deviceId: String, ttl: Duration) = withContext(Dispatchers.IO) {
        connection.sync().setex(key(deviceId), ttl.seconds, "1")
        Unit
    }

    override fun close() {
        client.shutdown()
    }

    private fun key(deviceId: String) = "denylist:device:$deviceId"
}
