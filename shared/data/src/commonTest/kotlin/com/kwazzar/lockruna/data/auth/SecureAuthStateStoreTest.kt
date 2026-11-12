package com.kwazzar.lockruna.data.auth

import com.kwazzar.lockruna.data.platform.InMemorySecureStorage
import com.kwazzar.lockruna.domain.auth.AuthTokens
import com.kwazzar.lockruna.domain.auth.AuthenticatedUser
import com.kwazzar.lockruna.domain.crypto.KdfParams
import kotlinx.coroutines.test.runTest
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SecureAuthStateStoreTest {

    private val sessionKey = "auth_session"

    private fun sampleUser(suffix: String = "1") = AuthenticatedUser(
        userId = "user-$suffix",
        deviceId = "device-$suffix",
        wrappedVaultKey = byteArrayOf(0x11, 0x22, 0x33, 0x44, 0x55),
        kdfSalt = byteArrayOf(0x21, 0x22, 0x23, 0x24),
        kdfParams = KdfParams(memoryKiB = 65536, iterations = 3, parallelism = 1),
        tokens = AuthTokens(accessToken = "access-$suffix", refreshToken = "refresh-$suffix", expiresInSeconds = 900),
    )

    @Test
    fun `a saved session round-trips with every field`() = runTest {
        val store = SecureAuthStateStore(InMemorySecureStorage())
        val user = sampleUser()

        store.saveSession(user)
        val loaded = store.loadSession()

        assertEquals(user.userId, loaded?.userId)
        assertEquals(user.deviceId, loaded?.deviceId)
        assertContentEquals(user.wrappedVaultKey, loaded?.wrappedVaultKey)
        assertContentEquals(user.kdfSalt, loaded?.kdfSalt)
        assertEquals(user.kdfParams, loaded?.kdfParams)
        assertEquals(user.tokens, loaded?.tokens)
    }

    @Test
    fun `no saved session loads as null without throwing`() = runTest {
        val store = SecureAuthStateStore(InMemorySecureStorage())

        assertNull(store.loadSession())
    }

    @Test
    fun `clear drops the saved session`() = runTest {
        val storage = InMemorySecureStorage()
        val store = SecureAuthStateStore(storage)
        store.saveSession(sampleUser())

        store.clear()

        assertNull(store.loadSession())
        assertTrue(storage.keys.isEmpty())
    }

    @Test
    fun `saving twice overwrites the single record`() = runTest {
        val storage = InMemorySecureStorage()
        val store = SecureAuthStateStore(storage)
        store.saveSession(sampleUser("1"))

        store.saveSession(sampleUser("2"))

        assertEquals(setOf(sessionKey), storage.keys)
        assertEquals("user-2", store.loadSession()?.userId)
    }

    @Test
    fun `a corrupted record loads as null and is deleted`() = runTest {
        val storage = InMemorySecureStorage(mapOf(sessionKey to "not json".encodeToByteArray()))
        val store = SecureAuthStateStore(storage)

        assertNull(store.loadSession())
        assertTrue(storage.keys.isEmpty())
    }

    @Test
    fun `the session toString exposes no key material`() = runTest {
        val user = sampleUser()
        val rendered = user.toString()

        assertFalse(rendered.contains(Base64.encode(user.wrappedVaultKey)))
        assertFalse(rendered.contains(Base64.encode(user.kdfSalt)))
        assertFalse(rendered.contains(user.tokens.accessToken))
        assertFalse(rendered.contains(user.tokens.refreshToken))
    }
}
