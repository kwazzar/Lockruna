package com.kwazzar.lockruna.data.auth

import com.kwazzar.lockruna.domain.auth.AuthStateStore
import com.kwazzar.lockruna.domain.auth.AuthenticatedUser

class InMemoryAuthStateStore : AuthStateStore {
    private var session: AuthenticatedUser? = null

    override suspend fun saveSession(user: AuthenticatedUser) {
        session = user
    }

    override suspend fun loadSession(): AuthenticatedUser? {
        return session
    }

    override suspend fun clear() {
        session = null
    }
}