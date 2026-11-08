package com.kwazzar.lockruna.server.identity.domain

import java.time.Instant

/**
 * Issues the credentials of a session: a short-lived access JWT and a rotating refresh token.
 */
interface TokenService {
    fun issueAccessToken(userId: String, deviceId: String): String

    suspend fun issueRefreshToken(userId: String, deviceId: String, familyId: String, familyCreatedAt: Instant): String

    fun verifyAccessToken(token: String): AccessTokenClaims
}

data class AccessTokenClaims(val userId: String, val deviceId: String)

/** Lifetime of an access token in seconds. */
const val ACCESS_TOKEN_SECONDS = 900
