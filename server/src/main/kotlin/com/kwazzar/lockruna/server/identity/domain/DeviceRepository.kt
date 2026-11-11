package com.kwazzar.lockruna.server.identity.domain

import java.time.Instant

data class DeviceRecord(
    val id: String,
    val userId: String,
    val name: String,
    val platform: String,
    val createdAt: Instant,
    val lastSeen: Instant?,
    val revokedAt: Instant?,
)

interface DeviceRepository {
    suspend fun create(device: DeviceRecord): DeviceRecord

    suspend fun listByUser(userId: String): List<DeviceRecord>

    suspend fun revoke(userId: String, deviceId: String): Boolean

    suspend fun findById(userId: String, deviceId: String): DeviceRecord?

    suspend fun touch(userId: String, deviceId: String, seenAt: Instant): Boolean

    suspend fun revokeAllForUser(userId: String, exceptDeviceId: String?, revokedAt: Instant): Int
}
