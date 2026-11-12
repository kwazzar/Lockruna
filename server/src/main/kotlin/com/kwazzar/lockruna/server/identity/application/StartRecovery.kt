package com.kwazzar.lockruna.server.identity.application

import com.kwazzar.lockruna.server.identity.domain.AuthKeys
import com.kwazzar.lockruna.server.identity.domain.RateLimiter
import com.kwazzar.lockruna.server.identity.domain.UserRepository
import java.time.Duration
import java.util.UUID

/**
 * Starts a recovery flow. Unknown emails get deterministic fake values of the same shape,
 * so the answer never reveals whether the account exists.
 */
class StartRecovery(
    private val users: UserRepository,
    private val rateLimiter: RateLimiter,
    private val authFakeSecret: ByteArray,
) {
    suspend operator fun invoke(email: String, clientIp: String): RecoveryStartResult {
        val normalized = email.trim().lowercase()
        rateLimiter.requireAllowed("recovery:email", AuthKeys.emailKey(normalized), RECOVERY_LIMIT, RECOVERY_WINDOW)
        rateLimiter.requireAllowed("recovery:ip", clientIp, RECOVERY_LIMIT, RECOVERY_WINDOW)

        val user = users.findByEmail(normalized)
        return if (user == null) {
            RecoveryStartResult(
                userId = UUID.nameUUIDFromBytes(AuthKeys.hash(authFakeSecret, normalized.toByteArray())).toString(),
                recoveryWrappedVaultKey = AuthKeys.hash(
                    authFakeSecret,
                    ("recovery-wrap:" + normalized).toByteArray(),
                ).copyOf(FAKE_WRAPPED_BYTES),
            )
        } else {
            RecoveryStartResult(
                userId = user.id,
                recoveryWrappedVaultKey = user.recoveryWrappedVaultKey,
            )
        }
    }
}

data class RecoveryStartResult(val userId: String, val recoveryWrappedVaultKey: ByteArray)

private const val RECOVERY_LIMIT = 5
private const val FAKE_WRAPPED_BYTES = 48
private val RECOVERY_WINDOW: Duration = Duration.ofMinutes(15)
