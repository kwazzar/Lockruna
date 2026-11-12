package com.kwazzar.lockruna.server.identity.api

import com.kwazzar.lockruna.contract.AuthResponseDto
import com.kwazzar.lockruna.contract.DeleteAccountRequest
import com.kwazzar.lockruna.contract.LoginRequest
import com.kwazzar.lockruna.contract.PreloginRequest
import com.kwazzar.lockruna.contract.PreloginResponse
import com.kwazzar.lockruna.contract.RecoveryCompleteRequest
import com.kwazzar.lockruna.contract.RecoveryStartResponseDto
import com.kwazzar.lockruna.contract.RefreshRequest
import com.kwazzar.lockruna.contract.RegisterRequest
import com.kwazzar.lockruna.contract.TokenPairDto
import com.kwazzar.lockruna.server.identity.application.AuthSession
import com.kwazzar.lockruna.server.identity.application.AuthenticateSession
import com.kwazzar.lockruna.server.identity.application.CompleteRecovery
import com.kwazzar.lockruna.server.identity.application.CompleteRecoveryCommand
import com.kwazzar.lockruna.server.identity.application.DeleteAccount
import com.kwazzar.lockruna.server.identity.application.LoginCommand
import com.kwazzar.lockruna.server.identity.application.LoginUser
import com.kwazzar.lockruna.server.identity.application.LogoutSession
import com.kwazzar.lockruna.server.identity.application.PreloginUser
import com.kwazzar.lockruna.server.identity.application.RefreshSession
import com.kwazzar.lockruna.server.identity.application.RegisterCommand
import com.kwazzar.lockruna.server.identity.application.RegisterUser
import com.kwazzar.lockruna.server.identity.application.StartRecovery
import com.kwazzar.lockruna.server.platform.error.Problem
import com.kwazzar.lockruna.server.platform.error.ProblemException
import com.kwazzar.lockruna.server.platform.web.receiveTextLimited
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.origin
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import java.util.Base64

/**
 * Identity endpoints: prelogin, register, login, refresh, logout, recovery.
 */
fun Route.identityRoutes() {
    val preloginUser by inject<PreloginUser>()
    val registerUser by inject<RegisterUser>()
    val loginUser by inject<LoginUser>()
    val refreshSession by inject<RefreshSession>()
    val logoutSession by inject<LogoutSession>()
    val authenticate by inject<AuthenticateSession>()
    val startRecovery by inject<StartRecovery>()
    val completeRecovery by inject<CompleteRecovery>()

    route("/auth") {
        post("/prelogin") {
            val request = call.receiveJson<PreloginRequest>()

            val result = preloginUser(request.email, call.clientIp())

            call.respond(PreloginResponse(kdfSalt = result.kdfSalt.toBase64(), kdfParams = result.kdfParams))
        }

        post("/register") {
            val request = call.receiveJson<RegisterRequest>()

            val session = registerUser(request.toCommand(call.clientIp()))

            call.respond(HttpStatusCode.Created, session.toResponse())
        }

        post("/login") {
            val request = call.receiveJson<LoginRequest>()

            val session = loginUser(request.toCommand(call.clientIp()))

            call.respond(session.toResponse())
        }

        post("/recovery/start") {
            val request = call.receiveJson<PreloginRequest>()

            val result = startRecovery(request.email, call.clientIp())

            call.respond(
                RecoveryStartResponseDto(
                    userId = result.userId,
                    recoveryWrappedVaultKey = result.recoveryWrappedVaultKey.toBase64(),
                ),
            )
        }

        post("/recovery/complete") {
            val request = call.receiveJson<RecoveryCompleteRequest>()

            val session = completeRecovery(request.toCommand(call.clientIp()))

            call.respond(session.toResponse())
        }

        post("/refresh") {
            val request = call.receiveJson<RefreshRequest>()

            val pair = refreshSession(request.refreshToken)

            call.respond(
                TokenPairDto(
                    accessToken = pair.accessToken,
                    refreshToken = pair.refreshToken,
                    expiresIn = pair.expiresIn,
                ),
            )
        }

        post("/logout") {
            val claims = call.requireSession(authenticate)

            logoutSession(claims.userId, claims.deviceId)

            call.respond(HttpStatusCode.NoContent)
        }
    }
}

/** Deleting the account and everything under it (US-11). */
fun Route.accountRoutes() {
    val authenticate by inject<AuthenticateSession>()
    val deleteAccount by inject<DeleteAccount>()

    delete("/account") {
        val claims = call.requireSession(authenticate)
        val request = call.receiveJson<DeleteAccountRequest>()

        deleteAccount(claims.userId, request.authKey.fromBase64())

        call.respond(HttpStatusCode.NoContent)
    }
}

private fun RegisterRequest.toCommand(clientIp: String) = RegisterCommand(
    email = email,
    authKey = authKey.fromBase64(),
    recoveryAuthKey = recoveryAuthKey.fromBase64(),
    kdfSalt = kdfSalt.fromBase64(),
    kdfParams = kdfParams,
    wrappedVaultKey = wrappedVaultKey.fromBase64(),
    recoveryWrappedVaultKey = recoveryWrappedVaultKey.fromBase64(),
    deviceName = device.name,
    devicePlatform = device.platform.requirePlatform(),
    clientIp = clientIp,
)

private fun RecoveryCompleteRequest.toCommand(clientIp: String) = CompleteRecoveryCommand(
    email = email,
    recoveryAuthKey = recoveryAuthKey.fromBase64(),
    newAuthKey = newAuthKey.fromBase64(),
    newKdfSalt = newKdfSalt.fromBase64(),
    newKdfParams = newKdfParams,
    newWrappedVaultKey = newWrappedVaultKey.fromBase64(),
    deviceName = device.name,
    devicePlatform = device.platform.requirePlatform(),
    clientIp = clientIp,
)

private fun LoginRequest.toCommand(clientIp: String) = LoginCommand(
    email = email,
    authKey = authKey.fromBase64(),
    deviceId = device.deviceId,
    deviceName = device.name,
    devicePlatform = device.platform.requirePlatform(),
    clientIp = clientIp,
)

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

private suspend inline fun <reified T> ApplicationCall.receiveJson(): T {
    val body = receiveTextLimited(IDENTITY_BODY_LIMIT)
    return try {
        Json.decodeFromString<T>(body)
    } catch (invalid: SerializationException) {
        throw ProblemException(
            problem = Problem(status = 400, code = "invalid_request", title = "Invalid request"),
            cause = invalid,
        )
    }
}

private const val IDENTITY_BODY_LIMIT = 64L * 1024
