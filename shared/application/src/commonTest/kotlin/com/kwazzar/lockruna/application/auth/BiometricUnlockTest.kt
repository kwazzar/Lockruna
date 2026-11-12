package com.kwazzar.lockruna.application.auth

import com.kwazzar.lockruna.application.support.FakeBiometricGate
import com.kwazzar.lockruna.application.support.FakeBiometricPolicyStore
import com.kwazzar.lockruna.application.support.FakeClock
import com.kwazzar.lockruna.application.support.FakeSecureKeyStore
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.VaultKey
import com.kwazzar.lockruna.domain.session.VaultSession
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private val <T, E> Result<T, E>.isSuccess: Boolean
    get() = this is Result.Success

private val <T, E> Result<T, E>.isFailure: Boolean
    get() = this is Result.Failure

private val <T, E> Result<T, E>.error: E?
    get() = if (this is Result.Failure) this.error else null

class EnableBiometricsTest {

    private val biometricGate = FakeBiometricGate()
    private val secureKeyStore = FakeSecureKeyStore()
    private val policyStore = FakeBiometricPolicyStore()
    private val session = VaultSession()
    private val clock = FakeClock(wallMillis = 1_700_000_000_000L)

    @Test
    fun biometricsUnavailableWhenGateNotAvailable() {
        runBlocking {
            biometricGate.isAvailableResult = false

            val enable = enable()
            val result = enable("reason")

            assertTrue(result.isFailure)
            assertEquals(AppError.BiometricsUnavailable, result.error)
        }
    }

    @Test
    fun biometricsUnavailableWhenAuthenticationFails() {
        runBlocking {
            biometricGate.authenticateResult = false

            val enable = enable()
            val result = enable("reason")

            assertTrue(result.isFailure)
            assertEquals(AppError.InvalidCredentials, result.error)
        }
    }

    @Test
    fun failsWhenSessionLocked() {
        runBlocking {
            biometricGate.isAvailableResult = true
            biometricGate.authenticateResult = true

            val enable = enable()
            val result = enable("reason")

            assertTrue(result.isFailure)
            assertEquals(AppError.VaultLocked, result.error)
        }
    }

    @Test
    fun successSavesVaultKey() {
        runBlocking {
            biometricGate.isAvailableResult = true
            biometricGate.authenticateResult = true
            session.unlock(VaultKey.from(ByteArray(32)))

            val enable = enable()
            val result = enable("reason")

            assertTrue(result.isSuccess)
            assertNotNull(secureKeyStore.storedKey)
        }
    }

    @Test
    fun enablingRecordsTheUnlockSoTheFirstBiometricUnlockPassesTheGate() {
        runBlocking {
            biometricGate.isAvailableResult = true
            biometricGate.authenticateResult = true
            session.unlock(VaultKey.from(ByteArray(32)))

            val enable = enable()
            val result = enable("reason")

            assertTrue(result.isSuccess)
            // Without this the very first biometric unlock fails before the user ever tried it.
            assertEquals(clock.nowMillis(), policyStore.lastSuccessfulUnlock())
            assertEquals(0, policyStore.failures())
        }
    }

    private fun enable() = EnableBiometrics(biometricGate, secureKeyStore, policyStore, session, clock)
}

class UnlockWithBiometricsTest {

    private val biometricGate = FakeBiometricGate()
    private val secureKeyStore = FakeSecureKeyStore()
    private val policyStore = FakeBiometricPolicyStore()
    private val session = VaultSession()
    private val clock = FakeClock()

    @Test
    fun failsWhenTheMasterPasswordWasNeverEntered() = runBlocking {
        // US-07: after a restart the master password is required, so the biometric path
        // stays closed until the password has been entered in this run.
        biometricGate.authenticateResult = true
        secureKeyStore.storedKey = VaultKey.from(ByteArray(32))

        val unlock = UnlockWithBiometrics(biometricGate, secureKeyStore, policyStore, session, clock)
        val result = unlock("reason")

        assertTrue(result.isFailure)
        assertEquals(AppError.InvalidCredentials, result.error)
    }

    @Test
    fun failsWhenFailureCountExceeds5() {
        runBlocking {
            policyStore.recordFailure()
            policyStore.recordFailure()
            policyStore.recordFailure()
            policyStore.recordFailure()
            policyStore.recordFailure()

            val unlock = UnlockWithBiometrics(biometricGate, secureKeyStore, policyStore, session, clock)
            val result = unlock("reason")

            assertTrue(result.isFailure)
            assertEquals(AppError.InvalidCredentials, result.error)
        }
    }

    @Test
    fun failsWhenLastSuccessOlderThan7Days() {
        runBlocking {
            clock.wallMillis = 1_700_000_000_000L
            val eightDaysAgo = clock.nowMillis() - 8L * 24 * 60 * 60 * 1000
            policyStore.setLastSuccessfulUnlock(eightDaysAgo)

            val unlock = UnlockWithBiometrics(biometricGate, secureKeyStore, policyStore, session, clock)
            val result = unlock("reason")

            assertTrue(result.isFailure)
            assertEquals(AppError.InvalidCredentials, result.error)
        }
    }

    @Test
    fun failsWhenBiometricAuthenticationFails() {
        runBlocking {
            // Set recent last success to pass 7-day gate
            policyStore.setLastSuccessfulUnlock(clock.nowMillis() - 1000)
            biometricGate.authenticateResult = false

            val unlock = UnlockWithBiometrics(biometricGate, secureKeyStore, policyStore, session, clock)
            val result = unlock("reason")

            assertTrue(result.isFailure)
            assertEquals(AppError.InvalidCredentials, result.error)
            assertEquals(1, policyStore.failures())
        }
    }

    @Test
    fun failsWhenNoKeyInSecureStore() {
        runBlocking {
            // Set recent last success to pass 7-day gate
            policyStore.setLastSuccessfulUnlock(clock.nowMillis() - 1000)
            biometricGate.authenticateResult = true

            val unlock = UnlockWithBiometrics(biometricGate, secureKeyStore, policyStore, session, clock)
            val result = unlock("reason")

            assertTrue(result.isFailure)
            assertEquals(AppError.Unauthorized, result.error)
        }
    }

    @Test
    fun successUnlocksSessionAndResetsPolicy() {
        runBlocking {
            biometricGate.authenticateResult = true
            secureKeyStore.storedKey = VaultKey.from(ByteArray(32))
            policyStore.setLastSuccessfulUnlock(clock.nowMillis() - 1000) // Recent
            policyStore.recordFailure() // Set some failures

            val unlock = UnlockWithBiometrics(biometricGate, secureKeyStore, policyStore, session, clock)
            val result = unlock("reason")

            assertTrue(result.isSuccess)
            assertNotNull(session.currentVaultKey())
            assertEquals(0, policyStore.failures())
            assertEquals(clock.nowMillis(), policyStore.lastSuccessfulUnlock()!!)
        }
    }
}
