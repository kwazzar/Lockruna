package com.kwazzar.lockruna.server.sync.infrastructure

import com.kwazzar.lockruna.server.platform.db.TransactionRunner
import com.kwazzar.lockruna.server.platform.db.Users
import com.kwazzar.lockruna.server.platform.db.VaultItems
import com.kwazzar.lockruna.server.platform.db.toColumn
import com.kwazzar.lockruna.server.platform.db.toUuid
import com.kwazzar.lockruna.server.sync.domain.ApplyResult
import com.kwazzar.lockruna.server.sync.domain.ChangeRecord
import com.kwazzar.lockruna.server.sync.domain.SealedItemRecord
import com.kwazzar.lockruna.server.sync.domain.VaultItemRepository
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant

class ExposedVaultItemRepository(private val transactions: TransactionRunner) : VaultItemRepository {
    override suspend fun listSince(userId: String, afterSyncSeq: Long, limit: Int): List<SealedItemRecord> =
        transactions.transaction {
            VaultItems.selectAll()
                .where { (VaultItems.userId eq userId.toUuid()) and (VaultItems.syncSeq greater afterSyncSeq) }
                .orderBy(VaultItems.syncSeq)
                .limit(limit)
                .map { it.toRecord() }
        }

    override suspend fun apply(
        userId: String,
        change: ChangeRecord,
        newSyncSeq: Long,
        updatedAt: Instant,
    ): ApplyResult = transactions.transaction {
        if (change.baseVersion == 0L) {
            insertNew(userId, change, newSyncSeq, updatedAt)
        } else {
            updateExisting(userId, change, newSyncSeq, updatedAt)
        }
    }

    override suspend fun currentSyncSeq(userId: String): Long = transactions.transaction {
        Users.select(Users.syncSeq)
            .where { Users.id eq userId.toUuid() }
            .firstOrNull()
            ?.get(Users.syncSeq) ?: 0L
    }

    override suspend fun nextSyncSeq(userId: String): Long = transactions.transaction {
        Users.update({ Users.id eq userId.toUuid() }) {
            it[Users.syncSeq] = Users.syncSeq + 1
        }
        Users.select(Users.syncSeq)
            .where { Users.id eq userId.toUuid() }
            .firstOrNull()
            ?.get(Users.syncSeq) ?: 0L
    }

    private fun insertNew(userId: String, change: ChangeRecord, newSyncSeq: Long, updatedAt: Instant): ApplyResult {
        VaultItems.insertIgnore {
            it[VaultItems.userId] = userId.toUuid()
            it[VaultItems.id] = change.id.toUuid()
            it[VaultItems.type] = change.type
            it[VaultItems.ciphertext] = if (change.deleted) null else change.ciphertext
            it[VaultItems.nonce] = if (change.deleted) null else change.nonce
            it[VaultItems.version] = 1L
            it[VaultItems.syncSeq] = newSyncSeq
            it[VaultItems.deletedAt] = if (change.deleted) updatedAt.toColumn() else null
            it[VaultItems.updatedAt] = updatedAt.toColumn()
        }

        val stored = findItem(userId, change.id)
            ?: error("item ${change.id} vanished right after insert")
        return if (stored.version == 1L && stored.syncSeq == newSyncSeq) {
            ApplyResult.Applied(stored.version, stored.updatedAt)
        } else {
            ApplyResult.Conflict(stored)
        }
    }

    private fun updateExisting(
        userId: String,
        change: ChangeRecord,
        newSyncSeq: Long,
        updatedAt: Instant,
    ): ApplyResult {
        val updated = VaultItems.update({
            (VaultItems.userId eq userId.toUuid()) and
                (VaultItems.id eq change.id.toUuid()) and
                (VaultItems.version eq change.baseVersion) and
                VaultItems.deletedAt.isNull()
        }) {
            it[VaultItems.type] = change.type
            it[VaultItems.ciphertext] = if (change.deleted) null else change.ciphertext
            it[VaultItems.nonce] = if (change.deleted) null else change.nonce
            it[VaultItems.version] = change.baseVersion + 1
            it[VaultItems.syncSeq] = newSyncSeq
            it[VaultItems.deletedAt] = if (change.deleted) updatedAt.toColumn() else null
            it[VaultItems.updatedAt] = updatedAt.toColumn()
        }

        if (updated > 0) return ApplyResult.Applied(change.baseVersion + 1, updatedAt)

        val server = findItem(userId, change.id)
            ?: error("item ${change.id} vanished during update")
        return ApplyResult.Conflict(server)
    }

    private fun findItem(userId: String, itemId: String): SealedItemRecord? = VaultItems.selectAll()
        .where { (VaultItems.userId eq userId.toUuid()) and (VaultItems.id eq itemId.toUuid()) }
        .firstOrNull()
        ?.toRecord()

    private fun ResultRow.toRecord() = SealedItemRecord(
        userId = this[VaultItems.userId].toString(),
        id = this[VaultItems.id].toString(),
        type = this[VaultItems.type],
        ciphertext = this[VaultItems.ciphertext],
        nonce = this[VaultItems.nonce],
        version = this[VaultItems.version],
        syncSeq = this[VaultItems.syncSeq],
        deletedAt = this[VaultItems.deletedAt]?.toInstant(),
        updatedAt = this[VaultItems.updatedAt].toInstant(),
    )
}
