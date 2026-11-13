package com.kwazzar.lockruna.application.vault

import com.kwazzar.lockruna.application.support.FakeClock
import com.kwazzar.lockruna.application.support.FakeIdGenerator
import com.kwazzar.lockruna.application.support.InMemoryVaultItemRepository
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.vault.ItemId
import com.kwazzar.lockruna.domain.vault.VaultItemType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VaultUseCasesTest {

    private val repository = InMemoryVaultItemRepository()
    private val clock = FakeClock(wallMillis = 1_000_000, monotonicMillis = 500_000)
    private val createItem = CreateItem(repository, FakeIdGenerator(), clock)
    private val updateItem = UpdateItem(repository, clock)
    private val deleteItem = DeleteItem(repository, clock)
    private val getItem = GetItem(repository)
    private val observeItems = ObserveItems(repository)

    private fun createCommand(
        type: VaultItemType = VaultItemType.NOTE,
        title: String = "Note",
        secret: String? = null,
        noteBody: String? = "note-body",
    ) = CreateItemCommand(
        type = type,
        title = title,
        username = null,
        secret = secret,
        url = null,
        folderId = null,
        noteBody = noteBody,
    )

    @Test
    fun `create validates input and stores a fresh draft`() = runTest {
        val rejected = createItem(createCommand(title = ""))
        assertTrue(rejected is Result.Failure)

        val created = createItem(createCommand())
        val result = created as Result.Success
        assertEquals(0L, result.value.version)
        assertEquals(true, result.value.dirty)
        assertEquals(VaultItemType.NOTE, result.value.type)
    }

    @Test
    fun `create rejects login without secret`() = runTest {
        val rejected = createItem(
            CreateItemCommand(
                type = VaultItemType.LOGIN,
                title = "Login",
                username = "user",
                secret = null,
                url = null,
                folderId = null,
            ),
        )

        assertTrue(rejected is Result.Failure)
    }

    @Test
    fun `update of a missing item fails`() = runTest {
        val missingId = (
            com.kwazzar.lockruna.domain.vault.ItemId.of(
                "00000000-0000-4000-8000-000000000009",
            ) as Result.Success
            ).value

        val result = updateItem(missingId, "New", null, "secret", null, null)

        assertTrue(result is Result.Failure)
        assertEquals(AppError.ItemNotFound, (result as Result.Failure).error)
    }

    @Test
    fun `update keeps the server version and marks the item dirty`() = runTest {
        val created = createItem(createCommand()) as Result.Success
        val itemId = created.value.id

        val updated = updateItem(itemId, "Renamed", null, null, null, null, noteBody = "updated-note-body") as Result.Success

        assertEquals(0L, updated.value.version)
        assertEquals(true, updated.value.dirty)
        assertEquals("Renamed", updated.value.title.value)
    }

    @Test
    fun `update without a secret draft preserves the existing secret`() = runTest {
        val created = createItem(createCommand(type = VaultItemType.LOGIN, secret = "secret-value")) as Result.Success

        val updated = updateItem(created.value.id, "Renamed", null, null, null, null) as Result.Success

        assertEquals("secret-value", updated.value.secret?.value)
    }

    @Test
    fun `delete soft deletes the item`() = runTest {
        val created = createItem(createCommand()) as Result.Success
        val itemId = created.value.id

        val deleted = deleteItem(itemId) as Result.Success

        assertEquals(true, deleted.value.deleted)
        assertEquals(0L, deleted.value.version)
    }

    @Test
    fun `get of a missing item succeeds with null`() = runTest {
        val missingId = (
            com.kwazzar.lockruna.domain.vault.ItemId.of(
                "00000000-0000-4000-8000-000000000009",
            ) as Result.Success
            ).value

        val result = getItem(missingId)

        assertTrue(result is Result.Success)
        assertEquals(null, (result as Result.Success).value)
    }

    @Test
    fun `observe emits created items`() = runTest {
        val created = createItem(createCommand()) as Result.Success

        val observed = observeItems().first()

        assertEquals(1, observed.size)
        assertEquals(created.value.id, observed.first().id)
    }
}
