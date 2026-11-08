package com.kwazzar.lockruna.server.platform.logging

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.calllogging.processingTimeMillis
import io.ktor.server.request.path

/**
 * Call logs carry method, path, status and duration only — never request or response bodies.
 */
object SafeCallLogging {
    fun format(method: String, path: String, status: Int, durationMs: Long): String =
        "$method $path $status ${durationMs}ms"
}

fun Application.installSafeCallLogging() {
    install(CallLogging) {
        format { call ->
            SafeCallLogging.format(
                method = call.request.local.method.value,
                path = call.request.path(),
                status = call.response.status()?.value ?: HttpStatusCode.OK.value,
                durationMs = call.processingTimeMillis(),
            )
        }
    }
}
