package com.kwazzar.lockruna.application.auth

import com.kwazzar.lockruna.domain.auth.BiometricPolicyStore
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.BiometricGate
import com.kwazzar.lockruna.domain.session.SecureKeyStore
import com.kwazzar.lockruna.domain.session.VaultSession

class EnableBiometrics(
    private val biometricGate: BiometricGate,
    private val secureKeyStore: SecureKeyStore,
    private val session: VaultSession
) {
    suspend operator fun invoke(reason: String): Result<Unit, AppError> {
        val isAvailable = biometricGate.isAvailable()
        if (!isAvailable) return Result.Failure(AppError.BiometricsUnavailable)

        val authenticated = biometricGate.authenticate(reason)
        if (!authenticated) return Result.Failure(AppError.InvalidCredentials)

        val vaultKey = session.currentVaultKey() ?: return Result.Failure(AppError.VaultLocked)

        secureKeyStore.saveVaultKey(vaultKey)

        return Result.Success(Unit)
    }
}