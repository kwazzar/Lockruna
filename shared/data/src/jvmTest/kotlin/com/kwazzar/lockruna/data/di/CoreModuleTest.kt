package com.kwazzar.lockruna.data.di

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kwazzar.lockruna.application.session.ApplyAutoLockPolicy
import com.kwazzar.lockruna.application.session.ObserveVaultState
import com.kwazzar.lockruna.application.vault.CopySecret
import com.kwazzar.lockruna.application.vault.CreateItem
import com.kwazzar.lockruna.data.db.PassKeeperDatabase
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.AutoLockPolicy
import com.kwazzar.lockruna.domain.session.AutoLockSettingsStore
import com.kwazzar.lockruna.domain.session.Clock
import com.kwazzar.lockruna.domain.session.VaultKey
import com.kwazzar.lockruna.domain.session.VaultSession
import com.kwazzar.lockruna.domain.vault.Clipboard
import com.kwazzar.lockruna.domain.vault.VaultItemRepository
import kotlinx.coroutines.test.runTest
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoreModuleTest {

    @Test
    fun `the core graph resolves`() = runTest {
        val application = graph(AutoLockPolicy.ONE_MINUTE)

        try {
            val koin = application.koin
            assertNotNull(koin.get<VaultItemRepository>())
            assertNotNull(koin.get<CreateItem>())
            assertNotNull(koin.get<CopySecret>())
            assertNotNull(koin.get<ObserveVaultState>())
            assertNotNull(koin.get<ApplyAutoLockPolicy>())
        } finally {
            application.close()
        }
    }

    @Test
    fun `the auto lock policy is the one the settings store holds`() = runTest {
        val immediate = graph(AutoLockPolicy.IMMEDIATE)
        val oneMinute = graph(AutoLockPolicy.ONE_MINUTE)

        try {
            val immediateSession = immediate.koin.get<VaultSession>()
            immediateSession.unlock(VaultKey.from(ByteArray(32)))
            assertTrue(immediate.koin.get<ApplyAutoLockPolicy>()(0L))
            assertNull(immediateSession.currentVaultKey())

            val keptSession = oneMinute.koin.get<VaultSession>()
            keptSession.unlock(VaultKey.from(ByteArray(32)))
            val backgroundedAt = oneMinute.koin.get<Clock>().monotonicMillis() - 1_000
            assertFalse(oneMinute.koin.get<ApplyAutoLockPolicy>()(backgroundedAt))
            assertNotNull(keptSession.currentVaultKey())
        } finally {
            immediate.close()
            oneMinute.close()
        }
    }

    private fun graph(autoLockPolicy: AutoLockPolicy): KoinApplication {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        PassKeeperDatabase.Schema.create(driver)
        val database = PassKeeperDatabase(driver)
        return koinApplication {
            modules(
                coreModule(),
                module {
                    single { database }
                    single<Clipboard> { FakeClipboard() }
                    // The platform module owns this binding in the real apps.
                    single<AutoLockSettingsStore> {
                        object : AutoLockSettingsStore {
                            override suspend fun load(): AutoLockPolicy = autoLockPolicy
                            override suspend fun save(policy: AutoLockPolicy) = Unit
                        }
                    }
                },
            )
        }
    }
}

private class FakeClipboard : Clipboard {
    override suspend fun copySecret(value: String): Result<Unit, AppError> = Result.Success(Unit)
}
