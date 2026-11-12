package com.kwazzar.lockruna.presentation.auth

import com.kwazzar.lockruna.application.auth.Register
import com.kwazzar.lockruna.domain.auth.DevicePlatform
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.VaultSession
import com.kwazzar.lockruna.presentation.support.FakeArgon2
import com.kwazzar.lockruna.presentation.support.FakeAuthApi
import com.kwazzar.lockruna.presentation.support.FakeAuthStateStore
import com.kwazzar.lockruna.presentation.support.FakeCryptoService
import com.kwazzar.lockruna.presentation.support.FakePasswordNormalizer
import com.kwazzar.lockruna.presentation.support.FixedIdGenerator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
import kotlin.test.assertTrue

class RegisterStoreTest {

    private val sessionStore = FakeAuthStateStore()
    private val authApi = FakeAuthApi()
    private val register = Register(
        authApi = authApi,
        argon2 = FakeArgon2(),
        normalizer = FakePasswordNormalizer(),
        crypto = FakeCryptoService(),
        authStateStore = sessionStore,
        session = VaultSession(),
        idGenerator = FixedIdGenerator(),
    )
    private val store = RegisterStore(register, DevicePlatform.IOS)

    @Test
    fun `a successful registration shows the recovery key once`() = runTest {
        fillValidForm()
        val effect = async(start = CoroutineStart.UNDISPATCHED) { store.effects.first() }

        store.dispatch(RegisterIntent.Submit)

        val ready = effect.await()
        assertTrue(ready is RegisterEffect.RecoveryKeyReady, "expected the recovery key, got $ready")
        assertEquals(12, (ready as RegisterEffect.RecoveryKeyReady).words.size)
        assertTrue(store.state.value.hasRecoveryWords)
        assertFalse(store.state.value.isSubmitting)
        // The words never live in the observable state.
        assertFalse(store.state.value.toString().contains(ready.words.first()))
    }

    @Test
    fun `confirming the words typed back finishes the registration`() = runTest {
        fillValidForm()
        val ready = submitAndAwaitRecoveryKey()
        val effect = async(start = CoroutineStart.UNDISPATCHED) { store.effects.first() }

        store.dispatch(RegisterIntent.ConfirmationChanged(ready.words.joinToString(" ")))
        store.dispatch(RegisterIntent.ConfirmRecoveryWords)

        assertEquals(RegisterEffect.Registered, effect.await())
        assertFalse(store.state.value.hasRecoveryWords)
    }

    @Test
    fun `words that do not match keep the user on the confirmation step`() = runTest {
        submitAndAwaitRecoveryKey()

        store.dispatch(
            RegisterIntent.ConfirmationChanged(
                "ability ability ability ability ability ability ability ability ability ability ability ability",
            ),
        )
        store.dispatch(RegisterIntent.ConfirmRecoveryWords)
        awaitUntil { store.state.value.error != null }

        assertTrue(store.state.value.error is AppError.InvalidRecoveryKey)
        // The words stay on screen: the registration is not finished.
        assertTrue(store.state.value.hasRecoveryWords)
    }

    @Test
    fun `a double submit registers once`() = runTest {
        fillValidForm()
        val gate = CompletableDeferred<Unit>()
        authApi.registerGate = gate
        val first = launch { store.dispatch(RegisterIntent.Submit) }
        yield()
        store.dispatch(RegisterIntent.Submit)
        gate.complete(Unit)
        first.join()

        assertEquals(1, authApi.registerCalls)
    }

    @Test
    fun `an existing email is reported on the same form`() = runTest {
        authApi.registerResult = com.kwazzar.lockruna.domain.core.Result.Failure(
            AppError.EmailAlreadyRegistered,
        )
        fillValidForm()

        store.dispatch(RegisterIntent.Submit)
        awaitUntil { store.state.value.error != null }

        assertEquals(AppError.EmailAlreadyRegistered, store.state.value.error)
        assertFalse(store.state.value.hasRecoveryWords)
    }

    @Test
    fun `a weak password is reported before any request`() = runTest {
        store.dispatch(RegisterIntent.EmailChanged("user@example.com"))
        store.dispatch(RegisterIntent.PasswordChanged("short"))
        store.dispatch(RegisterIntent.DeviceNameChanged("Phone"))

        store.dispatch(RegisterIntent.Submit)
        awaitUntil { store.state.value.error != null }

        assertTrue(store.state.value.error is AppError.WeakMasterPassword)
        assertEquals(0, authApi.registerCalls)
    }

    @Test
    fun `the password never reaches the state`() = runTest {
        val password = "correct horse battery staple"

        store.dispatch(RegisterIntent.PasswordChanged(password))

        assertFalse(store.state.value.toString().contains(password))
        assertTrue(store.state.value.hasPassword)
    }

    private suspend fun fillValidForm() {
        store.dispatch(RegisterIntent.EmailChanged("user@example.com"))
        store.dispatch(RegisterIntent.PasswordChanged("correct horse battery staple"))
        store.dispatch(RegisterIntent.DeviceNameChanged("Phone"))
    }

    private suspend fun submitAndAwaitRecoveryKey(): RegisterEffect.RecoveryKeyReady = coroutineScope {
        fillValidForm()
        store.dispatch(RegisterIntent.Submit)
        val effect = async(start = CoroutineStart.UNDISPATCHED) { store.effects.first() }
        effect.await() as RegisterEffect.RecoveryKeyReady
    }

    private suspend fun awaitUntil(condition: () -> Boolean) = withContext(Dispatchers.Default) {
        withTimeout(5_000) {
            while (!condition()) delay(10)
        }
    }
}
