package com.kwazzar.lockruna.data.auth

import com.kwazzar.lockruna.contract.AuthResponseDto
import com.kwazzar.lockruna.contract.DeviceInfoDto
import com.kwazzar.lockruna.contract.KdfParamsDto
import com.kwazzar.lockruna.contract.LoginRequest
import com.kwazzar.lockruna.contract.PreloginRequest
import com.kwazzar.lockruna.contract.PreloginResponse
import com.kwazzar.lockruna.contract.RefreshRequest
import com.kwazzar.lockruna.contract.RegisterRequest
import com.kwazzar.lockruna.contract.TokenPairDto
import com.kwazzar.lockruna.domain.auth.AuthApi
import com.kwazzar.lockruna.domain.auth.AuthTokens
import com.kwazzar.lockruna.domain.auth.AuthenticatedUser
import com.kwazzar.lockruna.domain.auth.DeviceIdentity
import com.kwazzar.lockruna.domain.auth.DevicePlatform
import com.kwazzar.lockruna.domain.auth.PreLoginData
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.crypto.KdfParams
import com.kwazzar.lockruna.domain.error.AppError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * The auth endpoints of the server. Bytes travel as Base64, exactly as `openapi.yaml` says.
 */
@OptIn(ExperimentalEncodingApi::class)
class KtorAuthApi(private val httpClient: HttpClient, private val baseUrl: String) : AuthApi {

    private val client: HttpClient = HttpClient(httpClient.engine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    override suspend fun prelogin(email: String): Result<PreLoginData, AppError> = try {
        val response = client.post("$baseUrl/auth/prelogin") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(PreloginRequest(email))
        }
        if (!response.status.isSuccess()) return response.toFailure()
        val dto = response.body<PreloginResponse>()
        Result.Success(
            PreLoginData(
                kdfSalt = Base64.decode(dto.kdfSalt),
                kdfParams = dto.kdfParams.toDomain(),
            ),
        )
    } catch (e: Exception) {
        handleException(e)
    }

    override suspend fun register(
        email: String,
        authKey: ByteArray,
        recoveryAuthKey: ByteArray,
        kdfSalt: ByteArray,
        kdfParams: KdfParams,
        wrappedVaultKey: ByteArray,
        recoveryWrappedVaultKey: ByteArray,
        device: DeviceIdentity,
    ): Result<AuthenticatedUser, AppError> = try {
        val response = client.post("$baseUrl/auth/register") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(
                RegisterRequest(
                    email = email,
                    authKey = Base64.encode(authKey),
                    recoveryAuthKey = Base64.encode(recoveryAuthKey),
                    kdfSalt = Base64.encode(kdfSalt),
                    kdfParams = kdfParams.toDto(),
                    wrappedVaultKey = Base64.encode(wrappedVaultKey),
                    recoveryWrappedVaultKey = Base64.encode(recoveryWrappedVaultKey),
                    device = device.toDto(),
                ),
            )
        }
        if (response.status == HttpStatusCode.Conflict) {
            return Result.Failure(AppError.EmailAlreadyRegistered)
        }
        if (!response.status.isSuccess()) return response.toFailure()
        Result.Success(response.body<AuthResponseDto>().toDomain())
    } catch (e: Exception) {
        handleException(e)
    }

    override suspend fun login(
        email: String,
        authKey: ByteArray,
        device: DeviceIdentity,
    ): Result<AuthenticatedUser, AppError> = try {
        val response = client.post("$baseUrl/auth/login") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(
                LoginRequest(
                    email = email,
                    authKey = Base64.encode(authKey),
                    device = device.toDto(),
                ),
            )
        }
        if (!response.status.isSuccess()) return response.toFailure()
        Result.Success(response.body<AuthResponseDto>().toDomain())
    } catch (e: Exception) {
        handleException(e)
    }

    override suspend fun refresh(refreshToken: String): Result<AuthTokens, AppError> = try {
        val response = client.post("$baseUrl/auth/refresh") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(RefreshRequest(refreshToken))
        }
        if (!response.status.isSuccess()) return response.toFailure()
        val dto = response.body<TokenPairDto>()
        Result.Success(AuthTokens(dto.accessToken, dto.refreshToken, dto.expiresIn))
    } catch (e: Exception) {
        handleException(e)
    }

    override suspend fun logout(): Result<Unit, AppError> = try {
        val response = client.post("$baseUrl/auth/logout") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }
        if (!response.status.isSuccess()) return response.toFailure()
        Result.Success(Unit)
    } catch (e: Exception) {
        handleException(e)
    }
}

internal fun DeviceIdentity.toDto() = DeviceInfoDto(
    deviceId = deviceId ?: "",
    name = name,
    platform = when (platform) {
        DevicePlatform.IOS -> "ios"
        DevicePlatform.ANDROID -> "android"
    },
)

internal fun KdfParams.toDto() = KdfParamsDto(
    alg = alg,
    memoryKiB = memoryKiB,
    iterations = iterations,
    parallelism = parallelism,
)

internal fun KdfParamsDto.toDomain() = KdfParams(
    alg = alg,
    memoryKiB = memoryKiB,
    iterations = iterations,
    parallelism = parallelism,
)

@OptIn(ExperimentalEncodingApi::class)
internal fun AuthResponseDto.toDomain() = AuthenticatedUser(
    userId = userId,
    deviceId = deviceId,
    wrappedVaultKey = Base64.decode(wrappedVaultKey),
    kdfSalt = Base64.decode(kdfSalt),
    kdfParams = kdfParams.toDomain(),
    tokens = AuthTokens(accessToken, refreshToken, expiresIn),
)

/** Maps an HTTP failure to the error the domain understands. */
internal fun HttpResponse.toFailure(): Result<Nothing, AppError> = when (status) {
    HttpStatusCode.Unauthorized -> Result.Failure(AppError.InvalidCredentials)
    HttpStatusCode.Conflict -> Result.Failure(AppError.EmailAlreadyRegistered)
    HttpStatusCode.TooManyRequests -> Result.Failure(AppError.RateLimited(retryAfterSeconds = 60))
    else -> Result.Failure(AppError.NetworkUnavailable)
}

private fun <T> handleException(exception: Exception): Result<T, AppError> =
    Result.Failure(AppError.NetworkUnavailable)
