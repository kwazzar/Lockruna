package com.kwazzar.lockruna.server.identity.infrastructure

import com.kwazzar.lockruna.server.identity.domain.DeviceRecord
import com.kwazzar.lockruna.server.identity.domain.DeviceRepository
import com.kwazzar.lockruna.server.platform.db.Devices
import com.kwazzar.lockruna.server.platform.db.TransactionRunner
import com.kwazzar.lockruna.server.platform.db.toColumn
import com.kwazzar.lockruna.server.platform.db.toUuid
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

class ExposedDeviceRepository(private val transactions: TransactionRunner) : DeviceRepository {
    override suspend fun create(device: DeviceRecord): DeviceRecord = transactions.transaction {
        Devices.insert {
            it[id] = device.id.toUuid()
            it[userId] = device.userId.toUuid()
            it[name] = device.name
            it[platform] = device.platform
            it[createdAt] = device.createdAt.toColumn()
            it[lastSeen] = device.lastSeen?.toColumn()
            it[revokedAt] = device.revokedAt?.toColumn()
        }
        device
    }

    override suspend fun listByUser(userId: String): List<DeviceRecord> = transactions.transaction {
        Devices.selectAll()
            .where { Devices.userId eq userId.toUuid() }
            .map { it.toRecord() }
    }

    override suspend fun revoke(userId: String, deviceId: String): Boolean = transactions.transaction {
        Devices.update({ (Devices.userId eq userId.toUuid()) and (Devices.id eq deviceId.toUuid()) }) {
            it[Devices.revokedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        } > 0
    }

    override suspend fun findById(userId: String, deviceId: String): DeviceRecord? = transactions.transaction {
        Devices.selectAll()
            .where { (Devices.userId eq userId.toUuid()) and (Devices.id eq deviceId.toUuid()) }
            .firstOrNull()
            ?.toRecord()
    }

    override suspend fun touch(userId: String, deviceId: String, seenAt: Instant): Boolean = transactions.transaction {
        Devices.update({ (Devices.userId eq userId.toUuid()) and (Devices.id eq deviceId.toUuid()) }) {
            it[Devices.lastSeen] = seenAt.toColumn()
        } > 0
    }

    override suspend fun revokeAllForUser(userId: String, exceptDeviceId: String?, revokedAt: Instant): Int = transactions.transaction {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        if (exceptDeviceId == null) {
            Devices.update({ Devices.userId eq userId.toUuid() }) {
                it[Devices.revokedAt] = now
            }
        } else {
            Devices.update({ (Devices.userId eq userId.toUuid()) and (Devices.id neq exceptDeviceId.toUuid()) }) {
                it[Devices.revokedAt] = now
            }
        }
    }

    private fun ResultRow.toRecord() = DeviceRecord(
        id = this[Devices.id].toString(),
        userId = this[Devices.userId].toString(),
        name = this[Devices.name],
        platform = this[Devices.platform],
        createdAt = this[Devices.createdAt].toInstant(),
        lastSeen = this[Devices.lastSeen]?.toInstant(),
        revokedAt = this[Devices.revokedAt]?.toInstant(),
    )
}
