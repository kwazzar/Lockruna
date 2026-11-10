package com.kwazzar.lockruna.application.auth

import com.kwazzar.lockruna.domain.auth.BiometricPolicyStore
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.BiometricGate
import com.kwazzar.lockruna.domain.session.Clock
import com.kwazzar.lockruna.domain.session.SecureKeyStore
import com.kwazzar.lockruna.domain.session.VaultSession

class UnlockWithBiometrics(
    private val biometricGate: BiometricGate,
    private val secureKeyStore: SecureKeyStore,
    private val biometricPolicyStore: BiometricPolicyStore,
    private val session: VaultSession,
    private val clock: Clock
) {
    suspend operator fun invoke(reason: String): Result<Unit, AppError> {
        // Gate 1: 5 failed attempts in this process
        if (biometricPolicyStore.failures() >= 5) {
            return Result.Failure(AppError.InvalidCredentials)
        }

        // Gate 2: 7-day gate
        val lastSuccess = biometricPolicyStore.lastSuccessfulUnlock()
        val now = clock.nowMillis()
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
        if (lastSuccess == null || now - lastSuccess > sevenDaysMs) {
            return Result.Failure(AppError.InvalidCredentials)
        }

        // Gate 3: Biometric authentication
        val authenticated = biometricGate.authenticate(reason)
        if (!authenticated) {
            biometricPolicyStore.recordFailure()
            return Result.Failure(AppError.InvalidCredentials)
        }

        // Gate 4: Load vault key from secure store
        val vaultKey = secureKeyStore.loadVaultKey() ?: return Result.Failure(AppError.Unauthorized)

        // Success: reset failures, update last success, unlock session
        biometricPolicyStore.resetFailures()
        biometricPolicyStore.setLastSuccessfulUnlock(now)
        session.unlock(vaultKey)

        return Result.Success(Unit)
    }
}