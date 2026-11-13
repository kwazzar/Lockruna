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
    val cardNumber: String? = null,
    val cardExpiry: String? = null,
    val cardCvv: String? = null,
    val noteBody: String? = null,
)

class CreateItem(
    private val repository: VaultItemRepository,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
) {
    @Suppress("ReturnCount")
    suspend operator fun invoke(command: CreateItemCommand): Result<VaultItem, AppError> {
        val titleResult = validateTitle(command.title)
        if (titleResult is Result.Failure) {
            return Result.Failure(titleResult.error)
        }
        val secretResult = validateOptional(command.secret) { Secret.of(it) }
        if (secretResult is Result.Failure) {
            return Result.Failure(secretResult.error)
        }
        val urlResult = validateOptional(command.url) { Url.of(it) }
        if (urlResult is Result.Failure) {
            return Result.Failure(urlResult.error)
        }
        val cardNumberResult = validateOptional(command.cardNumber) { Secret.of(it) }
        if (cardNumberResult is Result.Failure) {
            return Result.Failure(cardNumberResult.error)
        }
        val cardCvvResult = validateOptional(command.cardCvv) { Secret.of(it) }
        if (cardCvvResult is Result.Failure) {
            return Result.Failure(cardCvvResult.error)
        }
        val noteBodyResult = validateOptional(command.noteBody) { Secret.of(it) }
        if (noteBodyResult is Result.Failure) {
            return Result.Failure(noteBodyResult.error)
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
            cardNumber = (cardNumberResult as? Result.Success)?.value,
            cardExpiry = command.cardExpiry,
            cardCvv = (cardCvvResult as? Result.Success)?.value,
            noteBody = (noteBodyResult as? Result.Success)?.value,
        )
        return when (created) {
            is Result.Success -> repository.save(created.value)
            is Result.Failure -> Result.Failure(created.error)
        }
    }

    private fun validateTitle(title: String): Result<Title, AppError> = Title.of(title)

    private fun <T> validateOptional(value: String?, parser: (String) -> Result<T, AppError>): Result<T?, AppError> =
        value?.let { parser(it).map { it as T? } } ?: Result.Success(null)
}
