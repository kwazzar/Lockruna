package com.kwazzar.lockruna.presentation.di

import com.kwazzar.lockruna.application.session.LockVault
import com.kwazzar.lockruna.application.session.ObserveVaultState
import com.kwazzar.lockruna.application.vault.CopySecret
import com.kwazzar.lockruna.application.vault.CreateItem
import com.kwazzar.lockruna.application.vault.DeleteItem
import com.kwazzar.lockruna.application.vault.GetItem
import com.kwazzar.lockruna.application.vault.ObserveItems
import com.kwazzar.lockruna.application.vault.RevealSecret
import com.kwazzar.lockruna.application.vault.UpdateItem
import com.kwazzar.lockruna.presentation.auth.LoginStore
import com.kwazzar.lockruna.presentation.auth.UnlockStore
import com.kwazzar.lockruna.presentation.session.SessionStore
import com.kwazzar.lockruna.presentation.vault.ItemDetailStore
import com.kwazzar.lockruna.presentation.vault.ItemEditStore
import com.kwazzar.lockruna.presentation.vault.VaultListStore
import org.koin.dsl.module

val presentationModule = module {
    factory {
        VaultListStore(
            ObserveItems(get()),
            ObserveVaultState(get()),
            LockVault(get()),
            RevealSecret(get()),
            CopySecret(get(), get()),
        )
    }
    factory { ItemDetailStore(GetItem(get()), RevealSecret(get()), CopySecret(get(), get()), DeleteItem(get(), get())) }
    factory { ItemEditStore(CreateItem(get(), get(), get()), UpdateItem(get(), get()), GetItem(get())) }
    factory { LoginStore(get(), get()) }
    factory { UnlockStore(get(), get(), get(), get()) }
    factory { SessionStore(get(), get(), get()) }
}
