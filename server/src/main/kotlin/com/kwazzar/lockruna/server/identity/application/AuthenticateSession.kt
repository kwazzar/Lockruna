package com.kwazzar.lockruna.server.identity.application

import com.kwazzar.lockruna.server.identity.domain.AccessTokenClaims
import com.kwazzar.lockruna.server.identity.domain.DeviceDenylist
import com.kwazzar.lockruna.server.identity.domain.TokenService
import com.kwazzar.lockruna.server.platform.error.unauthorized

/**
 * Accepts a bearer token only when it is a valid access token of a device that was not revoked.
 */
class AuthenticateSession(private val tokens: TokenService, private val denylist: DeviceDenylist) {
    suspend operator fun invoke(token: String): AccessTokenClaims {
        val claims = tokens.verifyAccessToken(token) ?: throw unauthorized()
        if (denylist.isRevoked(claims.deviceId)) throw unauthorized()
        return claims
    }
}
