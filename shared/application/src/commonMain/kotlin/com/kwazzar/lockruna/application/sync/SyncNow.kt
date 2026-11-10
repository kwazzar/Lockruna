package com.kwazzar.lockruna.application.sync

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.Clock
import com.kwazzar.lockruna.domain.session.IdGenerator
import com.kwazzar.lockruna.domain.sync.AccessTokenProvider
import com.kwazzar.lockruna.domain.sync.Conflict
import com.kwazzar.lockruna.domain.sync.PendingChange
import com.kwazzar.lockruna.domain.sync.RemoteItem
import com.kwazzar.lockruna.domain.sync.SyncApi
import com.kwazzar.lockruna.domain.sync.SyncStore

private const val BATCH_LIMIT = 100
private const val STALE_AFTER_MILLIS = 90L * 24 * 60 * 60 * 1000L
private const val MAX_REPUSHES = 3

/** Sync engine: pulls remote changes, then pushes local ones (ADR-005 §7). */
class SyncNow(
    private val syncApi: SyncApi,
    private val store: SyncStore,
    private val accessTokenProvider: AccessTokenProvider,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
) {
    suspend operator fun invoke(): Result<Unit, AppError> {
        if (accessTokenProvider.token() == null) return Result.Failure(AppError.Unauthorized)

        if (isStale()) store.resetLocalData()

        val pulled = pullAll()
        if (pulled is Result.Failure) return Result.Failure(pulled.error)

        store.markSynced(clock.nowMillis())
        return pushAll()
    }

    private suspend fun isStale(): Boolean {
        val lastSync = store.lastSyncAtMillis() ?: return false
        return clock.nowMillis() - lastSync > STALE_AFTER_MILLIS
    }

    private suspend fun pullAll(): Result<Unit, AppError> {
        var cursor = store.cursor()
        while (true) {
            val page = when (val pulled = syncApi.pull(cursor)) {
                is Result.Failure -> return Result.Failure(pulled.error)
                is Result.Success -> pulled.value
            }
            for (item in page.items) {
                if (shouldApply(item)) store.applyRemote(item)
            }
            store.setCursor(page.nextCursor)
            if (!page.hasMore) return Result.Success(Unit)
            cursor = page.nextCursor
        }
    }

    private suspend fun shouldApply(item: RemoteItem): Boolean {
        val local = store.peek(item.id) ?: return true
        // A version below the one already seen is a rollback attempt.
        if (item.version < local.maxSeenVersion) return false
        // A dirty row keeps the local edit; the push resolves it with last-write-wins.
        return !local.dirty
    }

    private suspend fun pushAll(): Result<Unit, AppError> {
        val repushes = mutableMapOf<String, Int>()
        val pushed = mutableSetOf<String>()
        while (true) {
            val batch = nextBatch(pushed) ?: return Result.Success(Unit)
            pushed += batch.changes.map { it.id }

            val result = when (val response = syncApi.push(batch.changes, batch.key)) {
                is Result.Failure -> return Result.Failure(response.error)
                is Result.Success -> response.value
            }
            result.applied.forEach { store.markItemApplied(it.id, it.newVersion, it.updatedAtMillis) }

            val outcome = resolveConflicts(result.conflicts, batch.changes, repushes)
            store.clearOutbox()
            if (outcome.exhausted) return Result.Success(Unit)
            if (outcome.retry.isNotEmpty()) store.stageOutbox(idGenerator.newUuid(), outcome.retry)
        }
    }

    /** The staged batch is retried as is; a new batch is staged before it is pushed. */
    private suspend fun nextBatch(pushed: Set<String>): Batch? {
        val staged = store.pendingOutbox()
        if (staged != null) return Batch(staged.idempotencyKey, staged.changes)

        val dirty = store.dirtyChanges(BATCH_LIMIT).filterNot { it.id in pushed }
        if (dirty.isEmpty()) return null
        val key = idGenerator.newUuid()
        store.stageOutbox(key, dirty)
        return Batch(key, dirty)
    }

    /** Applies the server record when it wins and collects the changes that must be pushed again. */
    private suspend fun resolveConflicts(
        conflicts: List<Conflict>,
        batch: List<PendingChange>,
        repushes: MutableMap<String, Int>,
    ): ConflictOutcome {
        val retry = mutableListOf<PendingChange>()
        var exhausted = false
        for (conflict in conflicts) {
            val localUpdatedAt = store.peek(conflict.id)?.updatedAtMillis ?: 0L
            val serverWins = conflict.server.updatedAtMillis >= localUpdatedAt
            if (serverWins) {
                store.applyRemote(conflict.server)
            } else {
                val attempts = (repushes[conflict.id] ?: 0) + 1
                repushes[conflict.id] = attempts
                if (attempts > MAX_REPUSHES) {
                    exhausted = true
                } else {
                    val change = batch.firstOrNull { it.id == conflict.id }
                    if (change != null) retry += change.copy(baseVersion = conflict.server.version)
                }
            }
        }
        return ConflictOutcome(retry = retry, exhausted = exhausted)
    }

    private data class Batch(val key: String, val changes: List<PendingChange>)

    private data class ConflictOutcome(val retry: List<PendingChange>, val exhausted: Boolean)
}
