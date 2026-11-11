package com.kwazzar.lockruna.data.platform

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.session.Clock
import com.kwazzar.lockruna.domain.session.IdGenerator
import com.kwazzar.lockruna.domain.vault.ItemId
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import platform.CoreFoundation.CFAbsoluteTimeGetCurrent
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSUUID

actual fun platformClock(): Clock = object : Clock {
    override fun nowMillis(): Long = (CFAbsoluteTimeGetCurrent() * MILLIS_PER_SECOND).toLong()

    override fun monotonicMillis(): Long = (NSProcessInfo.processInfo.systemUptime * MILLIS_PER_SECOND).toLong()
}

actual fun platformIdGenerator(): IdGenerator = object : IdGenerator {
    override fun newItemId(): ItemId = when (val parsed = ItemId.of(newUuid())) {
        is Result.Success -> parsed.value
        is Result.Failure -> error("a generated uuid is always a valid item id")
    }

    override fun newUuid(): String = NSUUID().UUIDString
}

actual fun platformHttpClient(): HttpClient = HttpClient(Darwin) {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    install(HttpTimeout) {
        requestTimeoutMillis = HTTP_TIMEOUT_MILLIS
        connectTimeoutMillis = HTTP_TIMEOUT_MILLIS
        socketTimeoutMillis = HTTP_TIMEOUT_MILLIS
    }
}

private const val MILLIS_PER_SECOND = 1_000.0
private const val HTTP_TIMEOUT_MILLIS = 30_000L
