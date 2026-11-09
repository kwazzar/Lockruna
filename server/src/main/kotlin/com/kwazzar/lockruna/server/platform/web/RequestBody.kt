package com.kwazzar.lockruna.server.platform.web

import com.kwazzar.lockruna.server.platform.error.Problem
import com.kwazzar.lockruna.server.platform.error.ProblemException
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveChannel
import io.ktor.utils.io.readBuffer
import kotlinx.io.readByteArray

/**
 * Reads the request body as UTF-8 text and refuses anything larger than [maxBytes] before parsing it.
 */
suspend fun ApplicationCall.receiveTextLimited(maxBytes: Long): String {
    val bytes = receiveChannel().readBuffer(maxBytes + 1).readByteArray()
    if (bytes.size > maxBytes) {
        throw ProblemException(
            problem = Problem(status = 413, code = "payload_too_large", title = "Payload too large"),
        )
    }
    return bytes.toString(Charsets.UTF_8)
}
