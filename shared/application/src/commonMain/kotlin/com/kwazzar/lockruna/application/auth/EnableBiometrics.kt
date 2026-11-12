package com.kwazzar.lockruna.application.auth

import com.kwazzar.lockruna.domain.auth.BiometricPolicyStore
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.BiometricGate
import com.kwazzar.lockruna.domain.session.Clock
import com.kwazzar.lockruna.domain.session.SecureKeyStore
import com.kwazzar.lockruna.domain.session.VaultSession

/**
 * Enables biometric unlock for the current vault key.
 *
 * The successful unlock is recorded in the policy store, otherwise the first biometric
 * unlock would fail the seven-day gate before any biometric unlock ever happened.
 */
class EnableBiometrics(
    private val biometricGate: BiometricGate,
    private val secureKeyStore: SecureKeyStore,
    private val biometricPolicyStore: BiometricPolicyStore,
    private val session: VaultSession,
    private val clock: Clock,
) {
    suspend operator fun invoke(reason: String): Result<Unit, AppError> {
        val isAvailable = biometricGate.isAvailable()
        if (!isAvailable) return Result.Failure(AppError.BiometricsUnavailable)

        val authenticated = biometricGate.authenticate(reason)
        if (!authenticated) return Result.Failure(AppError.InvalidCredentials)

        val vaultKey = session.currentVaultKey() ?: return Result.Failure(AppError.VaultLocked)

        secureKeyStore.saveVaultKey(vaultKey)
        biometricPolicyStore.resetFailures()
        biometricPolicyStore.setLastSuccessfulUnlock(clock.nowMillis())

        return Result.Success(Unit)
    }
}