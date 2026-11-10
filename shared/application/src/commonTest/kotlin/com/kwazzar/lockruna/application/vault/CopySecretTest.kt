package com.kwazzar.lockruna.application.vault

import com.kwazzar.lockruna.application.support.InMemoryVaultItemRepository
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.vault.Clipboard
import com.kwazzar.lockruna.domain.vault.ItemId
import com.kwazzar.lockruna.domain.vault.Secret
import com.kwazzar.lockruna.domain.vault.Title
import com.kwazzar.lockruna.domain.vault.VaultItem
import com.kwazzar.lockruna.domain.vault.VaultItemType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CopySecretTest {

    private val repository = InMemoryVaultItemRepository()
    private val clipboard = RecordingClipboard()
    private val copySecret = CopySecret(repository, clipboard)

    @Test
    fun `missing item fails with item not found`() = runTest {
        val result = copySecret(itemId("00000000-0000-4000-8000-000000000009"))

        val failure = result as Result.Failure
        assertEquals(AppError.ItemNotFound, failure.error)
        assertTrue(clipboard.values.isEmpty())
    }

    @Test
    fun `item without a secret fails with invalid item`() = runTest {
        repository.save(createItem(VaultItemType.NOTE, null))

        val result = copySecret(itemId("00000000-0000-4000-8000-000000000001"))

        assertTrue((result as Result.Failure).error is AppError.InvalidItem)
        assertTrue(clipboard.values.isEmpty())
    }

    @Test
    fun `item with a secret is copied`() = runTest {
        repository.save(createItem(VaultItemType.LOGIN, "secret-value"))

        val result = copySecret(itemId("00000000-0000-4000-8000-000000000001"))

        assertEquals(Unit, (result as Result.Success).value)
        assertEquals(listOf("secret-value"), clipboard.values)
    }

    private suspend fun createItem(type: VaultItemType, secret: String?): VaultItem {
        val result = VaultItem.create(
            id = itemId("00000000-0000-4000-8000-000000000001"),
            type = type,
            title = (Title.of("Example") as Result.Success).value,
            username = "user",
            secret = secret?.let { (Secret.of(it) as Result.Success).value },
            url = null,
            folderId = null,
            nowMillis = 1_700_000_000_000L,
        )
        return (result as Result.Success).value
    }

    private fun itemId(raw: String): ItemId = (ItemId.of(raw) as Result.Success).value

    private class RecordingClipboard : Clipboard {
        val values = mutableListOf<String>()

        override suspend fun copySecret(value: String) {
            values += value
        }
    }
}
