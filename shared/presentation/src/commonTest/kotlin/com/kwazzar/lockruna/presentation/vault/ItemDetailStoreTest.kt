package com.kwazzar.lockruna.presentation.vault

import com.kwazzar.lockruna.application.vault.CopySecret
import com.kwazzar.lockruna.application.vault.DeleteItem
import com.kwazzar.lockruna.application.vault.GetItem
import com.kwazzar.lockruna.application.vault.RevealSecret
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.vault.ItemId
import com.kwazzar.lockruna.domain.vault.Secret
import com.kwazzar.lockruna.domain.vault.Title
import com.kwazzar.lockruna.domain.vault.VaultItem
import com.kwazzar.lockruna.domain.vault.VaultItemType
import com.kwazzar.lockruna.presentation.support.FakeClock
import com.kwazzar.lockruna.presentation.support.RecordingClipboard
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ItemDetailStoreTest {

    private val repository = InMemoryVaultItemRepository()
    private val clipboard = RecordingClipboard()
    private val store = ItemDetailStore(
        getItem = GetItem(repository),
        revealSecret = RevealSecret(repository),
        copySecret = CopySecret(repository, clipboard),
        deleteItem = DeleteItem(repository, FakeClock()),
    )

    @Test
    fun `open loads item without secret`() = runTest {
        val item = createItem()
        repository.save(item)

        store.open(item.id.value)

        assertEquals(item.id.value, store.state.value.item?.id)
        assertTrue(store.state.value.item?.hasSecret == true)
        assertFalse(store.state.value.toString().contains("secret-value"))
    }

    @Test
    fun `reveal without an effect collector does not mark the secret visible`() = runTest {
        val item = createItem()
        repository.save(item)
        store.open(item.id.value)

        store.dispatch(ItemDetailIntent.Reveal)

        assertFalse(store.state.value.secretVisible)
    }

    @Test
    fun `reveal emits secret and marks it visible`() = runTest {
        val item = createItem()
        repository.save(item)
        store.open(item.id.value)
        val effect = async(start = CoroutineStart.UNDISPATCHED) { store.effects.first() }

        store.dispatch(ItemDetailIntent.Reveal)

        val revealed = effect.await()
        assertEquals(ItemDetailEffect.SecretRevealed("secret-value"), revealed)
        assertFalse(revealed.toString().contains("secret-value"))
        assertTrue(store.state.value.secretVisible)
    }

    @Test
    fun `hide clears visibility without error`() = runTest {
        val item = createItem()
        repository.save(item)
        store.open(item.id.value)
        val effect = async(start = CoroutineStart.UNDISPATCHED) { store.effects.first() }
        store.dispatch(ItemDetailIntent.Reveal)
        effect.await()

        store.dispatch(ItemDetailIntent.Hide)

        assertFalse(store.state.value.secretVisible)
    }

    @Test
    fun `copy records secret and emits copied effect`() = runTest {
        val item = createItem()
        repository.save(item)
        store.open(item.id.value)
        val effect = async(start = CoroutineStart.UNDISPATCHED) { store.effects.first() }

        store.dispatch(ItemDetailIntent.Copy)

        assertEquals(ItemDetailEffect.SecretCopied, effect.await())
        assertEquals(listOf("secret-value"), clipboard.values)
    }

    @Test
    fun `edit emits navigation effect`() = runTest {
        val item = createItem()
        repository.save(item)
        store.open(item.id.value)
        val effect = async(start = CoroutineStart.UNDISPATCHED) { store.effects.first() }

        store.dispatch(ItemDetailIntent.Edit)

        assertEquals(ItemDetailEffect.NavigateToEdit(item.id.value), effect.await())
    }

    @Test
    fun `delete closes detail and disables further reveal`() = runTest {
        val item = createItem()
        repository.save(item)
        store.open(item.id.value)
        val closed = async(start = CoroutineStart.UNDISPATCHED) { store.effects.first() }

        store.dispatch(ItemDetailIntent.DeleteConfirmed)

        assertEquals(ItemDetailEffect.Closed, closed.await())
        assertNull(store.state.value.item)
        assertFalse(store.state.value.secretVisible)
        val reveal = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeoutOrNull(100) { store.effects.first() }
        }
        store.dispatch(ItemDetailIntent.Reveal)
        assertNull(reveal.await())
    }

    private fun createItem(): VaultItem {
        val result = VaultItem.create(
            id = (ItemId.of("00000000-0000-4000-8000-000000000001") as Result.Success).value,
            type = VaultItemType.LOGIN,
            title = (Title.of("GitHub") as Result.Success).value,
            username = "alice",
            secret = (Secret.of("secret-value") as Result.Success).value,
            url = null,
            folderId = null,
            nowMillis = 1_700_000_000_000L,
        )
        return (result as Result.Success).value
    }
}
