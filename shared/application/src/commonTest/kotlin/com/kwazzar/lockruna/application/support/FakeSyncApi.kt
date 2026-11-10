package com.kwazzar.lockruna.application.support

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.sync.PendingChange
import com.kwazzar.lockruna.domain.sync.PushResult
import com.kwazzar.lockruna.domain.sync.SyncApi
import com.kwazzar.lockruna.domain.sync.SyncPage

/** A push request as it reached the server. */
data class PushCall(val idempotencyKey: String, val changes: List<PendingChange>)

/** Scriptable sync transport: queued answers, recorded calls. */
class FakeSyncApi : SyncApi {
    val pullCursors = mutableListOf<String?>()
    val pushCalls = mutableListOf<PushCall>()
    val pullPages = mutableListOf<Result<SyncPage, AppError>>()
    val pushResults = mutableListOf<Result<PushResult, AppError>>()

    override suspend fun pull(cursor: String?): Result<SyncPage, AppError> {
        pullCursors += cursor
        return pullPages.removeFirstOrNull() ?: Result.Success(SyncPage(emptyList(), cursor.orEmpty(), false))
    }

    override suspend fun push(changes: List<PendingChange>, idempotencyKey: String): Result<PushResult, AppError> {
        pushCalls += PushCall(idempotencyKey, changes)
        return pushResults.removeFirstOrNull() ?: Result.Success(PushResult(emptyList(), emptyList()))
    }
}
