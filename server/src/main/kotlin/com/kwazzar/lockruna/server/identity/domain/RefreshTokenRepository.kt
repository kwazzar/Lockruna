package com.kwazzar.lockruna.server.identity.domain

import java.time.Instant

/**
 * A refresh token is stored as its SHA-256 hash only; the token itself never reaches the database.
 */
data class RefreshTokenRecord(
    val id: String,
    val deviceId: String,
    val userId: String,
    val familyId: String,
    val tokenHash: ByteArray,
    val expiresAt: Instant,
    val familyCreatedAt: Instant,
    val usedAt: Instant?,
    val revokedAt: Instant?,
)

interface RefreshTokenRepository {
    suspend fun insert(token: RefreshTokenRecord): RefreshTokenRecord

    suspend fun findByHash(hash: ByteArray): RefreshTokenRecord?

    suspend fun markUsed(userId: String, id: String, usedAt: Instant): Boolean

    suspend fun revokeFamily(userId: String, familyId: String, revokedAt: Instant): Int
}
