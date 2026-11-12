package com.kwazzar.lockruna.application.auth

import com.kwazzar.lockruna.domain.auth.DeviceApi
import com.kwazzar.lockruna.domain.auth.DeviceInfo
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError

/** The devices of the signed-in user (US-10). */
class ListDevices(private val deviceApi: DeviceApi) {
    suspend operator fun invoke(): Result<List<DeviceInfo>, AppError> = deviceApi.list()
}

/** Revokes another device: its tokens die and it disappears from the list. */
class RevokeDevice(private val deviceApi: DeviceApi) {
    suspend operator fun invoke(deviceId: String): Result<Unit, AppError> = deviceApi.revoke(deviceId)
}