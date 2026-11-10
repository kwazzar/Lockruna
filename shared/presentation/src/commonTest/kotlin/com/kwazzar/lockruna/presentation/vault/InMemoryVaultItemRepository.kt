package com.kwazzar.lockruna.presentation.vault

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.vault.ItemId
import com.kwazzar.lockruna.domain.vault.VaultItem
import com.kwazzar.lockruna.domain.vault.VaultItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class InMemoryVaultItemRepository : VaultItemRepository {
    private val items = linkedMapOf<String, VaultItem>()
    private val emissions = MutableSharedFlow<List<VaultItem>>(replay = 1)

    override suspend fun save(item: VaultItem): Result<VaultItem, AppError> {
        items[item.id.value] = item
        return Result.Success(item)
    }

    override suspend fun get(id: ItemId): Result<VaultItem?, AppError> = Result.Success(items[id.value])

    override fun observeAll(): Flow<List<VaultItem>> = emissions

    suspend fun emitItems() {
        emissions.emit(items.values.toList())
    }
}
