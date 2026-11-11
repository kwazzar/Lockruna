package com.kwazzar.lockruna.application.support

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
import com.kwazzar.lockruna.domain.session.Clock
import com.kwazzar.lockruna.domain.session.IdGenerator
import com.kwazzar.lockruna.domain.session.SecureKeyStore
import com.kwazzar.lockruna.domain.session.VaultKey
import com.kwazzar.lockruna.domain.vault.ItemId
import com.kwazzar.lockruna.domain.vault.VaultItem
import com.kwazzar.lockruna.domain.vault.VaultItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryVaultItemRepository : VaultItemRepository {
    private val store = mutableMapOf<String, VaultItem>()
    private val stateFlow = MutableStateFlow(emptyList<VaultItem>())

    override suspend fun save(item: VaultItem): Result<VaultItem, AppError> {
        store[item.id.value] = item
        stateFlow.value = store.values.toList()
        return Result.Success(item)
    }

    override suspend fun get(id: ItemId): Result<VaultItem?, AppError> = Result.Success(store[id.value])

    override fun observeAll() = stateFlow.asStateFlow()
}

class FakeClock(var wallMillis: Long = 0, var monotonicMillis: Long = 0) : Clock {
    override fun nowMillis(): Long = wallMillis
    override fun monotonicMillis(): Long = monotonicMillis
}

class FakeIdGenerator : IdGenerator {
    private var counter = 1

    override fun newItemId(): ItemId {
        val uuid = counter.toString(16).padStart(32, '0').let { s ->
            "${s.substring(
                0,
                8,
            )}-${s.substring(8, 12)}-4${s.substring(13, 16)}-8${s.substring(17, 20)}-${s.substring(20, 32)}"
        }
        counter++
        val result = ItemId.of(uuid)
        return when (result) {
            is Result.Success -> result.value
            is Result.Failure -> throw IllegalStateException("Invalid generated UUID: ${result.error}")
        }
    }

    override fun newUuid(): String {
        val uuid = counter.toString(16).padStart(32, '0').let { s ->
            "${s.substring(
                0,
                8,
            )}-${s.substring(8, 12)}-4${s.substring(13, 16)}-8${s.substring(17, 20)}-${s.substring(20, 32)}"
        }
        counter++
        return uuid
    }
}

class FakeAuthApi(
    var preloginResult: Result<PreLoginData, AppError> = Result.Success(
        PreLoginData(ByteArray(16), KdfParams.SUPPORTED),
    ),
    var registerResult: Result<AuthenticatedUser, AppError>? = null,
    var loginResult: Result<AuthenticatedUser, AppError>? = null,
    var refreshResult: Result<AuthTokens, AppError>? = null,
    var logoutResult: Result<Unit, AppError> = Result.Success(Unit),
) : AuthApi {
    var lastPreloginEmail: String? = null
    var lastRegisterEmail: String? = null
    var lastLoginEmail: String? = null

    override suspend fun prelogin(email: String): Result<PreLoginData, AppError> {
        lastPreloginEmail = email
        return preloginResult
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
    ): Result<AuthenticatedUser, AppError> {
        lastRegisterEmail = email
        return registerResult ?: Result.Failure(AppError.InvalidCredentials)
    }

    override suspend fun login(
        email: String,
        authKey: ByteArray,
        device: DeviceIdentity,
    ): Result<AuthenticatedUser, AppError> {
        lastLoginEmail = email
        return loginResult ?: Result.Failure(AppError.InvalidCredentials)
    }

    override suspend fun refresh(refreshToken: String): Result<AuthTokens, AppError> =
        refreshResult ?: Result.Failure(AppError.Unauthorized)

    override suspend fun logout(): Result<Unit, AppError> = logoutResult
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

class FakeBiometricPolicyStore : BiometricPolicyStore {
    private var failureCount = 0
    private var lastSuccess: Long? = null

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

/**
 * Deterministic Argon2 fake for use-case tests: hash = HKDF(password || salt, empty).
 * NOT for security use.
 */
class FakeArgon2 : Argon2 {
    var hashResult: ByteArray? = null

    override suspend fun hash(password: String, salt: ByteArray, params: KdfParams): ByteArray = hashResult ?: run {
        val bytes = (password + salt.toHexString()).encodeToByteArray()
        val result = ByteArray(32)
        val copyLen = minOf(bytes.size, 32)
        bytes.copyInto(result, 0, 0, copyLen)
        result
    }
}

class FakePasswordNormalizer : PasswordNormalizer {
    var normalized: String = ""
    override fun normalize(input: String): String = normalized
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

private fun ByteArray.toHexString(): String = joinToString("") { byte ->
    val value = byte.toInt() and 0xFF
    "${HEX_DIGITS[value shr 4]}${HEX_DIGITS[value and 0x0F]}"
}

private const val HEX_DIGITS = "0123456789abcdef"
