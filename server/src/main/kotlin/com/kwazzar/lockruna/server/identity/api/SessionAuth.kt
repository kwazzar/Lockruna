package com.kwazzar.lockruna.server.identity.api

import com.kwazzar.lockruna.server.identity.application.AuthenticateSession
import com.kwazzar.lockruna.server.identity.domain.AccessTokenClaims
import com.kwazzar.lockruna.server.platform.web.bearerToken
import io.ktor.server.application.ApplicationCall

/**
 * Reads the bearer token of the request and checks the session it belongs to.
 */
suspend fun ApplicationCall.requireSession(authenticate: AuthenticateSession): AccessTokenClaims =
    authenticate(bearerToken())
