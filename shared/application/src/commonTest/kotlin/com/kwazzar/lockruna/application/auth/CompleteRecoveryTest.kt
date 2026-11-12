package com.kwazzar.lockruna.application.auth

import com.kwazzar.lockruna.application.support.FakeArgon2
import com.kwazzar.lockruna.application.support.FakeAuthStateStore
import com.kwazzar.lockruna.application.support.FakeCryptoService
import com.kwazzar.lockruna.application.support.FakeIdGenerator
import com.kwazzar.lockruna.application.support.FakePasswordNormalizer
import com.kwazzar.lockruna.application.support.FakeRecoveryApi
import com.kwazzar.lockruna.domain.auth.DevicePlatform
import com.kwazzar.lockruna.domain.auth.SealedBlob
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.crypto.Bip39
import com.kwazzar.lockruna.domain.error.AppError
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

class CompleteRecoveryTest {

    private val recoveryApi = FakeRecoveryApi()
    private val argon2 = FakeArgon2()
    private val normalizer = FakePasswordNormalizer()
    private val crypto = FakeCryptoService()
    private val authStateStore = FakeAuthStateStore()
    private val session = VaultSession()
    private val idGenerator = FakeIdGenerator()

    private val recoveryKey = ByteArray(Bip39.ENTROPY_BYTES) { 6 }
    private val vaultKey = ByteArray(32) { 5 }

    private fun wireUp(): CompleteRecovery {
        val password = "newPassword1234"
        crypto.randomBytesResults = mapOf(
            16 to ByteArray(16) { 1 },
            32 to vaultKey,
            Bip39.ENTROPY_BYTES to recoveryKey,
            12 to ByteArray(12) { 9 },
        )
        normalizer.normalized = password
        argon2.hashResult = ByteArray(32) { 2 }
        crypto.decryptGcmResult = vaultKey
        recoveryApi.startResult = Result.Success(
            com.kwazzar.lockruna.domain.auth.RecoveryStartData(
                userId = "user-1",
                recoveryWrappedVaultKey = SealedBlob.pack(ByteArray(12), ByteArray(48)),
            ),
        )
        return CompleteRecovery(
            recoveryApi = recoveryApi,
            argon2 = argon2,
            normalizer = normalizer,
            crypto = crypto,
            authStateStore = authStateStore,
            session = session,
            idGenerator = idGenerator,
        )
    }

    @Test
    fun aValidRecoveryKeyRestoresTheVaultAndRotatesTheMasterPassword() = runBlocking {
        val recovery = wireUp()

        val result = recovery(
            RecoverCommand(
                email = "User@Example.com",
                recoveryWords = Bip39.encode(recoveryKey, crypto),
                newPassword = "newPassword1234",
                deviceName = "New Phone",
                deviceId = null,
                platform = DevicePlatform.IOS,
            ),
        )

        assertTrue(result.isSuccess, "expected success, got ${result.error}")
        // The vault key comes back from the recovery wrapper.
        assertNotNull(session.currentVaultKey())
        assertContentEquals(vaultKey, session.currentVaultKey()!!.bytes)
        // The new master password replaces the old one.
        assertNotNull(authStateStore.loadSession())
        assertTrue(recoveryApi.lastCompleteNewAuthKey!!.isNotEmpty())
        assertTrue(recoveryApi.lastCompleteRecoveryAuthKey!!.isNotEmpty())
        assertNotNull(recoveryApi.lastCompleteWrappedVaultKey)
        // The canonical email was used for every call.
        assertEquals("user@example.com", recoveryApi.lastStartEmail)
        assertEquals("user@example.com", recoveryApi.lastCompleteEmail)
        // The device the user is recovering on.
        assertEquals(DevicePlatform.IOS, recoveryApi.lastCompleteDevice?.platform)
        assertEquals("New Phone", recoveryApi.lastCompleteDevice?.name)
    }

    @Test
    fun aWrongWordCountNeverReachesTheServer() = runBlocking {
        val recovery = wireUp()

        val result = recovery(
            RecoverCommand(
                email = "user@example.com",
                recoveryWords = Bip39.encode(recoveryKey, crypto).take(11),
                newPassword = "newPassword1234",
                deviceName = "New Phone",
                deviceId = null,
                platform = DevicePlatform.ANDROID,
            ),
        )

        assertTrue(result.isFailure)
        assertTrue(result.error is AppError.InvalidRecoveryKey)
        assertEquals(0, recoveryApi.startCalls)
        assertNull(session.currentVaultKey())
    }

    @Test
    fun aWeakNewPasswordIsRejected() = runBlocking {
        val recovery = wireUp()

        val result = recovery(
            RecoverCommand(
                email = "user@example.com",
                recoveryWords = Bip39.encode(recoveryKey, crypto),
                newPassword = "short",
                deviceName = "New Phone",
                deviceId = null,
                platform = DevicePlatform.ANDROID,
            ),
        )

        assertTrue(result.isFailure)
        assertTrue(result.error is AppError.WeakMasterPassword)
        assertEquals(0, recoveryApi.startCalls)
    }

    @Test
    fun aServerRejectionKeepsTheVaultLocked() = runBlocking {
        val recovery = wireUp()
        recoveryApi.completeResult = Result.Failure(AppError.InvalidCredentials)

        val result = recovery(
            RecoverCommand(
                email = "user@example.com",
                recoveryWords = Bip39.encode(recoveryKey, crypto),
                newPassword = "newPassword1234",
                deviceName = "New Phone",
                deviceId = null,
                platform = DevicePlatform.ANDROID,
            ),
        )

        assertTrue(result.isFailure)
        assertEquals(AppError.InvalidCredentials, result.error)
        assertNull(session.currentVaultKey())
        assertNull(authStateStore.loadSession())
    }

    @Test
    fun anUnwrappableBlobIsAnInvalidCredential() = runBlocking {
        val recovery = wireUp()
        recoveryApi.startResult = Result.Success(
            com.kwazzar.lockruna.domain.auth.RecoveryStartData(
                userId = "user-1",
                recoveryWrappedVaultKey = SealedBlob.pack(ByteArray(12), ByteArray(48)),
            ),
        )
        crypto.decryptGcmFails = true

        val result = recovery(
            RecoverCommand(
                email = "user@example.com",
                recoveryWords = Bip39.encode(recoveryKey, crypto),
                newPassword = "newPassword1234",
                deviceName = "New Phone",
                deviceId = null,
                platform = DevicePlatform.ANDROID,
            ),
        )

        assertTrue(result.isFailure)
        assertEquals(AppError.InvalidCredentials, result.error)
    }
}
