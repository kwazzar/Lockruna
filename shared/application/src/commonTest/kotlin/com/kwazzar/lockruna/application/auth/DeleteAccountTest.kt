package com.kwazzar.lockruna.application.auth

import com.kwazzar.lockruna.application.support.FakeArgon2
import com.kwazzar.lockruna.application.support.FakeAuthStateStore
import com.kwazzar.lockruna.application.support.FakeCryptoService
import com.kwazzar.lockruna.application.support.FakePasswordNormalizer
import com.kwazzar.lockruna.application.support.FakeRecoveryApi
import com.kwazzar.lockruna.domain.auth.AuthTokens
import com.kwazzar.lockruna.domain.auth.AuthenticatedUser
import com.kwazzar.lockruna.domain.auth.DeviceInfo
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.crypto.HkdfLabels
import com.kwazzar.lockruna.domain.crypto.KdfParams
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.VaultKey
import com.kwazzar.lockruna.domain.session.VaultSession
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContentEquals
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

class DeleteAccountTest {

    private val api = FakeRecoveryApi()
    private val argon2 = FakeArgon2()
    private val normalizer = FakePasswordNormalizer()
    private val crypto = FakeCryptoService()
    private val authStateStore = FakeAuthStateStore()
    private val session = VaultSession()

    private val authKey = ByteArray(32) { 3 }

    private suspend fun wireUp(): DeleteAccount {
        val salt = ByteArray(16) { 1 }
        authStateStore.saveSession(
            AuthenticatedUser(
                userId = "user-1",
                deviceId = "device-1",
                wrappedVaultKey = ByteArray(48),
                kdfSalt = salt,
                kdfParams = KdfParams.SUPPORTED,
                tokens = AuthTokens("access-token", "refresh-token", 3600),
            ),
        )
        session.unlock(VaultKey.from(ByteArray(32) { 5 }))
        normalizer.normalized = "password1234"
        argon2.hashResult = ByteArray(32) { 2 }
        crypto.hkdfResults = mapOf(HkdfLabels.AUTH to authKey)
        return DeleteAccount(api, argon2, normalizer, crypto, authStateStore, session)
    }

    @Test
    fun deletingSendsTheDerivedAuthKeyAndKillsTheLocalSession() = runBlocking {
        val delete = wireUp()

        val result = delete("user@example.com", "password1234")

        assertTrue(result.isSuccess, "expected success, got ${result.error}")
        assertContentEquals(authKey, api.lastDeleteAuthKey)
        assertNull(authStateStore.loadSession())
        assertNull(session.currentVaultKey())
    }

    @Test
    fun aWeakPasswordIsRejectedBeforeAnyCall() = runBlocking {
        val delete = wireUp()

        val result = delete("user@example.com", "short")

        assertTrue(result.isFailure)
        assertTrue(result.error is AppError.WeakMasterPassword)
        assertNotNull(authStateStore.loadSession())
        assertEquals(null, api.lastDeleteAuthKey)
    }

    @Test
    fun withoutAStoredSessionThereIsNothingToDelete() = runBlocking {
        val delete = wireUp()
        authStateStore.clear()

        val result = delete("user@example.com", "password1234")

        assertTrue(result.isFailure)
        assertEquals(AppError.Unauthorized, result.error)
        assertNull(api.lastDeleteAuthKey)
    }

    @Test
    fun aServerFailureKeepsTheLocalSession() = runBlocking {
        val delete = wireUp()
        api.deleteResult = Result.Failure(AppError.NetworkUnavailable)

        val result = delete("user@example.com", "password1234")

        assertTrue(result.isFailure)
        assertEquals(AppError.NetworkUnavailable, result.error)
        assertNotNull(session.currentVaultKey())
        assertEquals("user-1", authStateStore.loadSession()?.userId)
    }
}

class DeviceUseCasesTest {

    @Test
    fun listDevicesReturnsWhatTheApiReturns() = runBlocking {
        val devices = listOf(DeviceInfo(id = "device-1", name = "Phone", platform = "ios", lastSeen = null, current = true))
        val api = FakeRecoveryApi().apply { listResult = Result.Success(devices) }

        val result = ListDevices(api)()

        assertTrue(result.isSuccess)
        assertEquals(devices, (result as Result.Success).value)
    }

    @Test
    fun revokeDevicePassesTheIdThrough() = runBlocking {
        val api = FakeRecoveryApi()

        val result = RevokeDevice(api)("device-9")

        assertTrue(result.isSuccess)
        assertEquals("device-9", api.lastRevokedDeviceId)
    }

    @Test
    fun aFailedListingSurfacesTheApiError() = runBlocking {
        val api = FakeRecoveryApi().apply { listResult = Result.Failure(AppError.NetworkUnavailable) }

        val result = ListDevices(api)()

        assertTrue(result.isFailure)
        assertEquals(AppError.NetworkUnavailable, result.error)
    }
}
