package com.kwazzar.lockruna.server.identity.application

import com.kwazzar.lockruna.server.audit.domain.AuditAction
import com.kwazzar.lockruna.server.audit.domain.AuditEvent
import com.kwazzar.lockruna.server.audit.domain.AuditEventRepository
import com.kwazzar.lockruna.contract.KdfParamsDto
import com.kwazzar.lockruna.server.identity.domain.ACCESS_TOKEN_SECONDS
import com.kwazzar.lockruna.server.identity.domain.AuthKeys
import com.kwazzar.lockruna.server.identity.domain.DeviceRecord
import com.kwazzar.lockruna.server.identity.domain.DeviceRepository
import com.kwazzar.lockruna.server.identity.domain.EmailAlreadyRegisteredException
import com.kwazzar.lockruna.server.identity.domain.RateLimiter
import com.kwazzar.lockruna.server.identity.domain.TokenService
import com.kwazzar.lockruna.server.identity.domain.UserRecord
import com.kwazzar.lockruna.server.identity.domain.UserRepository
import com.kwazzar.lockruna.server.platform.config.ServerSettings
import com.kwazzar.lockruna.server.platform.db.TransactionRunner
import com.kwazzar.lockruna.server.platform.error.Problem
import com.kwazzar.lockruna.server.platform.error.ProblemException
import kotlinx.serialization.json.Json
import java.time.Clock
import java.time.Duration
import java.util.UUID

/**
 * Creates the account, its first device and the session tokens in one transaction.
 */
class RegisterUser(
    private val users: UserRepository,
    private val devices: DeviceRepository,
    private val tokens: TokenService,
    private val rateLimiter: RateLimiter,
    private val transactions: TransactionRunner,
    private val settings: ServerSettings,
    private val audit: AuditEventRepository,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend operator fun invoke(command: RegisterCommand): AuthSession {
        val email = command.email.trim().lowercase()
        rateLimiter.requireAllowed("register:email", AuthKeys.emailKey(email), REGISTER_EMAIL_LIMIT, REGISTER_WINDOW)
        rateLimiter.requireAllowed("register:ip", command.clientIp, REGISTER_IP_LIMIT, REGISTER_WINDOW)
        if (users.findByEmail(email) != null) throw alreadyRegistered()

        return try {
            val session = transactions.transaction {
                val now = clock.instant()
                val user = users.create(
                    UserRecord(
                        id = UUID.randomUUID().toString(),
                        email = email,
                        authHash = AuthKeys.hash(settings.pepper, command.authKey),
                        recoveryAuthHash = AuthKeys.hash(settings.pepper, command.recoveryAuthKey),
                        kdfSalt = command.kdfSalt,
                        kdfParams = Json.encodeToString(KdfParamsDto.serializer(), command.kdfParams),
                        wrappedVaultKey = command.wrappedVaultKey,
                        recoveryWrappedVaultKey = command.recoveryWrappedVaultKey,
                        syncSeq = 0,
                    ),
                )
                val device = devices.create(
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
                val accessToken = tokens.issueAccessToken(user.id, device.id)
                val refreshToken = tokens.issueRefreshToken(user.id, device.id, UUID.randomUUID().toString(), now)

                AuthSession(
                    userId = user.id,
                    deviceId = device.id,
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    expiresIn = ACCESS_TOKEN_SECONDS,
                    kdfSalt = user.kdfSalt,
                    kdfParams = command.kdfParams,
                    wrappedVaultKey = user.wrappedVaultKey,
                )
            }
            audit.record(AuditEvent(
                userId = session.userId,
                deviceId = session.deviceId,
                action = AuditAction.REGISTER,
                ip = command.clientIp,
                ts = clock.instant().toEpochMilli(),
            ))
            session
        } catch (ignored: EmailAlreadyRegisteredException) {
            throw alreadyRegistered()
        }
    }

    private fun alreadyRegistered() = ProblemException(
        problem = Problem(status = 409, code = "email_already_registered", title = "Email already registered"),
    )
}

data class RegisterCommand(
    val email: String,
    val authKey: ByteArray,
    val recoveryAuthKey: ByteArray,
    val kdfSalt: ByteArray,
    val kdfParams: KdfParamsDto,
    val wrappedVaultKey: ByteArray,
    val recoveryWrappedVaultKey: ByteArray,
    val deviceName: String,
    val devicePlatform: String,
    val clientIp: String,
)

private const val REGISTER_EMAIL_LIMIT = 3
private const val REGISTER_IP_LIMIT = 10
private val REGISTER_WINDOW: Duration = Duration.ofHours(1)
