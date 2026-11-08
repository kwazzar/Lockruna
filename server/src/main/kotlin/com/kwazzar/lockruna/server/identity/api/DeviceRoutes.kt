package com.kwazzar.lockruna.server.identity.api

import com.kwazzar.lockruna.contract.DeviceDto
import com.kwazzar.lockruna.server.identity.application.AuthenticateSession
import com.kwazzar.lockruna.server.identity.application.ListDevices
import com.kwazzar.lockruna.server.identity.application.RevokeDevice
import com.kwazzar.lockruna.server.identity.domain.DeviceRecord
import com.kwazzar.lockruna.server.platform.error.Problem
import com.kwazzar.lockruna.server.platform.error.ProblemException
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

/**
 * The devices of the signed-in user.
 */
fun Route.deviceRoutes() {
    val authenticate by inject<AuthenticateSession>()
    val listDevices by inject<ListDevices>()
    val revokeDevice by inject<RevokeDevice>()

    route("/devices") {
        get {
            val claims = call.requireSession(authenticate)

            val devices = listDevices(claims.userId)

            call.respond(devices.map { it.toDto(currentDeviceId = claims.deviceId) })
        }

        delete("/{deviceId}") {
            val claims = call.requireSession(authenticate)
            val deviceId = call.parameters["deviceId"] ?: throw ProblemException(
                problem = Problem(status = 400, code = "invalid_request", title = "Invalid request"),
            )

            revokeDevice(claims.userId, deviceId)

            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun DeviceRecord.toDto(currentDeviceId: String) = DeviceDto(
    id = id,
    name = name,
    platform = platform,
    lastSeen = lastSeen?.toString(),
    current = id == currentDeviceId,
)
