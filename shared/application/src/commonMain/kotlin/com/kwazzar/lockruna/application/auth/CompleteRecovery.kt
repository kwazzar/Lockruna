package com.kwazzar.lockruna.application.auth

import com.kwazzar.lockruna.application.identity.ValidateMasterPassword
import com.kwazzar.lockruna.domain.auth.AuthStateStore
import com.kwazzar.lockruna.domain.auth.DeviceIdentity
import com.kwazzar.lockruna.domain.auth.DevicePlatform
import com.kwazzar.lockruna.domain.auth.RecoveryApi
import com.kwazzar.lockruna.domain.auth.SealedBlob
import com.kwazzar.lockruna.domain.auth.canonicalEmail
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.crypto.Argon2
import com.kwazzar.lockruna.domain.crypto.Bip39
import com.kwazzar.lockruna.domain.crypto.CryptoService
import com.kwazzar.lockruna.domain.crypto.HkdfLabels
import com.kwazzar.lockruna.domain.crypto.KdfParams
import com.kwazzar.lockruna.domain.crypto.PasswordNormalizer
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.IdGenerator
import com.kwazzar.lockruna.domain.session.VaultKey
import com.kwazzar.lockruna.domain.session.VaultSession

data class RecoverCommand(
    val email: String,
    val recoveryWords: List<String>,
    val newPassword: String,
    val deviceName: String,
    val deviceId: String?,
    val platform: DevicePlatform,
) {
    override fun toString(): String =
        "RecoverCommand(email=$email, deviceName=$deviceName, deviceId=$deviceId, platform=$platform)"
}

/**
 * Restores access with the recovery key: the vault key comes back from the recovery wrapper,
 * a new master password replaces the old one and every other device is revoked by the server.
 */
class CompleteRecovery(
    private val recoveryApi: RecoveryApi,
    private val argon2: Argon2,
    private val normalizer: PasswordNormalizer,
    private val crypto: CryptoService,
    private val authStateStore: AuthStateStore,
    private val session: VaultSession,
    private val idGenerator: IdGenerator,
) {
    suspend operator fun invoke(command: RecoverCommand): Result<Unit, AppError> {
        val email = canonicalEmail(command.email)

        // The new password is local input: reject it before any request leaves the device.
        val passwordValidation = ValidateMasterPassword()(command.newPassword)
        if (passwordValidation is Result.Failure) return passwordValidation

        val recoveryKey = when (val decoded = Bip39.decode(command.recoveryWords, crypto)) {
            is Result.Failure -> return decoded
            is Result.Success -> decoded.value
        }

        val started = when (val result = recoveryApi.start(email)) {
            is Result.Failure -> return result
            is Result.Success -> result.value
        }

        val recoveryWrapKey = crypto.hkdfSha256(recoveryKey, null, HkdfLabels.RECOVERY_WRAP, HKDF_OUTPUT_LENGTH)
        val vaultKeyBytes = KeyDerivation.unwrapVaultKey(
            kek = recoveryWrapKey,
            blob = started.recoveryWrappedVaultKey,
            aadEmail = email,
            crypto = crypto,
        ) ?: return Result.Failure(AppError.InvalidCredentials)

        val newKdfSalt = crypto.randomBytes(KdfParams.DEFAULT_SALT_LENGTH)
        val masterKey = KeyDerivation.normalizeAndDerive(command.newPassword, newKdfSalt, argon2, normalizer)
        val newAuthKey = KeyDerivation.authKey(masterKey, crypto)
        val newKek = KeyDerivation.kek(masterKey, crypto)
        val newWrappedVaultKey = wrapKey(newKek, vaultKeyBytes, email, crypto)
        val recoveryAuthKey = crypto.hkdfSha256(recoveryKey, null, HkdfLabels.RECOVERY_AUTH, HKDF_OUTPUT_LENGTH)

        val device = DeviceIdentity(
            deviceId = command.deviceId ?: idGenerator.newUuid(),
            name = command.deviceName,
            platform = command.platform,
        )

        val user = when (
            val result = recoveryApi.complete(
                email = email,
                recoveryAuthKey = recoveryAuthKey,
                newAuthKey = newAuthKey,
                newKdfSalt = newKdfSalt,
                newKdfParams = KdfParams.SUPPORTED,
                newWrappedVaultKey = newWrappedVaultKey,
                device = device,
            )
        ) {
            is Result.Failure -> return result
            is Result.Success -> result.value
        }

        authStateStore.saveSession(user)
        session.unlock(VaultKey.from(vaultKeyBytes))
        return Result.Success(Unit)
    }

    private fun wrapKey(key: ByteArray, plaintext: ByteArray, aad: String, crypto: CryptoService): ByteArray {
        val nonce = crypto.randomBytes(12)
        val ciphertext = crypto.encryptGcm(key, plaintext, aad.encodeToByteArray())
        return SealedBlob.pack(nonce, ciphertext.ciphertext + ciphertext.tag)
    }
}

private const val HKDF_OUTPUT_LENGTH = 32
