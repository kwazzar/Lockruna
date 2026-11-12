package com.kwazzar.lockruna.data.auth

import com.kwazzar.lockruna.domain.auth.DeviceIdentity
import com.kwazzar.lockruna.domain.auth.DevicePlatform
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.crypto.KdfParams
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.sync.AccessTokenProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondOk
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.content.TextContent
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val <T, E> Result<T, E>.isSuccess: Boolean
    get() = this is Result.Success

private val <T, E> Result<T, E>.isFailure: Boolean
    get() = this is Result.Failure

private val <T, E> Result<T, E>.error: E?
    get() = if (this is Result.Failure) this.error else null

/**
 * The wire mapping of the auth endpoints: bytes travel as Base64, statuses become AppError.
 */
class KtorAuthApiHttpTest {

    private val authResponseJson = """{"accessToken":"a","refreshToken":"r","expiresIn":900,"userId":"u","deviceId":"d",""" +
        """"kdfSalt":"AAECAwQFBgcHCQ==",""" +
        """"kdfParams":{"alg":"argon2id","m":65536,"t":3,"p":1},""" +
        """"wrappedVaultKey":"AQIDBAUGBwgJCg=="}"""

    @Test
    fun `register sends base64 keys, never hex`() = runBlocking {
        var body: String? = null
        val api = api { request ->
            body = request.bodyText()
            respond(
                authResponseJson,
                HttpStatusCode.Created,
                headersOf("Content-Type" to listOf("application/json")),
            )
        }

        val result = api.register(
            email = "user@example.com",
            authKey = byteArrayOf(1, 2, 3, 4),
            recoveryAuthKey = byteArrayOf(5),
            kdfSalt = byteArrayOf(9, 10),
            kdfParams = KdfParams.SUPPORTED,
            wrappedVaultKey = byteArrayOf(11, 12),
            recoveryWrappedVaultKey = byteArrayOf(13, 14),
            device = DeviceIdentity(deviceId = null, name = "Phone", platform = DevicePlatform.ANDROID),
        )

        assertTrue(result is Result.Success)
        // The server decodes Base64; the old hex encoding made every registration fail against it.
        assertTrue(body!!.contains("\"authKey\":\"AQIDBA==\""), "body was $body")
        assertTrue(body!!.contains("\"kdfSalt\":\"CQo=\""), "body was $body")
        assertTrue(
            body!!.contains("\"device\":{\"deviceId\":\"\",\"name\":\"Phone\",\"platform\":\"android\"}"),
            "body was $body",
        )
        assertEquals("u", (result as Result.Success).value.userId)
    }

    @Test
    fun `register maps an existing email to its error`() = runBlocking {
        val api = api { respond("", HttpStatusCode.Conflict) }

        val result = api.register(
            email = "user@example.com",
            authKey = byteArrayOf(1),
            recoveryAuthKey = byteArrayOf(2),
            kdfSalt = byteArrayOf(3),
            kdfParams = KdfParams.SUPPORTED,
            wrappedVaultKey = byteArrayOf(4),
            recoveryWrappedVaultKey = byteArrayOf(5),
            device = DeviceIdentity(deviceId = null, name = "Phone", platform = DevicePlatform.IOS),
        )

        assertEquals(Result.Failure(AppError.EmailAlreadyRegistered), result)
    }

    @Test
    fun `login maps unknown credentials to invalid credentials`() = runBlocking {
        val api = api { respond("", HttpStatusCode.Unauthorized) }

        val result = api.login(
            email = "user@example.com",
            authKey = byteArrayOf(1, 2, 3, 4),
            device = DeviceIdentity(deviceId = null, name = "Phone", platform = DevicePlatform.IOS),
        )

        assertEquals(Result.Failure(AppError.InvalidCredentials), result)
    }

    @Test
    fun `prelogin returns the salt for acceptable parameters`() = runBlocking {
        val api = api {
            respond(
                """{"kdfSalt":"AAECAwQFBgcHCQ==","kdfParams":{"alg":"argon2id","m":65536,"t":3,"p":1}}""",
                HttpStatusCode.OK,
                headersOf("Content-Type" to listOf("application/json")),
            )
        }

        val result = api.prelogin("user@example.com")

        assertTrue(result.isSuccess)
        assertEquals(10, (result as Result.Success).value.kdfSalt.size)
    }

    private fun api(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): KtorAuthApi = KtorAuthApi(
        HttpClient(MockEngine) { engine { addHandler(handler) } },
        "https://api.example.com",
    )

    private fun HttpRequestData.bodyText(): String =
        (body as? TextContent)?.text ?: body.toString()
}

/** The recovery, device and account endpoints (US-03, US-10, US-11). */
class KtorRecoveryApiHttpTest {

    @Test
    fun `start posts the email and decodes the wrapped key from base64`() = runBlocking {
        var requestPath: String? = null
        var body: String? = null
        val api = recoveryApi("access-token") { request ->
            requestPath = request.url.encodedPath
            body = request.bodyText()
            respond(
                """{"userId":"6f0f1b1c-0000-4000-8000-000000000000",""" +
                    """"recoveryWrappedVaultKey":"AQIDBAUGBwgJCg=="}""",
                HttpStatusCode.OK,
                headersOf("Content-Type" to listOf("application/json")),
            )
        }

        val result = api.start("user@example.com")

        assertTrue(result.isSuccess, "expected success, got ${result.error}")
        assertEquals("/auth/recovery/start", requestPath)
        assertTrue(body!!.contains("user@example.com"))
        // Base64 of 0x01..0x08, not the hex "0102030405060708".
        assertEquals(
            (1..10).map { it.toByte() },
            (result as Result.Success).value.recoveryWrappedVaultKey.toList(),
        )
    }

