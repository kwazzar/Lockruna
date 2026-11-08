package com.kwazzar.lockruna.server.identity.api

import com.kwazzar.lockruna.server.identity.application.AuthenticateSession
import com.kwazzar.lockruna.server.identity.domain.AccessTokenClaims
import com.kwazzar.lockruna.server.platform.error.unauthorized
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall

/**
 * Reads the bearer token of the request and checks the session it belongs to.
 */
suspend fun ApplicationCall.requireSession(authenticate: AuthenticateSession): AccessTokenClaims {
    val header = request.headers[HttpHeaders.Authorization] ?: throw unauthorized()
    val parts = header.split(' ', limit = 2)
    if (parts.size != 2 || !parts[0].equals("Bearer", ignoreCase = true)) throw unauthorized()
    return authenticate(parts[1].trim())
}
