package com.kwazzar.lockruna.application.auth

import com.kwazzar.lockruna.application.support.FakeArgon2
import com.kwazzar.lockruna.application.support.FakeAuthStateStore
import com.kwazzar.lockruna.application.support.FakeBiometricPolicyStore
import com.kwazzar.lockruna.domain.auth.AuthTokens
import com.kwazzar.lockruna.domain.auth.AuthenticatedUser
import com.kwazzar.lockruna.domain.auth.SealedBlob
import com.kwazzar.lockruna.domain.crypto.KdfParams
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthStateStoreTest {

    private val fakeStore = FakeAuthStateStore()

    @Test
    fun saveLoadClearRoundtrip() = runBlocking {
        val user = createUser()
        fakeStore.saveSession(user)
        val loaded = fakeStore.loadSession()
        assertEquals(user, loaded)

        fakeStore.clear()
        assertNull(fakeStore.loadSession())
    }

    private fun createUser(): AuthenticatedUser = AuthenticatedUser(
        userId = "user-1",
        deviceId = "device-1",
        wrappedVaultKey = SealedBlob.pack(ByteArray(12), ByteArray(32)),
        keyNonce = ByteArray(12),
        kdfSalt = ByteArray(16),
        kdfParams = KdfParams.SUPPORTED,
        tokens = AuthTokens("access", "refresh", 3600),
    )
}

class BiometricPolicyStoreTest {

    private val store = FakeBiometricPolicyStore()

    @Test
    fun failureCounterIncrementsAndResets() = runBlocking {
        assertEquals(0, store.failures())
        store.recordFailure()
        assertEquals(1, store.failures())
        store.recordFailure()
        assertEquals(2, store.failures())
        store.resetFailures()
        assertEquals(0, store.failures())
    }

    @Test
    fun lastSuccessfulUnlockTimestamp() = runBlocking {
        assertNull(store.lastSuccessfulUnlock())
        store.setLastSuccessfulUnlock(1234567890L)
        assertEquals(1234567890L, store.lastSuccessfulUnlock())
    }
}

class FakeArgon2Test {

    @Test
    fun deterministicHashForSameInput() = runBlocking {
        val argon2 = FakeArgon2()
        val salt = "salt".toByteArray()
        val params = KdfParams.SUPPORTED

        val hash1 = argon2.hash("password", salt, params)
        val hash2 = argon2.hash("password", salt, params)
        val hash3 = argon2.hash("different", salt, params)

        assertEquals(hash1.contentToString(), hash2.contentToString())
        assertTrue(hash1.contentToString() != hash3.contentToString())
    }

    @Test
    fun differentSaltProducesDifferentHash() = runBlocking {
        val argon2 = FakeArgon2()
        val params = KdfParams.SUPPORTED

        val hash1 = argon2.hash("password", "salt1".toByteArray(), params)
        val hash2 = argon2.hash("password", "salt2".toByteArray(), params)

        assertTrue(hash1.contentToString() != hash2.contentToString())
    }
}
