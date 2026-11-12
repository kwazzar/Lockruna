package com.kwazzar.lockruna.data.auth

import com.kwazzar.lockruna.contract.AuthResponseDto
import com.kwazzar.lockruna.contract.DeleteAccountRequest
import com.kwazzar.lockruna.contract.DeviceDto
import com.kwazzar.lockruna.contract.PreloginRequest
import com.kwazzar.lockruna.contract.RecoveryCompleteRequest
import com.kwazzar.lockruna.contract.RecoveryStartResponseDto
import com.kwazzar.lockruna.domain.auth.AccountApi
import com.kwazzar.lockruna.domain.auth.AuthenticatedUser
import com.kwazzar.lockruna.domain.auth.DeviceApi
import com.kwazzar.lockruna.domain.auth.DeviceIdentity
import com.kwazzar.lockruna.domain.auth.DeviceInfo
import com.kwazzar.lockruna.domain.auth.RecoveryApi
import com.kwazzar.lockruna.domain.auth.RecoveryStartData
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.crypto.KdfParams
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.sync.AccessTokenProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Recovery (US-03), devices (US-10) and account deletion (US-11) over HTTP.
 */
@OptIn(ExperimentalEncodingApi::class)
class KtorRecoveryApi(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val accessTokenProvider: AccessTokenProvider,
) : RecoveryApi, DeviceApi, AccountApi {

    private val client: HttpClient = HttpClient(httpClient.engine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    override suspend fun start(email: String): Result<RecoveryStartData, AppError> = try {
        val response = client.post("$baseUrl/auth/recovery/start") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(com.kwazzar.lockruna.contract.PreloginRequest(email))
        }
        if (!response.status.isSuccess()) return response.toFailure()
        val dto = response.body<RecoveryStartResponseDto>()
        Result.Success(
            RecoveryStartData(
                userId = dto.userId,
                recoveryWrappedVaultKey = Base64.decode(dto.recoveryWrappedVaultKey),
            ),
        )
    } catch (e: Exception) {
        Result.Failure(AppError.NetworkUnavailable)
    }

    override suspend fun complete(
        email: String,
        recoveryAuthKey: ByteArray,
        newAuthKey: ByteArray,
        newKdfSalt: ByteArray,
        newKdfParams: KdfParams,
        newWrappedVaultKey: ByteArray,
        device: DeviceIdentity,
    ): Result<AuthenticatedUser, AppError> = try {
        val response = client.post("$baseUrl/auth/recovery/complete") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(
                RecoveryCompleteRequest(
                    email = email,
                    recoveryAuthKey = Base64.encode(recoveryAuthKey),
                    newAuthKey = Base64.encode(newAuthKey),
                    newKdfSalt = Base64.encode(newKdfSalt),
                    newKdfParams = newKdfParams.toDto(),
                    newWrappedVaultKey = Base64.encode(newWrappedVaultKey),
                    device = device.toDto(),
                ),
            )
        }
        if (!response.status.isSuccess()) return response.toFailure()
        Result.Success(response.body<AuthResponseDto>().toDomain())
    } catch (e: Exception) {
        Result.Failure(AppError.NetworkUnavailable)
    }

    override suspend fun list(): Result<List<DeviceInfo>, AppError> = try {
        val token = accessTokenProvider.token() ?: return Result.Failure(AppError.Unauthorized)
        val response = client.get("$baseUrl/devices") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (!response.status.isSuccess()) return response.toFailure()
        val devices = response.body<List<DeviceDto>>()
        Result.Success(
            devices.map { device ->
                DeviceInfo(
                    id = device.id,
                    name = device.name,
                    platform = device.platform,
                    lastSeen = device.lastSeen,
                    current = device.current,
                )
            },
        )
    } catch (e: Exception) {
        Result.Failure(AppError.NetworkUnavailable)
    }

    override suspend fun revoke(deviceId: String): Result<Unit, AppError> = try {
        val token = accessTokenProvider.token() ?: return Result.Failure(AppError.Unauthorized)
        val response = client.delete("$baseUrl/devices/$deviceId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (!response.status.isSuccess()) return response.toFailure()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(AppError.NetworkUnavailable)
    }

    override suspend fun delete(authKey: ByteArray): Result<Unit, AppError> = try {
        val token = accessTokenProvider.token() ?: return Result.Failure(AppError.Unauthorized)
        val response = client.delete("$baseUrl/account") {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(DeleteAccountRequest(Base64.encode(authKey)))
        }
        if (!response.status.isSuccess()) return response.toFailure()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Failure(AppError.NetworkUnavailable)
    }
}
