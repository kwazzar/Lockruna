package com.kwazzar.lockruna.data.sync

import com.kwazzar.lockruna.contract.PushRequestDto
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.sync.AccessTokenProvider
import com.kwazzar.lockruna.domain.sync.PendingChange
import com.kwazzar.lockruna.domain.vault.VaultItemType
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val BASE_URL = "https://api.test"
private const val EPOCH_2026_11_10 = 1_794_304_800_000L

class KtorSyncApiTest {

    private fun api(engine: MockEngine, token: String? = "token"): KtorSyncApi = KtorSyncApi(
        httpClient = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        },
        baseUrl = BASE_URL,
        accessTokenProvider = object : AccessTokenProvider {
            override suspend fun token(): String? = token
        },
    )

    private fun change() = PendingChange(
        id = "item-1",
        type = VaultItemType.LOGIN,
        ciphertext = byteArrayOf(1, 2, 3),
        nonce = byteArrayOf(4),
        baseVersion = 0,
    )

    private suspend fun MockRequestHandleScope.jsonResponse(content: String) = respond(
        content = content,
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
    )

    @Test
    fun `push sends the batch with bearer token and idempotency key`() = runTest {
        var captured: HttpRequestData? = null
        val engine = MockEngine { request ->
            captured = request
            jsonResponse(
                """{"applied":[{"id":"item-1","version":3,"updatedAt":"2026-11-10T10:00:00Z"}],"conflicts":[]}""",
            )
        }

        val result = api(engine).push(listOf(change()), "key-1")

        assertTrue(result is Result.Success)
        assertEquals("Bearer token", captured?.headers?.get(HttpHeaders.Authorization))
        assertEquals("key-1", captured?.headers?.get("Idempotency-Key"))
        assertEquals("/sync/push", captured?.url?.encodedPath)

        val body = (captured?.body as? TextContent)?.text
        val sent = Json.decodeFromString(PushRequestDto.serializer(), requireNotNull(body))
        assertEquals("item-1", sent.changes.single().id)
        assertEquals("login", sent.changes.single().type)
        assertEquals("AQID", sent.changes.single().ciphertext)
        assertEquals(0L, sent.changes.single().baseVersion)
    }

    @Test
    fun `push maps an applied change`() = runTest {
        val engine = MockEngine {
            jsonResponse(
                """{"applied":[{"id":"item-1","version":3,"updatedAt":"2026-11-10T10:00:00Z"}],"conflicts":[]}""",
            )
        }

        val result = api(engine).push(listOf(change()), "key-1")

        val applied = (result as Result.Success).value.applied.single()
        assertEquals("item-1", applied.id)
        assertEquals(3L, applied.newVersion)
        assertEquals(EPOCH_2026_11_10, applied.updatedAtMillis)
    }

    @Test
    fun `pull maps a tombstone to a deleted item`() = runTest {
        var captured: HttpRequestData? = null
        val engine = MockEngine { request ->
            captured = request
            jsonResponse(
                """{"items":[{"id":"item-1","type":"login","ciphertext":null,"nonce":null,"version":4,""" +
                    """"deletedAt":"2026-11-10T10:00:00Z","updatedAt":"2026-11-10T10:00:00Z"}],""" +
                    """"nextCursor":"cursor-2","hasMore":false}""",
            )
        }

        val result = api(engine).pull("cursor-1")

        val page = (result as Result.Success).value
        val item = page.items.single()
        assertTrue(item.deleted)
        assertEquals(4L, item.version)
        assertEquals(EPOCH_2026_11_10, item.updatedAtMillis)
        assertEquals("cursor-2", page.nextCursor)
        assertFalse(page.hasMore)
        assertEquals("/sync", captured?.url?.encodedPath)
        assertEquals("cursor-1", captured?.url?.parameters?.get("cursor"))
    }

    @Test
    fun `push maps a repeated idempotency key to a conflict`() = runTest {
        val engine = MockEngine { respond(content = "", status = HttpStatusCode.Conflict) }

        val result = api(engine).push(listOf(change()), "key-1")

        assertEquals(Result.Failure(AppError.IdempotencyConflict), result)
    }

    @Test
    fun `push maps a rate limited response`() = runTest {
        val engine = MockEngine {
            respond(
                content = "",
                status = HttpStatusCode.TooManyRequests,
                headers = headersOf(HttpHeaders.RetryAfter, "30"),
            )
        }

        val result = api(engine).push(listOf(change()), "key-1")

        assertEquals(Result.Failure(AppError.RateLimited(30)), result)
    }

    @Test
    fun `push maps an unauthorized response`() = runTest {
        val engine = MockEngine { respond(content = "", status = HttpStatusCode.Unauthorized) }

        val result = api(engine).push(listOf(change()), "key-1")

        assertEquals(Result.Failure(AppError.Unauthorized), result)
    }

    @Test
    fun `push maps a transport failure to a network error`() = runTest {
        val engine = MockEngine { throw IOException("offline") }

        val result = api(engine).push(listOf(change()), "key-1")

        assertEquals(Result.Failure(AppError.NetworkUnavailable), result)
    }

    @Test
    fun `pull without a token fails without a request`() = runTest {
        var calls = 0
        val engine = MockEngine {
            calls++
            jsonResponse("""{"items":[],"nextCursor":"","hasMore":false}""")
        }

        val result = api(engine, token = null).pull(null)

        assertEquals(Result.Failure(AppError.Unauthorized), result)
        assertEquals(0, calls)
    }

    @Test
    fun `pull maps a body the client cannot read`() = runTest {
        val engine = MockEngine { respond(content = "not json", status = HttpStatusCode.OK) }

        val result = api(engine).pull(null)

        assertEquals(Result.Failure(AppError.NetworkUnavailable), result)
    }
}
