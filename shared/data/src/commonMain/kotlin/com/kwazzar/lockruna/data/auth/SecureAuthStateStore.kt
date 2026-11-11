package com.kwazzar.lockruna.data.auth

import com.kwazzar.lockruna.data.platform.SecureStorage
import com.kwazzar.lockruna.domain.auth.AuthStateStore
import com.kwazzar.lockruna.domain.auth.AuthenticatedUser

/** Keeps the session in the platform secure storage; a record that cannot be decoded is dropped. */
class SecureAuthStateStore(private val storage: SecureStorage) : AuthStateStore {

    override suspend fun saveSession(user: AuthenticatedUser) {
        storage.write(KEY, AuthSessionCodec.encode(user))
    }

    override suspend fun loadSession(): AuthenticatedUser? {
        val bytes = storage.read(KEY) ?: return null
        val user = AuthSessionCodec.decode(bytes)
        if (user == null) {
            storage.delete(KEY)
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
