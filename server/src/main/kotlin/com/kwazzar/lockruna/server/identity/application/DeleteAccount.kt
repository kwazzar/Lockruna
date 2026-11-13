package com.kwazzar.lockruna.server.identity.application

import com.kwazzar.lockruna.server.audit.domain.AuditAction
import com.kwazzar.lockruna.server.audit.domain.AuditEvent
import com.kwazzar.lockruna.server.audit.domain.AuditEventRepository
import com.kwazzar.lockruna.server.identity.domain.ACCESS_TOKEN_SECONDS
import com.kwazzar.lockruna.server.identity.domain.AuthKeys
import com.kwazzar.lockruna.server.identity.domain.DeviceDenylist
import com.kwazzar.lockruna.server.identity.domain.DeviceRepository
import com.kwazzar.lockruna.server.identity.domain.RateLimiter
import com.kwazzar.lockruna.server.identity.domain.UserRecord
import com.kwazzar.lockruna.server.identity.domain.UserRepository
import com.kwazzar.lockruna.server.platform.config.ServerSettings
import com.kwazzar.lockruna.server.platform.error.Problem
import com.kwazzar.lockruna.server.platform.error.ProblemException
import java.time.Duration

/**
 * Deletes the account and all of its data after re-confirming the master password.
 * Access tokens of the removed devices are denied right away.
 */
class DeleteAccount(
    private val users: UserRepository,
    private val devices: DeviceRepository,
    private val denylist: DeviceDenylist,
    private val rateLimiter: RateLimiter,
    private val settings: ServerSettings,
    private val audit: AuditEventRepository,
) {
    suspend operator fun invoke(userId: String, authKey: ByteArray) {
        val user = verifyAuthKey(userId, authKey)

        val knownDevices = devices.listByUser(userId).map { it.id }
        if (!users.deleteById(userId)) {
            throw invalidCredentials()
        }
        val accessTtl = Duration.ofSeconds(ACCESS_TOKEN_SECONDS.toLong())
        knownDevices.forEach { denylist.revoke(it, accessTtl) }
        rateLimiter.resetForUser(userId)
        audit.record(
            AuditEvent(
                userId = userId,
                deviceId = null,
                action = AuditAction.ACCOUNT_DELETE,
                ip = null,
                ts = java.time.Instant.now().toEpochMilli(),
            ),
        )
    }

    private suspend fun verifyAuthKey(userId: String, authKey: ByteArray): UserRecord {
        val user = users.findById(userId) ?: throw invalidCredentials()
        val providedHash = AuthKeys.hash(settings.pepper, authKey)
        if (!AuthKeys.constantTimeEquals(providedHash, user.authHash)) {
            throw invalidCredentials()
        }
        return user
    }

    private fun invalidCredentials(): ProblemException =
        ProblemException(problem = Problem(status = 401, code = "invalid_credentials", title = "Invalid credentials"))
}
