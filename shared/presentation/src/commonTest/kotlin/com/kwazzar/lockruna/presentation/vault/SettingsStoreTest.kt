package com.kwazzar.lockruna.presentation.vault

import com.kwazzar.lockruna.application.auth.DisableBiometrics
import com.kwazzar.lockruna.application.auth.EnableBiometrics
import com.kwazzar.lockruna.application.auth.ObserveBiometricUnlock
import com.kwazzar.lockruna.domain.auth.AccountApi
import com.kwazzar.lockruna.domain.auth.AuthTokens
import com.kwazzar.lockruna.domain.auth.AuthenticatedUser
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.crypto.KdfParams
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.AutoLockPolicy
import com.kwazzar.lockruna.domain.session.AutoLockSettingsStore
import com.kwazzar.lockruna.domain.session.VaultKey
import com.kwazzar.lockruna.domain.session.VaultSession
import com.kwazzar.lockruna.presentation.support.FakeArgon2
import com.kwazzar.lockruna.presentation.support.FakeBiometricGate
import com.kwazzar.lockruna.presentation.support.FakeBiometricPolicyStore
import com.kwazzar.lockruna.presentation.support.FakeClock
import com.kwazzar.lockruna.presentation.support.FakeCryptoService
import com.kwazzar.lockruna.presentation.support.FakePasswordNormalizer
import com.kwazzar.lockruna.presentation.support.FakeSecureKeyStore
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsStoreTest {

    private val settingsStore = InMemoryAutoLockSettingsStore()
    private val biometricGate = FakeBiometricGate()
    private val secureKeyStore = FakeSecureKeyStore()
    private val session = VaultSession()
    private val clock = FakeClock(wallMillis = 42_000)
    private val policyStore = FakeBiometricPolicyStore()
    private val sessionStore = FakeAuthStateStore()
    private val accountApi = FakeAccountApi()
    private val store = SettingsStore(
        autoLockSettingsStore = settingsStore,
        observeBiometricUnlock = ObserveBiometricUnlock(biometricGate, secureKeyStore),
        enableBiometrics = EnableBiometrics(biometricGate, secureKeyStore, policyStore, session, clock),
        disableBiometrics = DisableBiometrics(secureKeyStore),
        deleteAccount = com.kwazzar.lockruna.application.auth.DeleteAccount(
            accountApi,
            FakeArgon2(),
            FakePasswordNormalizer(),
            FakeCryptoService(),
            sessionStore,
            session,
        ),
    )

    @Test
    fun `the stored timeout is the one shown`() = runTest {
        settingsStore.stored = AutoLockPolicy.FIVE_MINUTES

        store.dispatch(SettingsIntent.Load)
        awaitIdle()

        assertEquals(AutoLockPolicy.FIVE_MINUTES, store.state.value.policy)
        assertTrue(store.state.value.choices.contains(AutoLockPolicy.IMMEDIATE))
        assertTrue(store.state.value.choices.contains(AutoLockPolicy.FIFTEEN_MINUTES))
    }

    @Test
    fun `choosing another timeout persists it`() = runTest {
        store.dispatch(SettingsIntent.Load)
        awaitIdle()

        store.dispatch(SettingsIntent.PolicyChanged(AutoLockPolicy.FIFTEEN_MINUTES))
        awaitIdle()

        assertEquals(AutoLockPolicy.FIFTEEN_MINUTES, store.state.value.policy)
        assertEquals(AutoLockPolicy.FIFTEEN_MINUTES, settingsStore.stored)
    }

    @Test
    fun `biometrics are off until a key was stored`() = runTest {
        store.dispatch(SettingsIntent.Load)
        awaitIdle()

        assertTrue(store.state.value.biometricsAvailable)
        assertTrue(!store.state.value.biometricsEnabled)
    }

    @Test
    fun `turning biometrics on stores the key and reports the new state`() = runTest {
        session.unlock(VaultKey.from(ByteArray(32)))
        store.dispatch(SettingsIntent.Load)
        awaitIdle()

        store.dispatch(SettingsIntent.BiometricsToggled(true))
        awaitIdle()

        assertTrue(store.state.value.biometricsEnabled)
        assertTrue(secureKeyStore.storedKey != null)
        // The first successful unlock is recorded, or the next one fails the seven-day gate.
        assertEquals(clock.wallMillis, policyStore.lastSuccess)
    }

    @Test
    fun `turning biometrics off drops the key`() = runTest {
        session.unlock(VaultKey.from(ByteArray(32)))
        store.dispatch(SettingsIntent.Load)
        awaitIdle()
        store.dispatch(SettingsIntent.BiometricsToggled(true))
        awaitIdle()

        store.dispatch(SettingsIntent.BiometricsToggled(false))
        awaitIdle()

        assertTrue(secureKeyStore.storedKey == null)
        assertTrue(!store.state.value.biometricsEnabled)
    }

    @Test
    fun `biometrics the device cannot do are reported as unavailable`() = runTest {
        biometricGate.isAvailableResult = false
        session.unlock(VaultKey.from(ByteArray(32)))
        store.dispatch(SettingsIntent.Load)
        awaitIdle()

        store.dispatch(SettingsIntent.BiometricsToggled(true))
        awaitIdle()

        assertEquals(AppError.BiometricsUnavailable, store.state.value.error)
        assertTrue(secureKeyStore.storedKey == null)
    }

    @Test
    fun `deleting the account clears the local session`() = runTest {
        session.unlock(VaultKey.from(ByteArray(32)))
        sessionStore.saveSession(user())
        store.dispatch(SettingsIntent.Load)
        awaitIdle()

        store.dispatch(SettingsIntent.DeletionPasswordChanged("correct horse battery staple"))
        store.dispatch(SettingsIntent.DeleteAccount)
        awaitIdle()

        assertNull(sessionStore.loadSession())
        assertNull(session.currentVaultKey())
        assertTrue(awaitEffect() is SettingsEffect.AccountDeleted)
    }

    @Test
    fun `a weak deletion password keeps the account`() = runTest {
        sessionStore.saveSession(user())
        store.dispatch(SettingsIntent.Load)
        awaitIdle()

        store.dispatch(SettingsIntent.DeletionPasswordChanged("short"))
        store.dispatch(SettingsIntent.DeleteAccount)
        awaitIdle()

        assertTrue(store.state.value.error is AppError.WeakMasterPassword)
        assertTrue(sessionStore.loadSession() != null)
    }

    @Test
    fun `the deletion password never reaches the state`() = runTest {
        store.dispatch(SettingsIntent.DeletionPasswordChanged("correct horse battery staple"))

        assertFalse(store.state.value.toString().contains("correct horse battery staple"))
        assertTrue(store.state.value.hasPasswordForDeletion)
    }

    private suspend fun awaitEffect(): SettingsEffect = coroutineScope {
        val effect = async(start = CoroutineStart.UNDISPATCHED) { store.effects.first() }
        effect.await()
    }

    private suspend fun awaitIdle() = withContext(Dispatchers.Default) {
        withTimeout(5_000) {
            while (store.state.value.isLoading || store.state.value.isDeleting) delay(10)
        }
    }
}

private class InMemoryAutoLockSettingsStore(var stored: AutoLockPolicy = AutoLockPolicy.ONE_MINUTE) :
    AutoLockSettingsStore {
    override suspend fun load(): AutoLockPolicy = stored

    override suspend fun save(policy: AutoLockPolicy) {
        stored = policy
    }
}

private class FakeAuthStateStore : com.kwazzar.lockruna.domain.auth.AuthStateStore {
    private var session: AuthenticatedUser? = null

    override suspend fun saveSession(user: AuthenticatedUser) {
        session = user
    }

    override suspend fun loadSession(): AuthenticatedUser? = session

    override suspend fun clear() {
        session = null
    }
}

private class FakeAccountApi : AccountApi {
    var deleteCalls = 0

    override suspend fun delete(authKey: ByteArray): Result<Unit, AppError> {
        deleteCalls++
        return Result.Success(Unit)
    }
}

private fun user() = AuthenticatedUser(
    userId = "user-1",
    deviceId = "device-1",
    wrappedVaultKey = ByteArray(48),
    kdfSalt = ByteArray(16),
    kdfParams = KdfParams.SUPPORTED,
    tokens = AuthTokens("access", "refresh", 3600),
)
