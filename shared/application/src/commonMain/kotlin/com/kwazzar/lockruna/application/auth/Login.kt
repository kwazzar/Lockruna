package com.kwazzar.lockruna.application.auth

import com.kwazzar.lockruna.domain.auth.AuthApi
import com.kwazzar.lockruna.domain.auth.AuthStateStore
import com.kwazzar.lockruna.domain.auth.DeviceIdentity
import com.kwazzar.lockruna.domain.auth.DevicePlatform
import com.kwazzar.lockruna.domain.auth.PreLoginData
import com.kwazzar.lockruna.domain.auth.canonicalEmail
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.crypto.Argon2
import com.kwazzar.lockruna.domain.crypto.CryptoService
import com.kwazzar.lockruna.domain.crypto.KdfParams
import com.kwazzar.lockruna.domain.crypto.PasswordNormalizer
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.VaultKey
import com.kwazzar.lockruna.domain.session.VaultSession

data class CredentialsCommand(
    val email: String,
    val password: String,
    val deviceName: String,
    val deviceId: String?,
    val platform: DevicePlatform,
) {
    override fun toString(): String =
        "CredentialsCommand(email=$email, deviceName=$deviceName, deviceId=$deviceId, platform=$platform)"
}

class Login(
    private val authApi: AuthApi,
    private val argon2: Argon2,
    private val normalizer: PasswordNormalizer,
    private val crypto: CryptoService,
    private val authStateStore: AuthStateStore,
    private val session: VaultSession,
) {
    suspend operator fun invoke(command: CredentialsCommand): Result<Unit, AppError> {
        val email = canonicalEmail(command.email)
        val preloginData = when (val result = prelogin(email)) {
            is Result.Failure -> return result
            is Result.Success -> result.value
        }

        val masterKey = KeyDerivation.normalizeAndDerive(
            command.password,
            preloginData.kdfSalt,
            argon2,
            normalizer,
        )
        val authKey = KeyDerivation.authKey(masterKey, crypto)

        val device = DeviceIdentity(
            deviceId = command.deviceId ?: authStateStore.loadSession()?.deviceId,
            name = command.deviceName,
            platform = command.platform,
        )

        val loginResult = authApi.login(email, authKey, device)
        val user = when (loginResult) {
            is Result.Failure -> return loginResult
            is Result.Success -> loginResult.value
        }
        val kek = KeyDerivation.kek(masterKey, crypto)
        val vaultKeyBytes = KeyDerivation.unwrapVaultKey(kek, user.wrappedVaultKey, email, crypto)
            ?: return Result.Failure(AppError.InvalidCredentials)

        authStateStore.saveSession(user)
        session.unlock(VaultKey.from(vaultKeyBytes))

        return Result.Success(Unit)
    }

    private suspend fun prelogin(email: String): Result<PreLoginData, AppError> {
        val data = when (val result = authApi.prelogin(email)) {
            is Result.Failure -> return result
            is Result.Success -> result.value
        }
        if (!KdfParams.isAcceptable(data.kdfParams)) {
            return Result.Failure(AppError.WeakKdfParams(data.kdfParams.toString()))
        }
        return Result.Success(data)
    }
}
