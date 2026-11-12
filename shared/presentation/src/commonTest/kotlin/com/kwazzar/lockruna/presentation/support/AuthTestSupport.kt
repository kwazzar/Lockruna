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
import com.kwazzar.lockruna.domain.session.IdGenerator
import com.kwazzar.lockruna.domain.session.SecureKeyStore
import com.kwazzar.lockruna.domain.session.VaultKey
import com.kwazzar.lockruna.domain.vault.ItemId
import kotlinx.coroutines.CompletableDeferred

/** Deterministic ids for the store tests. */
class FixedIdGenerator : IdGenerator {
    override fun newItemId(): ItemId = (ItemId.of("00000000-0000-4000-8000-000000000001") as Result.Success).value

    override fun newUuid(): String = "00000000-0000-4000-8000-000000000001"
}

class FakeAuthApi(
    var preloginResult: Result<PreLoginData, AppError> = Result.Success(
        PreLoginData(ByteArray(16), KdfParams.SUPPORTED),
    ),
    var loginResult: Result<AuthenticatedUser, AppError>? = null,
    var registerResult: Result<AuthenticatedUser, AppError>? = null,
) : AuthApi {
    var loginCalls = 0
    var registerCalls = 0
    var lastLoginDevice: DeviceIdentity? = null
    var lastRegisterDevice: DeviceIdentity? = null
    var loginGate: CompletableDeferred<Unit>? = null
    var registerGate: CompletableDeferred<Unit>? = null

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
    ): Result<AuthenticatedUser, AppError> {
        registerGate?.await()
        registerCalls++
        lastRegisterDevice = device
        return registerResult ?: Result.Success(
            AuthenticatedUser(
                userId = "user-1",
                deviceId = device.deviceId ?: "device-1",
                wrappedVaultKey = wrappedVaultKey,
                kdfSalt = kdfSalt,
                kdfParams = KdfParams.SUPPORTED,
                tokens = AuthTokens("access-token", "refresh-token", 3600),
            ),
        )
    }

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
    var authenticateCalls = 0
    var authenticateGate: CompletableDeferred<Unit>? = null

    override suspend fun isAvailable(): Boolean = isAvailableResult

    override suspend fun authenticate(reason: String): Boolean {
        authenticateCalls++
        authenticateGate?.await()
        return authenticateResult
    }
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
