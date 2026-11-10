package com.kwazzar.lockruna.data.auth

import com.kwazzar.lockruna.domain.auth.AuthApi
import io.ktor.client.HttpClient

actual class KtorAuthApi(httpClient: HttpClient, baseUrl: String) : AuthApi {
    // Android implementation will use the same JVM implementation via expect/actual
    // This is a placeholder - the actual implementation is in jvmMain
    // The Android implementation would use Ktor's Android engine
    // For now, we delegate to the JVM implementation
    private val delegate = KtorAuthApi(httpClient, baseUrl)

    override suspend fun prelogin(email: String): Result<PreLoginData, AppError> = delegate.prelogin(email)
    override suspend fun register(
        email: String,
        authKey: ByteArray,
        recoveryAuthKey: ByteArray,
        kdfSalt: ByteArray,
        kdfParams: KdfParams,
        wrappedVaultKey: ByteArray,
        recoveryWrappedVaultKey: ByteArray,
        device: DeviceIdentity,
    ): Result<AuthenticatedUser, AppError> = delegate.register(
        email,
        authKey,
        recoveryAuthKey,
        kdfSalt,
        kdfParams,
        wrappedVaultKey,
        recoveryWrappedVaultKey,
        device,
    )
    override suspend fun login(
        email: String,
        authKey: ByteArray,
        device: DeviceIdentity,
    ): Result<AuthenticatedUser, AppError> = delegate.login(email, authKey, device)
    override suspend fun refresh(refreshToken: String): Result<AuthTokens, AppError> = delegate.refresh(refreshToken)
    override suspend fun logout(): Result<Unit, AppError> = delegate.logout()
}
