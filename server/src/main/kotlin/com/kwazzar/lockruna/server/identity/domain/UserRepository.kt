package com.kwazzar.lockruna.server.identity.domain

/**
 * The server never sees the auth key itself, only `HMAC(pepper, authKey)`.
 */
data class UserRecord(
    val id: String,
    val email: String,
    val authHash: ByteArray,
    val recoveryAuthHash: ByteArray,
    val kdfSalt: ByteArray,
    val kdfParams: String,
    val wrappedVaultKey: ByteArray,
    val recoveryWrappedVaultKey: ByteArray,
    val syncSeq: Long,
)

interface UserRepository {
    suspend fun findByEmail(email: String): UserRecord?

    suspend fun create(user: UserRecord): UserRecord

    suspend fun findById(id: String): UserRecord?

    suspend fun updateKeyMaterial(
        id: String,
        authHash: ByteArray,
        recoveryAuthHash: ByteArray,
        kdfSalt: ByteArray,
        wrappedVaultKey: ByteArray,
    ): Boolean
}
