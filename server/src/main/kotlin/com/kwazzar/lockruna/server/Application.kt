package com.kwazzar.lockruna.server

import io.ktor.http.ContentType
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main() {
    val config = ServerConfig.fromEnv()

    embeddedServer(Netty, port = config.port, host = config.host) {
        routing {
            get("/health") {
                call.respondText("""{"status":"ok"}""", ContentType.Application.Json)
            }
        }
    }.start(wait = true)
}
