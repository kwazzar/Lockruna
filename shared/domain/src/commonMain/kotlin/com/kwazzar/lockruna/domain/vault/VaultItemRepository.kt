package com.kwazzar.lockruna.domain.vault

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import kotlinx.coroutines.flow.Flow

interface VaultItemRepository {
    suspend fun save(item: VaultItem): Result<VaultItem, AppError>
    suspend fun get(id: ItemId): Result<VaultItem?, AppError>
    fun observeAll(): Flow<List<VaultItem>>
}