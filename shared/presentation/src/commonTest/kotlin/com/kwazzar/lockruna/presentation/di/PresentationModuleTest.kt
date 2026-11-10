package com.kwazzar.lockruna.presentation.di

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.session.Clock
import com.kwazzar.lockruna.domain.session.IdGenerator
import com.kwazzar.lockruna.domain.session.VaultSession
import com.kwazzar.lockruna.domain.vault.Clipboard
import com.kwazzar.lockruna.domain.vault.ItemId
import com.kwazzar.lockruna.domain.vault.VaultItemRepository
import com.kwazzar.lockruna.presentation.support.FakeClock
import com.kwazzar.lockruna.presentation.support.RecordingClipboard
import com.kwazzar.lockruna.presentation.vault.InMemoryVaultItemRepository
import com.kwazzar.lockruna.presentation.vault.ItemDetailStore
import com.kwazzar.lockruna.presentation.vault.ItemEditStore
import com.kwazzar.lockruna.presentation.vault.VaultListStore
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame

class PresentationModuleTest {

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `presentation module resolves every store`() {
        val koin = startKoin {
            modules(dependencies, presentationModule)
        }.koin

        assertNotNull(koin.get<VaultListStore>())
        assertNotNull(koin.get<ItemDetailStore>())
        assertNotNull(koin.get<ItemEditStore>())
    }

    @Test
    fun `stores are created per screen`() {
        val koin = startKoin {
            modules(dependencies, presentationModule)
        }.koin

        assertNotSame(koin.get<VaultListStore>(), koin.get<VaultListStore>())
        assertNotSame(koin.get<ItemDetailStore>(), koin.get<ItemDetailStore>())
        assertNotSame(koin.get<ItemEditStore>(), koin.get<ItemEditStore>())
    }

    private val dependencies = module {
        single<VaultItemRepository> { InMemoryVaultItemRepository() }
        single<Clipboard> { RecordingClipboard() }
        single<Clock> { FakeClock() }
        single<IdGenerator> { FixedIdGenerator() }
        single { VaultSession() }
    }

    private class FixedIdGenerator : IdGenerator {
        override fun newItemId(): ItemId = (ItemId.of(ITEM_ID) as Result.Success).value

        override fun newUuid(): String = ITEM_ID
    }

    private companion object {
        const val ITEM_ID = "00000000-0000-4000-8000-000000000001"
    }
}
