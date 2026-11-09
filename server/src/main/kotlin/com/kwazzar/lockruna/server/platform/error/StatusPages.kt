package com.kwazzar.lockruna.server.platform.error

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.respondText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

private val problemJson = Json
private val logger = LoggerFactory.getLogger("com.kwazzar.lockruna.server.error")

fun Application.installStatusPages() {
    install(StatusPages) {
        exception<ProblemException> { call, cause ->
            cause.headers.forEach { (name, value) -> call.response.headers.append(name, value) }
            call.respondProblem(cause.problem)
        }
        exception<BadRequestException> { call, _ ->
            call.respondProblem(
                Problem(status = 400, code = "invalid_request", title = "Invalid request"),
            )
        }
        exception<ContentTransformationException> { call, _ ->
            call.respondProblem(
                Problem(status = 400, code = "invalid_request", title = "Invalid request"),
            )
        }
        exception<Throwable> { call, cause ->
            if (cause is CancellationException) throw cause
            // The class and the path only: the message may carry request data.
            logger.error("Unhandled {} on {}", cause::class.qualifiedName, call.request.path())
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
