package com.kwazzar.lockruna.domain.vault

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError

/**
 * Vault item aggregate. Encrypted on disk, decrypted in memory when vault is unlocked.
 * Invariants:
 * - version starts at 0 for new items, increments on every update/delete
 * - dirty = true for any local change that hasn't been synced
 * - deleted = true for soft-deleted items (tombstones)
 * - secret is required for LOGIN type
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
) {
    override fun toString(): String {
        val usernameStr = username?.let { "len=${it.length}" }
        val secretStr = secret?.toString()
        val urlStr = url?.toString()
        return "VaultItem(id=$id, type=$type, title=$title, " +
            "username=$usernameStr, secret=$secretStr, url=$urlStr, " +
            "folderId=$folderId, version=$version, dirty=$dirty, " +
            "deleted=$deleted, updatedAtMillis=$updatedAtMillis)"
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
        ): Result<VaultItem, AppError> {
            // LOGIN requires secret
            if (type == VaultItemType.LOGIN && secret == null) {
                return Result.Failure(AppError.InvalidItem("LOGIN item requires a secret"))
            }
            // For other types, secret is optional but if provided must not be empty (validated by Secret.of)
            return Result.Success(
                VaultItem(
                    id = id,
                    type = type,
                    title = title,
                    username = username,
                    secret = secret,
                    url = url,
                    folderId = folderId,
                    version = 0,
                    dirty = true,
                    deleted = false,
                    updatedAtMillis = nowMillis,
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
        ): Result<VaultItem, AppError> = create(idGenerator(), type, title, username, secret, url, folderId, nowMillis)
    }

    fun update(
        title: Title,
        username: String?,
        secret: Secret?,
        url: Url?,
        folderId: ItemId?,
        nowMillis: Long,
    ): Result<VaultItem, AppError> {
        if (deleted) {
            return Result.Failure(AppError.InvalidItem("Cannot update a deleted item"))
        }
        // LOGIN requires secret
        if (type == VaultItemType.LOGIN && secret == null) {
            return Result.Failure(AppError.InvalidItem("LOGIN item requires a secret"))
        }
        return Result.Success(
            copy(
                title = title,
                username = username,
                secret = secret,
                url = url,
                folderId = folderId,
                version = version + 1,
                dirty = true,
                updatedAtMillis = nowMillis,
            ),
        )
    }

    fun delete(nowMillis: Long): Result<VaultItem, AppError> {
        if (deleted) {
            return Result.Failure(AppError.InvalidItem("Item is already deleted"))
        }
        return Result.Success(
            copy(
                deleted = true,
                version = version + 1,
                dirty = true,
                updatedAtMillis = nowMillis,
            ),
        )
    }
}
