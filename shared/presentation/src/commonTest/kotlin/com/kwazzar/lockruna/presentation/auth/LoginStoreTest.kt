package com.kwazzar.lockruna.presentation.auth

import com.kwazzar.lockruna.application.auth.Login
import com.kwazzar.lockruna.domain.auth.AuthTokens
import com.kwazzar.lockruna.domain.auth.AuthenticatedUser
import com.kwazzar.lockruna.domain.auth.DevicePlatform
import com.kwazzar.lockruna.domain.auth.SealedBlob
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.crypto.KdfParams
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.VaultSession
import com.kwazzar.lockruna.presentation.support.FakeArgon2
import com.kwazzar.lockruna.presentation.support.FakeAuthApi
import com.kwazzar.lockruna.presentation.support.FakeAuthStateStore
import com.kwazzar.lockruna.presentation.support.FakeCryptoService
import com.kwazzar.lockruna.presentation.support.FakePasswordNormalizer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoginStoreTest {

    private val password = "correct horse battery staple"
    private val vaultKey = ByteArray(32) { 4 }
    private val authApi = FakeAuthApi()
    private val argon2 = FakeArgon2()
    private val normalizer = FakePasswordNormalizer()
    private val authStateStore = FakeAuthStateStore()
    private val session = VaultSession()
    private val login = Login(
        authApi = authApi,
        argon2 = argon2,
        normalizer = normalizer,
        crypto = FakeCryptoService(),
        authStateStore = authStateStore,
        session = session,
    )
    private val store = LoginStore(login = login, platform = DevicePlatform.IOS)

    @Test
    fun `successful login emits logged in and unlocks the session`() = runTest {
        authApi.loginResult = Result.Success(user())
        normalizer.normalized = password
        store.dispatch(LoginIntent.EmailChanged("user@example.com"))
        store.dispatch(LoginIntent.PasswordChanged(password))
        store.dispatch(LoginIntent.DeviceNameChanged("Pixel"))
        val effect = async(start = CoroutineStart.UNDISPATCHED) { store.effects.first() }

        store.dispatch(LoginIntent.Submit)

        assertEquals(LoginEffect.LoggedIn, effect.await())
        assertFalse(store.state.value.isSubmitting)
        assertFalse(store.state.value.hasPassword)
        assertNull(store.state.value.error)
        assertEquals(VaultSession.State.Unlocked, session.state)
        assertEquals(DevicePlatform.IOS, authApi.lastLoginDevice?.platform)
    }

    @Test
    fun `invalid credentials surface as the same error for every account`() = runTest {
        authApi.loginResult = Result.Failure(AppError.InvalidCredentials)
        store.dispatch(LoginIntent.EmailChanged("user@example.com"))
        store.dispatch(LoginIntent.PasswordChanged(password))

        store.dispatch(LoginIntent.Submit)

        assertEquals(AppError.InvalidCredentials, store.state.value.error)
        assertFalse(store.state.value.isSubmitting)
        assertEquals(VaultSession.State.Locked, session.state)
    }

    @Test
    fun `a second submit while the first is in flight calls login once`() = runTest {
        authApi.loginGate = CompletableDeferred()
        authApi.loginResult = Result.Success(user())
        store.dispatch(LoginIntent.EmailChanged("user@example.com"))
        store.dispatch(LoginIntent.PasswordChanged(password))

        val first = launch { store.dispatch(LoginIntent.Submit) }
        yield()
        store.dispatch(LoginIntent.Submit)
        authApi.loginGate?.complete(Unit)
        first.join()

        assertEquals(1, authApi.loginCalls)
        assertFalse(store.state.value.hasPassword)
    }

    @Test
    fun `the password never reaches the state or an intent string`() = runTest {
        store.dispatch(LoginIntent.PasswordChanged(password))

        assertFalse(store.state.value.toString().contains(password))
        assertFalse(LoginIntent.PasswordChanged(password).toString().contains(password))
        assertTrue(store.state.value.hasPassword)
    }

    @Test
    fun `an effect emitted before the screen subscribes is not lost`() = runTest {
        authApi.loginResult = Result.Success(user())
        normalizer.normalized = password
        store.dispatch(LoginIntent.EmailChanged("user@example.com"))
        store.dispatch(LoginIntent.PasswordChanged(password))

        store.dispatch(LoginIntent.Submit)

        val effect = withTimeout(1_000) { store.effects.first() }
        assertEquals(LoginEffect.LoggedIn, effect)
    }

    @Test
    fun `error is dismissed on the next submit`() = runTest {
        authApi.loginResult = Result.Failure(AppError.InvalidCredentials)
        store.dispatch(LoginIntent.EmailChanged("user@example.com"))
        store.dispatch(LoginIntent.PasswordChanged(password))
        store.dispatch(LoginIntent.Submit)

        store.dispatch(LoginIntent.ErrorDismissed)

        assertNull(store.state.value.error)
    }

    private fun user(): AuthenticatedUser = AuthenticatedUser(
        userId = "user-1",
        deviceId = "device-1",
        wrappedVaultKey = SealedBlob.pack(ByteArray(12), vaultKey + ByteArray(16)),
        kdfSalt = ByteArray(16),
        kdfParams = KdfParams.SUPPORTED,
        tokens = AuthTokens("access", "refresh", 3600),
    )
}
