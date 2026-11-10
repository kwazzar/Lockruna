package com.kwazzar.lockruna.application.auth

import com.kwazzar.lockruna.application.auth.CredentialsCommand
import com.kwazzar.lockruna.application.auth.Login
import com.kwazzar.lockruna.application.support.FakeArgon2
import com.kwazzar.lockruna.application.support.FakeAuthApi
import com.kwazzar.lockruna.application.support.FakeAuthStateStore
import com.kwazzar.lockruna.application.support.FakeClock
import com.kwazzar.lockruna.application.support.FakeCryptoService
import com.kwazzar.lockruna.application.support.FakeIdGenerator
import com.kwazzar.lockruna.application.support.FakePasswordNormalizer
import com.kwazzar.lockruna.domain.auth.AuthenticatedUser
import com.kwazzar.lockruna.domain.auth.AuthTokens
import com.kwazzar.lockruna.domain.auth.BiometricPolicyStore
import com.kwazzar.lockruna.domain.auth.DeviceIdentity
import com.kwazzar.lockruna.domain.auth.DevicePlatform
import com.kwazzar.lockruna.domain.auth.SealedBlob
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.crypto.CryptoService
import com.kwazzar.lockruna.domain.crypto.KdfParams
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.Clock
import com.kwazzar.lockruna.domain.session.IdGenerator
import com.kwazzar.lockruna.domain.session.VaultKey
import com.kwazzar.lockruna.domain.session.VaultSession
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val <T, E> Result<T, E>.isSuccess: Boolean
    get() = this is Result.Success

private val <T, E> Result<T, E>.isFailure: Boolean
    get() = this is Result.Failure

private val <T, E> Result<T, E>.error: E?
    get() = if (this is Result.Failure) this.error else null

class LoginTest {

    private val clock = FakeClock()
    private val idGenerator = FakeIdGenerator()
    private val crypto = FakeCryptoService()
    private val argon2 = FakeArgon2()
    private val normalizer = FakePasswordNormalizer()
    private val authApi = FakeAuthApi()
    private val authStateStore = FakeAuthStateStore()
    private val session = VaultSession()

    @Test
    fun successfulLoginUnlocksSession() = runBlocking {
        val email = "user@example.com"
        val password = "password123"
        val salt = "salt123456789012".toByteArray()
        val masterKey = ByteArray(32) { 1 }
        val authKey = ByteArray(32) { 2 }
        val kek = ByteArray(32) { 3 }
        val vaultKey = ByteArray(32) { 4 }

        // Setup prelogin response
        authApi.preloginResult = Result.Success(
            com.kwazzar.lockruna.domain.auth.PreLoginData(salt, KdfParams.SUPPORTED)
        )

        // Setup login response
        val wrappedVaultKey = SealedBlob.pack(ByteArray(12), vaultKey)
        val user = AuthenticatedUser(
            userId = "user-1",
            deviceId = "device-1",
            wrappedVaultKey = wrappedVaultKey,
            keyNonce = ByteArray(12),
            kdfSalt = salt,
            kdfParams = KdfParams.SUPPORTED,
            tokens = AuthTokens("access-token", "refresh-token", 3600)
        )
        authApi.loginResult = Result.Success(user)

        // Setup fake crypto
        normalizer.normalized = password
        argon2.hashResult = masterKey
        crypto.hkdfResults = mapOf(
            "passkeeper/auth/v1" to authKey,
            "passkeeper/kek/v1" to kek
        )
        crypto.decryptGcmResult = vaultKey

        val login = Login(
            authApi = authApi,
            argon2 = argon2,
            normalizer = normalizer,
            crypto = crypto,
            authStateStore = authStateStore,
            session = session
        )

        val result = login(CredentialsCommand(
            email = email,
            password = password,
            deviceName = "Test Device",
            deviceId = "device-1"
        ))

        assertTrue(result.isSuccess)
        assertNotNull(session.currentVaultKey())
        assertEquals(vaultKey.contentToString(), session.currentVaultKey()!!.bytes.contentToString())
        assertEquals(user, authStateStore.loadSession())
    }

    @Test
    fun weakKdfParamsReturnsFailure() = runBlocking {
        val email = "user@example.com"
        val weakParams = KdfParams(memoryKiB = 1000, iterations = 1, parallelism = 1)

        authApi.preloginResult = Result.Success(
            com.kwazzar.lockruna.domain.auth.PreLoginData(ByteArray(16), weakParams)
        )

        val login = Login(
            authApi = authApi,
            argon2 = argon2,
            normalizer = normalizer,
            crypto = crypto,
            authStateStore = authStateStore,
            session = session
        )

        val result = login(CredentialsCommand(
            email = email,
            password = "password123",
            deviceName = "Test Device",
            deviceId = "device-1"
        ))

        assertTrue(result.isFailure)
        assertTrue(result.error is AppError.WeakKdfParams)
    }

    @Test
    fun loginInvalidCredentials() = runBlocking {
        val email = "user@example.com"
        val salt = "salt123456789012".toByteArray()

        authApi.preloginResult = Result.Success(
            com.kwazzar.lockruna.domain.auth.PreLoginData(salt, KdfParams.SUPPORTED)
        )
        authApi.loginResult = Result.Failure(AppError.InvalidCredentials)

        val login = Login(
            authApi = authApi,
            argon2 = argon2,
            normalizer = normalizer,
            crypto = crypto,
            authStateStore = authStateStore,
            session = session
        )

        val result = login(CredentialsCommand(
            email = email,
            password = "password123",
            deviceName = "Test Device",
            deviceId = "device-1"
        ))

        assertTrue(result.isFailure)
        assertEquals(AppError.InvalidCredentials, result.error)
    }

