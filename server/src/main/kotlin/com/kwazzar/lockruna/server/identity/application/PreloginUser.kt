package com.kwazzar.lockruna.server.identity.application

import com.kwazzar.lockruna.contract.KdfParamsDto
import com.kwazzar.lockruna.server.identity.domain.AuthKeys
import com.kwazzar.lockruna.server.identity.domain.RateLimiter
import com.kwazzar.lockruna.server.identity.domain.UserRepository
import kotlinx.serialization.json.Json
import java.time.Duration

/**
 * KDF parameters for an email. Unknown emails get deterministic fake values of the same shape,
 * so the answer never reveals whether the account exists.
 */
class PreloginUser(
    private val users: UserRepository,
    private val rateLimiter: RateLimiter,
    private val authFakeSecret: ByteArray,
) {
    suspend operator fun invoke(email: String, clientIp: String): PreloginResult {
        val normalized = email.trim().lowercase()
        rateLimiter.requireAllowed(
            "prelogin:email",
            AuthKeys.emailKey(normalized),
            PRELOGIN_EMAIL_LIMIT,
            PRELOGIN_WINDOW,
        )
        rateLimiter.requireAllowed("prelogin:ip", clientIp, PRELOGIN_IP_LIMIT, PRELOGIN_WINDOW)

        val user = users.findByEmail(normalized)
        return if (user == null) {
            PreloginResult(
                kdfSalt = AuthKeys.hash(authFakeSecret, normalized.toByteArray()).copyOf(FAKE_SALT_BYTES),
                kdfParams = DEFAULT_KDF_PARAMS,
            )
        } else {
            PreloginResult(
                kdfSalt = user.kdfSalt,
                kdfParams = Json.decodeFromString(KdfParamsDto.serializer(), user.kdfParams),
            )
        }
    }
}

data class PreloginResult(val kdfSalt: ByteArray, val kdfParams: KdfParamsDto)

private const val FAKE_SALT_BYTES = 16
private const val PRELOGIN_EMAIL_LIMIT = 30
private const val PRELOGIN_IP_LIMIT = 120
private val PRELOGIN_WINDOW: Duration = Duration.ofHours(1)
private val DEFAULT_KDF_PARAMS = KdfParamsDto(memoryKiB = 65536, iterations = 3)
