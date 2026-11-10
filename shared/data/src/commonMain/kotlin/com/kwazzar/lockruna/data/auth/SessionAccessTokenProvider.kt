package com.kwazzar.lockruna.data.auth

import com.kwazzar.lockruna.domain.auth.AccessTokenProvider
import com.kwazzar.lockruna.domain.auth.AuthStateStore

class SessionAccessTokenProvider(private val authStateStore: AuthStateStore) : AccessTokenProvider {
    override suspend fun token(): String? = authStateStore.loadSession()?.tokens?.accessToken
}
