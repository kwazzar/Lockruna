package com.kwazzar.lockruna.server.sync.infrastructure

import com.kwazzar.lockruna.server.platform.db.IdempotencyKeys
import com.kwazzar.lockruna.server.platform.db.TransactionRunner
import com.kwazzar.lockruna.server.platform.db.toColumn
import com.kwazzar.lockruna.server.platform.db.toUuid
import com.kwazzar.lockruna.server.sync.domain.IdempotencyRepository
import com.kwazzar.lockruna.server.sync.domain.StoredResponse
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.Instant

class ExposedIdempotencyRepository(private val transactions: TransactionRunner) : IdempotencyRepository {
    override suspend fun find(userId: String, key: String): StoredResponse? = transactions.transaction {
        IdempotencyKeys.selectAll()
            .where {
                (IdempotencyKeys.userId eq userId.toUuid()) and
                    (IdempotencyKeys.idempotencyKey eq key.toUuid())
            }
            .firstOrNull()
            ?.toRecord()
    }

    override suspend fun store(
        userId: String,
        key: String,
        bodyHash: ByteArray,
        response: String,
        createdAt: Instant,
    ): StoredResponse = transactions.transaction {
        IdempotencyKeys.insertIgnore {
            it[IdempotencyKeys.userId] = userId.toUuid()
            it[IdempotencyKeys.idempotencyKey] = key.toUuid()
            it[IdempotencyKeys.bodyHash] = bodyHash
            it[IdempotencyKeys.response] = response
            it[IdempotencyKeys.createdAt] = createdAt.toColumn()
        }
        find(userId, key) ?: error("idempotency record $key vanished right after insert")
    }

    private fun ResultRow.toRecord() = StoredResponse(
        bodyHash = this[IdempotencyKeys.bodyHash],
        response = this[IdempotencyKeys.response],
        createdAt = this[IdempotencyKeys.createdAt].toInstant(),
    )
}
