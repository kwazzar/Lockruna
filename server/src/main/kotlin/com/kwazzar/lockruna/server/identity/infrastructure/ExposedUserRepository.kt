package com.kwazzar.lockruna.server.identity.infrastructure

import com.kwazzar.lockruna.server.identity.domain.UserRecord
import com.kwazzar.lockruna.server.identity.domain.UserRepository
import com.kwazzar.lockruna.server.platform.db.TransactionRunner
import com.kwazzar.lockruna.server.platform.db.Users
import com.kwazzar.lockruna.server.platform.db.toUuid
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

class ExposedUserRepository(private val transactions: TransactionRunner) : UserRepository {
    override suspend fun findByEmail(email: String): UserRecord? = transactions.transaction {
        Users.selectAll()
            .where { Users.email.lowerCase() eq email.lowercase() }
            .firstOrNull()
            ?.toRecord()
    }

    override suspend fun create(user: UserRecord): UserRecord = transactions.transaction {
        Users.insert {
            it[id] = user.id.toUuid()
            it[email] = user.email
            it[authHash] = user.authHash
            it[recoveryAuthHash] = user.recoveryAuthHash
            it[kdfSalt] = user.kdfSalt
            it[kdfParams] = user.kdfParams
            it[wrappedVaultKey] = user.wrappedVaultKey
            it[recoveryWrappedVaultKey] = user.recoveryWrappedVaultKey
            it[syncSeq] = user.syncSeq
        }
        user
    }

    override suspend fun findById(id: String): UserRecord? = transactions.transaction {
        Users.selectAll()
            .where { Users.id eq id.toUuid() }
            .firstOrNull()
            ?.toRecord()
    }

    override suspend fun updateKeyMaterial(
        id: String,
        authHash: ByteArray,
        recoveryAuthHash: ByteArray,
        kdfSalt: ByteArray,
        wrappedVaultKey: ByteArray,
    ): Boolean = transactions.transaction {
        Users.update({ Users.id eq id.toUuid() }) {
            it[Users.authHash] = authHash
            it[Users.recoveryAuthHash] = recoveryAuthHash
            it[Users.kdfSalt] = kdfSalt
            it[Users.wrappedVaultKey] = wrappedVaultKey
        } > 0
    }

    private fun ResultRow.toRecord() = UserRecord(
        id = this[Users.id].toString(),
        email = this[Users.email],
        authHash = this[Users.authHash],
        recoveryAuthHash = this[Users.recoveryAuthHash],
        kdfSalt = this[Users.kdfSalt],
        kdfParams = this[Users.kdfParams],
        wrappedVaultKey = this[Users.wrappedVaultKey],
        recoveryWrappedVaultKey = this[Users.recoveryWrappedVaultKey],
        syncSeq = this[Users.syncSeq],
    )
}
