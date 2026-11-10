package com.kwazzar.lockruna.application.support

import com.kwazzar.lockruna.domain.sync.LocalRecord
import com.kwazzar.lockruna.domain.sync.Outbox
import com.kwazzar.lockruna.domain.sync.PendingChange
import com.kwazzar.lockruna.domain.sync.RemoteItem
import com.kwazzar.lockruna.domain.sync.SyncStore

/** In-memory sync state with recorded calls. */
class FakeSyncStore : SyncStore {
    var cursorValue: String? = null
        private set

    var lastSyncAt: Long? = null
        private set

    var outbox: Outbox? = null
        private set

    var resetCount = 0
        private set

    val records = mutableMapOf<String, LocalRecord>()
    private val staged = mutableMapOf<String, PendingChange>()

    /** Marks a local edit as dirty. */
    fun stageDirty(change: PendingChange, updatedAtMillis: Long = 0) {
        staged[change.id] = change
        records[change.id] = LocalRecord(
            id = change.id,
            version = change.baseVersion,
            updatedAtMillis = updatedAtMillis,
            ciphertext = change.ciphertext,
            nonce = change.nonce,
            dirty = true,
            maxSeenVersion = records[change.id]?.maxSeenVersion ?: 0,
        )
    }

    override suspend fun cursor(): String? = cursorValue

    override suspend fun setCursor(cursor: String?) {
        cursorValue = cursor
    }

    override suspend fun lastSyncAtMillis(): Long? = lastSyncAt

    override suspend fun markSynced(nowMillis: Long) {
        lastSyncAt = nowMillis
    }

    override suspend fun peek(id: String): LocalRecord? = records[id]

    override suspend fun dirtyChanges(limit: Int): List<PendingChange> = staged.values.take(limit)

    override suspend fun applyRemote(item: RemoteItem) {
        val previous = records[item.id]
        records[item.id] = LocalRecord(
            id = item.id,
            version = item.version,
            updatedAtMillis = item.updatedAtMillis,
            ciphertext = item.ciphertext,
            nonce = item.nonce,
            dirty = false,
            maxSeenVersion = maxOf(previous?.maxSeenVersion ?: 0, item.version),
        )
        staged.remove(item.id)
    }

    override suspend fun markItemApplied(id: String, newVersion: Long, updatedAtMillis: Long) {
        val previous = records[id]
        records[id] = LocalRecord(
            id = id,
            version = newVersion,
            updatedAtMillis = updatedAtMillis,
            ciphertext = previous?.ciphertext,
            nonce = previous?.nonce,
            dirty = false,
            maxSeenVersion = maxOf(previous?.maxSeenVersion ?: 0, newVersion),
        )
        staged.remove(id)
    }

    override suspend fun stageOutbox(idempotencyKey: String, changes: List<PendingChange>) {
        outbox = Outbox(idempotencyKey, changes)
    }

    override suspend fun pendingOutbox(): Outbox? = outbox

    override suspend fun clearOutbox() {
        outbox = null
    }

    override suspend fun resetLocalData() {
        resetCount++
        records.clear()
        staged.clear()
        outbox = null
        cursorValue = null
    }
}
