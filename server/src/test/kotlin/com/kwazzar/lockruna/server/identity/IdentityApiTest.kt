package com.kwazzar.lockruna.server.identity

import com.auth0.jwt.JWT
import com.kwazzar.lockruna.contract.DeviceInfoDto
import com.kwazzar.lockruna.contract.KdfParamsDto
import com.kwazzar.lockruna.contract.LoginRequest
import com.kwazzar.lockruna.contract.PreloginRequest
import com.kwazzar.lockruna.contract.RegisterRequest
import com.kwazzar.lockruna.server.module
import com.kwazzar.lockruna.server.support.InMemoryRateLimiter
import com.kwazzar.lockruna.server.support.PostgresTest
import com.kwazzar.lockruna.server.support.testSettings
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
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
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IdentityApiTest {
    private val settings = testSettings()
    private val json = Json

    @BeforeTest
    fun setUp() {
        PostgresTest.migrate()
        PostgresTest.reset()
    }

    @Test
    fun `prelogin for an unknown email returns deterministic fake parameters`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter()) }

        val first = prelogin("nobody@example.com")
        val second = prelogin("nobody@example.com")

        assertEquals(HttpStatusCode.OK, first.status)
        assertEquals(16, Base64.getDecoder().decode(first.body()["kdfSalt"]!!.jsonPrimitive.content).size)
        assertEquals("""{"alg":"argon2id","m":65536,"t":3,"p":1}""", first.body()["kdfParams"].toString())
        assertEquals(first.bodyAsText(), second.bodyAsText())
    }

    @Test
    fun `prelogin for a known email returns the stored salt`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter()) }
        val salt = randomBytes(16)
        register("user@example.com", salt = salt)

        val response = prelogin("user@example.com")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(Base64.getEncoder().encodeToString(salt), response.body()["kdfSalt"]!!.jsonPrimitive.content)
    }

    @Test
    fun `register creates an account and stores only the peppered auth key`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter()) }
        val authKey = randomBytes(32)

        val response = register("user@example.com", authKey = authKey)

        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.body()
        assertIsUuid(body["userId"]!!.jsonPrimitive.content)
        assertIsUuid(body["deviceId"]!!.jsonPrimitive.content)
        val stored = storedAuthHash("user@example.com")
        assertNotNull(stored)
        assertContentEquals(hmac(settings.pepper, authKey), stored)
        assertNotEquals(authKey.toList(), stored.toList())
    }

    @Test
    fun `register for an existing email is rejected`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter()) }
        register("user@example.com")

        val response = register("user@example.com")

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals("email_already_registered", response.body()["code"]!!.jsonPrimitive.content)
    }

    @Test
    fun `login with the correct auth key returns a token pair`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter()) }
        val authKey = randomBytes(32)
        val registered = register("user@example.com", authKey = authKey)
        val deviceId = registered.body()["deviceId"]!!.jsonPrimitive.content

        val response = login("user@example.com", authKey, deviceId)

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body()
        assertEquals(900, body["expiresIn"]!!.jsonPrimitive.int)
        assertEquals(deviceId, JWT.decode(body["accessToken"]!!.jsonPrimitive.content).getClaim("deviceId").asString())
    }

    @Test
    fun `register with an unsupported platform is rejected`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter()) }

        val response = register("user@example.com", platform = "windows")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("invalid_request", response.body()["code"]!!.jsonPrimitive.content)
    }

    @Test
    fun `login with a wrong auth key is unauthorized`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter()) }
        register("user@example.com", authKey = randomBytes(32))

        val response = login("user@example.com", randomBytes(32))

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals("invalid_credentials", response.body()["code"]!!.jsonPrimitive.content)
    }

    @Test
    fun `an unknown email and a wrong password produce the same response`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter()) }
        register("user@example.com")

        val wrongPassword = login("user@example.com", randomBytes(32))
        val unknownEmail = login("nobody@example.com", randomBytes(32))

        assertEquals(HttpStatusCode.Unauthorized, wrongPassword.status)
        assertEquals(HttpStatusCode.Unauthorized, unknownEmail.status)
        assertEquals("invalid_credentials", wrongPassword.body()["code"]!!.jsonPrimitive.content)
        assertEquals(wrongPassword.bodyAsText(), unknownEmail.bodyAsText())
    }

    @Test
    fun `the sixth failed login from one pair is blocked`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter()) }
        register("user@example.com", authKey = randomBytes(32))

        repeat(5) {
            assertEquals(HttpStatusCode.Unauthorized, login("user@example.com", randomBytes(32)).status)
        }

        val blocked = login("user@example.com", randomBytes(32))

        assertEquals(HttpStatusCode.TooManyRequests, blocked.status)
        assertNotNull(blocked.headers["Retry-After"])
    }

    @Test
    fun `login updates the last seen time of the device`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter()) }
        val authKey = randomBytes(32)
        val deviceId = register("user@example.com", authKey = authKey).body()["deviceId"]!!.jsonPrimitive.content
        ageLastSeen(deviceId)
        val before = lastSeen(deviceId)

        login("user@example.com", authKey, deviceId)

        val after = lastSeen(deviceId)
        assertNotNull(before)
        assertNotNull(after)
        assertTrue(after.isAfter(before))
    }

    @Test
    fun `a prelogin body of more than 64 kibibytes is rejected`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter()) }

        val response = client.post("/auth/prelogin") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"${"a".repeat(70 * 1024)}"}""")
        }

        assertEquals(HttpStatusCode.PayloadTooLarge, response.status)
    }

    @Test
    fun `the thirty first prelogin for one email is blocked`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter()) }

        repeat(30) { assertEquals(HttpStatusCode.OK, prelogin("user@example.com").status) }

        val blocked = prelogin("user@example.com")

        assertEquals(HttpStatusCode.TooManyRequests, blocked.status)
        assertNotNull(blocked.headers["Retry-After"])
    }

    @Test
    fun `the one hundred twenty first prelogin from one address is blocked`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter()) }

        repeat(120) { index -> assertEquals(HttpStatusCode.OK, prelogin("user$index@example.com").status) }

        assertEquals(HttpStatusCode.TooManyRequests, prelogin("last@example.com").status)
    }

    @Test
    fun `two concurrent registrations of one email create one account`() = testApplication {
        application { module(settings, PostgresTest.dataSource, InMemoryRateLimiter()) }

        val responses = coroutineScope {
            val first = async { register("race@example.com") }
            val second = async { register("race@example.com") }
            listOf(first.await(), second.await())
        }

        assertEquals(1, responses.count { it.status == HttpStatusCode.Created })
        val rejected = responses.single { it.status == HttpStatusCode.Conflict }
        assertEquals("email_already_registered", rejected.body()["code"]!!.jsonPrimitive.content)
    }

    private suspend fun ApplicationTestBuilder.prelogin(email: String): HttpResponse = client.post("/auth/prelogin") {
        contentType(ContentType.Application.Json)
        setBody(json.encodeToString(PreloginRequest.serializer(), PreloginRequest(email)))
    }

    private suspend fun ApplicationTestBuilder.register(
        email: String,
        authKey: ByteArray = randomBytes(32),
        salt: ByteArray = randomBytes(16),
        platform: String = "ios",
    ): HttpResponse = client.post("/auth/register") {
        contentType(ContentType.Application.Json)
        setBody(
            json.encodeToString(
                RegisterRequest.serializer(),
                RegisterRequest(
                    email = email,
                    authKey = Base64.getEncoder().encodeToString(authKey),
                    recoveryAuthKey = Base64.getEncoder().encodeToString(randomBytes(32)),
                    kdfSalt = Base64.getEncoder().encodeToString(salt),
                    kdfParams = KdfParamsDto(memoryKiB = 65536, iterations = 3),
                    wrappedVaultKey = Base64.getEncoder().encodeToString(randomBytes(32)),
                    recoveryWrappedVaultKey = Base64.getEncoder().encodeToString(randomBytes(32)),
                    device = DeviceInfoDto(name = "Test device", platform = platform),
                ),
            ),
        )
    }

    private suspend fun ApplicationTestBuilder.login(
        email: String,
        authKey: ByteArray,
        deviceId: String? = null,
    ): HttpResponse = client.post("/auth/login") {
        contentType(ContentType.Application.Json)
        setBody(
            json.encodeToString(
                LoginRequest.serializer(),
                LoginRequest(
                    email = email,
                    authKey = Base64.getEncoder().encodeToString(authKey),
                    device = DeviceInfoDto(deviceId = deviceId, name = "Test device", platform = "ios"),
                ),
            ),
        )
    }

    private suspend fun HttpResponse.body(): JsonObject = json.parseToJsonElement(bodyAsText()).jsonObject

    private fun assertIsUuid(value: String) {
        assertEquals(value, UUID.fromString(value).toString())
    }

    private fun storedAuthHash(email: String): ByteArray? = PostgresTest.dataSource.connection.use { connection ->
        connection.prepareStatement("SELECT auth_hash FROM users WHERE email = ?").use { statement ->
            statement.setString(1, email)
            statement.executeQuery().use { rows -> if (rows.next()) rows.getBytes(1) else null }
        }
    }

    private fun ageLastSeen(deviceId: String) {
        PostgresTest.dataSource.connection.use { connection ->
            connection.prepareStatement(
                "UPDATE devices SET last_seen = now() - interval '1 hour' WHERE id = ?::uuid",
            ).use { statement ->
                statement.setString(1, deviceId)
                statement.executeUpdate()
            }
        }
    }

    private fun lastSeen(deviceId: String): Instant? = PostgresTest.dataSource.connection.use { connection ->
        connection.prepareStatement("SELECT last_seen FROM devices WHERE id = ?::uuid").use { statement ->
            statement.setString(1, deviceId)
            statement.executeQuery().use { rows -> if (rows.next()) rows.getTimestamp(1)?.toInstant() else null }
        }
    }

    private fun hmac(pepper: ByteArray, key: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(pepper, "HmacSHA256"))
        return mac.doFinal(key)
    }

    private fun randomBytes(size: Int): ByteArray = ByteArray(size).also(SecureRandom()::nextBytes)
}
