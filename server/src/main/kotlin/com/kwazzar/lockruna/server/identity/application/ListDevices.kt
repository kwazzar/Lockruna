package com.kwazzar.lockruna.server.identity.application

import com.kwazzar.lockruna.server.identity.domain.DeviceRecord
import com.kwazzar.lockruna.server.identity.domain.DeviceRepository

/**
 * The active devices of one user, never of anybody else.
 */
class ListDevices(private val devices: DeviceRepository) {
    suspend operator fun invoke(userId: String): List<DeviceRecord> =
        devices.listByUser(userId).filter { it.revokedAt == null }
}
