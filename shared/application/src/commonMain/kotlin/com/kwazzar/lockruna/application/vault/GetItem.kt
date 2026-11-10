package com.kwazzar.lockruna.application.vault

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.vault.ItemId
import com.kwazzar.lockruna.domain.vault.VaultItem
import com.kwazzar.lockruna.domain.vault.VaultItemRepository

class GetItem(private val repository: VaultItemRepository) {
    suspend operator fun invoke(id: ItemId): Result<VaultItem?, AppError> = repository.get(id)
}
