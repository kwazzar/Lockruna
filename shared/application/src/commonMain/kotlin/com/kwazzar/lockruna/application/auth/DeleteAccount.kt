package com.kwazzar.lockruna.application.auth

import com.kwazzar.lockruna.application.identity.ValidateMasterPassword
import com.kwazzar.lockruna.domain.auth.AccountApi
import com.kwazzar.lockruna.domain.auth.AuthStateStore
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.crypto.Argon2
import com.kwazzar.lockruna.domain.crypto.CryptoService
import com.kwazzar.lockruna.domain.crypto.PasswordNormalizer
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.VaultSession

/**
 * Deletes the account (US-11). The master password is required again, and the local
 * session dies with the server-side account.
 */
class DeleteAccount(
    private val accountApi: AccountApi,
    private val argon2: Argon2,
    private val normalizer: PasswordNormalizer,
    private val crypto: CryptoService,
    private val authStateStore: AuthStateStore,
    private val session: VaultSession,
) {
    suspend operator fun invoke(email: String, password: String): Result<Unit, AppError> {
        val passwordValidation = ValidateMasterPassword()(password)
        if (passwordValidation is Result.Failure) return passwordValidation

        val authKey = when (val result = authKeyFor(email, password)) {
            is Result.Failure -> return result
            is Result.Success -> result.value
        }

        return when (val result = accountApi.delete(authKey)) {
            is Result.Failure -> result
            is Result.Success -> {
                authStateStore.clear()
                session.lock()
                Result.Success(Unit)
            }
        }
    }

    private suspend fun authKeyFor(email: String, password: String): Result<ByteArray, AppError> {
        val session = authStateStore.loadSession() ?: return Result.Failure(AppError.Unauthorized)
        val masterKey = KeyDerivation.normalizeAndDerive(
            password,
            session.kdfSalt,
            argon2,
            normalizer,
        )
        return Result.Success(KeyDerivation.authKey(masterKey, crypto))
    }
}
