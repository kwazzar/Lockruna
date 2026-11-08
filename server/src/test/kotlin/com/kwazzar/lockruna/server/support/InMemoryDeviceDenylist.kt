package com.kwazzar.lockruna.server.support

import com.kwazzar.lockruna.server.identity.domain.DeviceDenylist
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Denylist fake for tests: revoked device ids live in memory until their TTL is over.
 */
class InMemoryDeviceDenylist(private val clock: Clock = Clock.systemUTC()) : DeviceDenylist {
    private val revoked = ConcurrentHashMap<String, Instant>()

    override suspend fun isRevoked(deviceId: String): Boolean {
        val expiresAt = revoked[deviceId] ?: return false
        if (expiresAt <= clock.instant()) {
            revoked.remove(deviceId)
            return false
        }
        return true
    }

    override suspend fun revoke(deviceId: String, ttl: Duration) {
        revoked[deviceId] = clock.instant().plus(ttl)
    }
}
