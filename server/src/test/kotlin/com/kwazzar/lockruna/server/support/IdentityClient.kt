package com.kwazzar.lockruna.server.support

import com.kwazzar.lockruna.contract.DeviceInfoDto
import com.kwazzar.lockruna.contract.KdfParamsDto
import com.kwazzar.lockruna.contract.LoginRequest
import com.kwazzar.lockruna.contract.RefreshRequest
import com.kwazzar.lockruna.contract.RegisterRequest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.security.SecureRandom
import java.util.Base64

/**
 * HTTP client for the identity and device endpoints, shared by the server tests.
 */
class IdentityClient(private val builder: ApplicationTestBuilder) {
    private val json = Json

    suspend fun register(email: String, authKey: ByteArray): HttpResponse = builder.client.post("/auth/register") {
        contentType(ContentType.Application.Json)
        setBody(
            json.encodeToString(
                RegisterRequest.serializer(),
                RegisterRequest(
                    email = email,
                    authKey = Base64.getEncoder().encodeToString(authKey),
                    recoveryAuthKey = Base64.getEncoder().encodeToString(randomBytes(32)),
                    kdfSalt = Base64.getEncoder().encodeToString(randomBytes(16)),
                    kdfParams = KdfParamsDto(memoryKiB = 65536, iterations = 3),
                    wrappedVaultKey = Base64.getEncoder().encodeToString(randomBytes(32)),
                    recoveryWrappedVaultKey = Base64.getEncoder().encodeToString(randomBytes(32)),
                    device = DeviceInfoDto(name = "Test device", platform = "ios"),
                ),
            ),
        )
    }

    suspend fun login(email: String, authKey: ByteArray, deviceId: String? = null): HttpResponse =
        builder.client.post("/auth/login") {
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

    suspend fun refresh(refreshToken: String): HttpResponse = builder.client.post("/auth/refresh") {
        contentType(ContentType.Application.Json)
        setBody(json.encodeToString(RefreshRequest.serializer(), RefreshRequest(refreshToken)))
    }

    suspend fun logout(accessToken: String): HttpResponse = builder.client.post("/auth/logout") {
        bearer(accessToken)
    }

    suspend fun devices(accessToken: String): HttpResponse = builder.client.get("/devices") {
        bearer(accessToken)
    }

    suspend fun deleteDevice(accessToken: String, deviceId: String): HttpResponse =
        builder.client.delete("/devices/$deviceId") {
            bearer(accessToken)
        }
}

internal fun io.ktor.client.request.HttpRequestBuilder.bearer(accessToken: String) {
    header(HttpHeaders.Authorization, "Bearer $accessToken")
}

suspend fun HttpResponse.json(): JsonObject = Json.parseToJsonElement(bodyAsText()).jsonObject

fun randomBytes(size: Int): ByteArray = ByteArray(size).also(SecureRandom()::nextBytes)
