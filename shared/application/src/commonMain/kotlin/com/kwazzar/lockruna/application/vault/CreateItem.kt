package com.kwazzar.lockruna.application.vault

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.Clock
import com.kwazzar.lockruna.domain.session.IdGenerator
import com.kwazzar.lockruna.domain.vault.ItemId
import com.kwazzar.lockruna.domain.vault.Secret
import com.kwazzar.lockruna.domain.vault.Title
import com.kwazzar.lockruna.domain.vault.Url
import com.kwazzar.lockruna.domain.vault.VaultItem
import com.kwazzar.lockruna.domain.vault.VaultItemRepository
import com.kwazzar.lockruna.domain.vault.VaultItemType

data class CreateItemCommand(
    val type: VaultItemType,
    val title: String,
    val username: String?,
    val secret: String?,
    val url: String?,
    val folderId: ItemId?,
)

class CreateItem(
    private val repository: VaultItemRepository,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
) {
    suspend operator fun invoke(command: CreateItemCommand): Result<VaultItem, AppError> {
        val titleResult = Title.of(command.title)
        if (titleResult is Result.Failure) {
            return Result.Failure(titleResult.error)
        }
        val secretResult = command.secret?.let { Secret.of(it) }
        if (secretResult is Result.Failure) {
            return Result.Failure(secretResult.error)
        }
        val urlResult = command.url?.let { Url.of(it) }
        if (urlResult is Result.Failure) {
            return Result.Failure(urlResult.error)
        }
        val created = VaultItem.create(
            id = idGenerator.newItemId(),
            type = command.type,
            title = (titleResult as Result.Success).value,
            username = command.username,
            secret = (secretResult as? Result.Success)?.value,
            url = (urlResult as? Result.Success)?.value,
            folderId = command.folderId,
            nowMillis = clock.nowMillis(),
        )
        return when (created) {
            is Result.Success -> repository.save(created.value)
            is Result.Failure -> Result.Failure(created.error)
        }
    }
}
