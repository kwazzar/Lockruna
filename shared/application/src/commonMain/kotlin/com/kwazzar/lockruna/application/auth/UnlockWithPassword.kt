package com.kwazzar.lockruna.application.auth

import com.kwazzar.lockruna.domain.auth.AuthStateStore
import com.kwazzar.lockruna.domain.auth.BiometricPolicyStore
import com.kwazzar.lockruna.domain.auth.canonicalEmail
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.crypto.Argon2
import com.kwazzar.lockruna.domain.crypto.CryptoService
import com.kwazzar.lockruna.domain.crypto.PasswordNormalizer
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.Clock
import com.kwazzar.lockruna.domain.session.VaultKey
import com.kwazzar.lockruna.domain.session.VaultSession

class UnlockWithPassword(
    private val argon2: Argon2,
    private val normalizer: PasswordNormalizer,
    private val crypto: CryptoService,
    private val authStateStore: AuthStateStore,
    private val biometricPolicyStore: BiometricPolicyStore,
    private val session: VaultSession,
    private val clock: Clock,
) {
    suspend operator fun invoke(email: String, password: String): Result<Unit, AppError> {
        val user = authStateStore.loadSession() ?: return Result.Failure(AppError.Unauthorized)

        val masterKey = KeyDerivation.normalizeAndDerive(
            password,
            user.kdfSalt,
            argon2,
            normalizer,
        )
        val kek = KeyDerivation.kek(masterKey, crypto)
        val vaultKeyBytes = KeyDerivation.unwrapVaultKey(kek, user.wrappedVaultKey, canonicalEmail(email), crypto)
            ?: return Result.Failure(AppError.InvalidCredentials)

        session.unlock(VaultKey.from(vaultKeyBytes))
        // The master password re-arms the biometric path: five failed attempts and the
        // seven-day window start over (US-07).
        biometricPolicyStore.resetFailures()
        biometricPolicyStore.setLastSuccessfulUnlock(clock.nowMillis())

        return Result.Success(Unit)
    }
}
