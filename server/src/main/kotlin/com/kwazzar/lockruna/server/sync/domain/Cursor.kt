package com.kwazzar.lockruna.server.sync.domain

import java.util.Base64

/**
 * The opaque cursor of the sync feed: the `sync_seq` of the last delivered row, encoded.
 */
object Cursor {
    fun encode(syncSeq: Long): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(syncSeq.toString().toByteArray())

    fun decode(cursor: String?): Long {
        if (cursor == null) return 0
        val syncSeq = Base64.getUrlDecoder().decode(cursor).decodeToString().toLongOrNull()
            ?: throw IllegalArgumentException("malformed cursor")
        if (syncSeq < 0) throw IllegalArgumentException("malformed cursor")
        return syncSeq
    }
}
