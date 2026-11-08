package com.kwazzar.lockruna.server.platform.error

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val problemJson = Json

fun Application.installStatusPages() {
    install(StatusPages) {
        exception<ProblemException> { call, cause -> call.respondProblem(cause.problem) }
        exception<Throwable> { call, _ ->
            call.respondProblem(
                Problem(status = 500, code = "internal_error", title = "Internal server error"),
            )
        }
    }
}

suspend fun ApplicationCall.respondProblem(problem: Problem) {
    respondText(
        text = problemJson.encodeToString(problem),
        contentType = ContentType.Application.ProblemJson,
        status = HttpStatusCode.fromValue(problem.status),
    )
}
