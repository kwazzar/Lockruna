package com.kwazzar.lockruna.domain.sync

import com.kwazzar.lockruna.domain.vault.VaultItemType

/** Null-safe byte comparison: data classes compare arrays by identity, which is never what we mean. */
internal fun ByteArray?.contentEqualsOrNull(other: ByteArray?): Boolean = when {
    this == null -> other == null
    else -> other != null && contentEquals(other)
}

/** A local change waiting to be pushed to the server. */
data class PendingChange(
    val id: String,
    val type: VaultItemType,
    val ciphertext: ByteArray?,
    val nonce: ByteArray?,
    val baseVersion: Long,
    val deleted: Boolean = false,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as PendingChange
        return id == other.id &&
            type == other.type &&
            ciphertext.contentEqualsOrNull(other.ciphertext) &&
            nonce.contentEqualsOrNull(other.nonce) &&
            baseVersion == other.baseVersion &&
            deleted == other.deleted
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + (ciphertext?.contentHashCode() ?: 0)
        result = 31 * result + (nonce?.contentHashCode() ?: 0)
        result = 31 * result + baseVersion.hashCode()
        result = 31 * result + deleted.hashCode()
        return result
    }

    override fun toString() = "PendingChange(id=$id, baseVersion=$baseVersion)"
}

/** An item received from the server; [ciphertext] is null for a tombstone. */
data class RemoteItem(
    val id: String,
    val type: VaultItemType,
    val ciphertext: ByteArray?,
    val nonce: ByteArray?,
    val version: Long,
    val deleted: Boolean,
    val updatedAtMillis: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as RemoteItem
        return id == other.id &&
            type == other.type &&
            ciphertext.contentEqualsOrNull(other.ciphertext) &&
            nonce.contentEqualsOrNull(other.nonce) &&
            version == other.version &&
            deleted == other.deleted &&
            updatedAtMillis == other.updatedAtMillis
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + (ciphertext?.contentHashCode() ?: 0)
        result = 31 * result + (nonce?.contentHashCode() ?: 0)
        result = 31 * result + version.hashCode()
        result = 31 * result + deleted.hashCode()
        result = 31 * result + updatedAtMillis.hashCode()
        return result
    }

    override fun toString() = "RemoteItem(id=$id, version=$version, deleted=$deleted)"
}

/** One page of a cursor-based pull. */
data class SyncPage(val items: List<RemoteItem>, val nextCursor: String, val hasMore: Boolean)

/** A change accepted by the server; [updatedAtMillis] is the server timestamp. */
data class AppliedChange(val id: String, val newVersion: Long, val updatedAtMillis: Long)

/** A change rejected by the server, with the current server record. */
data class Conflict(val id: String, val server: RemoteItem)

/** Outcome of a push batch. */
data class PushResult(val applied: List<AppliedChange>, val conflicts: List<Conflict>)
