package com.kwazzar.lockruna.application.auth

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.BiometricGate
import com.kwazzar.lockruna.domain.session.SecureKeyStore

/**
 * Whether biometric unlock is usable: the device can do it, and whether the user turned it on.
 * Turning it on stores the vault key, so a stored key is the "on" signal (US-07).
 */
data class BiometricUnlockState(val available: Boolean, val enabled: Boolean)

class ObserveBiometricUnlock(private val biometricGate: BiometricGate, private val secureKeyStore: SecureKeyStore) {
    suspend operator fun invoke(): BiometricUnlockState = BiometricUnlockState(
        available = biometricGate.isAvailable(),
        enabled = secureKeyStore.loadVaultKey() != null,
    )
}

/** Turns biometric unlock off: the stored key is dropped (US-07). */
class DisableBiometrics(private val secureKeyStore: SecureKeyStore) {
    suspend operator fun invoke(): Result<Unit, AppError> {
        secureKeyStore.clear()
        return Result.Success(Unit)
    }
}
