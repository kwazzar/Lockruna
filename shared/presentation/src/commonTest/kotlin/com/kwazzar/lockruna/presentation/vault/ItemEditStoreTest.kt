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
import kotlin.test.assertNull
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
        assertFalse(store.state.value.hasSecretDraft)
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
        assertEquals(0L, updated.version)
        assertTrue(updated.dirty)
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
    fun `secret intent does not expose the secret in toString`() {
        val intent = ItemEditIntent.SecretChanged("secret-value")

        assertFalse(intent.toString().contains("secret-value"))
    }

    @Test
    fun `save clears the secret draft`() = runTest {
        store.dispatch(ItemEditIntent.TitleChanged("GitHub"))
        store.dispatch(ItemEditIntent.SecretChanged("secret-value"))
        val effect = async(start = CoroutineStart.UNDISPATCHED) { store.effects.first() }

        store.dispatch(ItemEditIntent.Save)

        assertEquals(ItemEditEffect.Saved(itemId.value), effect.await())
        assertFalse(store.state.value.hasSecretDraft)
        val secondEffect = async(start = CoroutineStart.UNDISPATCHED) { store.effects.first() }
        store.dispatch(ItemEditIntent.Save)
        assertIs<ItemEditEffect.ValidationError>(secondEffect.await())
        val saved = assertIs<Result.Success<VaultItem>>(repository.get(itemId)).value
        assertEquals(0L, saved.version)
    }

    @Test
    fun `an invalid existing id keeps edit mode and never creates an item`() = runTest {
        store.begin("not-an-id")

        assertEquals("not-an-id", store.state.value.existingId)
        val effect = async(start = CoroutineStart.UNDISPATCHED) { store.effects.first() }
        store.dispatch(ItemEditIntent.Save)

        assertIs<ItemEditEffect.ValidationError>(effect.await())
        val saved = assertIs<Result.Success<VaultItem?>>(repository.get(itemId)).value
        assertNull(saved)
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

class ItemEditTypeTest {

    private val repository = InMemoryVaultItemRepository()
    private val store = ItemEditStore(
        createItem = CreateItem(repository, FixedItemIdGenerator(), FakeClock(wallMillis = 1_000)),
        updateItem = UpdateItem(repository, FakeClock(wallMillis = 2_000)),
        getItem = GetItem(repository),
    )

    @Test
    fun `a card is created without a secret`() = runTest {
        store.dispatch(ItemEditIntent.TypeChanged(VaultItemType.CARD))
        store.dispatch(ItemEditIntent.TitleChanged("Visa"))
        val effect = async(start = CoroutineStart.UNDISPATCHED) { store.effects.first() }

        store.dispatch(ItemEditIntent.Save)

        assertEquals(ItemEditEffect.Saved("00000000-0000-4000-8000-000000000001"), effect.await())
        val saved = assertIs<Result.Success<VaultItem>>(repository.get(itemId())).value
        assertEquals(VaultItemType.CARD, saved.type)
    }

    @Test
    fun `a note is created with the note body as the secret`() = runTest {
        store.dispatch(ItemEditIntent.TypeChanged(VaultItemType.NOTE))
        store.dispatch(ItemEditIntent.TitleChanged("Wifi"))
        store.dispatch(ItemEditIntent.SecretChanged("the password of the guest network"))
        val effect = async(start = CoroutineStart.UNDISPATCHED) { store.effects.first() }

        store.dispatch(ItemEditIntent.Save)

        assertEquals(ItemEditEffect.Saved("00000000-0000-4000-8000-000000000001"), effect.await())
        val saved = assertIs<Result.Success<VaultItem>>(repository.get(itemId())).value
        assertEquals(VaultItemType.NOTE, saved.type)
        assertEquals("the password of the guest network", saved.secret?.value)
    }

    @Test
    fun `a login still requires a secret`() = runTest {
        store.dispatch(ItemEditIntent.TypeChanged(VaultItemType.LOGIN))
        store.dispatch(ItemEditIntent.TitleChanged("GitHub"))
        val effect = async(start = CoroutineStart.UNDISPATCHED) { store.effects.first() }

        store.dispatch(ItemEditIntent.Save)

        val validation = effect.await()
        assertIs<ItemEditEffect.ValidationError>(validation)
        assertTrue((validation as ItemEditEffect.ValidationError).error is AppError.InvalidItem)
    }

    @Test
    fun `editing an item shows the type it was created with`() = runTest {
        repository.save(
            assertIs<Result.Success<VaultItem>>(
                VaultItem.create(
                    id = itemId(),
                    type = VaultItemType.NOTE,
                    title = (Title.of("Wifi") as Result.Success).value,
                    username = null,
                    secret = null,
                    url = null,
                    folderId = null,
                    nowMillis = 1,
                ),
            ).value,
        )

        store.begin("00000000-0000-4000-8000-000000000001")

        assertEquals(VaultItemType.NOTE, store.state.value.type)
    }

    private fun itemId(): ItemId = (ItemId.of("00000000-0000-4000-8000-000000000001") as Result.Success).value
}

private class FixedItemIdGenerator : IdGenerator {
    override fun newItemId(): ItemId = (ItemId.of("00000000-0000-4000-8000-000000000001") as Result.Success).value

    override fun newUuid(): String = "00000000-0000-4000-8000-000000000001"
}
