package com.kwazzar.lockruna.server.sync

import com.kwazzar.lockruna.contract.ChangeDto
import com.kwazzar.lockruna.contract.PushResultDto
import com.kwazzar.lockruna.server.module
import com.kwazzar.lockruna.server.support.IdentityClient
import com.kwazzar.lockruna.server.support.InMemoryDeviceDenylist
import com.kwazzar.lockruna.server.support.InMemoryRateLimiter
import com.kwazzar.lockruna.server.support.PostgresTest
import com.kwazzar.lockruna.server.support.SyncClient
import com.kwazzar.lockruna.server.support.json
import com.kwazzar.lockruna.server.support.randomBytes
import com.kwazzar.lockruna.server.support.testSettings
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The archive of an item keeps at most ten versions and only the replaced ones.
 */
class ItemHistoryTest {
    private val settings = testSettings()

    @BeforeTest
    fun setUp() {
        PostgresTest.migrate()
        PostgresTest.reset()
    }

    @Test
    fun `history keeps the ten newest versions of an item`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter(), InMemoryDeviceDenylist()) }
        val client = SyncClient(this)
        val user = registerUser(this)
        val itemId = UUID.randomUUID().toString()

        val created = client.push(user.token, UUID.randomUUID().toString(), listOf(change(itemId, 0)))
        assertEquals(HttpStatusCode.OK, created.status)
        for (baseVersion in 1L..12L) {
            val response = client.push(user.token, UUID.randomUUID().toString(), listOf(change(itemId, baseVersion)))
            assertEquals(HttpStatusCode.OK, response.status)
        }

        val versions = historyVersions(user.userId, itemId)

        assertEquals(10, versions.size)
        assertEquals((3L..12L).toList(), versions)
    }

    @Test
    fun `a conflict creates no history row`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter(), InMemoryDeviceDenylist()) }
        val client = SyncClient(this)
        val user = registerUser(this)
        val itemId = UUID.randomUUID().toString()

        val created = client.push(user.token, UUID.randomUUID().toString(), listOf(change(itemId, 0)))
        assertEquals(HttpStatusCode.OK, created.status)
        val applied = client.push(user.token, UUID.randomUUID().toString(), listOf(change(itemId, 1)))
        assertEquals(HttpStatusCode.OK, applied.status)

        val conflict = client.push(user.token, UUID.randomUUID().toString(), listOf(change(itemId, 1)))
        assertEquals(HttpStatusCode.OK, conflict.status)
        assertEquals(1, Json.decodeFromString<PushResultDto>(conflict.bodyAsText()).conflicts.size)

        assertEquals(listOf(1L), historyVersions(user.userId, itemId))
    }
}

private data class RegisteredUser(val token: String, val userId: String)

private suspend fun registerUser(builder: ApplicationTestBuilder): RegisteredUser {
    val registration = IdentityClient(builder).register("user@example.com", randomBytes(32)).json()
    return RegisteredUser(
        token = registration["accessToken"]!!.jsonPrimitive.content,
        userId = registration["userId"]!!.jsonPrimitive.content,
    )
}

private fun change(id: String, baseVersion: Long): ChangeDto = ChangeDto(
    id = id,
    type = "login",
    ciphertext = Base64.getEncoder().encodeToString(randomBytes(32)),
    nonce = Base64.getEncoder().encodeToString(randomBytes(12)),
    baseVersion = baseVersion,
    deleted = false,
)

private fun historyVersions(userId: String, itemId: String): List<Long> =
    PostgresTest.dataSource.connection.use { connection ->
        val statement = connection.prepareStatement(
            "SELECT version FROM item_history WHERE user_id = ? AND item_id = ? ORDER BY version",
        )
        statement.use {
            it.setObject(1, UUID.fromString(userId))
            it.setObject(2, UUID.fromString(itemId))
            it.executeQuery().use { rows ->
                buildList { while (rows.next()) add(rows.getLong("version")) }
            }
        }
    }
