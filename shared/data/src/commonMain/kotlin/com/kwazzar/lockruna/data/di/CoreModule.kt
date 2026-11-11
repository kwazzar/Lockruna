package com.kwazzar.lockruna.data.di

import com.kwazzar.lockruna.application.session.ApplyAutoLockPolicy
import com.kwazzar.lockruna.application.session.LockVault
import com.kwazzar.lockruna.application.session.ObserveVaultState
import com.kwazzar.lockruna.application.vault.CopySecret
import com.kwazzar.lockruna.application.vault.CreateItem
import com.kwazzar.lockruna.application.vault.DeleteItem
import com.kwazzar.lockruna.application.vault.GetItem
import com.kwazzar.lockruna.application.vault.ObserveItems
import com.kwazzar.lockruna.application.vault.RevealSecret
import com.kwazzar.lockruna.application.vault.UpdateItem
import com.kwazzar.lockruna.data.crypto.Argon2Factory
import com.kwazzar.lockruna.data.crypto.PlatformCrypto
import com.kwazzar.lockruna.data.crypto.PlatformNormalizer
import com.kwazzar.lockruna.data.db.DatabaseDriverFactory
import com.kwazzar.lockruna.data.db.PassKeeperDatabase
import com.kwazzar.lockruna.data.db.SqlDelightVaultItemRepository
import com.kwazzar.lockruna.data.platform.platformClock
import com.kwazzar.lockruna.data.platform.platformIdGenerator
import com.kwazzar.lockruna.domain.crypto.Argon2
import com.kwazzar.lockruna.domain.crypto.CryptoService
import com.kwazzar.lockruna.domain.crypto.PasswordNormalizer
import com.kwazzar.lockruna.domain.session.AutoLockPolicy
import com.kwazzar.lockruna.domain.session.Clock
import com.kwazzar.lockruna.domain.session.IdGenerator
import com.kwazzar.lockruna.domain.session.VaultSession
import com.kwazzar.lockruna.domain.vault.VaultItemRepository
import org.koin.dsl.module

/**
 * Koin bindings of the client core: clock, ids, crypto, the local database and the vault use cases.
 * The platform module owns the driver factory, the clipboard and the secure storages.
 */
fun coreModule(autoLockPolicy: AutoLockPolicy = AutoLockPolicy.ONE_MINUTE) = module {
    single<Clock> { platformClock() }
    single<IdGenerator> { platformIdGenerator() }
    single { VaultSession() }
    single { PlatformCrypto() }
    single<CryptoService> { get<PlatformCrypto>() }
    single { PlatformNormalizer() }
    single<PasswordNormalizer> { get<PlatformNormalizer>() }
    single<Argon2> { Argon2Factory.create() }
    single { PassKeeperDatabase(get<DatabaseDriverFactory>().create()) }
    single<VaultItemRepository> { SqlDelightVaultItemRepository(get(), get(), get(), get()) }
    single { CreateItem(get(), get(), get()) }
    single { UpdateItem(get(), get()) }
    single { DeleteItem(get(), get()) }
    single { GetItem(get()) }
    single { ObserveItems(get()) }
    single { RevealSecret(get()) }
    single { CopySecret(get(), get()) }
    single { ObserveVaultState(get()) }
    single { LockVault(get()) }
    single { ApplyAutoLockPolicy(get(), get(), autoLockPolicy) }
}
