package com.kwazzar.lockruna.presentation.support

import com.kwazzar.lockruna.domain.auth.AuthApi
import com.kwazzar.lockruna.domain.auth.AuthStateStore
import com.kwazzar.lockruna.domain.auth.AuthTokens
import com.kwazzar.lockruna.domain.auth.AuthenticatedUser
import com.kwazzar.lockruna.domain.auth.BiometricPolicyStore
import com.kwazzar.lockruna.domain.auth.DeviceIdentity
import com.kwazzar.lockruna.domain.auth.PreLoginData
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.crypto.Argon2
import com.kwazzar.lockruna.domain.crypto.KdfParams
import com.kwazzar.lockruna.domain.crypto.PasswordNormalizer
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.BiometricGate
import com.kwazzar.lockruna.domain.session.SecureKeyStore
import com.kwazzar.lockruna.domain.session.VaultKey
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

class FakeBiometricGate : BiometricGate {
    var isAvailableResult = true
    var authenticateResult = true

    override suspend fun isAvailable(): Boolean = isAvailableResult

    override suspend fun authenticate(reason: String): Boolean = authenticateResult
}

class FakeSecureKeyStore : SecureKeyStore {
    var storedKey: VaultKey? = null

    override suspend fun saveVaultKey(key: VaultKey) {
        storedKey = key
    }

    override suspend fun loadVaultKey(): VaultKey? = storedKey

    override suspend fun clear() {
        storedKey = null
    }
}

class FakeBiometricPolicyStore : BiometricPolicyStore {
    var failureCount = 0
    var lastSuccess: Long? = null

    override suspend fun failures(): Int = failureCount

    override suspend fun recordFailure() {
        failureCount++
    }

    override suspend fun resetFailures() {
        failureCount = 0
    }

    override suspend fun setLastSuccessfulUnlock(millis: Long) {
        lastSuccess = millis
    }

    override suspend fun lastSuccessfulUnlock(): Long? = lastSuccess
}
