package com.kwazzar.lockruna.server.platform.web

import com.kwazzar.lockruna.server.platform.error.unauthorized
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall

/**
 * The bearer token of the request, or the answer for a missing or malformed one.
 */
fun ApplicationCall.bearerToken(): String {
    val header = request.headers[HttpHeaders.Authorization] ?: throw unauthorized()
    val parts = header.split(' ', limit = 2)
    if (parts.size != 2 || !parts[0].equals("Bearer", ignoreCase = true)) throw unauthorized()
    return parts[1].trim()
}
