package com.kwazzar.lockruna.presentation.support

import com.kwazzar.lockruna.domain.auth.AuthApi
import com.kwazzar.lockruna.domain.auth.AuthStateStore
import com.kwazzar.lockruna.domain.auth.AuthTokens
import com.kwazzar.lockruna.domain.auth.AuthenticatedUser
import com.kwazzar.lockruna.domain.auth.DeviceIdentity
import com.kwazzar.lockruna.domain.auth.PreLoginData
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.crypto.Argon2
import com.kwazzar.lockruna.domain.crypto.KdfParams
import com.kwazzar.lockruna.domain.crypto.PasswordNormalizer
import com.kwazzar.lockruna.domain.error.AppError
import kotlinx.coroutines.CompletableDeferred

class FakeAuthApi(
    var preloginResult: Result<PreLoginData, AppError> = Result.Success(
        PreLoginData(ByteArray(16), KdfParams.SUPPORTED),
    ),
    var loginResult: Result<AuthenticatedUser, AppError>? = null,
) : AuthApi {
    var loginCalls = 0
    var lastLoginDevice: DeviceIdentity? = null
    var loginGate: CompletableDeferred<Unit>? = null

    override suspend fun prelogin(email: String): Result<PreLoginData, AppError> = preloginResult

    override suspend fun register(
        email: String,
        authKey: ByteArray,
        recoveryAuthKey: ByteArray,
        kdfSalt: ByteArray,
        kdfParams: KdfParams,
        wrappedVaultKey: ByteArray,
        recoveryWrappedVaultKey: ByteArray,
        device: DeviceIdentity,
    ): Result<AuthenticatedUser, AppError> = Result.Failure(AppError.InvalidCredentials)

    override suspend fun login(
        email: String,
        authKey: ByteArray,
        device: DeviceIdentity,
    ): Result<AuthenticatedUser, AppError> {
        loginGate?.await()
        loginCalls++
        lastLoginDevice = device
        return loginResult ?: Result.Failure(AppError.InvalidCredentials)
    }

    override suspend fun refresh(refreshToken: String): Result<AuthTokens, AppError> =
        Result.Failure(AppError.Unauthorized)

    override suspend fun logout(): Result<Unit, AppError> = Result.Success(Unit)
}

class FakeArgon2 : Argon2 {
    var hashResult: ByteArray? = null

    override suspend fun hash(password: String, salt: ByteArray, params: KdfParams): ByteArray =
        hashResult ?: ByteArray(32) { 1 }
}

class FakePasswordNormalizer : PasswordNormalizer {
    var normalized: String = ""

    override fun normalize(raw: String): String = normalized
}

class FakeAuthStateStore : AuthStateStore {
    private var session: AuthenticatedUser? = null

    override suspend fun saveSession(user: AuthenticatedUser) {
        session = user
    }

    override suspend fun loadSession(): AuthenticatedUser? = session

    override suspend fun clear() {
        session = null
    }
}
