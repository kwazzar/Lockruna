package com.kwazzar.lockruna.server.sync.api

import com.kwazzar.lockruna.contract.AppliedChangeDto
import com.kwazzar.lockruna.contract.ChangeDto
import com.kwazzar.lockruna.contract.ConflictDto
import com.kwazzar.lockruna.contract.EncryptedItemDto
import com.kwazzar.lockruna.contract.ItemTypeDto
import com.kwazzar.lockruna.contract.PushRequestDto
import com.kwazzar.lockruna.contract.PushResultDto
import com.kwazzar.lockruna.contract.SyncPageDto
import com.kwazzar.lockruna.server.identity.application.AuthenticateSession
import com.kwazzar.lockruna.server.platform.error.Problem
import com.kwazzar.lockruna.server.platform.error.ProblemException
import com.kwazzar.lockruna.server.platform.web.bearerToken
import com.kwazzar.lockruna.server.platform.web.receiveTextLimited
import com.kwazzar.lockruna.server.sync.application.AppliedChange
import com.kwazzar.lockruna.server.sync.application.Conflict
import com.kwazzar.lockruna.server.sync.application.PullChanges
import com.kwazzar.lockruna.server.sync.application.PulledPage
import com.kwazzar.lockruna.server.sync.application.PushChanges
import com.kwazzar.lockruna.server.sync.application.PushOutcome
import com.kwazzar.lockruna.server.sync.domain.ChangeRecord
import com.kwazzar.lockruna.server.sync.domain.Cursor
import com.kwazzar.lockruna.server.sync.domain.SealedItemRecord
import io.ktor.http.ContentType
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID

/**
 * The sync endpoints: the feed of the vault and the single write path.
 */
fun Route.syncRoutes() {
    val authenticate by inject<AuthenticateSession>()
    val pushChanges by inject<PushChanges>()
    val pullChanges by inject<PullChanges>()

    route("/sync") {
        get {
            val claims = authenticate(call.bearerToken())

            val page = pullChanges(claims.userId, call.request.queryParameters["cursor"], call.limit())

            call.respond(page.toDto())
        }

        post("/push") {
            val claims = authenticate(call.bearerToken())
            val key = call.idempotencyKey()
            val body = call.receiveTextLimited(PUSH_BODY_LIMIT)
            val request = parse(body)

            val response = pushChanges(
                claims.userId,
                key,
                bodyHash(body),
                request.changes.map { it.toRecord() },
            ) { outcome -> Json.encodeToString(PushResultDto.serializer(), outcome.toDto()) }

            call.respondText(response, ContentType.Application.Json)
        }
    }
}

private fun ApplicationCall.limit(): Int {
    val raw = request.queryParameters["limit"] ?: return DEFAULT_LIMIT
    val limit = raw.toIntOrNull() ?: throw invalidRequest()
    if (limit !in 1..MAX_LIMIT) throw invalidRequest()
    return limit
}

private fun ApplicationCall.idempotencyKey(): String {
    val key = request.headers["Idempotency-Key"] ?: throw ProblemException(
        problem = Problem(
            status = 400,
            code = "idempotency_key_required",
            title = "Idempotency key required",
        ),
    )
    if (key.asUuid() == null) throw invalidRequest()
    return key
}

private fun parse(body: String): PushRequestDto = try {
    Json.decodeFromString(PushRequestDto.serializer(), body)
} catch (invalid: SerializationException) {
    throw ProblemException(
        problem = Problem(status = 400, code = "invalid_request", title = "Invalid request"),
        cause = invalid,
    )
}

private fun bodyHash(body: String): ByteArray =
    MessageDigest.getInstance("SHA-256").digest(body.toByteArray(Charsets.UTF_8))

private fun ChangeDto.toRecord(): ChangeRecord {
    if (id.asUuid() == null) throw invalidRequest()
    if (!deleted && (ciphertext == null || nonce == null)) throw invalidRequest()
    return ChangeRecord(
        id = id,
        type = type.requireItemType(),
        ciphertext = ciphertext?.fromBase64(),
        nonce = nonce?.fromBase64(),
        baseVersion = baseVersion,
        deleted = deleted,
    )
}

private fun String.requireItemType(): String =
    ItemTypeDto.entries.firstOrNull { it.name.equals(this, ignoreCase = true) }?.name?.lowercase()
        ?: throw invalidRequest()

private fun String.fromBase64(): ByteArray = try {
    Base64.getDecoder().decode(this)
} catch (invalid: IllegalArgumentException) {
    throw ProblemException(
        problem = Problem(status = 400, code = "invalid_request", title = "Invalid request"),
        cause = invalid,
    )
}

private fun String.asUuid(): UUID? = try {
    UUID.fromString(this)
} catch (ignored: IllegalArgumentException) {
    null
}

private fun PulledPage.toDto() = SyncPageDto(
    items = items.map { it.toDto() },
    nextCursor = Cursor.encode(nextCursor),
    hasMore = hasMore,
)

private fun SealedItemRecord.toDto() = EncryptedItemDto(
    id = id,
    type = type,
    ciphertext = ciphertext?.let { Base64.getEncoder().encodeToString(it) },
    nonce = nonce?.let { Base64.getEncoder().encodeToString(it) },
    version = version,
    deletedAt = deletedAt?.toString(),
    updatedAt = updatedAt.toString(),
)

private fun PushOutcome.toDto() = PushResultDto(
    applied = applied.map { it.toDto() },
    conflicts = conflicts.map { it.toDto() },
)

private fun AppliedChange.toDto() = AppliedChangeDto(
    id = id,
    version = version,
    updatedAt = updatedAt.toString(),
)

private fun Conflict.toDto() = ConflictDto(id = id, server = server.toDto())

private fun invalidRequest() = ProblemException(
    problem = Problem(status = 400, code = "invalid_request", title = "Invalid request"),
)

private const val DEFAULT_LIMIT = 100
private const val MAX_LIMIT = 500
private const val PUSH_BODY_LIMIT = 10L * 1024 * 1024
