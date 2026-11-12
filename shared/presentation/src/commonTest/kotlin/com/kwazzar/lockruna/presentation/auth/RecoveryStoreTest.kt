package com.kwazzar.lockruna.presentation.auth

import com.kwazzar.lockruna.application.auth.CompleteRecovery
import com.kwazzar.lockruna.domain.auth.DevicePlatform
import com.kwazzar.lockruna.domain.auth.RecoveryApi
import com.kwazzar.lockruna.domain.auth.RecoveryStartData
import com.kwazzar.lockruna.domain.auth.SealedBlob
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.crypto.KdfParams
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.VaultSession
import com.kwazzar.lockruna.presentation.support.FakeArgon2
import com.kwazzar.lockruna.presentation.support.FakeAuthStateStore
import com.kwazzar.lockruna.presentation.support.FakeCryptoService
import com.kwazzar.lockruna.presentation.support.FakePasswordNormalizer
import com.kwazzar.lockruna.presentation.support.FixedIdGenerator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecoveryStoreTest {

    private val authStateStore = FakeAuthStateStore()
    private val session = VaultSession()
    private val recoveryApi = FakeRecoveryApiForStore()
    private val store = RecoveryStore(
        CompleteRecovery(
            recoveryApi = recoveryApi,
            argon2 = FakeArgon2(),
            normalizer = FakePasswordNormalizer(),
            crypto = FakeCryptoService(),
            authStateStore = authStateStore,
            session = session,
            idGenerator = FixedIdGenerator(),
        ),
        DevicePlatform.ANDROID,
    )

    @Test
    fun `a valid recovery key unlocks the vault`() = runTest {
        fillValidForm()
        val effect = async(start = CoroutineStart.UNDISPATCHED) { store.effects.first() }

        store.dispatch(RecoveryIntent.Submit)

        assertEquals(RecoveryEffect.Recovered, effect.await())
        assertNotNull(session.currentVaultKey())
        assertFalse(store.state.value.isSubmitting)
    }

    @Test
    fun `a rejected recovery key is reported, the vault stays locked`() = runTest {
        recoveryApi.completeResult = Result.Failure(AppError.InvalidCredentials)
        fillValidForm()

        store.dispatch(RecoveryIntent.Submit)
        awaitUntil { store.state.value.error != null }

        assertEquals(AppError.InvalidCredentials, store.state.value.error)
        assertNull(session.currentVaultKey())
    }

    @Test
    fun `a double submit recovers once`() = runTest {
        fillValidForm()
        val gate = CompletableDeferred<Unit>()
        recoveryApi.startGate = gate
        val first = launch { store.dispatch(RecoveryIntent.Submit) }
        yield()
        store.dispatch(RecoveryIntent.Submit)
        gate.complete(Unit)
        first.join()

        assertEquals(1, recoveryApi.startCalls)
    }

    @Test
    fun `no request leaves the device with an incomplete key`() = runTest {
        store.dispatch(RecoveryIntent.EmailChanged("user@example.com"))
        store.dispatch(RecoveryIntent.RecoveryWordsChanged(listOf("abandon", "abandon")))
        store.dispatch(RecoveryIntent.NewPasswordChanged("correct horse battery staple"))
        store.dispatch(RecoveryIntent.DeviceNameChanged("Phone"))

        store.dispatch(RecoveryIntent.Submit)
        awaitUntil { store.state.value.error != null }

        assertTrue(store.state.value.error is AppError.InvalidRecoveryKey)
        assertEquals(0, recoveryApi.startCalls)
    }

    @Test
    fun `the new password never reaches the state`() = runTest {
        store.dispatch(RecoveryIntent.NewPasswordChanged("correct horse battery staple"))

        assertFalse(store.state.value.toString().contains("correct horse battery staple"))
        assertTrue(store.state.value.hasPassword)
    }

    private suspend fun fillValidForm() {
        store.dispatch(RecoveryIntent.EmailChanged("user@example.com"))
        store.dispatch(RecoveryIntent.RecoveryWordsChanged(recoveryApi.words()))
        store.dispatch(RecoveryIntent.NewPasswordChanged("correct horse battery staple"))
        store.dispatch(RecoveryIntent.DeviceNameChanged("Phone"))
    }

    private suspend fun awaitUntil(condition: () -> Boolean) = withContext(Dispatchers.Default) {
        withTimeout(5_000) {
            while (!condition()) delay(10)
        }
    }
}

/** A recovery port that answers like the server: a sealable blob and a session after completion. */
private class FakeRecoveryApiForStore : RecoveryApi {
    var startCalls = 0
    var startGate: CompletableDeferred<Unit>? = null
    var completeResult: Result<com.kwazzar.lockruna.domain.auth.AuthenticatedUser, AppError>? = null

    override suspend fun start(email: String): Result<RecoveryStartData, AppError> {
        startGate?.await()
        startCalls++
        return Result.Success(RecoveryStartData("user-1", SealedBlob.pack(ByteArray(12), ByteArray(48))))
    }

    override suspend fun complete(
        email: String,
        recoveryAuthKey: ByteArray,
        newAuthKey: ByteArray,
        newKdfSalt: ByteArray,
        newKdfParams: KdfParams,
        newWrappedVaultKey: ByteArray,
        device: com.kwazzar.lockruna.domain.auth.DeviceIdentity,
    ): Result<com.kwazzar.lockruna.domain.auth.AuthenticatedUser, AppError> = completeResult ?: Result.Success(
        com.kwazzar.lockruna.domain.auth.AuthenticatedUser(
            userId = "user-1",
            deviceId = device.deviceId ?: "device-1",
            wrappedVaultKey = newWrappedVaultKey,
            kdfSalt = newKdfSalt,
            kdfParams = KdfParams.SUPPORTED,
            tokens = com.kwazzar.lockruna.domain.auth.AuthTokens("access", "refresh", 3600),
        ),
    )

    /** The 12 words the fake crypto seals: sha256 is all zeros there, so the checksum bits are zero. */
    fun words(): List<String> = com.kwazzar.lockruna.domain.crypto.Bip39.encode(ByteArray(16), FakeCryptoService())
}
