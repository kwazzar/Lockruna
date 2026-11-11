package com.kwazzar.lockruna.application.auth

import com.kwazzar.lockruna.application.identity.ValidateMasterPassword
import com.kwazzar.lockruna.domain.auth.DeviceIdentity
import com.kwazzar.lockruna.domain.auth.DevicePlatform
import com.kwazzar.lockruna.domain.auth.SealedBlob
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.crypto.CryptoService
import com.kwazzar.lockruna.domain.crypto.HkdfLabels
import com.kwazzar.lockruna.domain.crypto.KdfParams
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.IdGenerator
import com.kwazzar.lockruna.domain.session.VaultKey
import com.kwazzar.lockruna.domain.session.VaultSession

data class RegisterCommand(val email: String, val password: String, val deviceName: String, val deviceId: String?)

data class RegisterOutcome(val recoveryKey: ByteArray)

class Register(
    private val authApi: com.kwazzar.lockruna.domain.auth.AuthApi,
    private val argon2: com.kwazzar.lockruna.domain.crypto.Argon2,
    private val normalizer: com.kwazzar.lockruna.domain.crypto.PasswordNormalizer,
    private val crypto: CryptoService,
    private val authStateStore: com.kwazzar.lockruna.domain.auth.AuthStateStore,
    private val session: VaultSession,
    private val idGenerator: IdGenerator,
) {
    suspend operator fun invoke(command: RegisterCommand): Result<RegisterOutcome, AppError> {
        val validatePassword = ValidateMasterPassword()
        val passwordValidation = validatePassword(command.password)
        when (passwordValidation) {
            is Result.Failure -> return passwordValidation
            is Result.Success -> Unit
        }

        val kdfSalt = crypto.randomBytes(16)
        val vaultKey = VaultKey.random(crypto)
        val recoveryKey = crypto.randomBytes(16)

        val masterKey = KeyDerivation.normalizeAndDerive(
            command.password,
            kdfSalt,
            argon2,
            normalizer,
        )
        val authKey = KeyDerivation.authKey(masterKey, crypto)
        val kek = KeyDerivation.kek(masterKey, crypto)

        val recoveryWrapKey = crypto.hkdfSha256(recoveryKey, null, HkdfLabels.RECOVERY_WRAP, 32)
        val recoveryAuthKey = crypto.hkdfSha256(recoveryKey, null, HkdfLabels.RECOVERY_AUTH, 32)

        val emailBytes = command.email.encodeToByteArray()

        val wrappedVaultKey = wrapKey(kek, vaultKey.bytes, emailBytes, crypto)
        val recoveryWrappedVaultKey = wrapKey(recoveryWrapKey, vaultKey.bytes, emailBytes, crypto)

        val device = DeviceIdentity(
            deviceId = command.deviceId ?: idGenerator.newUuid(),
            name = command.deviceName,
            platform = DevicePlatform.ANDROID,
        )

        val registerResult = authApi.register(
            email = command.email,
            authKey = authKey,
            recoveryAuthKey = recoveryAuthKey,
            kdfSalt = kdfSalt,
            kdfParams = KdfParams.SUPPORTED,
            wrappedVaultKey = wrappedVaultKey,
            recoveryWrappedVaultKey = recoveryWrappedVaultKey,
            device = device,
        )

        val user = when (registerResult) {
            is Result.Failure -> return registerResult
            is Result.Success -> registerResult.value
        }

        // Unwrap and unlock session
        val unwrappedVaultKey = KeyDerivation.unwrapVaultKey(kek, user.wrappedVaultKey, command.email, crypto)
        authStateStore.saveSession(user)
        session.unlock(VaultKey.from(unwrappedVaultKey))

        return Result.Success(RegisterOutcome(recoveryKey))
    }

    private fun wrapKey(key: ByteArray, plaintext: ByteArray, aad: ByteArray, crypto: CryptoService): ByteArray {
        val nonce = crypto.randomBytes(12)
        val ciphertext = crypto.encryptGcm(key, plaintext, aad)
        return SealedBlob.pack(nonce, ciphertext.ciphertext + ciphertext.tag)
    }
}
