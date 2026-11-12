package com.kwazzar.lockruna.data.auth

import com.kwazzar.lockruna.domain.auth.AuthApi
import com.kwazzar.lockruna.domain.auth.AuthTokens
import com.kwazzar.lockruna.domain.auth.AuthenticatedUser
import com.kwazzar.lockruna.domain.auth.SealedBlob
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.crypto.KdfParams
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val <T, E> Result<T, E>.isSuccess: Boolean
    get() = this is Result.Success

class InMemoryAuthStateStoreTest {

    private val store = InMemoryAuthStateStore()

    @Test
    fun saveLoadClearRoundtrip() = runBlocking {
        val user = AuthenticatedUser(
            userId = "user-1",
            deviceId = "device-1",
            wrappedVaultKey = SealedBlob.pack(ByteArray(12), ByteArray(32)),
            kdfSalt = ByteArray(16),
            kdfParams = KdfParams.SUPPORTED,
            tokens = AuthTokens("access", "refresh", 3600),
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
    private val fixedClock = object : com.kwazzar.lockruna.domain.session.Clock {
        override fun nowMillis(): Long = 1_000_000L
        override fun monotonicMillis(): Long = 0L
    }

    private fun fakeAuthApi(refreshResult: Result<AuthTokens, AppError>? = null) = object : AuthApi {
        override suspend fun prelogin(email: String) = Result.Failure<AppError>(AppError.NetworkUnavailable)
        override suspend fun register(
            email: String,
            authKey: ByteArray,
            recoveryAuthKey: ByteArray,
            kdfSalt: ByteArray,
            kdfParams: KdfParams,
            wrappedVaultKey: ByteArray,
            recoveryWrappedVaultKey: ByteArray,
            device: com.kwazzar.lockruna.domain.auth.DeviceIdentity,
        ) = Result.Failure<AppError>(AppError.NetworkUnavailable)
        override suspend fun login(
            email: String,
            authKey: ByteArray,
            device: com.kwazzar.lockruna.domain.auth.DeviceIdentity,
        ) = Result.Failure<AppError>(AppError.NetworkUnavailable)
        override suspend fun refresh(refreshToken: String) =
            refreshResult ?: Result.Failure(AppError.Unauthorized)
        override suspend fun logout() = Result.Success(Unit)
    }

    @Test
    fun returnsAccessTokenWhenSessionExists() = runBlocking {
        val user = AuthenticatedUser(
            userId = "user-1",
            deviceId = "device-1",
            wrappedVaultKey = SealedBlob.pack(ByteArray(12), ByteArray(32)),
            kdfSalt = ByteArray(16),
            kdfParams = KdfParams.SUPPORTED,
            tokens = AuthTokens("access-token", "refresh-token", 3600, issuedAtMillis = 1_000_000L),
        )
        authStateStore.saveSession(user)

        val provider = SessionAccessTokenProvider(authStateStore, fakeAuthApi(), fixedClock)
        val token = provider.token()

        assertEquals("access-token", token)
    }

    @Test
    fun returnsNullWhenNoSession() = runBlocking {
        val provider = SessionAccessTokenProvider(authStateStore, fakeAuthApi(), fixedClock)
        val token = provider.token()

        assertNull(token)
    }

    @Test
    fun refreshesWhenExpired() = runBlocking {
        val user = AuthenticatedUser(
            userId = "user-1",
            deviceId = "device-1",
            wrappedVaultKey = SealedBlob.pack(ByteArray(12), ByteArray(32)),
            kdfSalt = ByteArray(16),
            kdfParams = KdfParams.SUPPORTED,
            tokens = AuthTokens("old-access", "refresh-token", 3600, issuedAtMillis = 0),
        )
        authStateStore.saveSession(user)

        val api = fakeAuthApi(Result.Success(AuthTokens("new-access", "new-refresh", 3600)))
        val provider = SessionAccessTokenProvider(authStateStore, api, fixedClock)
        val token = provider.token()

        assertEquals("new-access", token)
        assertEquals("new-access", authStateStore.loadSession()?.tokens?.accessToken)
        assertEquals("new-refresh", authStateStore.loadSession()?.tokens?.refreshToken)
    }

    @Test
    fun clearsSessionOnRefreshFailure() = runBlocking {
        val user = AuthenticatedUser(
            userId = "user-1",
            deviceId = "device-1",
            wrappedVaultKey = SealedBlob.pack(ByteArray(12), ByteArray(32)),
            kdfSalt = ByteArray(16),
            kdfParams = KdfParams.SUPPORTED,
            tokens = AuthTokens("old-access", "refresh-token", 3600, issuedAtMillis = 0),
        )
        authStateStore.saveSession(user)

        val provider = SessionAccessTokenProvider(authStateStore, fakeAuthApi(Result.Failure(AppError.Unauthorized)), fixedClock)
        val token = provider.token()

        assertNull(token)
        assertNull(authStateStore.loadSession())
    }
}
