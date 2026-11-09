package com.kwazzar.lockruna.server.identity

import com.auth0.jwt.JWT
import com.kwazzar.lockruna.server.module
import com.kwazzar.lockruna.server.support.IdentityClient
import com.kwazzar.lockruna.server.support.InMemoryDeviceDenylist
import com.kwazzar.lockruna.server.support.InMemoryRateLimiter
import com.kwazzar.lockruna.server.support.PostgresTest
import com.kwazzar.lockruna.server.support.json
import com.kwazzar.lockruna.server.support.randomBytes
import com.kwazzar.lockruna.server.support.testSettings
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import java.security.MessageDigest
import java.sql.Timestamp
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class TokenFlowTest {
    private val settings = testSettings()

    @BeforeTest
    fun setUp() {
        PostgresTest.migrate()
        PostgresTest.reset()
    }

    @Test
    fun `refresh rotates the token and reusing the old one revokes the family`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter(), InMemoryDeviceDenylist()) }
        val client = IdentityClient(this)
        val authKey = randomBytes(32)
        client.register("user@example.com", authKey)
        val refreshToken = client.login("user@example.com", authKey).json()["refreshToken"]!!.jsonPrimitive.content

        val rotated = client.refresh(refreshToken)

        assertEquals(HttpStatusCode.OK, rotated.status)
        val newToken = rotated.json()["refreshToken"]!!.jsonPrimitive.content
        assertNotEquals(refreshToken, newToken)
        assertNotNull(usedAt(refreshToken))

        assertEquals(HttpStatusCode.Unauthorized, client.refresh(refreshToken).status)
        assertEquals(HttpStatusCode.Unauthorized, client.refresh(newToken).status)
    }

    @Test
    fun `two concurrent refreshes of one token succeed once and revoke the family`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter(), InMemoryDeviceDenylist()) }
        val client = IdentityClient(this)
        val authKey = randomBytes(32)
        client.register("user@example.com", authKey)
        val refreshToken = client.login("user@example.com", authKey).json()["refreshToken"]!!.jsonPrimitive.content

        val responses = coroutineScope {
            val first = async { client.refresh(refreshToken) }
            val second = async { client.refresh(refreshToken) }
            listOf(first.await(), second.await())
        }

        assertEquals(1, responses.count { it.status == HttpStatusCode.OK })
        assertEquals(1, responses.count { it.status == HttpStatusCode.Unauthorized })
        val rotated = responses.single { it.status == HttpStatusCode.OK }.json()
        val newToken = rotated["refreshToken"]!!.jsonPrimitive.content
        assertEquals(HttpStatusCode.Unauthorized, client.refresh(newToken).status)
    }

    @Test
    fun `refresh rejects a family older than ninety days`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter(), InMemoryDeviceDenylist()) }
        val client = IdentityClient(this)
        val authKey = randomBytes(32)
        client.register("user@example.com", authKey)
        val refreshToken = client.login("user@example.com", authKey).json()["refreshToken"]!!.jsonPrimitive.content
        ageFamily(refreshToken)

        val response = client.refresh(refreshToken)

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `the access token carries the device id and expires in fifteen minutes`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter(), InMemoryDeviceDenylist()) }
        val client = IdentityClient(this)
        val authKey = randomBytes(32)
        client.register("user@example.com", authKey)

        val login = client.login("user@example.com", authKey).json()

        assertEquals(900, login["expiresIn"]!!.jsonPrimitive.int)
        val deviceId = login["deviceId"]!!.jsonPrimitive.content
        assertEquals(deviceId, JWT.decode(login["accessToken"]!!.jsonPrimitive.content).getClaim("deviceId").asString())
    }

    @Test
    fun `logout revokes the refresh token of the current device`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter(), InMemoryDeviceDenylist()) }
        val client = IdentityClient(this)
        val authKey = randomBytes(32)
        client.register("user@example.com", authKey)
        val login = client.login("user@example.com", authKey).json()
        val accessToken = login["accessToken"]!!.jsonPrimitive.content
        val refreshToken = login["refreshToken"]!!.jsonPrimitive.content

        val logout = client.logout(accessToken)

        assertEquals(HttpStatusCode.NoContent, logout.status)
        assertEquals(HttpStatusCode.Unauthorized, client.refresh(refreshToken).status)
    }

    private fun usedAt(refreshToken: String): Timestamp? = PostgresTest.dataSource.connection.use { connection ->
        connection.prepareStatement("SELECT used_at FROM refresh_tokens WHERE token_hash = ?").use { statement ->
            statement.setBytes(1, sha256(refreshToken))
            statement.executeQuery().use { rows -> if (rows.next()) rows.getTimestamp(1) else null }
        }
    }

    private fun ageFamily(refreshToken: String) {
        PostgresTest.dataSource.connection.use { connection ->
            connection.prepareStatement(
                "UPDATE refresh_tokens SET family_created_at = now() - interval '91 days' WHERE token_hash = ?",
            ).use { statement ->
                statement.setBytes(1, sha256(refreshToken))
                statement.executeUpdate()
            }
        }
    }

    private fun sha256(value: String): ByteArray = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
}
