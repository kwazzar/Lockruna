package com.kwazzar.lockruna.presentation.vault

import com.kwazzar.lockruna.application.session.LockVault
import com.kwazzar.lockruna.application.vault.CopySecret
import com.kwazzar.lockruna.application.vault.ObserveItems
import com.kwazzar.lockruna.application.vault.RevealSecret
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.session.VaultKey
import com.kwazzar.lockruna.domain.session.VaultSession
import com.kwazzar.lockruna.domain.vault.ItemId
import com.kwazzar.lockruna.domain.vault.Secret
import com.kwazzar.lockruna.domain.vault.Title
import com.kwazzar.lockruna.domain.vault.VaultItem
import com.kwazzar.lockruna.domain.vault.VaultItemType
import com.kwazzar.lockruna.presentation.support.FakeCryptoService
import com.kwazzar.lockruna.presentation.support.RecordingClipboard
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VaultListStoreTest {

    private val repository = InMemoryVaultItemRepository()
    private val clipboard = RecordingClipboard()
    private val session = VaultSession()
    private val store = VaultListStore(
        observeItems = ObserveItems(repository),
        lockVault = LockVault(session),
        revealSecret = RevealSecret(repository),
        copySecret = CopySecret(repository, clipboard),
    )

    @Test
    fun `observed items replace loading state`() = runTest {
        assertTrue(store.state.value.isLoading)
        repository.save(createItem("GitHub", "alice"))
        repository.emitItems()

        val state = withTimeout(1_000) {
            store.state.first { !it.isLoading }
        }

        assertEquals(listOf("GitHub"), state.items.map { it.title })
    }

    @Test
    fun `search matches title and username without case`() = runTest {
        repository.save(createItem("GitHub", "alice", "00000000-0000-4000-8000-000000000001"))
        repository.save(createItem("Google", "bob", "00000000-0000-4000-8000-000000000002"))
        repository.emitItems()
        withTimeout(1_000) { store.state.first { !it.isLoading } }

        store.dispatch(VaultListIntent.Search("  ALICE "))

        assertEquals(listOf("GitHub"), store.state.value.items.map { it.title })
        store.dispatch(VaultListIntent.Search("google"))
        assertEquals(listOf("Google"), store.state.value.items.map { it.title })
        store.dispatch(VaultListIntent.Search("   "))
        assertEquals(2, store.state.value.items.size)
    }

    @Test
    fun `item click emits open effect`() = runTest {
        val effect = async(start = CoroutineStart.UNDISPATCHED) { store.effects.first() }

        store.dispatch(VaultListIntent.ItemClicked("item-id"))

        assertEquals(VaultListEffect.OpenItem("item-id"), effect.await())
    }

    @Test
    fun `lock locks the vault`() = runTest {
        session.unlock(VaultKey.random(FakeCryptoService()))

        store.dispatch(VaultListIntent.Lock)

        assertEquals(VaultSession.State.Locked, session.state)
    }

    @Test
    fun `reveal emits secret without storing it in state`() = runTest {
        val item = createItem("GitHub", "alice")
        repository.save(item)
        repository.emitItems()
        withTimeout(1_000) { store.state.first { !it.isLoading } }
        val effect = async(start = CoroutineStart.UNDISPATCHED) { store.effects.first() }

        store.dispatch(VaultListIntent.RevealSecret(item.id.value))

        val revealed = effect.await()
        assertEquals(VaultListEffect.SecretRevealed("secret-value"), revealed)
        assertFalse(revealed.toString().contains("secret-value"))
        assertTrue(store.state.value.items.single().hasSecret)
        assertFalse(store.state.value.toString().contains("secret-value"))
    }

    @Test
    fun `copy records secret and emits copied effect`() = runTest {
        val item = createItem("GitHub", "alice")
        repository.save(item)
        repository.emitItems()
        withTimeout(1_000) { store.state.first { !it.isLoading } }
        val effect = async(start = CoroutineStart.UNDISPATCHED) { store.effects.first() }

        store.dispatch(VaultListIntent.CopySecret(item.id.value))

        assertEquals(VaultListEffect.SecretCopied, effect.await())
        assertEquals(listOf("secret-value"), clipboard.values)
    }

    @Test
    fun `reveal for a missing item emits no effect`() = runTest {
        val effect = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(100) { store.effects.first() }
        }

        store.dispatch(
            VaultListIntent.RevealSecret("00000000-0000-4000-8000-000000000009"),
        )

        assertTrue(runCatching { effect.await() }.isFailure)
    }

    @Test
    fun `secret effect is not replayed to a new collector`() = runTest {
        val item = createItem("GitHub", "alice")
        repository.save(item)
        repository.emitItems()
        withTimeout(1_000) { store.state.first { !it.isLoading } }
        val firstEffect = async(start = CoroutineStart.UNDISPATCHED) { store.effects.first() }
        store.dispatch(VaultListIntent.RevealSecret(item.id.value))
        assertEquals(VaultListEffect.SecretRevealed("secret-value"), firstEffect.await())

        val replayedEffect = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(100) { store.effects.first() }
        }

        assertTrue(runCatching { replayedEffect.await() }.isFailure)
        assertTrue(store.effects.replayCache.isEmpty())
    }

    private fun createItem(
        title: String,
        username: String,
        id: String = "00000000-0000-4000-8000-000000000001",
    ): VaultItem {
        val result = VaultItem.create(
            id = (ItemId.of(id) as Result.Success).value,
            type = VaultItemType.LOGIN,
            title = (Title.of(title) as Result.Success).value,
            username = username,
            secret = (Secret.of("secret-value") as Result.Success).value,
            url = null,
            folderId = null,
            nowMillis = 1_700_000_000_000L,
        )
        return (result as Result.Success).value
    }
}
