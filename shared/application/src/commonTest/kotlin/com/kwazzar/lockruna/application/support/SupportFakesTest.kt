package com.kwazzar.lockruna.application.support

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.Clock
import com.kwazzar.lockruna.domain.session.IdGenerator
import com.kwazzar.lockruna.domain.vault.ItemId
import com.kwazzar.lockruna.domain.vault.Secret
import com.kwazzar.lockruna.domain.vault.Title
import com.kwazzar.lockruna.domain.vault.VaultItem
import com.kwazzar.lockruna.domain.vault.VaultItemType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SupportFakesTest {

    @Test
    fun `InMemoryVaultItemRepository save and get`() = runTest {
        val repo = InMemoryVaultItemRepository()
        val id = (ItemId.of("00000000-0000-4000-8000-000000000001") as Result.Success).value
        val title = (Title.of("Test") as Result.Success).value
        val secret = (Secret.of("secret") as Result.Success).value
        val item = VaultItem(
            id = id,
            type = VaultItemType.LOGIN,
            title = title,
            username = "user",
            secret = secret,
            url = null,
            folderId = null,
            version = 0,
            dirty = true,
            deleted = false,
            updatedAtMillis = 1000,
        )

        val saveResult = repo.save(item)
        assertTrue(saveResult is Result.Success)

        val getResult = repo.get(id)
        assertTrue(getResult is Result.Success)
        val retrieved = (getResult as Result.Success).value
        assertNotNull(retrieved)
        assertEquals(item.id, retrieved!!.id)
        assertEquals(item.title, retrieved.title)
        assertEquals(item.secret, retrieved.secret)
    }

    @Test
    fun `InMemoryVaultItemRepository observeAll emits list`() = runTest {
        val repo = InMemoryVaultItemRepository()
        val id1 = (ItemId.of("00000000-0000-4000-8000-000000000001") as Result.Success).value
        val id2 = (ItemId.of("00000000-0000-4000-8000-000000000002") as Result.Success).value
        val title = (Title.of("Test") as Result.Success).value
        val secret = (Secret.of("secret") as Result.Success).value

        val item1 = VaultItem(id1, VaultItemType.LOGIN, title, "user", secret, null, null, 0, true, false, 1000)
        val item2 = VaultItem(id2, VaultItemType.NOTE, title, null, null, null, null, 0, true, false, 2000)

        repo.save(item1)
        repo.save(item2)

        val observed = repo.observeAll().first()
        assertEquals(2, observed.size)
    }

    @Test
    fun `FakeClock returns set values`() {
        val clock = FakeClock(wallMillis = 1_000_000, monotonicMillis = 500_000)
        assertEquals(1_000_000, clock.nowMillis())
        assertEquals(500_000, clock.monotonicMillis())
    }

    @Test
    fun `FakeIdGenerator produces different UUIDs`() {
        val gen = FakeIdGenerator()
        val uuid1 = gen.newUuid()
        val uuid2 = gen.newUuid()
        assertTrue(uuid1 != uuid2)
        assertTrue(uuid1.length == 36)
        assertTrue(uuid2.length == 36)
    }

    @Test
    fun `FakeIdGenerator newItemId returns valid ItemId`() {
        val gen = FakeIdGenerator()
        val itemId = gen.newItemId()
        assertNotNull(itemId)
        assertTrue(itemId.value.length == 36)
    }
}