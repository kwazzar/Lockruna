package com.kwazzar.lockruna.application.support

import com.kwazzar.lockruna.domain.auth.AccountApi
import com.kwazzar.lockruna.domain.auth.AuthTokens
import com.kwazzar.lockruna.domain.auth.AuthenticatedUser
import com.kwazzar.lockruna.domain.auth.DeviceApi
import com.kwazzar.lockruna.domain.auth.DeviceIdentity
import com.kwazzar.lockruna.domain.auth.DeviceInfo
import com.kwazzar.lockruna.domain.auth.RecoveryApi
import com.kwazzar.lockruna.domain.auth.RecoveryStartData
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.crypto.KdfParams
import com.kwazzar.lockruna.domain.error.AppError

class FakeRecoveryApi :
    RecoveryApi,
    DeviceApi,
    AccountApi {
    var startResult: Result<RecoveryStartData, AppError> = Result.Success(
        RecoveryStartData("user-1", ByteArray(48)),
    )
    var completeResult: Result<AuthenticatedUser, AppError>? = null
    var listResult: Result<List<DeviceInfo>, AppError> = Result.Success(emptyList())
    var revokeResult: Result<Unit, AppError> = Result.Success(Unit)
    var deleteResult: Result<Unit, AppError> = Result.Success(Unit)

    var startCalls: Int = 0
    var lastStartEmail: String? = null
    var lastCompleteEmail: String? = null
    var lastCompleteRecoveryAuthKey: ByteArray? = null
    var lastCompleteNewAuthKey: ByteArray? = null
    var lastCompleteWrappedVaultKey: ByteArray? = null
    var lastCompleteDevice: DeviceIdentity? = null
    var lastRevokedDeviceId: String? = null
    var lastDeleteAuthKey: ByteArray? = null

    override suspend fun start(email: String): Result<RecoveryStartData, AppError> {
        startCalls++
        lastStartEmail = email
        return startResult
    }

    override suspend fun complete(
        email: String,
        recoveryAuthKey: ByteArray,
        newAuthKey: ByteArray,
        newKdfSalt: ByteArray,
        newKdfParams: KdfParams,
        newWrappedVaultKey: ByteArray,
        device: DeviceIdentity,
    ): Result<AuthenticatedUser, AppError> {
        lastCompleteEmail = email
        lastCompleteRecoveryAuthKey = recoveryAuthKey
        lastCompleteNewAuthKey = newAuthKey
        lastCompleteWrappedVaultKey = newWrappedVaultKey
        lastCompleteDevice = device
        return completeResult ?: Result.Success(
            AuthenticatedUser(
                userId = "user-1",
                deviceId = device.deviceId ?: "device-new",
                wrappedVaultKey = newWrappedVaultKey,
                kdfSalt = newKdfSalt,
                kdfParams = KdfParams.SUPPORTED,
                tokens = AuthTokens("access-token", "refresh-token", 3600),
            ),
        )
    }

    override suspend fun list(): Result<List<DeviceInfo>, AppError> = listResult

    override suspend fun revoke(deviceId: String): Result<Unit, AppError> {
        lastRevokedDeviceId = deviceId
        return revokeResult
    }

    override suspend fun delete(authKey: ByteArray): Result<Unit, AppError> {
        lastDeleteAuthKey = authKey
        return deleteResult
    }
}
