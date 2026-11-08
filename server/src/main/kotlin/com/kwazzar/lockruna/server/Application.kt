package com.kwazzar.lockruna.server

import com.kwazzar.lockruna.server.platform.config.ServerSettings
import com.kwazzar.lockruna.server.platform.di.serverModule
import com.kwazzar.lockruna.server.platform.error.installStatusPages
import com.kwazzar.lockruna.server.platform.logging.installSafeCallLogging
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.koin.ktor.plugin.Koin

fun main() {
    val settings = ServerSettings.fromEnv()

    embeddedServer(Netty, port = settings.port, host = settings.host) {
        module(settings)
    }.start(wait = true)
}

fun Application.module(settings: ServerSettings = ServerSettings.fromEnv()) {
    install(ContentNegotiation) { json() }
    installStatusPages()
    installSafeCallLogging()
    install(Koin) { modules(serverModule(settings)) }

    routing {
        get("/health") {
            call.respondText("""{"status":"ok"}""", ContentType.Application.Json)
        }
    }
}
