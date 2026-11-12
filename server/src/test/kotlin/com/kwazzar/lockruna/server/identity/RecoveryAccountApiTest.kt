package com.kwazzar.lockruna.server.identity

import com.kwazzar.lockruna.contract.DeleteAccountRequest
import com.kwazzar.lockruna.contract.DeviceInfoDto
import com.kwazzar.lockruna.contract.KdfParamsDto
import com.kwazzar.lockruna.contract.PreloginRequest
import com.kwazzar.lockruna.contract.RecoveryCompleteRequest
import com.kwazzar.lockruna.server.module
import com.kwazzar.lockruna.server.support.InMemoryDeviceDenylist
import com.kwazzar.lockruna.server.support.InMemoryRateLimiter
import com.kwazzar.lockruna.server.support.PostgresTest
import com.kwazzar.lockruna.server.support.json
import com.kwazzar.lockruna.server.support.randomBytes
import com.kwazzar.lockruna.server.support.testSettings
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Recovery and account deletion: anti-enumeration, key material replacement and cascade removal.
 */
class RecoveryAccountApiTest {
    private val settings = testSettings()
    private val json = Json

    @BeforeTest
    fun setUp() {
        PostgresTest.migrate()
        PostgresTest.reset()
    }

    @Test
    fun `recovery start for an unknown email returns deterministic fake data`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter(), InMemoryDeviceDenylist()) }

        val first = recoveryStart("nobody@example.com")
        val second = recoveryStart("nobody@example.com")

        assertEquals(HttpStatusCode.OK, first.status)
        assertEquals(first.bodyAsText(), second.bodyAsText())
        val body = first.parsed()
        assertTrue(body["userId"]!!.jsonPrimitive.content.isNotBlank())
        assertEquals(48, Base64.getDecoder().decode(body["recoveryWrappedVaultKey"]!!.jsonPrimitive.content).size)
    }

    @Test
    fun `recovery start for a known email returns the stored wrapper`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter(), InMemoryDeviceDenylist()) }
        val email = "known@example.com"
        val wrapped = randomBytes(48)
        register(email, recoveryWrappedVaultKey = wrapped)

        val response = recoveryStart(email)

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.parsed()
        assertEquals(
            Base64.getEncoder().encodeToString(wrapped),
            body["recoveryWrappedVaultKey"]!!.jsonPrimitive.content,
        )
        assertEquals(userIdOf(email), body["userId"]!!.jsonPrimitive.content)
    }

    @Test
    fun `recovery complete with a wrong recovery key is unauthorized`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter(), InMemoryDeviceDenylist()) }
        val email = "wrong@example.com"
        register(email)

        val response = recoveryComplete(email, recoveryAuthKey = randomBytes(32))

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals("invalid_credentials", response.parsed()["code"]!!.jsonPrimitive.content)
    }

    @Test
    fun `recovery complete for an unknown email is unauthorized`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter(), InMemoryDeviceDenylist()) }

        val response = recoveryComplete("ghost@example.com", randomBytes(32))

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals("invalid_credentials", response.parsed()["code"]!!.jsonPrimitive.content)
    }

    @Test
    fun `recovery complete replaces the key material and revokes the other devices`() = testApplication {
        val rateLimiter = InMemoryRateLimiter()
        application { module(settings, PostgresTest.dataSource, rateLimiter, InMemoryDeviceDenylist()) }
        val email = "rotate@example.com"
        val recoveryAuthKey = randomBytes(32)
        val authKey = randomBytes(32)
        register(email, authKey = authKey, recoveryAuthKey = recoveryAuthKey)
        val firstLogin = login(email, authKey)
        assertEquals(HttpStatusCode.OK, firstLogin.status)
        val oldDeviceId = firstLogin.parsed()["deviceId"]!!.jsonPrimitive.content

        val newSalt = randomBytes(16)
        val newWrapped = randomBytes(32)
        val response = recoveryComplete(
            email = email,
            recoveryAuthKey = recoveryAuthKey,
            newAuthKey = randomBytes(32),
            newKdfSalt = newSalt,
            newWrappedVaultKey = newWrapped,
        )

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.parsed()
        assertEquals(Base64.getEncoder().encodeToString(newSalt), body["kdfSalt"]!!.jsonPrimitive.content)
        assertEquals(Base64.getEncoder().encodeToString(newWrapped), body["wrappedVaultKey"]!!.jsonPrimitive.content)
        assertNotEquals(oldDeviceId, body["deviceId"]!!.jsonPrimitive.content)
        assertNotNull(storedAuthHash(email))
        // The old device is revoked, so its access token no longer authenticates.
        assertEquals(HttpStatusCode.Unauthorized, devices(firstLogin.accessToken()).status)
    }

    @Test
    fun `recovery complete blocks after the rate limit`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter(), InMemoryDeviceDenylist()) }
        val email = "flood@example.com"
        register(email)

        val statuses = (1..6).map { recoveryComplete(email, randomBytes(32)).status }

        assertTrue(statuses.drop(5).all { it == HttpStatusCode.TooManyRequests })
    }

    @Test
    fun `delete account with a wrong master password is unauthorized`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter(), InMemoryDeviceDenylist()) }
        val email = "delete-me@example.com"
        val authKey = randomBytes(32)
        register(email, authKey = authKey)
        val accessToken = login(email, authKey).accessToken()

        val response = deleteAccount(accessToken, randomBytes(32))

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertNotNull(storedAuthHash(email))
    }

    @Test
    fun `delete account cascades over the user data`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter(), InMemoryDeviceDenylist()) }
        val email = "gone@example.com"
        val authKey = randomBytes(32)
        register(email, authKey = authKey)
        val accessToken = login(email, authKey).accessToken()

        val response = deleteAccount(accessToken, authKey)

        assertEquals(HttpStatusCode.NoContent, response.status)
        assertNull(storedAuthHash(email))
        assertEquals(0, userCount())
        assertEquals(0, deviceCount())
    }

    @Test
    fun `a deleted account cannot use its access token`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter(), InMemoryDeviceDenylist()) }
        val email = "replay@example.com"
        val authKey = randomBytes(32)
        register(email, authKey = authKey)
        val accessToken = login(email, authKey).accessToken()
        deleteAccount(accessToken, authKey)

        assertEquals(HttpStatusCode.Unauthorized, devices(accessToken).status)
    }

    private suspend fun ApplicationTestBuilder.recoveryStart(email: String): HttpResponse =
        client.post("/auth/recovery/start") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(PreloginRequest.serializer(), PreloginRequest(email)))
        }

    private suspend fun ApplicationTestBuilder.recoveryComplete(
        email: String,
        recoveryAuthKey: ByteArray,
        newAuthKey: ByteArray = randomBytes(32),
        newKdfSalt: ByteArray = randomBytes(16),
        newWrappedVaultKey: ByteArray = randomBytes(32),
    ): HttpResponse = client.post("/auth/recovery/complete") {
        contentType(ContentType.Application.Json)
        setBody(
            json.encodeToString(
                RecoveryCompleteRequest.serializer(),
                RecoveryCompleteRequest(
                    email = email,
                    recoveryAuthKey = base64(recoveryAuthKey),
                    newAuthKey = base64(newAuthKey),
                    newKdfSalt = base64(newKdfSalt),
                    newKdfParams = KdfParamsDto(memoryKiB = 65536, iterations = 3),
                    newWrappedVaultKey = base64(newWrappedVaultKey),
                    device = DeviceInfoDto(name = "Recovered device", platform = "android"),
                ),
            ),
        )
    }

    private suspend fun ApplicationTestBuilder.deleteAccount(accessToken: String, authKey: ByteArray): HttpResponse =
        client.delete("/account") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(DeleteAccountRequest.serializer(), DeleteAccountRequest(base64(authKey))))
        }

    private suspend fun ApplicationTestBuilder.register(
        email: String,
        authKey: ByteArray = randomBytes(32),
        recoveryAuthKey: ByteArray = randomBytes(32),
        recoveryWrappedVaultKey: ByteArray = randomBytes(32),
    ): HttpResponse = client.post("/auth/register") {
        contentType(ContentType.Application.Json)
        setBody(
            json.encodeToString(
                com.kwazzar.lockruna.contract.RegisterRequest.serializer(),
                com.kwazzar.lockruna.contract.RegisterRequest(
                    email = email,
                    authKey = base64(authKey),
                    recoveryAuthKey = base64(recoveryAuthKey),
                    kdfSalt = base64(randomBytes(16)),
                    kdfParams = KdfParamsDto(memoryKiB = 65536, iterations = 3),
                    wrappedVaultKey = base64(randomBytes(32)),
                    recoveryWrappedVaultKey = base64(recoveryWrappedVaultKey),
                    device = DeviceInfoDto(name = "Test device", platform = "ios"),
                ),
            ),
        )
    }

    private suspend fun ApplicationTestBuilder.login(email: String, authKey: ByteArray): HttpResponse =
        client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(
                json.encodeToString(
                    com.kwazzar.lockruna.contract.LoginRequest.serializer(),
                    com.kwazzar.lockruna.contract.LoginRequest(
                        email = email,
                        authKey = base64(authKey),
                        device = DeviceInfoDto(name = "Second device", platform = "android"),
                    ),
                ),
            )
        }

    private suspend fun ApplicationTestBuilder.devices(accessToken: String): HttpResponse = client.get("/devices") {
        header(HttpHeaders.Authorization, "Bearer $accessToken")
    }

    private fun userIdOf(email: String): String = storedRow(email)?.get("id") as String

    private fun storedRow(email: String): Map<String, Any?>? = PostgresTest.dataSource.connection.use { connection ->
        connection.prepareStatement("SELECT id, auth_hash FROM users WHERE email = ?").use { statement ->
            statement.setString(1, email)
            statement.executeQuery().use { rows ->
                if (!rows.next()) return null
                mapOf("id" to rows.getString("id"), "auth_hash" to rows.getBytes("auth_hash"))
            }
        }
    }

    private fun storedAuthHash(email: String): ByteArray? = storedRow(email)?.get("auth_hash") as ByteArray?

    private fun userCount(): Int = count("SELECT COUNT(*) FROM users")

    private fun deviceCount(): Int = count("SELECT COUNT(*) FROM devices")

    private fun count(sql: String): Int = PostgresTest.dataSource.connection.use { connection ->
        connection.createStatement().use { statement ->
            statement.executeQuery(sql).use { rows ->
                rows.next()
                rows.getInt(1)
            }
        }
    }

    private fun base64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    private suspend fun HttpResponse.accessToken(): String = parsed()["accessToken"]!!.jsonPrimitive.content

    private suspend fun HttpResponse.parsed(): JsonObject = json()
}
