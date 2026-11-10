package com.kwazzar.lockruna.application.vault

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.vault.Clipboard
import com.kwazzar.lockruna.domain.vault.ItemId
import com.kwazzar.lockruna.domain.vault.VaultItemRepository

class CopySecret(private val repository: VaultItemRepository, private val clipboard: Clipboard) {
    suspend operator fun invoke(id: ItemId): Result<Unit, AppError> {
        val item = when (val found = repository.get(id)) {
            is Result.Failure -> return Result.Failure(found.error)
            is Result.Success -> found.value ?: return Result.Failure(AppError.ItemNotFound)
        }
        val secret = item.secret ?: return Result.Failure(AppError.InvalidItem("Vault item has no secret"))
        clipboard.copySecret(secret.value)
        return Result.Success(Unit)
    }
}
