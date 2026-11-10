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
    suspend operator fun invoke(
        id: ItemId,
        title: String,
        username: String?,
        secret: String?,
        url: String?,
        folderId: ItemId?,
    ): Result<VaultItem, AppError> {
        val existing = when (val found = findExisting(id)) {
            is Result.Success -> found.value
            is Result.Failure -> return Result.Failure(found.error)
        }

        val fields = when (val parsed = parseFields(title, secret, url)) {
            is Result.Success -> parsed.value
            is Result.Failure -> return Result.Failure(parsed.error)
        }
        val updated = when (
            val result = existing.update(
                title = fields.title,
                username = username,
                secret = fields.secret,
                url = fields.url,
                folderId = folderId,
                nowMillis = clock.nowMillis(),
            )
        ) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(result.error)
        }
        return repository.save(updated)
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
        val titleResult = Title.of(title)
        if (titleResult is Result.Failure) {
            return Result.Failure(titleResult.error)
        }
        val secretResult = secret?.let { Secret.of(it) }
        if (secretResult is Result.Failure) {
            return Result.Failure(secretResult.error)
        }
        val urlResult = url?.let { Url.of(it) }
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
}
