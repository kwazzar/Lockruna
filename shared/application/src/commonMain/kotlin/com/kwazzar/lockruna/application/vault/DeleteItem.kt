package com.kwazzar.lockruna.application.vault

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.Clock
import com.kwazzar.lockruna.domain.vault.ItemId
import com.kwazzar.lockruna.domain.vault.VaultItem
import com.kwazzar.lockruna.domain.vault.VaultItemRepository

class DeleteItem(private val repository: VaultItemRepository, private val clock: Clock) {
    suspend operator fun invoke(id: ItemId): Result<VaultItem, AppError> {
        val existing = when (val found = repository.get(id)) {
            is Result.Success -> found.value
            is Result.Failure -> return Result.Failure(found.error)
        } ?: return Result.Failure(AppError.ItemNotFound)

        val deleted = when (val result = existing.delete(clock.nowMillis())) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(result.error)
        }
        return repository.save(deleted)
    }
}
