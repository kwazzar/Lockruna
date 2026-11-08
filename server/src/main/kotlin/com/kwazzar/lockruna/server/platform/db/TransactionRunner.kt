package com.kwazzar.lockruna.server.platform.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

/**
 * Transaction boundary for the application layer: one unit of work, one database transaction.
 */
interface TransactionRunner {
    suspend fun <T> transaction(block: suspend () -> T): T
}

class ExposedTransactionRunner(private val database: Database) : TransactionRunner {
    override suspend fun <T> transaction(block: suspend () -> T): T = withContext(Dispatchers.IO) {
        suspendTransaction(database) { block() }
    }
}