    @Test
    fun loginRateLimited() = runBlocking {
        val email = "user@example.com"
        val salt = "salt123456789012".toByteArray()

        authApi.preloginResult = Result.Success(
            com.kwazzar.lockruna.domain.auth.PreLoginData(salt, KdfParams.SUPPORTED)
        )
        authApi.loginResult = Result.Failure(AppError.RateLimited(60))

        val login = Login(
            authApi = authApi,
            argon2 = argon2,
            normalizer = normalizer,
            crypto = crypto,
            authStateStore = authStateStore,
            session = session
        )

        val result = login(CredentialsCommand(
            email = email,
            password = "password123",
            deviceName = "Test Device",
            deviceId = "device-1"
        ))

        assertTrue(result.isFailure)
        assertEquals(AppError.RateLimited(60), result.error)
    }
}

class LogoutTest {

    private val authApi = FakeAuthApi()
    private val authStateStore = FakeAuthStateStore()
    private val session = VaultSession()

    @Test
    fun logoutClearsSessionAndLocks() = runBlocking {
        // Setup logged in session
        val user = AuthenticatedUser(
            userId = "user-1",
            deviceId = "device-1",
            wrappedVaultKey = SealedBlob.pack(ByteArray(12), ByteArray(32)),
            keyNonce = ByteArray(12),
            kdfSalt = ByteArray(16),
            kdfParams = KdfParams.SUPPORTED,
            tokens = AuthTokens("access", "refresh", 3600)
        )
        authStateStore.saveSession(user)
        session.unlock(VaultKey.from(ByteArray(32)))

        val logout = Logout(authApi, authStateStore, session)
        val result = logout()

        assertTrue(result.isSuccess)
        assertNull(authStateStore.loadSession())
        assertNull(session.currentVaultKey())
    }
}

class UnlockWithPasswordTest {

    private val argon2 = FakeArgon2()
    private val normalizer = FakePasswordNormalizer()
    private val crypto = FakeCryptoService()
    private val authStateStore = FakeAuthStateStore()
    private val session = VaultSession()

    @Test
    fun unlockWithPasswordFromStoredSession() = runBlocking {
        val email = "user@example.com"
        val password = "password123"
        val salt = "salt123456789012".toByteArray()
        val masterKey = ByteArray(32) { 1 }
        val kek = ByteArray(32) { 3 }
        val vaultKey = ByteArray(32) { 4 }

        // Setup stored session
        val wrappedVaultKey = SealedBlob.pack(ByteArray(12), vaultKey)
        val user = AuthenticatedUser(
            userId = "user-1",
            deviceId = "device-1",
            wrappedVaultKey = wrappedVaultKey,
            keyNonce = ByteArray(12),
            kdfSalt = salt,
            kdfParams = KdfParams.SUPPORTED,
            tokens = AuthTokens("access", "refresh", 3600)
        )
        authStateStore.saveSession(user)

        // Setup fakes
        normalizer.normalized = password
        argon2.hashResult = masterKey
        crypto.hkdfResults = mapOf("passkeeper/kek/v1" to kek)
        crypto.decryptGcmResult = vaultKey

        val unlock = UnlockWithPassword(argon2, normalizer, crypto, authStateStore, session)
        val result = unlock(email, password)

        assertTrue(result.isSuccess)
        assertNotNull(session.currentVaultKey())
        assertEquals(vaultKey.contentToString(), session.currentVaultKey()!!.bytes.contentToString())
    }

    @Test
    fun unlockFailsWhenNoStoredSession() = runBlocking {
        val unlock = UnlockWithPassword(argon2, normalizer, crypto, authStateStore, session)
        val result = unlock("user@example.com", "password123")

        assertTrue(result.isFailure)
        assertEquals(AppError.Unauthorized, result.error)
    }

    @Test
    fun unlockSucceedsEvenWhenAlreadyUnlocked() = runBlocking {
        val email = "user@example.com"
        val password = "password123"
        val salt = "salt123456789012".toByteArray()
        val masterKey = ByteArray(32) { 1 }
        val kek = ByteArray(32) { 3 }
        val vaultKey = ByteArray(32) { 4 }

        val wrappedVaultKey = SealedBlob.pack(ByteArray(12), vaultKey)
        val user = AuthenticatedUser(
            userId = "user-1",
            deviceId = "device-1",
            wrappedVaultKey = wrappedVaultKey,
            keyNonce = ByteArray(12),
            kdfSalt = salt,
            kdfParams = KdfParams.SUPPORTED,
            tokens = AuthTokens("access", "refresh", 3600)
        )
        authStateStore.saveSession(user)
        session.unlock(VaultKey.from(ByteArray(32) { 5 })) // Different key

        normalizer.normalized = password
        argon2.hashResult = masterKey
        crypto.hkdfResults = mapOf("passkeeper/kek/v1" to kek)
        crypto.decryptGcmResult = vaultKey

        val unlock = UnlockWithPassword(argon2, normalizer, crypto, authStateStore, session)
        val result = unlock(email, password)

        assertTrue(result.isSuccess)
        assertEquals(vaultKey.contentToString(), session.currentVaultKey()!!.bytes.contentToString())
    }
}