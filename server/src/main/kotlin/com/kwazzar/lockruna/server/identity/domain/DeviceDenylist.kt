package com.kwazzar.lockruna.server.identity.domain

import java.time.Duration

/**
 * Devices whose access tokens must be rejected right away, even while the JWT is still valid.
 */
interface DeviceDenylist {
    suspend fun isRevoked(deviceId: String): Boolean

    suspend fun revoke(deviceId: String, ttl: Duration)
}
