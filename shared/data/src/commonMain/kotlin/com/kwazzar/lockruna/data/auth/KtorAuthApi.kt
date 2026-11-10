package com.kwazzar.lockruna.data.auth

import com.kwazzar.lockruna.domain.auth.AuthApi
import com.kwazzar.lockruna.domain.auth.AuthTokens
import com.kwazzar.lockruna.domain.auth.AuthenticatedUser
import com.kwazzar.lockruna.domain.auth.DeviceIdentity
import com.kwazzar.lockruna.domain.auth.DevicePlatform
import com.kwazzar.lockruna.domain.auth.PreLoginData
import com.kwazzar.lockruna.domain.auth.SealedBlob
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.crypto.CryptoService
import com.kwazzar.lockruna.domain.crypto.KdfParams
import com.kwazzar.lockruna.domain.error.AppError
import io.ktor.client.HttpClient
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

class KtorAuthApi(
    private val httpClient: HttpClient,
    private val baseUrl: String
) : AuthApi {

    override suspend fun prelogin(email: String): Result<PreLoginData, AppError> = try {
        val response = httpClient.post("$baseUrl/auth/prelogin") {
            contentType(ContentType.Application.Json)
            setBody(PreloginRequest(email))
        }
        val dto = response.body<PreloginResponse>()
        Result.Success(PreLoginData(
            kdfSalt = dto.kdfSalt.hexToByteArray(),
            kdfParams = KdfParams(
                alg = dto.kdfParams.alg,
                memoryKiB = dto.kdfParams.memoryKiB,
                iterations = dto.kdfParams.iterations,
                parallelism = dto.kdfParams.parallelism,
                saltLength = dto.kdfParams.saltLength,
                outputLength = dto.kdfParams.outputLength
            )
        ))
    } catch (e: Exception) {
        return handleException(e)
    }

    override suspend fun register(
        email: String,
        authKey: ByteArray,
        recoveryAuthKey: ByteArray,
        kdfSalt: ByteArray,
        kdfParams: KdfParams,
        wrappedVaultKey: ByteArray,
        recoveryWrappedVaultKey: ByteArray,
        device: DeviceIdentity
    ): Result<AuthenticatedUser, AppError> = try {
        val response = httpClient.post("$baseUrl/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(
                email = email,
                authKey = authKey.hex(),
                recoveryAuthKey = recoveryAuthKey.hex(),
                kdfSalt = kdfSalt.hex(),
                kdfParams = KdfParamsDto(
                    alg = kdfParams.alg,
                    memoryKiB = kdfParams.memoryKiB,
                    iterations = kdfParams.iterations,
                    parallelism = kdfParams.parallelism,
                    saltLength = kdfParams.saltLength,
                    outputLength = kdfParams.outputLength
                ),
                wrappedVaultKey = wrappedVaultKey.hex(),
                recoveryWrappedVaultKey = recoveryWrappedVaultKey.hex(),
                device = DeviceDto(
                    deviceId = device.deviceId ?: "",
                    name = device.name,
                    platform = when (device.platform) {
                        DevicePlatform.IOS -> "ios"
                        DevicePlatform.ANDROID -> "android"
                    }
                )
            ))
        }
        val dto = response.body<AuthResponse>()
        Result.Success(toAuthenticatedUser(dto))
    } catch (e: Exception) {
        return handleException(e)
    }

    override suspend fun login(
        email: String,
        authKey: ByteArray,
        device: DeviceIdentity
    ): Result<AuthenticatedUser, AppError> = try {
        val response = httpClient.post("$baseUrl/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(
                email = email,
                authKey = authKey.hex(),
                device = DeviceDto(
                    deviceId = device.deviceId ?: "",
                    name = device.name,
                    platform = when (device.platform) {
                        DevicePlatform.IOS -> "ios"
                        DevicePlatform.ANDROID -> "android"
                    }
                )
            ))
        }
        val dto = response.body<AuthResponse>()
        Result.Success(toAuthenticatedUser(dto))
    } catch (e: Exception) {
        return handleException(e)
    }

    override suspend fun refresh(refreshToken: String): Result<AuthTokens, AppError> = try {
        val response = httpClient.post("$baseUrl/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(refreshToken))
        }
        val dto = response.body<TokenPairResponse>()
        Result.Success(AuthTokens(dto.accessToken, dto.refreshToken, dto.expiresInSeconds))
    } catch (e: Exception) {
        return handleException(e)
    }

    override suspend fun logout(): Result<Unit, AppError> = try {
        httpClient.post("$baseUrl/auth/logout") {
            contentType(ContentType.Application.Json)
        }
        Result.Success(Unit)
    } catch (e: Exception) {
        return handleException(e)
    }

    private fun toAuthenticatedUser(dto: AuthResponse): AuthenticatedUser {
        return AuthenticatedUser(
            userId = dto.userId,
            deviceId = dto.deviceId,
            wrappedVaultKey = dto.wrappedVaultKey.hexToByteArray(),
            keyNonce = dto.keyNonce.hexToByteArray(),
            kdfSalt = dto.kdfSalt.hexToByteArray(),
            kdfParams = KdfParams(
                alg = dto.kdfParams.alg,
                memoryKiB = dto.kdfParams.memoryKiB,
                iterations = dto.kdfParams.iterations,
                parallelism = dto.kdfParams.parallelism,
                saltLength = dto.kdfParams.saltLength,
                outputLength = dto.kdfParams.outputLength
            ),
            tokens = AuthTokens(dto.accessToken, dto.refreshToken, dto.expiresInSeconds)
        )
    }

    private fun <T> handleException(e: Exception): Result<T, AppError> {
        // ResponseException is JVM-only, use message matching for common code
        // Platform-specific implementations can override with proper exception handling
        return Result.Failure(AppError.NetworkUnavailable)
    }
}

@Serializable
data class PreloginRequest(val email: String)

@Serializable
data class PreloginResponse(
    val kdfSalt: String,
    val kdfParams: KdfParamsDto
)

@Serializable
data class RegisterRequest(
    val email: String,
    val authKey: String,
    val recoveryAuthKey: String,
    val kdfSalt: String,
    val kdfParams: KdfParamsDto,
    val wrappedVaultKey: String,
    val recoveryWrappedVaultKey: String,
    val device: DeviceDto
)

@Serializable
data class LoginRequest(
    val email: String,
    val authKey: String,
    val device: DeviceDto
)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class AuthResponse(
    val userId: String,
    val deviceId: String,
    val wrappedVaultKey: String,
    val keyNonce: String,
    val kdfSalt: String,
    val kdfParams: KdfParamsDto,
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Int
)

@Serializable
data class TokenPairResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Int
)

@Serializable
data class KdfParamsDto(
    val alg: String,
    val memoryKiB: Int,
    val iterations: Int,
    val parallelism: Int,
    val saltLength: Int,
    val outputLength: Int
)

@Serializable
data class DeviceDto(
    val deviceId: String,
    val name: String,
    val platform: String
)

private fun String.hexToByteArray(): ByteArray {
    return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}

private fun ByteArray.hex(): String {
    return joinToString("") { "%02x".format(it) }
}