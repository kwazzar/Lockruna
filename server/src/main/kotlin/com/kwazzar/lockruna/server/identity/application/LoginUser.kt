package com.kwazzar.lockruna.server.identity.application

import com.kwazzar.lockruna.server.audit.domain.AuditAction
import com.kwazzar.lockruna.server.audit.domain.AuditEvent
import com.kwazzar.lockruna.server.audit.domain.AuditEventRepository
import com.kwazzar.lockruna.contract.KdfParamsDto
import com.kwazzar.lockruna.server.identity.domain.ACCESS_TOKEN_SECONDS
import com.kwazzar.lockruna.server.identity.domain.AuthKeys
import com.kwazzar.lockruna.server.identity.domain.DeviceRecord
import com.kwazzar.lockruna.server.identity.domain.DeviceRepository
import com.kwazzar.lockruna.server.identity.domain.RateLimiter
import com.kwazzar.lockruna.server.identity.domain.TokenService
import com.kwazzar.lockruna.server.identity.domain.UserRecord
import com.kwazzar.lockruna.server.identity.domain.UserRepository
import com.kwazzar.lockruna.server.platform.config.ServerSettings
import com.kwazzar.lockruna.server.platform.error.Problem
import com.kwazzar.lockruna.server.platform.error.ProblemException
import kotlinx.serialization.json.Json
import java.time.Clock
import java.time.Duration
import java.util.UUID

/**
 * Authenticates a session. A wrong password and an unknown email produce the same 401.
 */
class LoginUser(
    private val users: UserRepository,
    private val devices: DeviceRepository,
    private val tokens: TokenService,
    private val rateLimiter: RateLimiter,
    private val settings: ServerSettings,
    private val audit: AuditEventRepository,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend operator fun invoke(command: LoginCommand): AuthSession {
        val email = command.email.trim().lowercase()
        val user = users.findByEmail(email)
        val authHash = AuthKeys.hash(settings.pepper, command.authKey)
        val matches = AuthKeys.constantTimeEquals(authHash, user?.authHash ?: ByteArray(SHA_256_BYTES))
        if (user == null || !matches) {
            audit.record(AuditEvent(
                userId = null,
                deviceId = command.deviceId,
                action = AuditAction.LOGIN_FAILED,
                ip = command.clientIp,
                ts = clock.instant().toEpochMilli(),
            ))
            rateLimiter.requireAllowed("login:email", AuthKeys.emailKey(email), LOGIN_LIMIT, LOGIN_WINDOW)
            rateLimiter.requireAllowed("login:ip", command.clientIp, LOGIN_LIMIT, LOGIN_WINDOW)
            throw ProblemException(
                problem = Problem(status = 401, code = "invalid_credentials", title = "Invalid credentials"),
            )
        }

        val device = resolveDevice(user, command)
        val accessToken = tokens.issueAccessToken(user.id, device.id)
        val refreshToken = tokens.issueRefreshToken(user.id, device.id, UUID.randomUUID().toString(), clock.instant())

        val session = AuthSession(
            userId = user.id,
            deviceId = device.id,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = ACCESS_TOKEN_SECONDS,
            kdfSalt = user.kdfSalt,
            kdfParams = Json.decodeFromString(KdfParamsDto.serializer(), user.kdfParams),
            wrappedVaultKey = user.wrappedVaultKey,
        )
        audit.record(AuditEvent(
            userId = session.userId,
            deviceId = session.deviceId,
            action = AuditAction.LOGIN,
            ip = command.clientIp,
            ts = clock.instant().toEpochMilli(),
        ))
        return session
    }

    private suspend fun resolveDevice(user: UserRecord, command: LoginCommand): DeviceRecord {
        val requested = command.deviceId
        if (requested != null) {
            val existing = devices.findById(user.id, requested)
            if (existing != null) {
                if (existing.revokedAt != null) {
                    throw ProblemException(
                        problem = Problem(status = 401, code = "device_revoked", title = "Device revoked"),
                    )
                }
                devices.touch(user.id, existing.id, clock.instant())
                return existing
            }
        }
        val now = clock.instant()
        return devices.create(
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
    }
}

data class LoginCommand(
    val email: String,
    val authKey: ByteArray,
    val deviceId: String?,
    val deviceName: String,
    val devicePlatform: String,
    val clientIp: String,
)

private const val LOGIN_LIMIT = 5
private const val SHA_256_BYTES = 32
private val LOGIN_WINDOW: Duration = Duration.ofMinutes(15)
