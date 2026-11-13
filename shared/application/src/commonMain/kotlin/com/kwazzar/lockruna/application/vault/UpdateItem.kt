package com.kwazzar.lockruna.application.vault

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.Clock
import com.kwazzar.lockruna.domain.vault.ItemId
import com.kwazzar.lockruna.domain.vault.Secret
import com.kwazzar.lockruna.domain.vault.Title
import com.kwazzar.lockruna.domain.vault.Url
import com.kwazzar.lockruna.domain.vault.VaultItem
import com.kwazzar.lockruna.domain.vault.VaultItemRepository

class UpdateItem(private val repository: VaultItemRepository, private val clock: Clock) {
    @Suppress("ReturnCount")
    suspend operator fun invoke(
        id: ItemId,
        title: String,
        username: String?,
        secret: String?,
        url: String?,
        folderId: ItemId?,
        cardNumber: String? = null,
        cardExpiry: String? = null,
        cardCvv: String? = null,
        noteBody: String? = null,
    ): Result<VaultItem, AppError> {
        val existing = findExisting(id)
        if (existing is Result.Failure) {
            return existing
        }
        val existingItem = (existing as Result.Success).value

        val fields = parseFields(title, secret, url)
        if (fields is Result.Failure) {
            return fields
        }

        val cardNumberResult = validateOptional(cardNumber) { Secret.of(it) }
        if (cardNumberResult is Result.Failure) {
            return Result.Failure(cardNumberResult.error)
        }
        val cardCvvResult = validateOptional(cardCvv) { Secret.of(it) }
        if (cardCvvResult is Result.Failure) {
            return Result.Failure(cardCvvResult.error)
        }
        val noteBodyResult = validateOptional(noteBody) { Secret.of(it) }
        if (noteBodyResult is Result.Failure) {
            return Result.Failure(noteBodyResult.error)
        }

        val updated = existingItem.update(
            title = (fields as Result.Success).value.title,
            username = username,
            secret = (fields as Result.Success).value.secret ?: existingItem.secret,
            url = (fields as Result.Success).value.url,
            folderId = folderId,
            nowMillis = clock.nowMillis(),
            cardNumber = (cardNumberResult as? Result.Success)?.value,
            cardExpiry = cardExpiry,
            cardCvv = (cardCvvResult as? Result.Success)?.value,
            noteBody = (noteBodyResult as? Result.Success)?.value,
        )

        return when (updated) {
            is Result.Success -> repository.save(updated.value)
            is Result.Failure -> Result.Failure(updated.error)
        }
    }

    private data class Fields(val title: Title, val secret: Secret?, val url: Url?)

    private suspend fun findExisting(id: ItemId): Result<VaultItem, AppError> {
        val found = repository.get(id)
        if (found is Result.Failure) {
            return Result.Failure(found.error)
        }
        return (found as Result.Success).value?.let { Result.Success(it) }
            ?: Result.Failure(AppError.ItemNotFound)
    }

    private fun parseFields(title: String, secret: String?, url: String?): Result<Fields, AppError> {
        val titleResult = validateTitle(title)
        if (titleResult is Result.Failure) {
            return Result.Failure(titleResult.error)
        }
        val secretResult = validateOptional(secret) { Secret.of(it) }
        if (secretResult is Result.Failure) {
            return Result.Failure(secretResult.error)
        }
        val urlResult = validateOptional(url) { Url.of(it) }
        if (urlResult is Result.Failure) {
            return Result.Failure(urlResult.error)
        }
        return Result.Success(
            Fields(
                title = (titleResult as Result.Success).value,
                secret = (secretResult as? Result.Success)?.value,
                url = (urlResult as? Result.Success)?.value,
            ),
        )
    }

    private fun validateTitle(title: String): Result<Title, AppError> = Title.of(title)

    private fun <T> validateOptional(value: String?, parser: (String) -> Result<T, AppError>): Result<T?, AppError> =
        value?.let { parser(it).map { it as T? } } ?: Result.Success(null)
}
