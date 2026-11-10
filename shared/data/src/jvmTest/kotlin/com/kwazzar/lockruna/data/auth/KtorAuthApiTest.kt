package com.kwazzar.lockruna.data.auth

import com.kwazzar.lockruna.domain.auth.AuthTokens
import com.kwazzar.lockruna.domain.auth.AuthenticatedUser
import com.kwazzar.lockruna.domain.auth.SealedBlob
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.crypto.KdfParams
import com.kwazzar.lockruna.domain.error.AppError
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

class InMemoryAuthStateStoreTest {

    private val store = InMemoryAuthStateStore()

    @Test
    fun saveLoadClearRoundtrip() = runBlocking {
        val user = AuthenticatedUser(
            userId = "user-1",
            deviceId = "device-1",
            wrappedVaultKey = SealedBlob.pack(ByteArray(12), ByteArray(32)),
            keyNonce = ByteArray(12),
            kdfSalt = ByteArray(16),
            kdfParams = KdfParams.SUPPORTED,
            tokens = AuthTokens("access", "refresh", 3600)
        )

        store.saveSession(user)
        val loaded = store.loadSession()
        assertEquals(user, loaded)

        store.clear()
        assertNull(store.loadSession())
    }
}

class SessionAccessTokenProviderTest {

    private val authStateStore = InMemoryAuthStateStore()

    @Test
    fun returnsAccessTokenWhenSessionExists() = runBlocking {
        val user = AuthenticatedUser(
            userId = "user-1",
            deviceId = "device-1",
            wrappedVaultKey = SealedBlob.pack(ByteArray(12), ByteArray(32)),
            keyNonce = ByteArray(12),
            kdfSalt = ByteArray(16),
            kdfParams = KdfParams.SUPPORTED,
            tokens = AuthTokens("access-token", "refresh-token", 3600)
        )
        authStateStore.saveSession(user)

        val provider = SessionAccessTokenProvider(authStateStore)
        val token = provider.token()

        assertEquals("access-token", token)
    }

    @Test
    fun returnsNullWhenNoSession() = runBlocking {
        val provider = SessionAccessTokenProvider(authStateStore)
        val token = provider.token()

        assertNull(token)
    }
}