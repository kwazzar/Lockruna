package com.kwazzar.lockruna.server.sync.application

import com.kwazzar.lockruna.server.platform.error.Problem
import com.kwazzar.lockruna.server.platform.error.ProblemException
import com.kwazzar.lockruna.server.sync.domain.Cursor
import com.kwazzar.lockruna.server.sync.domain.SealedItemRecord
import com.kwazzar.lockruna.server.sync.domain.VaultItemRepository

/**
 * A page of the vault of the user: the rows after the cursor and the cursor of the next page.
 */
class PullChanges(private val vaultItems: VaultItemRepository) {
    suspend operator fun invoke(userId: String, cursor: String?, limit: Int): PulledPage {
        val after = try {
            Cursor.decode(cursor)
        } catch (invalid: IllegalArgumentException) {
            throw ProblemException(
                problem = Problem(status = 400, code = "invalid_request", title = "Invalid request"),
                cause = invalid,
            )
        }
        val rows = vaultItems.listSince(userId, after, limit + 1)
        val page = rows.take(limit)
        return PulledPage(
            items = page,
            nextCursor = page.lastOrNull()?.syncSeq ?: after,
            hasMore = rows.size > limit,
        )
    }
}

data class PulledPage(val items: List<SealedItemRecord>, val nextCursor: Long, val hasMore: Boolean)
