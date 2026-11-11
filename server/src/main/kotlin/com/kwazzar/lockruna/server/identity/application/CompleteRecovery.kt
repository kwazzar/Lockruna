package com.kwazzar.lockruna.server.identity.application

import com.kwazzar.lockruna.contract.KdfParamsDto
import com.kwazzar.lockruna.server.identity.domain.ACCESS_TOKEN_SECONDS
import com.kwazzar.lockruna.server.identity.domain.AuthKeys
import com.kwazzar.lockruna.server.identity.domain.DeviceDenylist
import com.kwazzar.lockruna.server.identity.domain.DeviceRecord
import com.kwazzar.lockruna.server.identity.domain.DeviceRepository
import com.kwazzar.lockruna.server.identity.domain.RateLimiter
import com.kwazzar.lockruna.server.identity.domain.RefreshTokenRepository
import com.kwazzar.lockruna.server.identity.domain.TokenService
import com.kwazzar.lockruna.server.identity.domain.UserRepository
import com.kwazzar.lockruna.server.platform.config.ServerSettings
import com.kwazzar.lockruna.server.platform.error.Problem
import com.kwazzar.lockruna.server.platform.error.ProblemException
import kotlinx.serialization.json.Json
import java.time.Clock
import java.time.Duration
import java.util.UUID

/**
 * Completes a recovery flow: verifies the recovery auth key, replaces the key material,
 * creates a new device and revokes every other device and its tokens.
 */
class CompleteRecovery(
    private val users: UserRepository,
    private val devices: DeviceRepository,
    private val refreshTokens: RefreshTokenRepository,
    private val denylist: DeviceDenylist,
    private val tokens: TokenService,
    private val rateLimiter: RateLimiter,
    private val settings: ServerSettings,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend operator fun invoke(command: CompleteRecoveryCommand): AuthSession {
        val email = command.email.trim().lowercase()
        rateLimiter.requireAllowed("recovery:email", AuthKeys.emailKey(email), RECOVERY_LIMIT, RECOVERY_WINDOW)
        rateLimiter.requireAllowed("recovery:ip", command.clientIp, RECOVERY_LIMIT, RECOVERY_WINDOW)

        val user = users.findByEmail(email) ?: throw invalidCredentials()
        val providedHash = AuthKeys.hash(settings.pepper, command.recoveryAuthKey)
        if (!AuthKeys.constantTimeEquals(providedHash, user.recoveryAuthHash)) {
            throw invalidCredentials()
        }

        val now = clock.instant()
        val others = devices.listByUser(user.id).map { it.id }
        val newDevice = devices.create(
            DeviceRecord(
                id = UUID.randomUUID().toString(),
                userId = user.id,
                name = command.deviceName,
                platform = command.devicePlatform,
                createdAt = now,
                lastSeen = now,
                revokedAt = null,
            ),
        )
        val keyMaterial = users.updateKeyMaterial(
            id = user.id,
            authHash = AuthKeys.hash(settings.pepper, command.newAuthKey),
            recoveryAuthHash = user.recoveryAuthHash,
            kdfSalt = command.newKdfSalt,
            wrappedVaultKey = command.newWrappedVaultKey,
        )
        if (!keyMaterial) throw invalidCredentials()

        devices.revokeAllForUser(user.id, newDevice.id, now)
        refreshTokens.revokeAllForUser(user.id, now)
        val accessTtl = Duration.ofSeconds(ACCESS_TOKEN_SECONDS.toLong())
        others.forEach { denylist.revoke(it, accessTtl) }

        return AuthSession(
            userId = user.id,
            deviceId = newDevice.id,
            accessToken = tokens.issueAccessToken(user.id, newDevice.id),
            refreshToken = tokens.issueRefreshToken(user.id, newDevice.id, UUID.randomUUID().toString(), now),
            expiresIn = ACCESS_TOKEN_SECONDS,
            kdfSalt = command.newKdfSalt,
            kdfParams = command.newKdfParams,
            wrappedVaultKey = command.newWrappedVaultKey,
        )
    }

    private fun invalidCredentials(): ProblemException =
        ProblemException(problem = Problem(status = 401, code = "invalid_credentials", title = "Invalid credentials"))
}

data class CompleteRecoveryCommand(
    val email: String,
    val recoveryAuthKey: ByteArray,
    val newAuthKey: ByteArray,
    val newKdfSalt: ByteArray,
    val newKdfParams: KdfParamsDto,
    val newWrappedVaultKey: ByteArray,
    val deviceName: String,
    val devicePlatform: String,
    val clientIp: String,
)

private const val RECOVERY_LIMIT = 5
private val RECOVERY_WINDOW: Duration = Duration.ofMinutes(15)