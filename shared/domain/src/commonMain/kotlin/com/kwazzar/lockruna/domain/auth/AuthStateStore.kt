package com.kwazzar.lockruna.domain.auth

interface AuthStateStore {
    suspend fun saveSession(user: AuthenticatedUser)
    suspend fun loadSession(): AuthenticatedUser?
    suspend fun clear()
}
