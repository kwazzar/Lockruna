package com.kwazzar.lockruna.domain.sync

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError

/** Sync transport: pull pages by cursor and push batches of local changes. */
interface SyncApi {
    /** Fetches the page following [cursor], or the first page when it is null. */
    suspend fun pull(cursor: String?): Result<SyncPage, AppError>

    /**
     * Pushes a batch of changes.
     *
     * [idempotencyKey] identifies the batch: a retry of the same batch must reuse the same key,
     * a changed batch must use a new one.
     */
    suspend fun push(changes: List<PendingChange>, idempotencyKey: String): Result<PushResult, AppError>
}

/** Provides the access token; null means the session is not authenticated. */
interface AccessTokenProvider {
    suspend fun token(): String?
}
