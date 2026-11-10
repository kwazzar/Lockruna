package com.kwazzar.lockruna.application.sync

import com.kwazzar.lockruna.application.support.FakeSyncApi
import com.kwazzar.lockruna.application.support.FakeSyncStore
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.sync.Outbox
import com.kwazzar.lockruna.domain.sync.PendingChange
import com.kwazzar.lockruna.domain.sync.PushResult
import com.kwazzar.lockruna.domain.sync.RemoteItem
import com.kwazzar.lockruna.domain.sync.SyncPage
import com.kwazzar.lockruna.domain.vault.VaultItemType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncPortsTest {

    @Test
    fun `fake sync api returns queued pages and results`() = runTest {
        val api = FakeSyncApi()
        val page = SyncPage(
            items = listOf(remoteItem("item-1", version = 3)),
            nextCursor = "cursor-1",
            hasMore = false,
        )
        api.pullPages += Result.Success(page)
        api.pushResults += Result.Success(PushResult(applied = emptyList(), conflicts = emptyList()))

        val pulled = api.pull("cursor-0")
        assertEquals(page, (pulled as Result.Success).value)
        assertEquals(listOf<String?>("cursor-0"), api.pullCursors)

        val pushed = api.push(listOf(pendingChange("item-1")), "key-1")
        assertTrue(pushed is Result.Success)
        assertEquals("key-1", api.pushCalls.single().idempotencyKey)
        assertEquals(listOf(pendingChange("item-1")), api.pushCalls.single().changes)
    }

    @Test
    fun `fake sync store roundtrips the outbox`() = runTest {
        val store = FakeSyncStore()
        val change = pendingChange("item-1")

        store.stageOutbox("key-1", listOf(change))
        assertEquals(Outbox("key-1", listOf(change)), store.pendingOutbox())

        store.clearOutbox()
        assertNull(store.pendingOutbox())
    }

    @Test
    fun `dirty changes returns the staged values`() = runTest {
        val store = FakeSyncStore()
        val change = pendingChange("item-1", baseVersion = 2)

        store.stageDirty(change)

        assertEquals(listOf(change), store.dirtyChanges(10))
        assertEquals(emptyList(), store.dirtyChanges(0))
    }

    @Test
    fun `sync types keep ciphertext out of toString`() {
        assertEquals(
            "PendingChange(id=item-1, baseVersion=0)",
            pendingChange("item-1", ciphertext = byteArrayOf(1, 2, 3)).toString(),
        )
        assertEquals(
            "RemoteItem(id=item-1, version=4, deleted=false)",
            remoteItem("item-1", version = 4, ciphertext = byteArrayOf(4, 5, 6)).toString(),
        )
    }

    @Test
    fun `app error covers idempotency conflict and registered email`() {
        val errors: List<AppError> = listOf(AppError.IdempotencyConflict, AppError.EmailAlreadyRegistered)

        assertEquals(2, errors.distinct().size)
    }

    private fun pendingChange(id: String, baseVersion: Long = 0, ciphertext: ByteArray? = byteArrayOf(7)) =
        PendingChange(
            id = id,
            type = VaultItemType.LOGIN,
            ciphertext = ciphertext,
            nonce = byteArrayOf(1),
            baseVersion = baseVersion,
        )

    private fun remoteItem(id: String, version: Long, ciphertext: ByteArray? = byteArrayOf(8)) = RemoteItem(
        id = id,
        type = VaultItemType.LOGIN,
        ciphertext = ciphertext,
        nonce = byteArrayOf(2),
        version = version,
        deleted = false,
        updatedAtMillis = 1_000L,
    )
}
