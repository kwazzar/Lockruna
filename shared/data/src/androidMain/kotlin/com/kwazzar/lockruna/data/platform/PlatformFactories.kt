package com.kwazzar.lockruna.data.platform

import android.os.SystemClock
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.session.Clock
import com.kwazzar.lockruna.domain.session.IdGenerator
import com.kwazzar.lockruna.domain.vault.ItemId
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.util.UUID

actual fun platformClock(): Clock = object : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun monotonicMillis(): Long = SystemClock.elapsedRealtime()
}

actual fun platformIdGenerator(): IdGenerator = object : IdGenerator {
    override fun newItemId(): ItemId = when (val parsed = ItemId.of(newUuid())) {
        is Result.Success -> parsed.value
        is Result.Failure -> error("a generated uuid is always a valid item id")
    }

    override fun newUuid(): String = UUID.randomUUID().toString()
}

actual fun platformHttpClient(): HttpClient = HttpClient(OkHttp) {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    install(HttpTimeout) {
        requestTimeoutMillis = HTTP_TIMEOUT_MILLIS
        connectTimeoutMillis = HTTP_TIMEOUT_MILLIS
        socketTimeoutMillis = HTTP_TIMEOUT_MILLIS
    }
}

private const val HTTP_TIMEOUT_MILLIS = 30_000L
