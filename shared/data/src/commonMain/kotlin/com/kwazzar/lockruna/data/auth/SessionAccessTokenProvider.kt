package com.kwazzar.lockruna.data.auth

import com.kwazzar.lockruna.domain.auth.AuthApi
import com.kwazzar.lockruna.domain.auth.AuthStateStore
import com.kwazzar.lockruna.domain.auth.AuthenticatedUser
import com.kwazzar.lockruna.domain.session.Clock
import com.kwazzar.lockruna.domain.sync.AccessTokenProvider

/**
 * Reads the access token from the stored session, refreshing it transparently when
 * it is about to expire (or its issuance is unknown). On a failed refresh the
 * session is cleared so the caller surfaces an unauthorized state.
 */
class SessionAccessTokenProvider(
    private val authStateStore: AuthStateStore,
    private val authApi: AuthApi,
    private val clock: Clock,
) : AccessTokenProvider {

    override suspend fun token(): String? {
        val session = authStateStore.loadSession() ?: return null
        val tokens = session.tokens
        if (!tokens.isExpired(clock.nowMillis())) {
            return tokens.accessToken
        }
        return refresh(session)
    }

    private suspend fun refresh(session: AuthenticatedUser): String? {
        val result = authApi.refresh(session.tokens.refreshToken)
        return when (result) {
            is com.kwazzar.lockruna.domain.core.Result.Success -> {
                val newTokens = result.value
                authStateStore.saveSession(session.copy(tokens = newTokens))
                newTokens.accessToken
            }
            is com.kwazzar.lockruna.domain.core.Result.Failure -> {
                authStateStore.clear()
                null
            }
        }
    }
}
