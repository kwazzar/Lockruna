package com.kwazzar.lockruna.server.identity.application

import com.kwazzar.lockruna.contract.KdfParamsDto
import com.kwazzar.lockruna.server.identity.domain.AuthKeys
import com.kwazzar.lockruna.server.identity.domain.UserRepository
import kotlinx.serialization.json.Json

/**
 * KDF parameters for an email. Unknown emails get deterministic fake values of the same shape,
 * so the answer never reveals whether the account exists.
 */
class PreloginUser(private val users: UserRepository, private val authFakeSecret: ByteArray) {
    suspend operator fun invoke(email: String): PreloginResult {
        val user = users.findByEmail(email)
        return if (user == null) {
            PreloginResult(
                kdfSalt = AuthKeys.hash(authFakeSecret, email.trim().lowercase().toByteArray()).copyOf(FAKE_SALT_BYTES),
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
private val DEFAULT_KDF_PARAMS = KdfParamsDto(memoryKiB = 65536, iterations = 3)
