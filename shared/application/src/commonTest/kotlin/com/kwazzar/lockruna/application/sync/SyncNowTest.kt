package com.kwazzar.lockruna.application.sync

import com.kwazzar.lockruna.application.support.FakeAccessTokenProvider
import com.kwazzar.lockruna.application.support.FakeClock
import com.kwazzar.lockruna.application.support.FakeIdGenerator
import com.kwazzar.lockruna.application.support.FakeSyncApi
import com.kwazzar.lockruna.application.support.FakeSyncStore
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.sync.AppliedChange
import com.kwazzar.lockruna.domain.sync.Conflict
import com.kwazzar.lockruna.domain.sync.LocalRecord
import com.kwazzar.lockruna.domain.sync.PendingChange
import com.kwazzar.lockruna.domain.sync.PushResult
import com.kwazzar.lockruna.domain.sync.RemoteItem
import com.kwazzar.lockruna.domain.sync.SyncPage
import com.kwazzar.lockruna.domain.vault.VaultItemType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val NOW = 1_700_000_000_000L
private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

class SyncNowTest {

    private val api = FakeSyncApi()
    private val store = FakeSyncStore()
    private val clock = FakeClock(wallMillis = NOW)
    private val tokens = FakeAccessTokenProvider()
    private val sync = SyncNow(api, store, tokens, FakeIdGenerator(), clock)

    private fun change(id: String, baseVersion: Long = 0) = PendingChange(
        id = id,
        type = VaultItemType.LOGIN,
        ciphertext = byteArrayOf(1, 2, 3),
        nonce = byteArrayOf(4),
        baseVersion = baseVersion,
    )

    private fun remote(id: String, version: Long, updatedAt: Long = NOW) = RemoteItem(
        id = id,
        type = VaultItemType.LOGIN,
        ciphertext = byteArrayOf(5, 6),
        nonce = byteArrayOf(7),
        version = version,
        deleted = false,
        updatedAtMillis = updatedAt,
    )

    @Test
    fun `pulls remote items and pushes local changes`() = runTest {
        api.pullPages += Result.Success(
            SyncPage(items = listOf(remote("remote-1", version = 2)), nextCursor = "cursor-2", hasMore = false),
        )
        store.stageDirty(change("local-1"), updatedAtMillis = NOW - 1_000)
        api.pushResults += Result.Success(
            PushResult(
                applied = listOf(AppliedChange("local-1", newVersion = 1, updatedAtMillis = NOW)),
                conflicts = emptyList(),
            ),
        )

        val result = sync()

        assertTrue(result is Result.Success)
        assertEquals("cursor-2", store.cursor())
        assertEquals(NOW, store.lastSyncAtMillis())
        assertEquals(2L, store.peek("remote-1")?.version)
        assertEquals(1L, store.peek("local-1")?.version)
        assertFalse(store.peek("local-1")?.dirty ?: true)
        assertEquals(1, api.pushCalls.size)
        assertEquals(listOf("local-1"), api.pushCalls.single().changes.map { it.id })
    }

    @Test
    fun `skips a remote item older than the version already seen`() = runTest {
        store.records["remote-1"] = LocalRecord(
            id = "remote-1",
            version = 5,
            updatedAtMillis = NOW,
            ciphertext = byteArrayOf(1),
            nonce = byteArrayOf(2),
            dirty = false,
            maxSeenVersion = 5,
        )
        api.pullPages += Result.Success(
            SyncPage(items = listOf(remote("remote-1", version = 3)), nextCursor = "cursor-2", hasMore = false),
        )

        sync()

        assertEquals(5L, store.peek("remote-1")?.version)
        assertEquals(5L, store.peek("remote-1")?.maxSeenVersion)
    }

    @Test
    fun `keeps a dirty local row when the pull brings a newer remote version`() = runTest {
        store.stageDirty(change("item-1", baseVersion = 1), updatedAtMillis = NOW)
        api.pullPages += Result.Success(
            SyncPage(
                items = listOf(remote("item-1", version = 4, updatedAt = NOW + 1)),
                nextCursor = "c",
                hasMore = false,
            ),
        )

        sync()

        assertEquals(1L, store.peek("item-1")?.version)
        assertTrue(store.peek("item-1")?.dirty ?: false)
    }

