package com.kwazzar.lockruna.data.auth

import com.kwazzar.lockruna.domain.auth.AuthStateStore
import com.kwazzar.lockruna.domain.sync.AccessTokenProvider

class SessionAccessTokenProvider(private val authStateStore: AuthStateStore) : AccessTokenProvider {
    override suspend fun token(): String? = authStateStore.loadSession()?.tokens?.accessToken
}
