package com.kwazzar.lockruna.presentation.di

import com.kwazzar.lockruna.application.session.LockVault
import com.kwazzar.lockruna.application.vault.CopySecret
import com.kwazzar.lockruna.application.vault.CreateItem
import com.kwazzar.lockruna.application.vault.DeleteItem
import com.kwazzar.lockruna.application.vault.GetItem
import com.kwazzar.lockruna.application.vault.ObserveItems
import com.kwazzar.lockruna.application.vault.RevealSecret
import com.kwazzar.lockruna.application.vault.UpdateItem
import com.kwazzar.lockruna.presentation.vault.ItemDetailStore
import com.kwazzar.lockruna.presentation.vault.ItemEditStore
import com.kwazzar.lockruna.presentation.vault.VaultListStore
import org.koin.dsl.module

val presentationModule = module {
    single { VaultListStore(ObserveItems(get()), LockVault(get()), RevealSecret(get()), CopySecret(get(), get())) }
    single { ItemDetailStore(GetItem(get()), RevealSecret(get()), CopySecret(get(), get()), DeleteItem(get(), get())) }
    single { ItemEditStore(CreateItem(get(), get(), get()), UpdateItem(get(), get()), GetItem(get())) }
}