    @Test
    fun `complete posts the rotated keys and returns the session`() = runBlocking {
        var body: String? = null
        val api = recoveryApi("access-token") { request ->
            body = request.bodyText()
            respond(
                """{"accessToken":"a","refreshToken":"r","expiresIn":900,"userId":"u","deviceId":"d",""" +
                    """"kdfSalt":"AAECAwQFBgcHCQ==",""" +
                    """"kdfParams":{"alg":"argon2id","m":65536,"t":3,"p":1},""" +
                    """"wrappedVaultKey":"AQIDBAUGBwgJCg=="}""",
                HttpStatusCode.OK,
                headersOf("Content-Type" to listOf("application/json")),
            )
        }

        val result = api.complete(
            email = "user@example.com",
            recoveryAuthKey = byteArrayOf(1, 2, 3, 4),
            newAuthKey = byteArrayOf(5, 6, 7, 8),
            newKdfSalt = byteArrayOf(9, 10),
            newKdfParams = KdfParams.SUPPORTED,
            newWrappedVaultKey = byteArrayOf(11, 12),
            device = DeviceIdentity(deviceId = "device-1", name = "Phone", platform = DevicePlatform.ANDROID),
        )

        assertTrue(result.isSuccess, "expected success, got ${result.error}")
        val user = (result as Result.Success).value
        assertEquals("u", user.userId)
        assertEquals("d", user.deviceId)
        assertEquals(10, user.kdfSalt.size)
        assertTrue(body!!.contains("\"recoveryAuthKey\":\"AQIDBA==\""), "body was $body")
        assertTrue(body!!.contains("\"newAuthKey\":\"BQYHCA==\""), "body was $body")
        assertTrue(body!!.contains("\"platform\":\"android\""), "body was $body")
    }

    @Test
    fun `complete surfaces a rejected recovery key as invalid credentials`() = runBlocking {
        val api = recoveryApi("access-token") { respond("", HttpStatusCode.Unauthorized) }

        val result = api.complete(
            email = "user@example.com",
            recoveryAuthKey = byteArrayOf(1),
            newAuthKey = byteArrayOf(2),
            newKdfSalt = byteArrayOf(3),
            newKdfParams = KdfParams.SUPPORTED,
            newWrappedVaultKey = byteArrayOf(4),
            device = DeviceIdentity(deviceId = null, name = "Phone", platform = DevicePlatform.IOS),
        )

        assertTrue(result.isFailure)
        assertEquals(AppError.InvalidCredentials, result.error)
    }

    @Test
    fun `list sends the bearer token and marks the current device`() = runBlocking {
        var authorization: String? = null
        val api = recoveryApi("access-token") { request ->
            authorization = request.headers["Authorization"]
            respond(
                """[{"id":"device-1","name":"Phone","platform":"ios","lastSeen":"2026-11-11T10:00:00Z","current":true},""" +
                    """{"id":"device-2","name":"Tablet","platform":"android","current":false}]""",
                HttpStatusCode.OK,
                headersOf("Content-Type" to listOf("application/json")),
            )
        }

        val result = api.list()

        assertTrue(result.isSuccess, "expected success, got ${result.error}")
        assertEquals("Bearer access-token", authorization)
        val devices = (result as Result.Success).value
        assertEquals(2, devices.size)
        assertEquals("device-1", devices.first().id)
        assertTrue(devices.first().current)
        assertEquals("2026-11-11T10:00:00Z", devices.first().lastSeen)
        assertTrue(!devices.last().current)
    }

    @Test
    fun `list without a session never reaches the server`() = runBlocking {
        val api = recoveryApi(null) { error("the server must not be called") }

        val result = api.list()

        assertTrue(result.isFailure)
        assertEquals(AppError.Unauthorized, result.error)
    }

    @Test
    fun `revoke deletes the device`() = runBlocking {
        var requestPath: String? = null
        val api = recoveryApi("access-token") { request ->
            requestPath = request.url.encodedPath
            respondOk()
        }

        val result = api.revoke("device-9")

        assertTrue(result.isSuccess, "expected success, got ${result.error}")
        assertEquals("/devices/device-9", requestPath)
    }

    @Test
    fun `delete sends the auth key and the bearer token`() = runBlocking {
        var authorization: String? = null
        var body: String? = null
        var requestPath: String? = null
        val api = recoveryApi("access-token") { request ->
            authorization = request.headers["Authorization"]
            body = request.bodyText()
            requestPath = request.url.encodedPath
            respondOk()
        }

        val result = api.delete(byteArrayOf(1, 2, 3, 4))

        assertTrue(result.isSuccess, "expected success, got ${result.error}")
        assertEquals("Bearer access-token", authorization)
        assertEquals("/account", requestPath)
        assertTrue(body!!.contains("\"authKey\":\"AQIDBA==\""), "body was $body")
    }

    @Test
    fun `a rate limited response is reported to the user`() = runBlocking {
        val api = recoveryApi("access-token") { respond("", HttpStatusCode.TooManyRequests) }

        val result = api.start("user@example.com")

        assertTrue(result.isFailure)
        assertTrue(result.error is AppError.RateLimited)
    }

    private fun recoveryApi(
        token: String?,
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): KtorRecoveryApi {
        val client = HttpClient(MockEngine) { engine { addHandler(handler) } }
        val provider = object : AccessTokenProvider {
            override suspend fun token(): String? = token
        }
        return KtorRecoveryApi(client, "https://api.example.com", provider)
    }

    private fun HttpRequestData.bodyText(): String =
        (body as? TextContent)?.text ?: body.toString()
}
