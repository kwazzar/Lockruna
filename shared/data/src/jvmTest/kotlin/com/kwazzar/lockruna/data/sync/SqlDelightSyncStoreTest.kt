package com.kwazzar.lockruna.data.sync

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kwazzar.lockruna.data.db.PassKeeperDatabase
import com.kwazzar.lockruna.domain.sync.PendingChange
import com.kwazzar.lockruna.domain.sync.RemoteItem
import com.kwazzar.lockruna.domain.vault.VaultItemType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SqlDelightSyncStoreTest {

    private fun createStore(): Pair<SqlDelightSyncStore, PassKeeperDatabase> {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        PassKeeperDatabase.Schema.create(driver)
        val database = PassKeeperDatabase(driver)
        return SqlDelightSyncStore(database) to database
    }

    private fun PassKeeperDatabase.seedRow(
        id: String,
        version: Long = 0,
        dirty: Boolean = false,
        deleted: Boolean = false,
        maxSeenVersion: Long = version,
        ciphertext: ByteArray? = byteArrayOf(9),
        nonce: ByteArray? = byteArrayOf(8),
        updatedAt: Long = 100L,
    ) {
        passKeeperDatabaseQueries.upsertVaultItem(
            id = id,
            type = VaultItemType.LOGIN.name,
            ciphertext = ciphertext,
            nonce = nonce,
            version = version,
            sync_seq = 0,
            deleted = deleted,
            dirty = dirty,
            max_seen_version = maxSeenVersion,
            updated_at = updatedAt,
        )
    }

    private fun change(id: String = "item-1", baseVersion: Long = 2, deleted: Boolean = false) = PendingChange(
        id = id,
        type = VaultItemType.LOGIN,
        ciphertext = byteArrayOf(1, 2, 3),
        nonce = byteArrayOf(4),
        baseVersion = baseVersion,
        deleted = deleted,
    )

    @Test
    fun `outbox roundtrips through the database`() = runTest {
        val (store, _) = createStore()
        val changes = listOf(change(), change(id = "item-2", baseVersion = 5, deleted = true))

        store.stageOutbox("key-1", changes)

        val pending = store.pendingOutbox()
        assertEquals("key-1", pending?.idempotencyKey)
        assertEquals(changes, pending?.changes)

        store.clearOutbox()
        assertNull(store.pendingOutbox())
    }

    @Test
    fun `apply remote overwrites the row and keeps the losing version`() = runTest {
        val (store, database) = createStore()
        database.seedRow(id = "item-1", version = 1, dirty = true, maxSeenVersion = 1)

        store.applyRemote(
            RemoteItem(
                id = "item-1",
                type = VaultItemType.LOGIN,
                ciphertext = byteArrayOf(5, 6),
                nonce = byteArrayOf(7),
                version = 5,
                deleted = false,
                updatedAtMillis = 500L,
            ),
        )

        val record = store.peek("item-1")
        assertEquals(5L, record?.version)
        assertEquals(500L, record?.updatedAtMillis)
        assertEquals(false, record?.dirty)
        assertEquals(5L, record?.maxSeenVersion)
        assertTrue(record?.ciphertext?.contentEquals(byteArrayOf(5, 6)) == true)

        val history = database.passKeeperDatabaseQueries.getItemHistory("item-1").executeAsList()
        assertEquals(1, history.size)
        assertEquals(1L, history.single().version)
        assertTrue(history.single().ciphertext.contentEquals(byteArrayOf(9)))
    }

    @Test
    fun `apply remote keeps the larger max seen version`() = runTest {
        val (store, database) = createStore()
        database.seedRow(id = "item-1", version = 1, maxSeenVersion = 7)

        store.applyRemote(
            RemoteItem(
                id = "item-1",
                type = VaultItemType.LOGIN,
                ciphertext = byteArrayOf(5),
                nonce = byteArrayOf(6),
                version = 3,
                deleted = false,
                updatedAtMillis = 500L,
            ),
        )

        assertEquals(7L, store.peek("item-1")?.maxSeenVersion)
    }

    @Test
    fun `dirty changes return the dirty rows with their local version`() = runTest {
        val (store, database) = createStore()
        database.seedRow(id = "item-1", version = 3, dirty = true)
        database.seedRow(id = "item-2", version = 4, dirty = false)

        val changes = store.dirtyChanges(10)

        assertEquals(1, changes.size)
        assertEquals("item-1", changes.single().id)
        assertEquals(3L, changes.single().baseVersion)
    }

    @Test
    fun `mark item applied stores the server version and timestamp`() = runTest {
        val (store, database) = createStore()
        database.seedRow(id = "item-1", version = 3, dirty = true, maxSeenVersion = 3)

        store.markItemApplied("item-1", newVersion = 4, updatedAtMillis = 900L)

        val record = store.peek("item-1")
        assertEquals(4L, record?.version)
        assertEquals(900L, record?.updatedAtMillis)
        assertEquals(false, record?.dirty)
        assertEquals(4L, record?.maxSeenVersion)
    }

    @Test
    fun `sync state keeps cursor and last sync time`() = runTest {
        val (store, _) = createStore()

        store.setCursor("cursor-1")
        store.markSynced(1_000L)

        assertEquals("cursor-1", store.cursor())
        assertEquals(1_000L, store.lastSyncAtMillis())

        store.setCursor("cursor-2")
        assertEquals("cursor-2", store.cursor())
        assertEquals(1_000L, store.lastSyncAtMillis())
    }

    @Test
    fun `reset local data clears items, history and outbox`() = runTest {
        val (store, database) = createStore()
        database.seedRow(id = "item-1", version = 1)
        database.passKeeperDatabaseQueries.insertItemHistory(
            item_id = "item-1",
            version = 0,
            item_type = "login",
            aad_version = 0,
            ciphertext = byteArrayOf(1),
            nonce = byteArrayOf(2),
            updated_at = 50L,
        )
        store.stageOutbox("key-1", listOf(change()))
        store.setCursor("cursor-1")

        store.resetLocalData()

        assertNull(store.peek("item-1"))
        assertNull(store.pendingOutbox())
        assertNull(store.cursor())
        assertEquals(emptyList(), store.dirtyChanges(10))
        assertEquals(0, database.passKeeperDatabaseQueries.getItemHistory("item-1").executeAsList().size)
    }
}
