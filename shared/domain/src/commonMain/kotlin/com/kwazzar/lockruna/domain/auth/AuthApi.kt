package com.kwazzar.lockruna.domain.auth

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.crypto.KdfParams as DomainKdfParams

interface AuthApi {
    suspend fun prelogin(email: String): Result<PreLoginData, AppError>
    suspend fun register(
        email: String,
        authKey: ByteArray,
        recoveryAuthKey: ByteArray,
        kdfSalt: ByteArray,
        kdfParams: DomainKdfParams,
        wrappedVaultKey: ByteArray,
        recoveryWrappedVaultKey: ByteArray,
        device: DeviceIdentity,
    ): Result<AuthenticatedUser, AppError>
    suspend fun login(email: String, authKey: ByteArray, device: DeviceIdentity): Result<AuthenticatedUser, AppError>
    suspend fun refresh(refreshToken: String): Result<AuthTokens, AppError>
    suspend fun logout(): Result<Unit, AppError>
}
