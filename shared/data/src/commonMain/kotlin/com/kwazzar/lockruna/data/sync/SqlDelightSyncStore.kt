package com.kwazzar.lockruna.data.sync

import com.kwazzar.lockruna.contract.ChangeDto
import com.kwazzar.lockruna.data.db.PassKeeperDatabase
import com.kwazzar.lockruna.domain.sync.LocalRecord
import com.kwazzar.lockruna.domain.sync.Outbox
import com.kwazzar.lockruna.domain.sync.PendingChange
import com.kwazzar.lockruna.domain.sync.RemoteItem
import com.kwazzar.lockruna.domain.sync.SyncStore
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private const val HISTORY_SIZE = 10L

/** Local sync state over SQLDelight: cursor, outbox and the sealed records (ADR-005). */
class SqlDelightSyncStore(private val database: PassKeeperDatabase) : SyncStore {

    private val queries get() = database.passKeeperDatabaseQueries
    private val json = Json { ignoreUnknownKeys = true }
    private val changeList = ListSerializer(ChangeDto.serializer())

    override suspend fun cursor(): String? = syncState()?.cursor

    override suspend fun setCursor(cursor: String?) {
        val state = syncState()
        queries.upsertSyncState(cursor = cursor, last_sync_at = state?.last_sync_at)
    }

    override suspend fun lastSyncAtMillis(): Long? = syncState()?.last_sync_at

    override suspend fun markSynced(nowMillis: Long) {
        val state = syncState()
        queries.upsertSyncState(cursor = state?.cursor, last_sync_at = nowMillis)
    }

    override suspend fun peek(id: String): LocalRecord? = queries.getVaultItem(id).executeAsOneOrNull()?.let { row ->
        LocalRecord(
            id = row.id,
            version = row.version,
            updatedAtMillis = row.updated_at,
            ciphertext = row.ciphertext,
            nonce = row.nonce,
            dirty = row.dirty,
            maxSeenVersion = row.max_seen_version,
        )
    }

    override suspend fun dirtyChanges(limit: Int): List<PendingChange> =
        queries.selectDirtyItems(limit.toLong()).executeAsList().map { row ->
            PendingChange(
                id = row.id,
                type = row.type.toItemType(),
                ciphertext = row.ciphertext,
                nonce = row.nonce,
                baseVersion = row.version,
                deleted = row.deleted,
            )
        }

    override suspend fun applyRemote(item: RemoteItem) {
        database.transaction {
            val previous = queries.getVaultItem(item.id).executeAsOneOrNull()
            if (previous?.ciphertext != null && previous.nonce != null) {
                // Server version is clean, so AAD version = version (not version + 1)
                queries.insertLosingVersion(
                    item_id = item.id,
                    version = previous.version,
                    item_type = previous.type,
                    aad_version = previous.version,
                    ciphertext = previous.ciphertext,
                    nonce = previous.nonce,
                    updated_at = previous.updated_at,
                )
                queries.deleteOldHistory(item.id, HISTORY_SIZE)
            }
            queries.upsertRemoteItem(
                id = item.id,
                type = item.type.name,
                ciphertext = item.ciphertext,
                nonce = item.nonce,
                version = item.version,
                deleted = item.deleted,
                max_seen_version = maxOf(previous?.max_seen_version ?: 0, item.version),
                updated_at = item.updatedAtMillis,
            )
        }
    }

    override suspend fun markItemApplied(id: String, newVersion: Long, updatedAtMillis: Long) {
        queries.markItemApplied(
            id = id,
            version = newVersion,
            max_seen_version = newVersion,
            updated_at = updatedAtMillis,
        )
    }

    override suspend fun stageOutbox(idempotencyKey: String, changes: List<PendingChange>) {
        val body = json.encodeToString(changeList, changes.map { it.toDto() })
        database.transaction {
            queries.deleteOutbox()
            queries.insertOutbox(idempotency_key = idempotencyKey, body = body)
        }
    }

    override suspend fun pendingOutbox(): Outbox? {
        val row = queries.selectOutbox().executeAsOneOrNull() ?: return null
        // A body we cannot read is dropped: the dirty rows are still local and get pushed again.
        val changes =
            runCatching { json.decodeFromString(changeList, row.body).map { it.toDomain() } }.getOrNull() ?: return null
        return Outbox(idempotencyKey = row.idempotency_key, changes = changes)
    }

    override suspend fun clearOutbox() {
        queries.deleteOutbox()
    }

    override suspend fun resetLocalData() {
        database.transaction {
            queries.deleteAllVaultItems()
            queries.deleteAllItemHistory()
            queries.deleteOutbox()
            queries.clearSyncCursor()
        }
    }

    private fun syncState() = queries.selectSyncState().executeAsOneOrNull()
}
