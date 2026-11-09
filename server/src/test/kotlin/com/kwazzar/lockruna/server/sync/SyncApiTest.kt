package com.kwazzar.lockruna.server.sync

import com.kwazzar.lockruna.contract.ChangeDto
import com.kwazzar.lockruna.contract.PushResultDto
import com.kwazzar.lockruna.contract.SyncPageDto
import com.kwazzar.lockruna.server.module
import com.kwazzar.lockruna.server.support.IdentityClient
import com.kwazzar.lockruna.server.support.InMemoryDeviceDenylist
import com.kwazzar.lockruna.server.support.InMemoryRateLimiter
import com.kwazzar.lockruna.server.support.PostgresTest
import com.kwazzar.lockruna.server.support.SyncClient
import com.kwazzar.lockruna.server.support.bearer
import com.kwazzar.lockruna.server.support.json
import com.kwazzar.lockruna.server.support.randomBytes
import com.kwazzar.lockruna.server.support.testSettings
import com.kwazzar.lockruna.server.sync.application.PushChanges
import com.kwazzar.lockruna.server.sync.domain.ChangeRecord
import com.kwazzar.lockruna.server.sync.infrastructure.ExposedIdempotencyRepository
import com.kwazzar.lockruna.server.sync.infrastructure.ExposedVaultItemRepository
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SyncApiTest {
    private val settings = testSettings()

    @BeforeTest
    fun setUp() {
        PostgresTest.migrate()
        PostgresTest.reset()
    }

    @Test
    fun `push without an idempotency key is rejected`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter(), InMemoryDeviceDenylist()) }
        val client = SyncClient(this)
        val token = registerUser("user@example.com")

        val response = client.push(token, null, listOf(change()))

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("idempotency_key_required", response.json()["code"]!!.jsonPrimitive.content)
    }

    @Test
    fun `push of more than 100 changes is rejected`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter(), InMemoryDeviceDenylist()) }
        val client = SyncClient(this)
        val token = registerUser("user@example.com")

        val response = client.push(token, UUID.randomUUID().toString(), List(101) { change() })

        assertEquals(HttpStatusCode.PayloadTooLarge, response.status)
    }

    @Test
    fun `a new item is applied with version 1 and the same key replays the same body`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter(), InMemoryDeviceDenylist()) }
        val client = SyncClient(this)
        val token = registerUser("user@example.com")
        val key = UUID.randomUUID().toString()
        val changes = listOf(change())

        val first = client.push(token, key, changes)

        assertEquals(HttpStatusCode.OK, first.status)
        val result = Json.decodeFromString<PushResultDto>(first.bodyAsText())
        assertEquals(1, result.applied.size)
        assertEquals(1L, result.applied.single().version)
        assertEquals(changes.single().id, result.applied.single().id)
        assertEquals(0, result.conflicts.size)

        val replay = client.push(token, key, changes)

        assertEquals(HttpStatusCode.OK, replay.status)
        assertEquals(first.bodyAsText(), replay.bodyAsText())
    }

    @Test
    fun `the same idempotency key with another body is rejected`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter(), InMemoryDeviceDenylist()) }
        val client = SyncClient(this)
        val token = registerUser("user@example.com")
        val key = UUID.randomUUID().toString()
        client.push(token, key, listOf(change()))

        val response = client.push(token, key, listOf(change()))

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals("idempotency_key_reuse", response.json()["code"]!!.jsonPrimitive.content)
    }

    @Test
    fun `an item that already exists is a conflict with the server version`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter(), InMemoryDeviceDenylist()) }
        val client = SyncClient(this)
        val token = registerUser("user@example.com")
        val item = change()
        client.push(token, UUID.randomUUID().toString(), listOf(item))

        val response = client.push(token, UUID.randomUUID().toString(), listOf(item))

        assertEquals(HttpStatusCode.OK, response.status)
        val result = Json.decodeFromString<PushResultDto>(response.bodyAsText())
        assertEquals(0, result.applied.size)
        assertEquals(1, result.conflicts.size)
        assertEquals(item.id, result.conflicts.single().id)
        assertEquals(1L, result.conflicts.single().server.version)
    }

    @Test
    fun `two devices pushing the same version produce one applied and one conflict`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter(), InMemoryDeviceDenylist()) }
        val client = SyncClient(this)
        val identity = IdentityClient(this)
        val authKey = randomBytes(32)
        identity.register("user@example.com", authKey)
        val firstToken = identity.login("user@example.com", authKey).json()["accessToken"]!!.jsonPrimitive.content
        val secondToken = identity.login("user@example.com", authKey).json()["accessToken"]!!.jsonPrimitive.content
        val changes = listOf(change())

        val responses = coroutineScope {
            val first = async { client.push(firstToken, UUID.randomUUID().toString(), changes) }
            val second = async { client.push(secondToken, UUID.randomUUID().toString(), changes) }
            listOf(first.await(), second.await())
        }

        val results = responses.map { Json.decodeFromString<PushResultDto>(it.bodyAsText()) }
        assertEquals(1, results.count { it.applied.isNotEmpty() })
        assertEquals(1, results.count { it.conflicts.isNotEmpty() })
    }

    @Test
    fun `pull returns the items with an opaque cursor and stops at the end`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter(), InMemoryDeviceDenylist()) }
        val client = SyncClient(this)
        val token = registerUser("user@example.com")
        val item = change()
        client.push(token, UUID.randomUUID().toString(), listOf(item))
        client.push(
            token,
            UUID.randomUUID().toString(),
            listOf(item.copy(baseVersion = 1, deleted = true, ciphertext = null, nonce = null)),
        )

        val page = Json.decodeFromString<SyncPageDto>(client.pull(token).bodyAsText())

        assertEquals(1, page.items.size)
        val stored = page.items.single()
        assertEquals(item.id, stored.id)
        assertEquals(2L, stored.version)
        assertNotNull(stored.deletedAt)
        assertNull(stored.ciphertext)
        assertNull(stored.nonce)
        assertNotEquals("2", page.nextCursor)

        val tail = Json.decodeFromString<SyncPageDto>(client.pull(token, page.nextCursor).bodyAsText())

        assertEquals(0, tail.items.size)
        assertEquals(false, tail.hasMore)
    }

    @Test
    fun `a user neither sees nor changes the items of another user`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter(), InMemoryDeviceDenylist()) }
        val client = SyncClient(this)
        val ownerToken = registerUser("owner@example.com")
        val otherToken = registerUser("other@example.com")
        val item = change()
        client.push(ownerToken, UUID.randomUUID().toString(), listOf(item))

        val otherPage = Json.decodeFromString<SyncPageDto>(client.pull(otherToken).bodyAsText())

        assertEquals(0, otherPage.items.size)

        val response = client.push(otherToken, UUID.randomUUID().toString(), listOf(item.copy(baseVersion = 1)))

        assertEquals(HttpStatusCode.OK, response.status)
        val result = Json.decodeFromString<PushResultDto>(response.bodyAsText())
        assertEquals(0, result.applied.size)
        assertEquals(1, result.conflicts.size)

        val ownerPage = Json.decodeFromString<SyncPageDto>(client.pull(ownerToken).bodyAsText())
        assertEquals(1L, ownerPage.items.single().version)
    }

    @Test
    fun `a ciphertext of more than 64 KiB is rejected`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter(), InMemoryDeviceDenylist()) }
        val client = SyncClient(this)
        val token = registerUser("user@example.com")

        val response = client.push(
            token,
            UUID.randomUUID().toString(),
            listOf(change(ciphertext = randomBytes(64 * 1024 + 1))),
        )

        assertEquals(HttpStatusCode.PayloadTooLarge, response.status)
    }

    @Test
    fun `a change with a non-uuid id is rejected`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter(), InMemoryDeviceDenylist()) }
        val client = SyncClient(this)
        val token = registerUser("user@example.com")

        val response = client.push(token, UUID.randomUUID().toString(), listOf(change(id = "not-a-uuid")))

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("invalid_request", response.json()["code"]!!.jsonPrimitive.content)
    }

    @Test
    fun `a change with an unknown type is rejected`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter(), InMemoryDeviceDenylist()) }
        val client = SyncClient(this)
        val token = registerUser("user@example.com")

        val response = client.push(token, UUID.randomUUID().toString(), listOf(change(type = "bogus")))

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `a live change without ciphertext or nonce is rejected`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter(), InMemoryDeviceDenylist()) }
        val client = SyncClient(this)
        val token = registerUser("user@example.com")

        val withoutCiphertext = client.push(
            token,
            UUID.randomUUID().toString(),
            listOf(change().copy(ciphertext = null)),
        )
        val withoutNonce = client.push(token, UUID.randomUUID().toString(), listOf(change().copy(nonce = null)))

        assertEquals(HttpStatusCode.BadRequest, withoutCiphertext.status)
        assertEquals(HttpStatusCode.BadRequest, withoutNonce.status)
    }

    @Test
    fun `a push body of more than ten mebibytes is rejected`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter(), InMemoryDeviceDenylist()) }
        val token = registerUser("user@example.com")
        val body = """{"changes":[]}""" + " ".repeat(11 * 1024 * 1024)

        val response = client.post("/sync/push") {
            contentType(ContentType.Application.Json)
            bearer(token)
            header("Idempotency-Key", UUID.randomUUID().toString())
            setBody(body)
        }

        assertEquals(HttpStatusCode.PayloadTooLarge, response.status)
    }

    @Test
    fun `the sixty first push within a minute is blocked`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter(), InMemoryDeviceDenylist()) }
        val client = SyncClient(this)
        val token = registerUser("user@example.com")
        val key = UUID.randomUUID().toString()
        val changes = listOf(change())

        repeat(60) { assertEquals(HttpStatusCode.OK, client.push(token, key, changes).status) }

        val blocked = client.push(token, key, changes)

        assertEquals(HttpStatusCode.TooManyRequests, blocked.status)
        assertNotNull(blocked.headers["Retry-After"])
    }

    @Test
    fun `two concurrent updates of the same version apply once`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter(), InMemoryDeviceDenylist()) }
        val client = SyncClient(this)
        val identity = IdentityClient(this)
        val authKey = randomBytes(32)
        identity.register("user@example.com", authKey)
        val firstToken = identity.login("user@example.com", authKey).json()["accessToken"]!!.jsonPrimitive.content
        val secondToken = identity.login("user@example.com", authKey).json()["accessToken"]!!.jsonPrimitive.content
        val item = change()
        client.push(firstToken, UUID.randomUUID().toString(), listOf(item))

        val responses = coroutineScope {
            val first = async {
                client.push(firstToken, UUID.randomUUID().toString(), listOf(item.copy(baseVersion = 1)))
            }
            val second = async {
                client.push(secondToken, UUID.randomUUID().toString(), listOf(item.copy(baseVersion = 1)))
            }
            listOf(first.await(), second.await())
        }

        val results = responses.map { Json.decodeFromString<PushResultDto>(it.bodyAsText()) }
        assertEquals(1, results.count { it.applied.isNotEmpty() })
        assertEquals(2L, results.single { it.applied.isNotEmpty() }.applied.single().version)
        assertEquals(1, results.count { it.conflicts.isNotEmpty() })
        assertEquals(2L, results.single { it.conflicts.isNotEmpty() }.conflicts.single().server.version)
    }

    @Test
    fun `a push that cannot answer applies nothing`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter(), InMemoryDeviceDenylist()) }
        val userId = registerUserId("user@example.com")
        val vaultItems = ExposedVaultItemRepository(PostgresTest.transactions)
        val push = PushChanges(
            vaultItems,
            ExposedIdempotencyRepository(PostgresTest.transactions),
            PostgresTest.transactions,
            InMemoryRateLimiter(),
        )

        assertFailsWith<IllegalStateException> {
            push(userId, UUID.randomUUID().toString(), ByteArray(32), listOf(record())) { error("no answer") }
        }

        assertEquals(0, vaultItems.listSince(userId, 0, 10).size)
    }
}

private suspend fun ApplicationTestBuilder.register(email: String): JsonObject =
    IdentityClient(this).register(email, randomBytes(32)).json()

private suspend fun ApplicationTestBuilder.registerUser(email: String): String =
    register(email)["accessToken"]!!.jsonPrimitive.content

private suspend fun ApplicationTestBuilder.registerUserId(email: String): String =
    register(email)["userId"]!!.jsonPrimitive.content

private fun record(id: String = UUID.randomUUID().toString()): ChangeRecord = ChangeRecord(
    id = id,
    type = "login",
    ciphertext = randomBytes(32),
    nonce = randomBytes(12),
    baseVersion = 0,
    deleted = false,
)

private fun change(
    id: String = UUID.randomUUID().toString(),
    type: String = "login",
    ciphertext: ByteArray = randomBytes(32),
): ChangeDto = ChangeDto(
    id = id,
    type = type,
    ciphertext = Base64.getEncoder().encodeToString(ciphertext),
    nonce = Base64.getEncoder().encodeToString(randomBytes(12)),
    baseVersion = 0,
    deleted = false,
)
