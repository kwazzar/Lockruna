package com.kwazzar.lockruna.server.support

import kotlin.test.Test
import kotlin.test.assertTrue

class MigrationTest {
    @Test
    fun `creates every baseline table`() {
        PostgresTest.migrate()

        val tables = PostgresTest.tableNames()

        listOf("users", "devices", "refresh_tokens", "vault_items", "idempotency_keys").forEach { table ->
            assertTrue(table in tables, "missing table $table")
        }
    }

    @Test
    fun `creates the baseline indexes`() {
        PostgresTest.migrate()

        val indexes = PostgresTest.indexNames()

        listOf("uq_users_email", "idx_refresh_family", "idx_items_sync").forEach { index ->
            assertTrue(index in indexes, "missing index $index")
        }
    }
}
