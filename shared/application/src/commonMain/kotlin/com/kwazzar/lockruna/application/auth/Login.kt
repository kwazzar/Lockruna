package com.kwazzar.lockruna.application.auth

import com.kwazzar.lockruna.domain.auth.AuthApi
import com.kwazzar.lockruna.domain.auth.AuthStateStore
import com.kwazzar.lockruna.domain.auth.AuthenticatedUser
import com.kwazzar.lockruna.domain.auth.DeviceIdentity
import com.kwazzar.lockruna.domain.auth.DevicePlatform
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.crypto.Argon2
import com.kwazzar.lockruna.domain.crypto.CryptoService
import com.kwazzar.lockruna.domain.crypto.PasswordNormalizer
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.VaultKey
import com.kwazzar.lockruna.domain.session.VaultSession

data class CredentialsCommand(
    val email: String,
    val password: String,
    val deviceName: String,
    val deviceId: String?
)

class Login(
    private val authApi: AuthApi,
    private val argon2: Argon2,
    private val normalizer: PasswordNormalizer,
    private val crypto: CryptoService,
    private val authStateStore: AuthStateStore,
    private val session: VaultSession
) {
    suspend operator fun invoke(command: CredentialsCommand): Result<Unit, AppError> {
        val preloginResult = authApi.prelogin(command.email)
        val preloginData = when (preloginResult) {
            is Result.Failure -> return preloginResult
            is Result.Success -> preloginResult.value
        }
        if (!com.kwazzar.lockruna.domain.crypto.KdfParams.isAcceptable(preloginData.kdfParams)) {
            return Result.Failure(AppError.WeakKdfParams(preloginData.kdfParams.toString()))
        }

        val masterKey = KeyDerivation.normalizeAndDerive(
            command.password,
            preloginData.kdfSalt,
            argon2,
            normalizer
        )
        val authKey = KeyDerivation.authKey(masterKey, crypto)

        val device = DeviceIdentity(
            deviceId = command.deviceId ?: "unknown",
            name = command.deviceName,
            platform = DevicePlatform.ANDROID
        )

        val loginResult = authApi.login(command.email, authKey, device)
        val user = when (loginResult) {
            is Result.Failure -> return loginResult
            is Result.Success -> loginResult.value
        }
        val kek = KeyDerivation.kek(masterKey, crypto)
        val vaultKeyBytes = KeyDerivation.unwrapVaultKey(kek, user.wrappedVaultKey, command.email, crypto)

        authStateStore.saveSession(user)
        session.unlock(VaultKey.from(vaultKeyBytes))

        return Result.Success(Unit)
    }
}