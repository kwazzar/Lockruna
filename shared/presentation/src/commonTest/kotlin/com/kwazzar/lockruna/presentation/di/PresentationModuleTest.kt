package com.kwazzar.lockruna.presentation.di

import com.kwazzar.lockruna.application.auth.EnableBiometrics
import com.kwazzar.lockruna.application.auth.Login
import com.kwazzar.lockruna.application.auth.UnlockWithBiometrics
import com.kwazzar.lockruna.application.auth.UnlockWithPassword
import com.kwazzar.lockruna.application.session.ApplyAutoLockPolicy
import com.kwazzar.lockruna.application.session.LockVault
import com.kwazzar.lockruna.application.session.ObserveVaultState
import com.kwazzar.lockruna.domain.auth.AuthApi
import com.kwazzar.lockruna.domain.auth.AuthStateStore
import com.kwazzar.lockruna.domain.auth.BiometricPolicyStore
import com.kwazzar.lockruna.domain.auth.DevicePlatform
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.crypto.Argon2
import com.kwazzar.lockruna.domain.crypto.CryptoService
import com.kwazzar.lockruna.domain.crypto.PasswordNormalizer
import com.kwazzar.lockruna.domain.session.AutoLockPolicy
import com.kwazzar.lockruna.domain.session.AutoLockSettingsStore
import com.kwazzar.lockruna.domain.session.BiometricGate
import com.kwazzar.lockruna.domain.session.Clock
import com.kwazzar.lockruna.domain.session.IdGenerator
import com.kwazzar.lockruna.domain.session.SecureKeyStore
import com.kwazzar.lockruna.domain.session.VaultSession
import com.kwazzar.lockruna.domain.vault.Clipboard
import com.kwazzar.lockruna.domain.vault.ItemId
import com.kwazzar.lockruna.domain.vault.VaultItemRepository
import com.kwazzar.lockruna.presentation.auth.LoginStore
import com.kwazzar.lockruna.presentation.auth.UnlockStore
import com.kwazzar.lockruna.presentation.session.SessionStore
import com.kwazzar.lockruna.presentation.support.FakeArgon2
import com.kwazzar.lockruna.presentation.support.FakeAuthApi
import com.kwazzar.lockruna.presentation.support.FakeAuthStateStore
import com.kwazzar.lockruna.presentation.support.FakeBiometricGate
import com.kwazzar.lockruna.presentation.support.FakeBiometricPolicyStore
import com.kwazzar.lockruna.presentation.support.FakeClock
import com.kwazzar.lockruna.presentation.support.FakeCryptoService
import com.kwazzar.lockruna.presentation.support.FakePasswordNormalizer
import com.kwazzar.lockruna.presentation.support.FakeSecureKeyStore
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
        assertNotNull(koin.get<LoginStore>())
        assertNotNull(koin.get<UnlockStore>())
        assertNotNull(koin.get<SessionStore>())
    }

    @Test
    fun `stores are created per screen`() {
        val koin = startKoin {
            modules(dependencies, presentationModule)
        }.koin

        assertNotSame(koin.get<VaultListStore>(), koin.get<VaultListStore>())
        assertNotSame(koin.get<ItemDetailStore>(), koin.get<ItemDetailStore>())
        assertNotSame(koin.get<ItemEditStore>(), koin.get<ItemEditStore>())
        assertNotSame(koin.get<LoginStore>(), koin.get<LoginStore>())
        assertNotSame(koin.get<UnlockStore>(), koin.get<UnlockStore>())
        assertNotSame(koin.get<SessionStore>(), koin.get<SessionStore>())
    }

    private val dependencies = module {
        single<VaultItemRepository> { InMemoryVaultItemRepository() }
        single<Clipboard> { RecordingClipboard() }
        single<Clock> { FakeClock() }
        single<IdGenerator> { FixedIdGenerator() }
        single { VaultSession() }
        single { AutoLockPolicy.ONE_MINUTE }
        single { DevicePlatform.ANDROID }
        single<AuthApi> { FakeAuthApi() }
        single<Argon2> { FakeArgon2() }
        single<PasswordNormalizer> { FakePasswordNormalizer() }
        single<CryptoService> { FakeCryptoService() }
        single<AuthStateStore> { FakeAuthStateStore() }
        single<BiometricGate> { FakeBiometricGate() }
        single<SecureKeyStore> { FakeSecureKeyStore() }
        single<BiometricPolicyStore> { FakeBiometricPolicyStore() }
        single { Login(get(), get(), get(), get(), get(), get(), get(), get()) }
        single { UnlockWithPassword(get(), get(), get(), get(), get(), get(), get()) }
        single { UnlockWithBiometrics(get(), get(), get(), get(), get()) }
        single { EnableBiometrics(get(), get(), get(), get(), get()) }
        single<AutoLockSettingsStore> {
            object : AutoLockSettingsStore {
                override suspend fun load(): AutoLockPolicy = AutoLockPolicy.ONE_MINUTE
                override suspend fun save(policy: AutoLockPolicy) = Unit
            }
        }
        single { ObserveVaultState(get()) }
        single { LockVault(get()) }
        single { ApplyAutoLockPolicy(get(), get(), get()) }
    }

    private class FixedIdGenerator : IdGenerator {
        override fun newItemId(): ItemId = (ItemId.of(ITEM_ID) as Result.Success).value

        override fun newUuid(): String = ITEM_ID
    }

    private companion object {
        const val ITEM_ID = "00000000-0000-4000-8000-000000000001"
    }
}
