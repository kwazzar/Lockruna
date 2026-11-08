package com.kwazzar.lockruna.server.platform.error

import kotlinx.serialization.Serializable

/**
 * RFC 9457 problem document. `code` is the stable machine-readable identifier clients branch on.
 */
@Serializable
data class Problem(val status: Int, val code: String, val title: String, val type: String = "about:blank")

/**
 * Thrown by use cases and routes to produce a problem response. The message carries the title only,
 * never request data. `headers` carries response headers such as `Retry-After`.
 */
class ProblemException(val problem: Problem, val headers: Map<String, String> = emptyMap(), cause: Throwable? = null) :
    RuntimeException(problem.title, cause)

/** The answer for a missing, malformed or revoked access token. */
fun unauthorized(): ProblemException =
    ProblemException(problem = Problem(status = 401, code = "unauthorized", title = "Unauthorized"))
