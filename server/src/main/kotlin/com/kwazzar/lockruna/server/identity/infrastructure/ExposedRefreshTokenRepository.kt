package com.kwazzar.lockruna.server.identity.infrastructure

import com.kwazzar.lockruna.server.identity.domain.RefreshTokenRecord
import com.kwazzar.lockruna.server.identity.domain.RefreshTokenRepository
import com.kwazzar.lockruna.server.platform.db.Devices
import com.kwazzar.lockruna.server.platform.db.RefreshTokens
import com.kwazzar.lockruna.server.platform.db.TransactionRunner
import com.kwazzar.lockruna.server.platform.db.toColumn
import com.kwazzar.lockruna.server.platform.db.toUuid
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant

class ExposedRefreshTokenRepository(private val transactions: TransactionRunner) : RefreshTokenRepository {
    override suspend fun insert(token: RefreshTokenRecord): RefreshTokenRecord = transactions.transaction {
        RefreshTokens.insert {
            it[id] = token.id.toUuid()
            it[deviceId] = token.deviceId.toUuid()
            it[familyId] = token.familyId.toUuid()
            it[tokenHash] = token.tokenHash
            it[expiresAt] = token.expiresAt.toColumn()
            it[familyCreatedAt] = token.familyCreatedAt.toColumn()
            it[usedAt] = token.usedAt?.toColumn()
            it[revokedAt] = token.revokedAt?.toColumn()
        }
        token
    }

    override suspend fun findByHash(hash: ByteArray): RefreshTokenRecord? = transactions.transaction {
        val token = RefreshTokens.selectAll()
            .where { RefreshTokens.tokenHash eq hash }
            .firstOrNull() ?: return@transaction null
        val device = Devices.selectAll()
            .where { Devices.id eq token[RefreshTokens.deviceId] }
            .firstOrNull() ?: return@transaction null
        token.toRecord(device[Devices.userId].toString())
    }

    override suspend fun markUsed(userId: String, id: String, usedAt: Instant): Boolean = transactions.transaction {
        val token = RefreshTokens.selectAll()
            .where { RefreshTokens.id eq id.toUuid() }
            .firstOrNull() ?: return@transaction false
        val owner = Devices.selectAll()
            .where { Devices.id eq token[RefreshTokens.deviceId] }
            .firstOrNull() ?: return@transaction false
        if (owner[Devices.userId].toString() != userId) return@transaction false

        RefreshTokens.update({
            (RefreshTokens.id eq id.toUuid()) and
                RefreshTokens.usedAt.isNull() and
                RefreshTokens.revokedAt.isNull()
        }) {
            it[RefreshTokens.usedAt] = usedAt.toColumn()
        } > 0
    }

    override suspend fun revokeFamily(userId: String, familyId: String, revokedAt: Instant): Int =
        transactions.transaction {
            val deviceIds = Devices.select(Devices.id)
                .where { Devices.userId eq userId.toUuid() }
                .map { it[Devices.id] }
            if (deviceIds.isEmpty()) return@transaction 0

            RefreshTokens.update({
                (RefreshTokens.familyId eq familyId.toUuid()) and (RefreshTokens.deviceId inList deviceIds)
            }) {
                it[RefreshTokens.revokedAt] = revokedAt.toColumn()
            }
        }

    override suspend fun revokeDeviceTokens(userId: String, deviceId: String, revokedAt: Instant): Int =
        transactions.transaction {
            val owner = Devices.selectAll()
                .where { Devices.id eq deviceId.toUuid() }
                .firstOrNull() ?: return@transaction 0
            if (owner[Devices.userId].toString() != userId) return@transaction 0

            RefreshTokens.update({
                (RefreshTokens.deviceId eq deviceId.toUuid()) and RefreshTokens.revokedAt.isNull()
            }) {
                it[RefreshTokens.revokedAt] = revokedAt.toColumn()
            }
        }

    private fun ResultRow.toRecord(userId: String) = RefreshTokenRecord(
        id = this[RefreshTokens.id].toString(),
        deviceId = this[RefreshTokens.deviceId].toString(),
        userId = userId,
        familyId = this[RefreshTokens.familyId].toString(),
        tokenHash = this[RefreshTokens.tokenHash],
        expiresAt = this[RefreshTokens.expiresAt].toInstant(),
        familyCreatedAt = this[RefreshTokens.familyCreatedAt].toInstant(),
        usedAt = this[RefreshTokens.usedAt]?.toInstant(),
        revokedAt = this[RefreshTokens.revokedAt]?.toInstant(),
    )
}
