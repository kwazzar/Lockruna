package com.kwazzar.lockruna.domain.auth

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.crypto.KdfParams
import com.kwazzar.lockruna.domain.error.AppError

/** Where a recovery flow starts: the wrapper the client needs to unwrap the vault key. */
data class RecoveryStartData(val userId: String, val recoveryWrappedVaultKey: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RecoveryStartData) return false
        return userId == other.userId && recoveryWrappedVaultKey.contentEquals(other.recoveryWrappedVaultKey)
    }

    override fun hashCode(): Int = 31 * userId.hashCode() + recoveryWrappedVaultKey.contentHashCode()

    override fun toString(): String = "RecoveryStartData(userId=$userId, recoveryWrappedVaultKey=<redacted>)"
}

/** A device of the user, as the settings screen shows it. [lastSeen] is an ISO-8601 timestamp. */
data class DeviceInfo(val id: String, val name: String, val platform: String, val lastSeen: String?, val current: Boolean)

/** Account recovery (US-03) and device management (US-10) talk to the server through this port. */
interface RecoveryApi {
    suspend fun start(email: String): Result<RecoveryStartData, AppError>

    suspend fun complete(
        email: String,
        recoveryAuthKey: ByteArray,
        newAuthKey: ByteArray,
        newKdfSalt: ByteArray,
        newKdfParams: KdfParams,
        newWrappedVaultKey: ByteArray,
        device: DeviceIdentity,
    ): Result<AuthenticatedUser, AppError>
}

interface DeviceApi {
    suspend fun list(): Result<List<DeviceInfo>, AppError>

    suspend fun revoke(deviceId: String): Result<Unit, AppError>
}

/** Deleting an account needs the master password again (US-11). */
interface AccountApi {
    suspend fun delete(authKey: ByteArray): Result<Unit, AppError>
}