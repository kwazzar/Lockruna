package com.kwazzar.lockruna.domain.sync

/** A local vault record as the sync engine sees it. */
data class LocalRecord(
    val id: String,
    val version: Long,
    val updatedAtMillis: Long,
    val ciphertext: ByteArray?,
    val nonce: ByteArray?,
    val dirty: Boolean,
    val maxSeenVersion: Long,
) {
    override fun toString(): String = "LocalRecord(id=$id, version=$version, dirty=$dirty)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as LocalRecord
        return id == other.id &&
            version == other.version &&
            updatedAtMillis == other.updatedAtMillis &&
            ciphertext.contentEqualsOrNull(other.ciphertext) &&
            nonce.contentEqualsOrNull(other.nonce) &&
            dirty == other.dirty &&
            maxSeenVersion == other.maxSeenVersion
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + version.hashCode()
        result = 31 * result + updatedAtMillis.hashCode()
        result = 31 * result + (ciphertext?.contentHashCode() ?: 0)
        result = 31 * result + (nonce?.contentHashCode() ?: 0)
        result = 31 * result + dirty.hashCode()
        result = 31 * result + maxSeenVersion.hashCode()
        return result
    }
}

/** Local sync state: cursor, outbox and the sealed records themselves. */
interface SyncStore {
    suspend fun cursor(): String?

    suspend fun setCursor(cursor: String?)

    suspend fun lastSyncAtMillis(): Long?

    suspend fun markSynced(nowMillis: Long)

    suspend fun peek(id: String): LocalRecord?

    /** Dirty records ready to push; [PendingChange.baseVersion] is the local row version. */
    suspend fun dirtyChanges(limit: Int): List<PendingChange>

    /** Upserts a server item; the previous content goes to the item history and dirty is cleared. */
    suspend fun applyRemote(item: RemoteItem)

    /** Marks a pushed change as applied with the server version and timestamp. */
    suspend fun markItemApplied(id: String, newVersion: Long, updatedAtMillis: Long)

    /** Stores the batch and its key until the server answers. */
    suspend fun stageOutbox(idempotencyKey: String, changes: List<PendingChange>)

    suspend fun pendingOutbox(): Outbox?

    suspend fun clearOutbox()

    /** Wipes the pull state so the next sync pulls everything again. */
    suspend fun resetLocalData()
}

/** A staged push batch with the key it must be retried with. */
data class Outbox(val idempotencyKey: String, val changes: List<PendingChange>)