    @Test
    fun `applies the server record when the server is newer`() = runTest {
        store.stageDirty(change("item-1", baseVersion = 1), updatedAtMillis = NOW - 10_000)
        api.pushResults += Result.Success(
            PushResult(
                applied = emptyList(),
                conflicts = listOf(Conflict("item-1", remote("item-1", version = 4, updatedAt = NOW))),
            ),
        )

        sync()

        assertEquals(4L, store.peek("item-1")?.version)
        assertFalse(store.peek("item-1")?.dirty ?: true)
        assertEquals(1, api.pushCalls.size)
    }

    @Test
    fun `re-pushes with the server version when the local change is newer`() = runTest {
        store.stageDirty(change("item-1", baseVersion = 1), updatedAtMillis = NOW)
        api.pushResults += Result.Success(
            PushResult(
                applied = emptyList(),
                conflicts = listOf(Conflict("item-1", remote("item-1", version = 4, updatedAt = NOW - 10_000))),
            ),
        )
        api.pushResults += Result.Success(
            PushResult(
                applied = listOf(AppliedChange("item-1", newVersion = 5, updatedAtMillis = NOW)),
                conflicts = emptyList(),
            ),
        )

        sync()

        assertEquals(2, api.pushCalls.size)
        assertEquals(4L, api.pushCalls[1].changes.single().baseVersion)
        assertTrue(api.pushCalls[1].idempotencyKey != api.pushCalls[0].idempotencyKey)
        assertEquals(5L, store.peek("item-1")?.version)
        assertFalse(store.peek("item-1")?.dirty ?: true)
    }

    @Test
    fun `keeps the staged batch when the network fails and retries it with the same key`() = runTest {
        store.stageDirty(change("item-1"), updatedAtMillis = NOW)
        api.pushResults += Result.Failure(AppError.NetworkUnavailable)

        val first = sync()

        assertEquals(Result.Failure(AppError.NetworkUnavailable), first)
        assertEquals("item-1", store.pendingOutbox()?.changes?.single()?.id)

        api.pushResults += Result.Success(
            PushResult(
                applied = listOf(AppliedChange("item-1", newVersion = 1, updatedAtMillis = NOW)),
                conflicts = emptyList(),
            ),
        )
        val second = sync()

        assertTrue(second is Result.Success)
        assertEquals(2, api.pushCalls.size)
        assertEquals(api.pushCalls[0].idempotencyKey, api.pushCalls[1].idempotencyKey)
        assertEquals(api.pushCalls[0].changes, api.pushCalls[1].changes)
    }

    @Test
    fun `pushes dirty changes in batches of at most one hundred`() = runTest {
        repeat(150) { index -> store.stageDirty(change("item-$index"), updatedAtMillis = NOW) }
        api.pushResults += Result.Success(
            PushResult(
                applied = (0 until 100).map { AppliedChange("item-$it", newVersion = 1, updatedAtMillis = NOW) },
                conflicts = emptyList(),
            ),
        )
        api.pushResults += Result.Success(
            PushResult(
                applied = (100 until 150).map { AppliedChange("item-$it", newVersion = 1, updatedAtMillis = NOW) },
                conflicts = emptyList(),
            ),
        )

        sync()

        assertEquals(2, api.pushCalls.size)
        assertEquals(100, api.pushCalls[0].changes.size)
        assertEquals(50, api.pushCalls[1].changes.size)
    }

    @Test
    fun `resets local data when the last sync is older than ninety days`() = runTest {
        store.markSynced(NOW - 91 * DAY_MILLIS)
        store.stageDirty(change("item-1"), updatedAtMillis = NOW)

        sync()

        assertEquals(1, store.resetCount)
        assertNull(api.pullCursors.first())
        assertEquals(0, api.pushCalls.size)
    }

    @Test
    fun `fails without a request when there is no access token`() = runTest {
        tokens.tokenValue = null

        val result = sync()

        assertEquals(Result.Failure(AppError.Unauthorized), result)
        assertEquals(0, api.pullCursors.size)
        assertEquals(0, api.pushCalls.size)
    }

    @Test
    fun `does not push the same change twice`() = runTest {
        store.stageDirty(change("item-1"), updatedAtMillis = NOW)
        api.pushResults += Result.Success(
            PushResult(
                applied = listOf(AppliedChange("item-1", newVersion = 1, updatedAtMillis = NOW)),
                conflicts = emptyList(),
            ),
        )

        sync()
        val pushesAfterFirstSync = api.pushCalls.size
        sync()

        assertEquals(pushesAfterFirstSync, api.pushCalls.size)
    }
}
