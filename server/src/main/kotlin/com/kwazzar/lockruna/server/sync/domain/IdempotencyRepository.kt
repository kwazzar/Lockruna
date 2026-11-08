package com.kwazzar.lockruna.server.sync.domain

import java.time.Instant

data class StoredResponse(val bodyHash: ByteArray, val response: String, val createdAt: Instant)

interface IdempotencyRepository {
    suspend fun find(userId: String, key: String): StoredResponse?

    suspend fun store(
        userId: String,
        key: String,
        bodyHash: ByteArray,
        response: String,
        createdAt: Instant,
    ): StoredResponse
}
