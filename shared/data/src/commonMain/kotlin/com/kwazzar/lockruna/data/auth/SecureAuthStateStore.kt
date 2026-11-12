package com.kwazzar.lockruna.data.auth

import com.kwazzar.lockruna.data.platform.SecureStorage
import com.kwazzar.lockruna.domain.auth.AuthStateStore
import com.kwazzar.lockruna.domain.auth.AuthenticatedUser
import com.kwazzar.lockruna.domain.session.Clock

/** Keeps the session in the platform secure storage; a record that cannot be decoded is dropped. */
class SecureAuthStateStore(private val storage: SecureStorage, private val clock: Clock) : AuthStateStore {

    override suspend fun saveSession(user: AuthenticatedUser) {
        val stamped = user.copy(tokens = user.tokens.copy(issuedAtMillis = clock.nowMillis()))
        storage.write(KEY, AuthSessionCodec.encode(stamped))
    }

    override suspend fun loadSession(): AuthenticatedUser? {
        val bytes = storage.read(KEY) ?: return null
        val user = AuthSessionCodec.decode(bytes)
        // A record without the AAD email can never unwrap the vault key: drop it and
        // let the user sign in again rather than offer an unlock that always fails.
        if (user == null || user.email.isEmpty()) {
            storage.delete(KEY)
            return null
        }
        return user
    }

    override suspend fun clear() {
        storage.delete(KEY)
    }

    private companion object {
        private const val KEY = "auth_session"
    }
}
