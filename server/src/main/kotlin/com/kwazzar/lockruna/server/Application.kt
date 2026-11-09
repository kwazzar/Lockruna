package com.kwazzar.lockruna.server

import com.kwazzar.lockruna.server.identity.api.deviceRoutes
import com.kwazzar.lockruna.server.identity.api.identityRoutes
import com.kwazzar.lockruna.server.identity.domain.DeviceDenylist
import com.kwazzar.lockruna.server.identity.domain.RateLimiter
import com.kwazzar.lockruna.server.platform.config.ServerSettings
import com.kwazzar.lockruna.server.platform.db.DatabaseFactory
import com.kwazzar.lockruna.server.platform.di.serverModule
import com.kwazzar.lockruna.server.platform.error.installStatusPages
import com.kwazzar.lockruna.server.platform.logging.installSafeCallLogging
import com.kwazzar.lockruna.server.platform.redis.RedisDeviceDenylist
import com.kwazzar.lockruna.server.platform.redis.RedisRateLimiter
import com.kwazzar.lockruna.server.sync.api.syncRoutes
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.koin.ktor.plugin.Koin
import javax.sql.DataSource

fun main() {
    val settings = ServerSettings.fromEnv()

    embeddedServer(Netty, port = settings.port, host = settings.host) {
        module(settings)
    }.start(wait = true)
}

fun Application.module(
    settings: ServerSettings = ServerSettings.fromEnv(),
    dataSource: DataSource? = null,
    rateLimiter: RateLimiter? = null,
    denylist: DeviceDenylist? = null,
) {
    val source = dataSource ?: DatabaseFactory.createDataSource(settings.databaseUrl).also { owned ->
        monitor.subscribe(ApplicationStopped) { owned.close() }
    }
    val limiter = rateLimiter ?: RedisRateLimiter(settings.redisUrl).also { owned ->
        monitor.subscribe(ApplicationStopped) { owned.close() }
    }
    val blocked = denylist ?: RedisDeviceDenylist(settings.redisUrl).also { owned ->
        monitor.subscribe(ApplicationStopped) { owned.close() }
    }

    install(ContentNegotiation) { json() }
    installStatusPages()
    installSafeCallLogging()
    install(Koin) { modules(serverModule(settings, source, limiter, blocked)) }

    routing {
        get("/health") {
            call.respondText("""{"status":"ok"}""", ContentType.Application.Json)
        }
        identityRoutes()
        deviceRoutes()
        syncRoutes()
    }
}
