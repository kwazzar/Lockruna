package com.kwazzar.lockruna.server.sync.domain

import java.time.Instant

/**
 * A vault item as the server sees it: ciphertext only, never plaintext.
 */
data class SealedItemRecord(
    val userId: String,
    val id: String,
    val type: String,
    val ciphertext: ByteArray?,
    val nonce: ByteArray?,
    val version: Long,
    val syncSeq: Long,
    val deletedAt: Instant?,
    val updatedAt: Instant,
)

data class ChangeRecord(
    val id: String,
    val type: String,
    val ciphertext: ByteArray?,
    val nonce: ByteArray?,
    val baseVersion: Long,
    val deleted: Boolean,
)

sealed interface ApplyResult {
    data class Applied(val version: Long, val updatedAt: Instant) : ApplyResult

    data class Conflict(val server: SealedItemRecord) : ApplyResult
}

interface VaultItemRepository {
    suspend fun listSince(userId: String, afterSyncSeq: Long, limit: Int): List<SealedItemRecord>

    suspend fun apply(userId: String, change: ChangeRecord, newSyncSeq: Long, updatedAt: Instant): ApplyResult

    suspend fun currentSyncSeq(userId: String): Long

    suspend fun nextSyncSeq(userId: String): Long
}
