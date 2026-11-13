package com.kwazzar.lockruna.domain.vault

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VaultItemTest {

    private val nowMillis = 1_700_000_000_000L
    private val validItemId = "00000000-0000-4000-8000-000000000001"
    private val validTitle = "Test Item"
    private val validSecret = "secret-value"
    private val validUrl = "https://example.com"
    private val validUsername = "user"

    @Test
    fun `Title of rejects blank`() {
        assertTrue(Title.of("") is Result.Failure)
        assertTrue(Title.of("   ") is Result.Failure)
    }

    @Test
    fun `Title of accepts non blank`() {
        val result = Title.of("GitHub")
        assertTrue(result is Result.Success)
        assertEquals("GitHub", (result as Result.Success).value.value)
    }

    @Test
    fun `Secret of rejects empty`() {
        assertTrue(Secret.of("") is Result.Failure)
    }

    @Test
    fun `Secret of accepts non empty`() {
        val result = Secret.of("secret-value")
        assertTrue(result is Result.Success)
        assertEquals("secret-value", (result as Result.Success).value.value)
    }

    @Test
    fun `ItemId of rejects invalid UUID`() {
        assertTrue(ItemId.of("not-a-uuid") is Result.Failure)
        assertTrue(ItemId.of("00000000-0000-0000-0000-000000000000") is Result.Failure) // not v4
        assertTrue(ItemId.of("00000000-0000-4000-8000-00000000000") is Result.Failure) // too short
        assertTrue(ItemId.of("gggggggg-0000-4000-8000-000000000001") is Result.Failure) // invalid hex
    }

    @Test
    fun `ItemId of accepts valid UUID v4`() {
        val result = ItemId.of("00000000-0000-4000-8000-000000000001")
        assertTrue(result is Result.Success)
        assertEquals("00000000-0000-4000-8000-000000000001", (result as Result.Success).value.value)
    }

    @Test
    fun `Url of rejects non http`() {
        assertTrue(Url.of("ftp://host") is Result.Failure)
        assertTrue(Url.of("ws://host") is Result.Failure)
        assertTrue(Url.of("example.com") is Result.Failure)
    }

    @Test
    fun `Url of accepts http and https`() {
        assertTrue(Url.of("http://example.com") is Result.Success)
        assertTrue(Url.of("https://example.com") is Result.Success)
        assertTrue(Url.of("  https://example.com  ") is Result.Success)
    }

    @Test
    fun `VaultItem create LOGIN without secret fails`() {
        val id = (ItemId.of(validItemId) as Result.Success).value
        val title = (Title.of(validTitle) as Result.Success).value
        val result = VaultItem.create(
            id,
            VaultItemType.LOGIN,
            title,
            validUsername,
            null,
            (Url.of(validUrl) as Result.Success).value,
            null,
            nowMillis,
        )
        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error is AppError.InvalidItem)
    }

    @Test
    fun `VaultItem create NOTE without body fails`() {
        val id = (ItemId.of(validItemId) as Result.Success).value
        val title = (Title.of(validTitle) as Result.Success).value
        val result = VaultItem.create(id, VaultItemType.NOTE, title, null, null, null, null, nowMillis)
        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error is AppError.InvalidItem)
    }

    @Test
    fun `VaultItem create NOTE with body succeeds`() {
        val id = (ItemId.of(validItemId) as Result.Success).value
        val title = (Title.of(validTitle) as Result.Success).value
        val body = (Secret.of("note-value") as Result.Success).value
        val result = VaultItem.create(
            id = id,
            type = VaultItemType.NOTE,
            title = title,
            username = null,
            secret = null,
            url = null,
            folderId = null,
            nowMillis = nowMillis,
            noteBody = body,
        )
        assertTrue(result is Result.Success)
        val item = (result as Result.Success).value
        assertEquals(body, item.noteBody)
        assertEquals(0, item.version)
        assertTrue(item.dirty)
        assertFalse(item.deleted)
    }

    @Test
    fun `VaultItem create CARD without number fails`() {
        val id = (ItemId.of(validItemId) as Result.Success).value
        val title = (Title.of(validTitle) as Result.Success).value
        val result = VaultItem.create(
            id = id,
            type = VaultItemType.CARD,
            title = title,
            username = null,
            secret = null,
            url = null,
            folderId = null,
            nowMillis = nowMillis,
        )
        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error is AppError.InvalidItem)
    }

    @Test
    fun `VaultItem create CARD stores typed details`() {
        val id = (ItemId.of(validItemId) as Result.Success).value
        val title = (Title.of(validTitle) as Result.Success).value
        val number = (Secret.of("card-number") as Result.Success).value
        val cvv = (Secret.of("card-cvv") as Result.Success).value
        val result = VaultItem.create(
            id = id,
            type = VaultItemType.CARD,
            title = title,
            username = "holder",
            secret = null,
            url = null,
            folderId = null,
            nowMillis = nowMillis,
            cardNumber = number,
            cardExpiry = "12/30",
            cardCvv = cvv,
        )
        assertTrue(result is Result.Success)
        val item = (result as Result.Success).value
        assertEquals(number, item.cardNumber)
        assertEquals("12/30", item.cardExpiry)
        assertEquals(cvv, item.cardCvv)
        assertNull(item.secret)
    }

    @Test
    fun `VaultItem update CARD without number fails`() {
        val id = (ItemId.of(validItemId) as Result.Success).value
        val title = (Title.of(validTitle) as Result.Success).value
        val number = (Secret.of("card-number") as Result.Success).value
        val created = (
            VaultItem.create(
                id = id,
                type = VaultItemType.CARD,
                title = title,
                username = null,
                secret = null,
                url = null,
                folderId = null,
                nowMillis = nowMillis,
                cardNumber = number,
            ) as Result.Success
            ).value

        val result = created.update(
            title = title,
            username = null,
            secret = null,
            url = null,
            folderId = null,
            nowMillis = nowMillis + 1,
        )

        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error is AppError.InvalidItem)
    }

    @Test
    fun `VaultItem update NOTE without body fails`() {
        val id = (ItemId.of(validItemId) as Result.Success).value
        val title = (Title.of(validTitle) as Result.Success).value
        val body = (Secret.of("note-value") as Result.Success).value
        val created = (
            VaultItem.create(
                id = id,
                type = VaultItemType.NOTE,
                title = title,
                username = null,
                secret = null,
                url = null,
                folderId = null,
                nowMillis = nowMillis,
                noteBody = body,
            ) as Result.Success
            ).value

        val result = created.update(
            title = title,
            username = null,
            secret = null,
            url = null,
            folderId = null,
            nowMillis = nowMillis + 1,
        )

        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error is AppError.InvalidItem)
    }

    @Test
    fun `VaultItem create sets version 0 dirty true deleted false`() {
        val id = (ItemId.of(validItemId) as Result.Success).value
        val title = (Title.of(validTitle) as Result.Success).value
        val secret = (Secret.of(validSecret) as Result.Success).value
        val result = VaultItem.create(
            id,
            VaultItemType.LOGIN,
            title,
            validUsername,
            secret,
            (Url.of(validUrl) as Result.Success).value,
            null,
            nowMillis,
        )
        assertTrue(result is Result.Success)
        val item = (result as Result.Success).value
        assertEquals(0, item.version)
        assertTrue(item.dirty)
        assertFalse(item.deleted)
        assertEquals(nowMillis, item.updatedAtMillis)
    }

    @Test
    fun `VaultItem update keeps the server version and sets dirty`() {
        val id = (ItemId.of(validItemId) as Result.Success).value
        val title = (Title.of(validTitle) as Result.Success).value
        val secret = (Secret.of(validSecret) as Result.Success).value
        val created = (
            VaultItem.create(
                id,
                VaultItemType.LOGIN,
                title,
                validUsername,
                secret,
                (Url.of(validUrl) as Result.Success).value,
                null,
                nowMillis,
            ) as Result.Success
            ).value

        val newTitle = (Title.of("Updated Title") as Result.Success).value
        val newSecret = (Secret.of("new-secret") as Result.Success).value
        val result = created.update(
            newTitle,
            validUsername,
            newSecret,
            (Url.of(validUrl) as Result.Success).value,
            null,
            nowMillis + 1000,
        )
        assertTrue(result is Result.Success)
        val updated = (result as Result.Success).value
        assertEquals(0, updated.version)
        assertTrue(updated.dirty)
        assertEquals(nowMillis + 1000, updated.updatedAtMillis)
        assertEquals("Updated Title", updated.title.value)
        assertEquals("new-secret", updated.secret?.value)
    }

    @Test
    fun `VaultItem update on deleted item fails`() {
        val id = (ItemId.of(validItemId) as Result.Success).value
        val title = (Title.of(validTitle) as Result.Success).value
        val secret = (Secret.of(validSecret) as Result.Success).value
        val created = (
            VaultItem.create(
                id,
                VaultItemType.LOGIN,
                title,
                validUsername,
                secret,
                (Url.of(validUrl) as Result.Success).value,
                null,
                nowMillis,
            ) as Result.Success
            ).value
        val deleted = (created.delete(nowMillis + 1000) as Result.Success).value

        val newTitle = (Title.of("Updated") as Result.Success).value
        val result = deleted.update(
            newTitle,
            validUsername,
            secret,
            (Url.of(validUrl) as Result.Success).value,
            null,
            nowMillis + 2000,
        )
        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error is AppError.InvalidItem)
    }

    @Test
    fun `VaultItem delete sets deleted true keeps the server version sets dirty`() {
        val id = (ItemId.of(validItemId) as Result.Success).value
        val title = (Title.of(validTitle) as Result.Success).value
        val secret = (Secret.of(validSecret) as Result.Success).value
        val created = (
            VaultItem.create(
                id,
                VaultItemType.LOGIN,
                title,
                validUsername,
                secret,
                (Url.of(validUrl) as Result.Success).value,
                null,
                nowMillis,
            ) as Result.Success
            ).value

        val result = created.delete(nowMillis + 1000)
        assertTrue(result is Result.Success)
        val deleted = (result as Result.Success).value
        assertTrue(deleted.deleted)
        assertEquals(0, deleted.version)
        assertTrue(deleted.dirty)
        assertEquals(nowMillis + 1000, deleted.updatedAtMillis)
    }

    @Test
    fun `VaultItem delete on already deleted fails`() {
        val id = (ItemId.of(validItemId) as Result.Success).value
        val title = (Title.of(validTitle) as Result.Success).value
        val secret = (Secret.of(validSecret) as Result.Success).value
        val created = (
            VaultItem.create(
                id,
                VaultItemType.LOGIN,
                title,
                validUsername,
                secret,
                (Url.of(validUrl) as Result.Success).value,
                null,
                nowMillis,
            ) as Result.Success
            ).value
        val deleted = (created.delete(nowMillis + 1000) as Result.Success).value

        val result = deleted.delete(nowMillis + 2000)
        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error is AppError.InvalidItem)
    }

    @Test
    fun `toString does not contain secret or title values`() {
        val id = (ItemId.of(validItemId) as Result.Success).value
        val title = (Title.of(validTitle) as Result.Success).value
        val secret = (Secret.of(validSecret) as Result.Success).value
        val item = (
            VaultItem.create(
                id,
                VaultItemType.LOGIN,
                title,
                validUsername,
                secret,
                (Url.of(validUrl) as Result.Success).value,
                null,
                nowMillis,
            ) as Result.Success
            ).value

        val str = item.toString()
        assertTrue(!str.contains("secret-value"))
        assertTrue(!str.contains("Test Item"))
        assertTrue(str.contains("Secret(len="))
        assertTrue(str.contains("Title(len="))
    }

    @Test
    fun `VO toString does not leak values`() {
        assertTrue((Secret.of("secret") as Result.Success).value.toString().contains("len=6"))
        assertTrue((Title.of("title") as Result.Success).value.toString().contains("len=5"))
        assertTrue((Url.of("https://example.com") as Result.Success).value.toString().contains("len="))
        assertTrue(
            (ItemId.of("00000000-0000-4000-8000-000000000001") as Result.Success).value.toString().contains("len=36"),
        )
    }
}
