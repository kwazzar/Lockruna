package com.kwazzar.lockruna.presentation.vault

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.vault.ItemId
import com.kwazzar.lockruna.domain.vault.Secret
import com.kwazzar.lockruna.domain.vault.Title
import com.kwazzar.lockruna.domain.vault.VaultItem
import com.kwazzar.lockruna.domain.vault.VaultItemType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ItemUiMappingTest {

    @Test
    fun `vault item maps to item UI without its secret`() {
        val item = createLoginItem()

        val ui = item.toItemUi()

        assertEquals(item.id.value, ui.id)
        assertEquals(item.type, ui.type)
        assertEquals(item.title.value, ui.title)
        assertEquals(item.username, ui.username)
        assertEquals(item.folderId?.value, ui.folderId)
        assertEquals(item.updatedAtMillis, ui.updatedAtMillis)
        assertTrue(ui.hasSecret)
        assertFalse(ui.toString().contains("secret-value"))
    }

    private fun createLoginItem(): VaultItem {
        val result = VaultItem.create(
            id = (ItemId.of("00000000-0000-4000-8000-000000000001") as Result.Success).value,
            type = VaultItemType.LOGIN,
            title = (Title.of("Example") as Result.Success).value,
            username = "user",
            secret = (Secret.of("secret-value") as Result.Success).value,
            url = null,
            folderId = null,
            nowMillis = 1_700_000_000_000L,
        )

        return (result as Result.Success).value.also { item ->
            assertEquals(0L, item.version)
        }
    }
}
