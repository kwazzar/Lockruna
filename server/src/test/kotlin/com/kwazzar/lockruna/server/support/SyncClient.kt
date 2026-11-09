package com.kwazzar.lockruna.server.support

import com.kwazzar.lockruna.contract.ChangeDto
import com.kwazzar.lockruna.contract.PushRequestDto
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.serialization.json.Json

/**
 * HTTP client for the sync endpoints, shared by the server tests.
 */
class SyncClient(private val builder: ApplicationTestBuilder) {
    suspend fun push(accessToken: String, idempotencyKey: String?, changes: List<ChangeDto>): HttpResponse =
        builder.client.post("/sync/push") {
            contentType(ContentType.Application.Json)
            bearer(accessToken)
            if (idempotencyKey != null) header("Idempotency-Key", idempotencyKey)
            setBody(Json.encodeToString(PushRequestDto.serializer(), PushRequestDto(changes)))
        }

    suspend fun pull(accessToken: String, cursor: String? = null, limit: Int? = null): HttpResponse =
        builder.client.get("/sync") {
            bearer(accessToken)
            if (cursor != null) parameter("cursor", cursor)
            if (limit != null) parameter("limit", limit.toString())
        }
}
