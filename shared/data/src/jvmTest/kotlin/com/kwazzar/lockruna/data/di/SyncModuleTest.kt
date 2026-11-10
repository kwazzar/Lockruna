package com.kwazzar.lockruna.data.di

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kwazzar.lockruna.application.sync.SyncNow
import com.kwazzar.lockruna.data.db.FakeClock
import com.kwazzar.lockruna.data.db.PassKeeperDatabase
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.session.Clock
import com.kwazzar.lockruna.domain.session.IdGenerator
import com.kwazzar.lockruna.domain.sync.AccessTokenProvider
import com.kwazzar.lockruna.domain.sync.SyncApi
import com.kwazzar.lockruna.domain.sync.SyncStore
import com.kwazzar.lockruna.domain.vault.ItemId
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertNotNull

private const val TEST_UUID = "00000000-0000-4000-8000-000000000001"

private object TestIdGenerator : IdGenerator {
    override fun newItemId(): ItemId = when (val parsed = ItemId.of(TEST_UUID)) {
        is Result.Success -> parsed.value
        is Result.Failure -> error("test uuid must be valid")
    }

    override fun newUuid(): String = TEST_UUID
}

class SyncModuleTest {

    @Test
    fun `the sync graph resolves`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        PassKeeperDatabase.Schema.create(driver)
        val database = PassKeeperDatabase(driver)
        val application = koinApplication {
            modules(
                syncModule(
                    httpClient = HttpClient(MockEngine { error("no request expected") }),
                    baseUrl = "https://api.test",
                    accessTokenProvider = object : AccessTokenProvider {
                        override suspend fun token(): String? = "token"
                    },
                ),
                module {
                    single { database }
                    single<IdGenerator> { TestIdGenerator }
                    single<Clock> { FakeClock() }
                },
            )
        }

        try {
            assertNotNull(application.koin.get<SyncApi>())
            assertNotNull(application.koin.get<SyncStore>())
            assertNotNull(application.koin.get<SyncNow>())
        } finally {
            application.close()
        }
    }
}
