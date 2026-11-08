package com.kwazzar.lockruna.server.identity.api

import com.kwazzar.lockruna.contract.AuthResponseDto
import com.kwazzar.lockruna.contract.LoginRequest
import com.kwazzar.lockruna.contract.PreloginRequest
import com.kwazzar.lockruna.contract.PreloginResponse
import com.kwazzar.lockruna.contract.RegisterRequest
import com.kwazzar.lockruna.server.identity.application.AuthSession
import com.kwazzar.lockruna.server.identity.application.LoginCommand
import com.kwazzar.lockruna.server.identity.application.LoginUser
import com.kwazzar.lockruna.server.identity.application.PreloginUser
import com.kwazzar.lockruna.server.identity.application.RegisterCommand
import com.kwazzar.lockruna.server.identity.application.RegisterUser
import com.kwazzar.lockruna.server.platform.error.Problem
import com.kwazzar.lockruna.server.platform.error.ProblemException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.origin
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject
import java.util.Base64

/**
 * Identity endpoints: prelogin, register and login.
 */
fun Route.identityRoutes() {
    val preloginUser by inject<PreloginUser>()
    val registerUser by inject<RegisterUser>()
    val loginUser by inject<LoginUser>()

    route("/auth") {
        post("/prelogin") {
            val request = call.receive<PreloginRequest>()

            val result = preloginUser(request.email)

            call.respond(PreloginResponse(kdfSalt = result.kdfSalt.toBase64(), kdfParams = result.kdfParams))
        }

        post("/register") {
            val request = call.receive<RegisterRequest>()

            val session = registerUser(
                RegisterCommand(
                    email = request.email,
                    authKey = request.authKey.fromBase64(),
                    recoveryAuthKey = request.recoveryAuthKey.fromBase64(),
                    kdfSalt = request.kdfSalt.fromBase64(),
                    kdfParams = request.kdfParams,
                    wrappedVaultKey = request.wrappedVaultKey.fromBase64(),
                    recoveryWrappedVaultKey = request.recoveryWrappedVaultKey.fromBase64(),
                    deviceName = request.device.name,
                    devicePlatform = request.device.platform.requirePlatform(),
                    clientIp = call.clientIp(),
                ),
            )

            call.respond(HttpStatusCode.Created, session.toResponse())
        }

        post("/login") {
            val request = call.receive<LoginRequest>()

            val session = loginUser(
                LoginCommand(
                    email = request.email,
                    authKey = request.authKey.fromBase64(),
                    deviceId = request.device.deviceId,
                    deviceName = request.device.name,
                    devicePlatform = request.device.platform.requirePlatform(),
                    clientIp = call.clientIp(),
                ),
            )

            call.respond(session.toResponse())
        }
    }
}

private fun AuthSession.toResponse() = AuthResponseDto(
    accessToken = accessToken,
    refreshToken = refreshToken,
    expiresIn = expiresIn,
    userId = userId,
    deviceId = deviceId,
    kdfSalt = kdfSalt.toBase64(),
    kdfParams = kdfParams,
    wrappedVaultKey = wrappedVaultKey.toBase64(),
)

private fun ByteArray.toBase64(): String = Base64.getEncoder().encodeToString(this)

private fun String.fromBase64(): ByteArray = try {
    Base64.getDecoder().decode(this)
} catch (invalid: IllegalArgumentException) {
    throw ProblemException(
        problem = Problem(status = 400, code = "invalid_request", title = "Invalid request"),
        cause = invalid,
    )
}

private fun String.requirePlatform(): String {
    if (this != "ios" && this != "android") {
        throw ProblemException(problem = Problem(status = 400, code = "invalid_request", title = "Invalid request"))
    }
    return this
}

private fun ApplicationCall.clientIp(): String = request.origin.remoteHost
