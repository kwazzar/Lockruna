package com.kwazzar.lockruna.presentation.auth

import com.kwazzar.lockruna.application.auth.EnableBiometrics
import com.kwazzar.lockruna.application.auth.UnlockWithBiometrics
import com.kwazzar.lockruna.application.auth.UnlockWithPassword
import com.kwazzar.lockruna.domain.auth.AuthTokens
import com.kwazzar.lockruna.domain.auth.AuthenticatedUser
import com.kwazzar.lockruna.domain.auth.SealedBlob
import com.kwazzar.lockruna.domain.crypto.KdfParams
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.VaultKey
import com.kwazzar.lockruna.domain.session.VaultSession
import com.kwazzar.lockruna.presentation.support.FakeArgon2
import com.kwazzar.lockruna.presentation.support.FakeAuthStateStore
import com.kwazzar.lockruna.presentation.support.FakeBiometricGate
import com.kwazzar.lockruna.presentation.support.FakeBiometricPolicyStore
import com.kwazzar.lockruna.presentation.support.FakeClock
import com.kwazzar.lockruna.presentation.support.FakeCryptoService
import com.kwazzar.lockruna.presentation.support.FakePasswordNormalizer
import com.kwazzar.lockruna.presentation.support.FakeSecureKeyStore
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UnlockStoreTest {

    private val password = "correct horse battery staple"
    private val vaultKey = ByteArray(32) { 4 }
    private val argon2 = FakeArgon2()
    private val normalizer = FakePasswordNormalizer()
    private val authStateStore = FakeAuthStateStore()
    private val biometricGate = FakeBiometricGate()
    private val secureKeyStore = FakeSecureKeyStore()
    private val policyStore = FakeBiometricPolicyStore()
    private val clock = FakeClock(wallMillis = 10_000)
    private val session = VaultSession()
    private val crypto = FakeCryptoService()

    private val store = UnlockStore(
        unlockWithPassword = UnlockWithPassword(argon2, normalizer, crypto, authStateStore, session),
        unlockWithBiometrics = UnlockWithBiometrics(biometricGate, secureKeyStore, policyStore, session, clock),
        enableBiometrics = EnableBiometrics(biometricGate, secureKeyStore, session),
        biometricGate = biometricGate,
    )

    @Test
    fun `biometric availability is reported on start`() = runTest {
        awaitBiometricAvailability(true)

        assertTrue(store.state.value.biometricAvailable)
    }

    @Test
    fun `password unlock emits unlocked and opens the vault`() = runTest {
        authStateStore.saveSession(user())
        normalizer.normalized = password
        store.dispatch(UnlockIntent.EmailChanged("user@example.com"))
        store.dispatch(UnlockIntent.PasswordChanged(password))
        val effect = async(start = CoroutineStart.UNDISPATCHED) { store.effects.first() }

        store.dispatch(UnlockIntent.SubmitPassword)

        assertEquals(UnlockEffect.Unlocked, effect.await())
        assertEquals(VaultSession.State.Unlocked, session.state)
        assertFalse(store.state.value.hasPassword)
    }

    @Test
    fun `a failed unlock reports the same error for every reason`() = runTest {
        store.dispatch(UnlockIntent.EmailChanged("user@example.com"))
        store.dispatch(UnlockIntent.PasswordChanged(password))

        store.dispatch(UnlockIntent.SubmitPassword)

        assertEquals(AppError.InvalidCredentials, store.state.value.error)
        assertFalse(store.state.value.isUnlocking)
        assertEquals(VaultSession.State.Locked, session.state)
    }

    @Test
    fun `biometric unlock emits unlocked`() = runTest {
        policyStore.lastSuccess = clock.wallMillis
        secureKeyStore.storedKey = VaultKey.from(vaultKey)
        val effect = async(start = CoroutineStart.UNDISPATCHED) { store.effects.first() }

        store.dispatch(UnlockIntent.SubmitBiometrics)

        assertEquals(UnlockEffect.Unlocked, effect.await())
        assertEquals(VaultSession.State.Unlocked, session.state)
    }

    @Test
    fun `a refused biometric keeps the password path open`() = runTest {
        policyStore.lastSuccess = clock.wallMillis
        biometricGate.authenticateResult = false
        store.dispatch(UnlockIntent.SubmitBiometrics)

        assertEquals(AppError.InvalidCredentials, store.state.value.error)
        assertEquals(VaultSession.State.Locked, session.state)

        authStateStore.saveSession(user())
        normalizer.normalized = password
        store.dispatch(UnlockIntent.EmailChanged("user@example.com"))
        store.dispatch(UnlockIntent.PasswordChanged(password))
        val effect = async(start = CoroutineStart.UNDISPATCHED) { store.effects.first() }

        store.dispatch(UnlockIntent.SubmitPassword)

        assertEquals(UnlockEffect.Unlocked, effect.await())
        assertEquals(VaultSession.State.Unlocked, session.state)
    }

    @Test
    fun `enabling biometrics stores the vault key`() = runTest {
        authStateStore.saveSession(user())
        normalizer.normalized = password
        store.dispatch(UnlockIntent.EmailChanged("user@example.com"))
        store.dispatch(UnlockIntent.PasswordChanged(password))
        store.dispatch(UnlockIntent.SubmitPassword)
        val effect = async(start = CoroutineStart.UNDISPATCHED) { store.effects.first() }

        store.dispatch(UnlockIntent.EnableBiometrics)

        assertEquals(UnlockEffect.BiometricsEnabled, effect.await())
        assertNotNull(secureKeyStore.storedKey)
    }

    @Test
    fun `the password never reaches the state`() = runTest {
        store.dispatch(UnlockIntent.PasswordChanged(password))

        assertFalse(store.state.value.toString().contains(password))
        assertTrue(store.state.value.hasPassword)
    }

    private suspend fun awaitBiometricAvailability(expected: Boolean) = withContext(Dispatchers.Default) {
        withTimeout(5_000) {
            while (store.state.value.biometricAvailable != expected) delay(10)
        }
    }

    private fun user(): AuthenticatedUser = AuthenticatedUser(
        userId = "user-1",
        deviceId = "device-1",
        wrappedVaultKey = SealedBlob.pack(ByteArray(12), vaultKey + ByteArray(16)),
        keyNonce = ByteArray(12),
        kdfSalt = ByteArray(16),
        kdfParams = KdfParams.SUPPORTED,
        tokens = AuthTokens("access", "refresh", 3600),
    )
}
