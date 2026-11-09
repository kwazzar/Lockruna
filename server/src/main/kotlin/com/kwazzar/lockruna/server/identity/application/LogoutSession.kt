package com.kwazzar.lockruna.server.identity.application

import com.kwazzar.lockruna.server.identity.domain.RefreshTokenRepository
import java.time.Clock

/**
 * Ends the session of a device: its refresh tokens die, the access token expires on its own.
 */
class LogoutSession(private val refreshTokens: RefreshTokenRepository, private val clock: Clock = Clock.systemUTC()) {
    suspend operator fun invoke(userId: String, deviceId: String) {
        refreshTokens.revokeDeviceTokens(userId, deviceId, clock.instant())
    }
}
