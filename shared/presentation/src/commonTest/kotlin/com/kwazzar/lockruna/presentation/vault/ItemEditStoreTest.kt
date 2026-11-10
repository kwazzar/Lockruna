package com.kwazzar.lockruna.presentation.vault

import com.kwazzar.lockruna.application.vault.CreateItem
import com.kwazzar.lockruna.application.vault.GetItem
import com.kwazzar.lockruna.application.vault.UpdateItem
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.IdGenerator
import com.kwazzar.lockruna.domain.vault.ItemId
import com.kwazzar.lockruna.domain.vault.Secret
import com.kwazzar.lockruna.domain.vault.Title
import com.kwazzar.lockruna.domain.vault.Url
import com.kwazzar.lockruna.domain.vault.VaultItem
import com.kwazzar.lockruna.domain.vault.VaultItemRepository
import com.kwazzar.lockruna.domain.vault.VaultItemType
import com.kwazzar.lockruna.presentation.support.FakeClock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ItemEditStoreTest {

    private val itemId = itemId("00000000-0000-4000-8000-000000000001")
    private val repository = InMemoryVaultItemRepository()
    private val store = ItemEditStore(
        createItem = CreateItem(repository, FixedIdGenerator(itemId), FakeClock(wallMillis = 1_000)),
        updateItem = UpdateItem(repository, FakeClock(wallMillis = 2_000)),
        getItem = GetItem(repository),
    )

    @Test
    fun `save creates a login and emits its id`() = runTest {
        store.dispatch(ItemEditIntent.TitleChanged("GitHub"))
        store.dispatch(ItemEditIntent.UsernameChanged("alice"))
        store.dispatch(ItemEditIntent.SecretChanged("secret-value"))
        store.dispatch(ItemEditIntent.UrlChanged("https://example.com"))
        val effect = async(start = CoroutineStart.UNDISPATCHED) { store.effects.first() }

        store.dispatch(ItemEditIntent.Save)

        assertEquals(ItemEditEffect.Saved(itemId.value), effect.await())
        val saved = assertIs<Result.Success<VaultItem>>(repository.get(itemId)).value
        assertEquals("GitHub", saved.title.value)
        assertEquals("alice", saved.username)
        assertEquals("secret-value", saved.secret?.value)
        assertEquals("https://example.com", saved.url?.value)
        assertTrue(store.state.value.hasSecretDraft)
        assertFalse(store.state.value.toString().contains("secret-value"))
    }

    @Test
    fun `begin loads an existing item without copying its secret`() = runTest {
        repository.save(createExistingItem())

        store.begin(itemId.value)

        assertEquals(itemId.value, store.state.value.existingId)
        assertEquals("GitHub", store.state.value.title)
        assertEquals("alice", store.state.value.username)
        assertEquals("https://example.com", store.state.value.url)
        assertFalse(store.state.value.hasSecretDraft)
        assertFalse(store.state.value.toString().contains("secret-value"))
    }

    @Test
    fun `begin clears a secret draft from the previous screen`() = runTest {
        repository.save(createExistingItem())
        store.dispatch(ItemEditIntent.SecretChanged("secret-value"))

        store.begin(itemId.value)

        assertFalse(store.state.value.hasSecretDraft)
        val effect = async(start = CoroutineStart.UNDISPATCHED) { store.effects.first() }
        store.dispatch(ItemEditIntent.Save)

        assertEquals(ItemEditEffect.Saved(itemId.value), effect.await())
        val updated = assertIs<Result.Success<VaultItem>>(repository.get(itemId)).value
        assertEquals("secret-value", updated.secret?.value)
    }

    @Test
    fun `save updates an existing item and preserves its unchanged secret`() = runTest {
        repository.save(createExistingItem())
        store.begin(itemId.value)
        store.dispatch(ItemEditIntent.TitleChanged("Renamed"))
        val effect = async(start = CoroutineStart.UNDISPATCHED) { store.effects.first() }

        store.dispatch(ItemEditIntent.Save)

        assertEquals(ItemEditEffect.Saved(itemId.value), effect.await())
        val updated = assertIs<Result.Success<VaultItem>>(repository.get(itemId)).value
        assertEquals("Renamed", updated.title.value)
        assertEquals("secret-value", updated.secret?.value)
        assertEquals(1L, updated.version)
    }

    @Test
    fun `save with a blank title emits a validation error`() = runTest {
        store.dispatch(ItemEditIntent.TitleChanged("   "))
        store.dispatch(ItemEditIntent.SecretChanged("secret-value"))
        val effect = async(start = CoroutineStart.UNDISPATCHED) { store.effects.first() }

        store.dispatch(ItemEditIntent.Save)

        val validation = assertIs<ItemEditEffect.ValidationError>(effect.await())
        assertIs<AppError.InvalidItem>(validation.error)
        assertFalse(store.state.value.isSaving)
    }

    @Test
    fun `save with an empty secret for a login emits a validation error`() = runTest {
        store.dispatch(ItemEditIntent.TitleChanged("GitHub"))
        store.dispatch(ItemEditIntent.SecretChanged(""))
        val effect = async(start = CoroutineStart.UNDISPATCHED) { store.effects.first() }

        store.dispatch(ItemEditIntent.Save)

        val validation = assertIs<ItemEditEffect.ValidationError>(effect.await())
        assertIs<AppError.InvalidItem>(validation.error)
        assertFalse(store.state.value.isSaving)
    }

    @Test
    fun `save with an explicitly empty secret for an existing login emits a validation error`() = runTest {
        repository.save(createExistingItem())
        store.begin(itemId.value)
        store.dispatch(ItemEditIntent.SecretChanged(""))
        val effect = async(start = CoroutineStart.UNDISPATCHED) { store.effects.first() }

        store.dispatch(ItemEditIntent.Save)

        val validation = assertIs<ItemEditEffect.ValidationError>(effect.await())
        assertIs<AppError.InvalidItem>(validation.error)
        assertFalse(store.state.value.isSaving)
    }

    @Test
    fun `save is ignored while another save is in progress`() = runTest {
        val blockingRepository = BlockingVaultItemRepository()
        val blockingStore = ItemEditStore(
            createItem = CreateItem(blockingRepository, FixedIdGenerator(itemId), FakeClock()),
            updateItem = UpdateItem(blockingRepository, FakeClock()),
            getItem = GetItem(blockingRepository),
        )
        blockingStore.dispatch(ItemEditIntent.TitleChanged("GitHub"))
        blockingStore.dispatch(ItemEditIntent.SecretChanged("secret-value"))
        val effect = async(start = CoroutineStart.UNDISPATCHED) { blockingStore.effects.first() }
        val firstSave = async { blockingStore.dispatch(ItemEditIntent.Save) }
        blockingRepository.saveStarted.await()

        assertTrue(blockingStore.state.value.isSaving)
        blockingStore.dispatch(ItemEditIntent.Save)
        blockingRepository.finishSave.complete(Unit)
        firstSave.await()

        assertEquals(1, blockingRepository.saveCount)
        assertEquals(ItemEditEffect.Saved(itemId.value), effect.await())
    }

    private fun createExistingItem(): VaultItem {
        val result = VaultItem.create(
            id = itemId,
            type = VaultItemType.LOGIN,
            title = (Title.of("GitHub") as Result.Success).value,
            username = "alice",
            secret = (Secret.of("secret-value") as Result.Success).value,
            url = (Url.of("https://example.com") as Result.Success).value,
            folderId = null,
            nowMillis = 1_000,
        )
        return (result as Result.Success).value
    }

    private fun itemId(raw: String): ItemId = (ItemId.of(raw) as Result.Success).value

    private class FixedIdGenerator(private val itemId: ItemId) : IdGenerator {
        override fun newItemId(): ItemId = itemId

        override fun newUuid(): String = itemId.value
    }

    private class BlockingVaultItemRepository : VaultItemRepository {
        val saveStarted = CompletableDeferred<Unit>()
        val finishSave = CompletableDeferred<Unit>()
        var saveCount = 0

        override suspend fun save(item: VaultItem): Result<VaultItem, AppError> {
            saveCount++
            saveStarted.complete(Unit)
            finishSave.await()
            return Result.Success(item)
        }

        override suspend fun get(id: ItemId): Result<VaultItem?, AppError> = Result.Success(null)

        override fun observeAll(): Flow<List<VaultItem>> = emptyFlow()
    }
}
