package com.kwazzar.lockruna.application.auth

import com.kwazzar.lockruna.application.auth.CredentialsCommand
import com.kwazzar.lockruna.application.auth.Login
import com.kwazzar.lockruna.application.support.FakeArgon2
import com.kwazzar.lockruna.application.support.FakeAuthApi
import com.kwazzar.lockruna.application.support.FakeAuthStateStore
import com.kwazzar.lockruna.application.support.FakeBiometricPolicyStore
import com.kwazzar.lockruna.application.support.FakeClock
import com.kwazzar.lockruna.application.support.FakeCryptoService
import com.kwazzar.lockruna.application.support.FakeIdGenerator
import com.kwazzar.lockruna.application.support.FakePasswordNormalizer
import com.kwazzar.lockruna.domain.auth.AuthTokens
import com.kwazzar.lockruna.domain.auth.AuthenticatedUser
import com.kwazzar.lockruna.domain.auth.DevicePlatform
import com.kwazzar.lockruna.domain.auth.SealedBlob
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.crypto.KdfParams
import com.kwazzar.lockruna.domain.error.AppError
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
    private val biometricPolicyStore = FakeBiometricPolicyStore()
    private val session = VaultSession()

    @Test
    fun successfulLoginUnlocksSession() = runBlocking {
        val email = "user@example.com"
        val password = "password123"
        val salt = "salt123456789012".encodeToByteArray()
        val masterKey = ByteArray(32) { 1 }
        val authKey = ByteArray(32) { 2 }
        val kek = ByteArray(32) { 3 }
        val vaultKey = ByteArray(32) { 4 }

        // Setup prelogin response
        authApi.preloginResult = Result.Success(
            com.kwazzar.lockruna.domain.auth.PreLoginData(salt, KdfParams.SUPPORTED),
        )

        // Setup login response
        val wrappedVaultKey = SealedBlob.pack(ByteArray(12), vaultKey)
        val user = AuthenticatedUser(
            userId = "user-1",
            deviceId = "device-1",
            wrappedVaultKey = wrappedVaultKey,
            kdfSalt = salt,
            kdfParams = KdfParams.SUPPORTED,
            tokens = AuthTokens("access-token", "refresh-token", 3600),
            email = "user@example.com",
        )
        authApi.loginResult = Result.Success(user)

        // Setup fake crypto
        normalizer.normalized = password
        argon2.hashResult = masterKey
        crypto.hkdfResults = mapOf(
            "passkeeper/auth/v1" to authKey,
            "passkeeper/kek/v1" to kek,
        )
        crypto.decryptGcmResult = vaultKey

        val login = Login(
            authApi = authApi,
            argon2 = argon2,
            normalizer = normalizer,
            crypto = crypto,
            authStateStore = authStateStore,
            biometricPolicyStore = biometricPolicyStore,
            session = session,
            clock = clock,
        )

        val result = login(
            CredentialsCommand(
                email = email,
                password = password,
                deviceName = "Test Device",
                deviceId = "device-1",
                platform = DevicePlatform.ANDROID,
            ),
        )

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
            com.kwazzar.lockruna.domain.auth.PreLoginData(ByteArray(16), weakParams),
        )

        val login = Login(
            authApi = authApi,
            argon2 = argon2,
            normalizer = normalizer,
            crypto = crypto,
            authStateStore = authStateStore,
            biometricPolicyStore = biometricPolicyStore,
            session = session,
            clock = clock,
        )

        val result = login(
            CredentialsCommand(
                email = email,
                password = "password123",
                deviceName = "Test Device",
                deviceId = "device-1",
                platform = DevicePlatform.ANDROID,
            ),
        )

        assertTrue(result.isFailure)
        assertTrue(result.error is AppError.WeakKdfParams)
    }

    @Test
    fun loginInvalidCredentials() = runBlocking {
        val email = "user@example.com"
        val salt = "salt123456789012".encodeToByteArray()

        authApi.preloginResult = Result.Success(
            com.kwazzar.lockruna.domain.auth.PreLoginData(salt, KdfParams.SUPPORTED),
        )
        authApi.loginResult = Result.Failure(AppError.InvalidCredentials)

        val login = Login(
            authApi = authApi,
            argon2 = argon2,
            normalizer = normalizer,
            crypto = crypto,
            authStateStore = authStateStore,
            biometricPolicyStore = biometricPolicyStore,
            session = session,
            clock = clock,
        )

        val result = login(
            CredentialsCommand(
                email = email,
                password = "password123",
                deviceName = "Test Device",
                deviceId = "device-1",
                platform = DevicePlatform.ANDROID,
            ),
        )

        assertTrue(result.isFailure)
        assertEquals(AppError.InvalidCredentials, result.error)
    }

    @Test
    fun loginRateLimited() = runBlocking {
        val email = "user@example.com"
        val salt = "salt123456789012".encodeToByteArray()

        authApi.preloginResult = Result.Success(
            com.kwazzar.lockruna.domain.auth.PreLoginData(salt, KdfParams.SUPPORTED),
        )
        authApi.loginResult = Result.Failure(AppError.RateLimited(60))

        val login = Login(
            authApi = authApi,
            argon2 = argon2,
            normalizer = normalizer,
            crypto = crypto,
            authStateStore = authStateStore,
            biometricPolicyStore = biometricPolicyStore,
            session = session,
            clock = clock,
        )

        val result = login(
            CredentialsCommand(
                email = email,
                password = "password123",
                deviceName = "Test Device",
                deviceId = "device-1",
                platform = DevicePlatform.ANDROID,
            ),
        )

        assertTrue(result.isFailure)
        assertEquals(AppError.RateLimited(60), result.error)
    }

    @Test
    fun devicePlatformComesFromTheCommand() = runBlocking {
        authApi.preloginResult = Result.Success(
            com.kwazzar.lockruna.domain.auth.PreLoginData(ByteArray(16), KdfParams.SUPPORTED),
        )

        val login = Login(
            authApi = authApi,
            argon2 = argon2,
            normalizer = normalizer,
            crypto = crypto,
            authStateStore = authStateStore,
            biometricPolicyStore = biometricPolicyStore,
            session = session,
            clock = clock,
        )

        login(
            CredentialsCommand(
                email = "user@example.com",
                password = "password123",
                deviceName = "Test Device",
                deviceId = "device-1",
                platform = DevicePlatform.IOS,
            ),
        )

        assertEquals(DevicePlatform.IOS, authApi.lastLoginDevice?.platform)
    }

    @Test
    fun aWrongMasterPasswordFailsWithInvalidCredentials() = runBlocking {
        authApi.preloginResult = Result.Success(
            com.kwazzar.lockruna.domain.auth.PreLoginData(ByteArray(16), KdfParams.SUPPORTED),
        )
        authApi.loginResult = Result.Success(authenticatedUser())
        crypto.decryptGcmFails = true

        val result = loginUseCase()(
            CredentialsCommand(
                email = "user@example.com",
                password = "password123",
                deviceName = "Test Device",
                deviceId = null,
                platform = DevicePlatform.ANDROID,
            ),
        )

        assertTrue(result.isFailure)
        assertEquals(AppError.InvalidCredentials, result.error)
        assertNull(session.currentVaultKey())
    }

    @Test
    fun theLoginReusesTheStoredDeviceId() = runBlocking {
        authApi.preloginResult = Result.Success(
            com.kwazzar.lockruna.domain.auth.PreLoginData(ByteArray(16), KdfParams.SUPPORTED),
        )
        authApi.loginResult = Result.Success(authenticatedUser())
        authStateStore.saveSession(authenticatedUser())

        loginUseCase()(
            CredentialsCommand(
                email = "user@example.com",
                password = "password123",
                deviceName = "Test Device",
                deviceId = null,
                platform = DevicePlatform.ANDROID,
            ),
        )

        assertEquals("device-1", authApi.lastLoginDevice?.deviceId)
    }

    @Test
    fun theEmailIsCanonicalisedForTheApiAndTheAad() = runBlocking {
        authApi.preloginResult = Result.Success(
            com.kwazzar.lockruna.domain.auth.PreLoginData(ByteArray(16), KdfParams.SUPPORTED),
        )
        authApi.loginResult = Result.Success(authenticatedUser())

        loginUseCase()(
            CredentialsCommand(
                email = "  User@Example.com  ",
                password = "password123",
                deviceName = "Test Device",
                deviceId = null,
                platform = DevicePlatform.ANDROID,
            ),
        )

        assertEquals("user@example.com", authApi.lastPreloginEmail)
        assertEquals("user@example.com", authApi.lastLoginEmail)
        assertEquals("user@example.com".encodeToByteArray().decodeToString(), crypto.lastAad?.decodeToString())
    }

    @Test
    fun signingInWithTheMasterPasswordRearmsTheBiometricPath() = runBlocking {
        authApi.preloginResult = Result.Success(
            com.kwazzar.lockruna.domain.auth.PreLoginData(ByteArray(16), KdfParams.SUPPORTED),
        )
        authApi.loginResult = Result.Success(authenticatedUser())
        normalizer.normalized = "password123"
        biometricPolicyStore.recordFailure()

        val result = loginUseCase()(
            CredentialsCommand(
                email = "user@example.com",
                password = "password123",
                deviceName = "Test Device",
                deviceId = null,
                platform = DevicePlatform.ANDROID,
            ),
        )

        assertTrue(result.isSuccess)
        // US-07: the master password re-arms the biometric path.
        assertEquals(clock.nowMillis(), biometricPolicyStore.lastSuccessfulUnlock())
        assertEquals(0, biometricPolicyStore.failures())
    }

    private fun loginUseCase() = Login(
        authApi = authApi,
        argon2 = argon2,
        normalizer = normalizer,
        crypto = crypto,
        authStateStore = authStateStore,
        biometricPolicyStore = biometricPolicyStore,
        session = session,
        clock = clock,
    )

    private fun authenticatedUser() = AuthenticatedUser(
        userId = "user-1",
        deviceId = "device-1",
        wrappedVaultKey = SealedBlob.pack(ByteArray(12), ByteArray(32) { 4 } + ByteArray(16)),
        kdfSalt = ByteArray(16),
        kdfParams = KdfParams.SUPPORTED,
        tokens = AuthTokens("access-token", "refresh-token", 3600),
    )
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
            kdfSalt = ByteArray(16),
            kdfParams = KdfParams.SUPPORTED,
            tokens = AuthTokens("access", "refresh", 3600),
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
    private val biometricPolicyStore = FakeBiometricPolicyStore()
    private val clock = FakeClock()
    private val session = VaultSession()

    @Test
    fun unlockWithPasswordFromStoredSession() = runBlocking {
        val email = "user@example.com"
        val password = "password123"
        val salt = "salt123456789012".encodeToByteArray()
        val masterKey = ByteArray(32) { 1 }
        val kek = ByteArray(32) { 3 }
        val vaultKey = ByteArray(32) { 4 }

        // Setup stored session
        val wrappedVaultKey = SealedBlob.pack(ByteArray(12), vaultKey)
        val user = AuthenticatedUser(
            userId = "user-1",
            deviceId = "device-1",
            wrappedVaultKey = wrappedVaultKey,
            kdfSalt = salt,
            kdfParams = KdfParams.SUPPORTED,
            tokens = AuthTokens("access", "refresh", 3600),
        )
        authStateStore.saveSession(user)

        // Setup fakes
        normalizer.normalized = password
        argon2.hashResult = masterKey
        crypto.hkdfResults = mapOf("passkeeper/kek/v1" to kek)
        crypto.decryptGcmResult = vaultKey

        val unlock = unlockUseCase()
        val result = unlock(email, password)

        assertTrue(result.isSuccess)
        assertNotNull(session.currentVaultKey())
        assertEquals(vaultKey.contentToString(), session.currentVaultKey()!!.bytes.contentToString())
    }

    @Test
    fun unlockFailsWhenNoStoredSession() = runBlocking {
        val unlock = unlockUseCase()
        val result = unlock("user@example.com", "password123")

        assertTrue(result.isFailure)
        assertEquals(AppError.Unauthorized, result.error)
    }

    @Test
    fun unlockSucceedsEvenWhenAlreadyUnlocked() = runBlocking {
        val email = "user@example.com"
        val password = "password123"
        val salt = "salt123456789012".encodeToByteArray()
        val masterKey = ByteArray(32) { 1 }
        val kek = ByteArray(32) { 3 }
        val vaultKey = ByteArray(32) { 4 }

        val wrappedVaultKey = SealedBlob.pack(ByteArray(12), vaultKey)
        val user = AuthenticatedUser(
            userId = "user-1",
            deviceId = "device-1",
            wrappedVaultKey = wrappedVaultKey,
            kdfSalt = salt,
            kdfParams = KdfParams.SUPPORTED,
            tokens = AuthTokens("access", "refresh", 3600),
        )
        authStateStore.saveSession(user)
        session.unlock(VaultKey.from(ByteArray(32) { 5 })) // Different key

        normalizer.normalized = password
        argon2.hashResult = masterKey
        crypto.hkdfResults = mapOf("passkeeper/kek/v1" to kek)
        crypto.decryptGcmResult = vaultKey

        val unlock = unlockUseCase()
        val result = unlock(email, password)

        assertTrue(result.isSuccess)
        assertEquals(vaultKey.contentToString(), session.currentVaultKey()!!.bytes.contentToString())
    }

    @Test
    fun aWrongMasterPasswordFailsWithInvalidCredentials() = runBlocking {
        authStateStore.saveSession(storedUser())
        crypto.decryptGcmFails = true

        val unlock = unlockUseCase()
        val result = unlock("user@example.com", "password123")

        assertTrue(result.isFailure)
        assertEquals(AppError.InvalidCredentials, result.error)
        assertNull(session.currentVaultKey())
    }

    @Test
    fun theEmailIsCanonicalisedForTheAad() = runBlocking {
        authStateStore.saveSession(storedUser())

        val unlock = unlockUseCase()
        unlock("  User@Example.com  ", "password123")

        assertEquals("user@example.com", crypto.lastAad?.decodeToString())
    }

    @Test
    fun theMasterPasswordRearmsTheBiometricPath() = runBlocking {
        authStateStore.saveSession(storedUser())
        normalizer.normalized = "password123"
        biometricPolicyStore.recordFailure()

        val unlock = unlockUseCase()
        val result = unlock("user@example.com", "password123")

        assertTrue(result.isSuccess)
        // US-07: the master password re-arms the biometric path.
        assertEquals(clock.nowMillis(), biometricPolicyStore.lastSuccessfulUnlock())
        assertEquals(0, biometricPolicyStore.failures())
    }

    private fun unlockUseCase() = UnlockWithPassword(
        argon2,
        normalizer,
        crypto,
        authStateStore,
        biometricPolicyStore,
        session,
        clock,
    )

    private fun storedUser() = AuthenticatedUser(
        userId = "user-1",
        deviceId = "device-1",
        wrappedVaultKey = SealedBlob.pack(ByteArray(12), ByteArray(32) { 4 } + ByteArray(16)),
        kdfSalt = ByteArray(16),
        kdfParams = KdfParams.SUPPORTED,
        tokens = AuthTokens("access-token", "refresh-token", 3600),
        // Sealed with the canonical email: the AAD comes from the session, not the field.
        email = "user@example.com",
    )
}
