package com.kwazzar.lockruna.application.auth

import com.kwazzar.lockruna.application.support.FakeAuthStateStore
import com.kwazzar.lockruna.domain.auth.AuthTokens
import com.kwazzar.lockruna.domain.auth.AuthenticatedUser
import com.kwazzar.lockruna.domain.auth.SealedBlob
import com.kwazzar.lockruna.domain.crypto.KdfParams
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HasStoredSessionTest {

    private val authStateStore = FakeAuthStateStore()
    private val hasStoredSession = HasStoredSession(authStateStore)

    @Test
    fun noSessionMeansNoStoredSession() = runBlocking {
        assertFalse(hasStoredSession())
    }

    @Test
    fun aSavedSessionMeansAStoredSession() = runBlocking {
        authStateStore.saveSession(
            AuthenticatedUser(
                userId = "user-1",
                deviceId = "device-1",
                wrappedVaultKey = SealedBlob.pack(ByteArray(12), ByteArray(48)),
                keyNonce = ByteArray(12),
                kdfSalt = ByteArray(16),
                kdfParams = KdfParams.SUPPORTED,
                tokens = AuthTokens("access", "refresh", 3600),
            ),
        )

        assertTrue(hasStoredSession())
    }
}
