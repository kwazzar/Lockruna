package com.kwazzar.lockruna.server.sync.application

import com.kwazzar.lockruna.server.platform.db.TransactionRunner
import com.kwazzar.lockruna.server.platform.error.Problem
import com.kwazzar.lockruna.server.platform.error.ProblemException
import com.kwazzar.lockruna.server.sync.domain.ApplyResult
import com.kwazzar.lockruna.server.sync.domain.ChangeRecord
import com.kwazzar.lockruna.server.sync.domain.IdempotencyRepository
import com.kwazzar.lockruna.server.sync.domain.SealedItemRecord
import com.kwazzar.lockruna.server.sync.domain.StoredResponse
import com.kwazzar.lockruna.server.sync.domain.VaultItemRepository
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant

/**
 * Applies the changes of one push in a single transaction and stores the response for the idempotency key.
 * `render` produces the response body inside the transaction, so a replay answers with the very same bytes.
 */
class PushChanges(
    private val vaultItems: VaultItemRepository,
    private val idempotency: IdempotencyRepository,
    private val transactions: TransactionRunner,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend operator fun invoke(
        userId: String,
        key: String,
        bodyHash: ByteArray,
        changes: List<ChangeRecord>,
        render: (PushOutcome) -> String,
    ): String {
        checkLimits(changes)

        return transactions.transaction {
            val stored = idempotency.find(userId, key)
            if (stored != null) return@transaction replay(stored, bodyHash)

            val outcome = applyChanges(userId, changes)
            val body = render(outcome)
            val saved = idempotency.store(userId, key, bodyHash, body, clock.instant())
            if (!MessageDigest.isEqual(saved.bodyHash, bodyHash)) throw keyReuse()
            saved.response
        }
    }

    private fun checkLimits(changes: List<ChangeRecord>) {
        if (changes.size > MAX_CHANGES) throw tooLarge()
        changes.forEach { change ->
            if ((change.ciphertext?.size ?: 0) > MAX_CIPHERTEXT_BYTES) throw tooLarge()
        }
    }

    private fun replay(stored: StoredResponse, bodyHash: ByteArray): String {
        if (!MessageDigest.isEqual(stored.bodyHash, bodyHash)) throw keyReuse()
        return stored.response
    }

    private suspend fun applyChanges(userId: String, changes: List<ChangeRecord>): PushOutcome {
        val applied = mutableListOf<AppliedChange>()
        val conflicts = mutableListOf<Conflict>()
        val now = clock.instant()
        for (change in changes) {
            val syncSeq = vaultItems.nextSyncSeq(userId)
            when (val result = vaultItems.apply(userId, change, syncSeq, now)) {
                is ApplyResult.Applied -> applied += AppliedChange(change.id, result.version, result.updatedAt)
                is ApplyResult.Conflict -> conflicts += Conflict(change.id, result.server)
            }
        }
        return PushOutcome(applied = applied, conflicts = conflicts)
    }

    private fun keyReuse() = ProblemException(
        problem = Problem(status = 409, code = "idempotency_key_reuse", title = "Idempotency key reuse"),
    )

    private fun tooLarge() = ProblemException(
        problem = Problem(status = 413, code = "payload_too_large", title = "Payload too large"),
    )

    private companion object {
        const val MAX_CHANGES = 100
        const val MAX_CIPHERTEXT_BYTES = 64 * 1024
    }
}

data class PushOutcome(val applied: List<AppliedChange>, val conflicts: List<Conflict>)

data class AppliedChange(val id: String, val version: Long, val updatedAt: Instant)

data class Conflict(val id: String, val server: SealedItemRecord)
