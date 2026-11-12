package com.kwazzar.lockruna.data.session

import com.kwazzar.lockruna.data.platform.SecureStorage
import com.kwazzar.lockruna.domain.auth.BiometricPolicyStore

/**
 * The biometric unlock policy.
 *
 * The failure count lives in the platform's protected storage, so five failed attempts
 * cannot be cleared by restarting the app. The last successful unlock stays in memory:
 * US-07 asks for the master password after every restart, and a persisted timestamp
 * would open the biometric path without it.
 */
class SecureBiometricPolicyStore(private val storage: SecureStorage) : BiometricPolicyStore {
    private var lastSuccess: Long? = null

    override suspend fun failures(): Int = storage.read(FAILURES_KEY)?.decodeToString()?.toIntOrNull() ?: 0

    override suspend fun recordFailure() {
        storage.write(FAILURES_KEY, (failures() + 1).toString().encodeToByteArray())
    }

    override suspend fun resetFailures() {
        storage.write(FAILURES_KEY, "0".encodeToByteArray())
    }

    override suspend fun setLastSuccessfulUnlock(millis: Long) {
        lastSuccess = millis
    }

    override suspend fun lastSuccessfulUnlock(): Long? = lastSuccess

    private companion object {
        const val FAILURES_KEY = "biometric_failures"
    }
}

/** A store that remembers nothing, for tools and tests that have no platform storage. */
class FixedBiometricPolicyStore : BiometricPolicyStore {
    private var failures = 0
    private var lastSuccess: Long? = null

    override suspend fun failures(): Int = failures

    override suspend fun recordFailure() {
        failures++
    }

    override suspend fun resetFailures() {
        failures = 0
    }

    override suspend fun setLastSuccessfulUnlock(millis: Long) {
        lastSuccess = millis
    }

    override suspend fun lastSuccessfulUnlock(): Long? = lastSuccess
}
