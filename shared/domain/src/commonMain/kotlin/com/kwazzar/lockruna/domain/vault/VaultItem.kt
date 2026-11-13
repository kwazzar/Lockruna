package com.kwazzar.lockruna.domain.vault

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError

/**
 * Vault item aggregate. Encrypted on disk, decrypted in memory when vault is unlocked.
 * Invariants:
 * - version is the server version of the record; it changes only when the server applies the change
 * - dirty = true for any local change that hasn't been synced
 * - deleted = true for soft-deleted items (tombstones)
 * - secret is required for LOGIN type
 * - cardNumber is required for CARD type
 * - noteBody is required for NOTE type
 * - updatedAtMillis is epoch millis from Clock.nowMillis()
 */
data class VaultItem(
    val id: ItemId,
    val type: VaultItemType,
    val title: Title,
    val username: String?,
    val secret: Secret?,
    val url: Url?,
    val folderId: ItemId?,
    val version: Long,
    val dirty: Boolean,
    val deleted: Boolean,
    val updatedAtMillis: Long,
    val cardNumber: Secret? = null,
    val cardExpiry: String? = null,
    val cardCvv: Secret? = null,
    val noteBody: Secret? = null,
) {
    override fun toString(): String {
        val usernameStr = username?.let { "len=${it.length}" }
        val secretStr = secret?.toString()
        val urlStr = url?.toString()
        return "VaultItem(id=$id, type=$type, title=$title, " +
            "username=$usernameStr, secret=$secretStr, url=$urlStr, " +
            "folderId=$folderId, version=$version, dirty=$dirty, " +
            "deleted=$deleted, updatedAtMillis=$updatedAtMillis, " +
            "cardNumber=${cardNumber?.toString()}, cardExpiry=${cardExpiry?.length}, " +
            "cardCvv=${cardCvv?.toString()}, noteBody=${noteBody?.toString()})"
    }

    companion object {
        fun create(
            id: ItemId,
            type: VaultItemType,
            title: Title,
            username: String?,
            secret: Secret?,
            url: Url?,
            folderId: ItemId?,
            nowMillis: Long,
            cardNumber: Secret? = null,
            cardExpiry: String? = null,
            cardCvv: Secret? = null,
            noteBody: Secret? = null,
        ): Result<VaultItem, AppError> {
            val validationError = requiredDetailsError(type, secret, cardNumber, noteBody)
            if (validationError != null) {
                return Result.Failure(validationError)
            }
            return Result.Success(
                VaultItem(
                    id = id,
                    type = type,
                    title = title,
                    username = username,
                    secret = secret.takeIf { type == VaultItemType.LOGIN },
                    url = url,
                    folderId = folderId,
                    version = 0,
                    dirty = true,
                    deleted = false,
                    updatedAtMillis = nowMillis,
                    cardNumber = cardNumber.takeIf { type == VaultItemType.CARD },
                    cardExpiry = cardExpiry.takeIf { type == VaultItemType.CARD },
                    cardCvv = cardCvv.takeIf { type == VaultItemType.CARD },
                    noteBody = noteBody.takeIf { type == VaultItemType.NOTE },
                ),
            )
        }

        fun createWithGeneratedId(
            type: VaultItemType,
            title: Title,
            username: String?,
            secret: Secret?,
            url: Url?,
            folderId: ItemId?,
            nowMillis: Long,
            idGenerator: () -> ItemId,
            cardNumber: Secret? = null,
            cardExpiry: String? = null,
            cardCvv: Secret? = null,
            noteBody: Secret? = null,
        ): Result<VaultItem, AppError> = create(
            id = idGenerator(),
            type = type,
            title = title,
            username = username,
            secret = secret,
            url = url,
            folderId = folderId,
            nowMillis = nowMillis,
            cardNumber = cardNumber,
            cardExpiry = cardExpiry,
            cardCvv = cardCvv,
            noteBody = noteBody,
        )

        private fun requiredDetailsError(
            type: VaultItemType,
            secret: Secret?,
            cardNumber: Secret?,
            noteBody: Secret?,
        ): AppError.InvalidItem? = when (type) {
            VaultItemType.LOGIN -> if (secret == null) {
                AppError.InvalidItem("LOGIN item requires a secret")
            } else {
                null
            }

            VaultItemType.CARD -> if (cardNumber == null) {
                AppError.InvalidItem("CARD item requires a card number")
            } else {
                null
            }

            VaultItemType.NOTE -> if (noteBody == null) {
                AppError.InvalidItem("NOTE item requires a body")
            } else {
                null
            }

            VaultItemType.FOLDER -> null
        }
    }

    fun update(
        title: Title,
        username: String?,
        secret: Secret?,
        url: Url?,
        folderId: ItemId?,
        nowMillis: Long,
        cardNumber: Secret? = null,
        cardExpiry: String? = null,
        cardCvv: Secret? = null,
        noteBody: Secret? = null,
    ): Result<VaultItem, AppError> {
        if (deleted) {
            return Result.Failure(AppError.InvalidItem("Cannot update a deleted item"))
        }
        val validationError = requiredDetailsError(type, secret, cardNumber, noteBody)
        if (validationError != null) {
            return Result.Failure(validationError)
        }
        return Result.Success(
            copy(
                title = title,
                username = username,
                secret = secret.takeIf { type == VaultItemType.LOGIN },
                url = url,
                folderId = folderId,
                dirty = true,
                updatedAtMillis = nowMillis,
                cardNumber = cardNumber.takeIf { type == VaultItemType.CARD },
                cardExpiry = cardExpiry.takeIf { type == VaultItemType.CARD },
                cardCvv = cardCvv.takeIf { type == VaultItemType.CARD },
                noteBody = noteBody.takeIf { type == VaultItemType.NOTE },
            ),
        )
    }

    fun validate(): Result<VaultItem, AppError> {
        val validationError = requiredDetailsError(type, secret, cardNumber, noteBody)
        return if (validationError == null) {
            Result.Success(this)
        } else {
            Result.Failure(validationError)
        }
    }

    fun delete(nowMillis: Long): Result<VaultItem, AppError> {
        if (deleted) {
            return Result.Failure(AppError.InvalidItem("Item is already deleted"))
        }
        return Result.Success(
            copy(
                deleted = true,
                dirty = true,
                updatedAtMillis = nowMillis,
            ),
        )
    }
}
