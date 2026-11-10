package com.kwazzar.lockruna.application.vault

import com.kwazzar.lockruna.application.support.FakeClock
import com.kwazzar.lockruna.application.support.FakeCryptoService
import com.kwazzar.lockruna.application.support.FakeIdGenerator
import com.kwazzar.lockruna.application.support.InMemoryVaultItemRepository
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.VaultKey
import com.kwazzar.lockruna.domain.session.VaultSession
import com.kwazzar.lockruna.domain.vault.ItemId
import com.kwazzar.lockruna.domain.vault.VaultItem
import com.kwazzar.lockruna.domain.vault.VaultItemRepository
import com.kwazzar.lockruna.domain.vault.VaultItemType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class VaultCycleTest {

    private val session = VaultSession().apply {
        unlock(VaultKey.random(FakeCryptoService()))
    }
    private val clock = FakeClock(wallMillis = 1_000_000, monotonicMillis = 1_000_000)
    private val repository = LockedAwareInMemoryRepository(InMemoryVaultItemRepository(), session)

    private val createItemUseCase = CreateItem(repository, FakeIdGenerator(), clock)
    private val updateItemUseCase = UpdateItem(repository, clock)
    private val deleteItemUseCase = DeleteItem(repository, clock)

    @Test
    fun `create update and delete move through versions`() = runTest {
        val created = createItemUseCase(
            CreateItemCommand(
                type = VaultItemType.LOGIN,
                title = "Service",
                username = "user",
                secret = "secret-value",
                url = "https://example.com",
                folderId = null,
            ),
        )
        val createdItem = (created as Result.Success).value
        assertEquals(0L, createdItem.version)
        val itemId = createdItem.id

        val updated = updateItemUseCase(
            itemId,
            "Renamed",
            "user",
            "secret-value-2",
            "https://example.com",
            null,
        )
        val updatedItem = (updated as Result.Success).value
        assertEquals(1L, updatedItem.version)
        assertEquals(true, updatedItem.dirty)

        val deleted = deleteItemUseCase(itemId)
        val deletedItem = (deleted as Result.Success).value
        assertEquals(2L, deletedItem.version)
        assertEquals(true, deletedItem.deleted)
    }

    @Test
    fun `save is refused while the session is locked`() = runTest {
        val saved = createItemUseCase(
            CreateItemCommand(
                type = VaultItemType.NOTE,
                title = "Note",
                username = null,
                secret = null,
                url = null,
                folderId = null,
            ),
        )
        val item = (saved as Result.Success).value

        session.lock()
        val refused = repository.save(item)

        assertEquals(true, (refused as Result.Failure).error is AppError.VaultLocked)
    }

    private class LockedAwareInMemoryRepository(
        private val delegate: InMemoryVaultItemRepository,
        private val session: VaultSession,
    ) : VaultItemRepository {
        override suspend fun save(item: VaultItem): Result<VaultItem, AppError> {
            if (session.currentVaultKey() == null) {
                return Result.Failure(AppError.VaultLocked)
            }
            return delegate.save(item)
        }

        override suspend fun get(id: ItemId): Result<VaultItem?, AppError> = delegate.get(id)

        override fun observeAll(): Flow<List<VaultItem>> = delegate.observeAll()
    }
}
