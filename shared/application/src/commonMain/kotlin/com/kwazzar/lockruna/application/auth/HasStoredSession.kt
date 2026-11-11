package com.kwazzar.lockruna.application.auth

import com.kwazzar.lockruna.domain.auth.AuthStateStore

/** Tells the app shell whether a session is stored, so it can pick the start screen. */
class HasStoredSession(private val authStateStore: AuthStateStore) {
    suspend operator fun invoke(): Boolean = authStateStore.loadSession() != null
}
