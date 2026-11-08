package com.kwazzar.lockruna.server.identity.application

import com.kwazzar.lockruna.server.identity.domain.ACCESS_TOKEN_SECONDS
import com.kwazzar.lockruna.server.identity.domain.DeviceDenylist
import com.kwazzar.lockruna.server.identity.domain.DeviceRepository
import com.kwazzar.lockruna.server.identity.domain.RefreshTokenRepository
import com.kwazzar.lockruna.server.platform.error.Problem
import com.kwazzar.lockruna.server.platform.error.ProblemException
import java.time.Clock
import java.time.Duration

/**
 * Revokes a device of the user: its refresh tokens die and its access tokens stop working at once.
 */
class RevokeDevice(
    private val devices: DeviceRepository,
    private val refreshTokens: RefreshTokenRepository,
    private val denylist: DeviceDenylist,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend operator fun invoke(userId: String, deviceId: String) {
        val now = clock.instant()
        if (!devices.revoke(userId, deviceId)) {
            throw ProblemException(
                problem = Problem(status = 404, code = "device_not_found", title = "Device not found"),
            )
        }
        refreshTokens.revokeDeviceTokens(userId, deviceId, now)
        denylist.revoke(deviceId, Duration.ofSeconds(ACCESS_TOKEN_SECONDS.toLong()))
    }
}
