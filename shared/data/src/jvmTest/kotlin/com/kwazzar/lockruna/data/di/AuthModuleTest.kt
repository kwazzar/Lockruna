package com.kwazzar.lockruna.data.di

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kwazzar.lockruna.application.auth.CompleteRecovery
import com.kwazzar.lockruna.application.auth.DeleteAccount
import com.kwazzar.lockruna.application.auth.EnableBiometrics
import com.kwazzar.lockruna.application.auth.HasStoredSession
import com.kwazzar.lockruna.application.auth.ListDevices
import com.kwazzar.lockruna.application.auth.Login
import com.kwazzar.lockruna.application.auth.Logout
import com.kwazzar.lockruna.application.auth.Register
import com.kwazzar.lockruna.application.auth.RevokeDevice
import com.kwazzar.lockruna.application.auth.UnlockWithBiometrics
import com.kwazzar.lockruna.application.auth.UnlockWithPassword
import com.kwazzar.lockruna.data.db.PassKeeperDatabase
import com.kwazzar.lockruna.data.session.FixedAutoLockSettingsStore
import com.kwazzar.lockruna.data.session.FixedBiometricPolicyStore
import com.kwazzar.lockruna.domain.auth.AccountApi
import com.kwazzar.lockruna.domain.auth.AuthApi
import com.kwazzar.lockruna.domain.auth.DeviceApi
import com.kwazzar.lockruna.domain.auth.RecoveryApi
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.BiometricGate
import com.kwazzar.lockruna.domain.session.SecureKeyStore
import com.kwazzar.lockruna.domain.sync.AccessTokenProvider
import com.kwazzar.lockruna.domain.vault.Clipboard
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The auth graph must resolve without a stack overflow: every type has exactly one
 * definition, so a port that points at another definition cannot point at itself.
 */
class AuthModuleTest {

    @Test
    fun `every auth port and use case resolves`() = runTest {
        val application = authGraph()

        try {
            val koin = application.koin
            assertNotNull(koin.get<AuthApi>())
            assertNotNull(koin.get<RecoveryApi>())
            assertNotNull(koin.get<DeviceApi>())
            assertNotNull(koin.get<AccountApi>())
            assertNotNull(koin.get<AccessTokenProvider>())
            assertNotNull(koin.get<Login>())
            assertNotNull(koin.get<Register>())
            assertNotNull(koin.get<EnableBiometrics>())
            assertNotNull(koin.get<CompleteRecovery>())
            assertNotNull(koin.get<DeleteAccount>())
            assertNotNull(koin.get<ListDevices>())
            assertNotNull(koin.get<RevokeDevice>())
            assertNotNull(koin.get<UnlockWithPassword>())
            assertNotNull(koin.get<UnlockWithBiometrics>())
            assertNotNull(koin.get<Logout>())
            assertNotNull(koin.get<HasStoredSession>())
        } finally {
            application.close()
        }
    }

    /**
     * The use cases the apps resolve at startup are covered by
     * `IosPlatformGraphTest`, which can see the real iOS platform module. The JVM has
     * no `platformModule()`, so a store graph test here would need a fake platform and
     * would prove nothing about the real bindings.
     */
    @Test
    fun `every use case from the auth flow resolves against the data graph`() = runTest {
        val application = authGraph()

        try {
            val koin = application.koin
            assertNotNull(koin.get<Login>())
            assertNotNull(koin.get<Register>())
            assertNotNull(koin.get<EnableBiometrics>())
            assertNotNull(koin.get<UnlockWithBiometrics>())
            assertNotNull(koin.get<CompleteRecovery>())
            assertNotNull(koin.get<DeleteAccount>())
            assertNotNull(koin.get<ListDevices>())
            assertNotNull(koin.get<RevokeDevice>())
        } finally {
            application.close()
        }
    }

    @Test
    fun `the auth api and the recovery api are different instances`() = runTest {
        val application = authGraph()

        try {
            val koin = application.koin
            assertTrue(koin.get<AuthApi>() !== koin.get<RecoveryApi>())
        } finally {
            application.close()
        }
    }

    @Test
    fun `the access token comes from the stored session`() = runTest {
        val application = authGraph()

        try {
            val provider = application.koin.get<AccessTokenProvider>()
            assertIs<Result<Unit, AppError>>(runBlocking { Result.Success(Unit) })
            assertTrue(runBlocking { provider.token() == null })
        } finally {
            application.close()
        }
    }

    private fun mockClient(): HttpClient =
        HttpClient(MockEngine) { engine { addHandler { respond("", HttpStatusCode.OK) } } }

    private fun mockClientModule() = module {
        single {
            PassKeeperDatabase(
                JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { PassKeeperDatabase.Schema.create(it) },
            )
        }
    }

    private fun authGraph(): KoinApplication = koinApplication {
        modules(
            coreModule(),
            mockClientModule(),
            platformBindings(),
            authModule(mockClient(), "https://api.example.com"),
            syncModule(mockClient(), "https://api.example.com"),
        )
    }

    /** What the Android and iOS platform modules bind, minus the platform itself. */
    private fun platformBindings() = module {
        single<Clipboard> { com.kwazzar.lockruna.data.di.FakeClipboard() }
        single<com.kwazzar.lockruna.domain.auth.AuthStateStore> {
            com.kwazzar.lockruna.data.auth.InMemoryAuthStateStore()
        }
        single<com.kwazzar.lockruna.domain.session.AutoLockSettingsStore> { FixedAutoLockSettingsStore() }
        single<com.kwazzar.lockruna.domain.auth.BiometricPolicyStore> { FixedBiometricPolicyStore() }
        single<BiometricGate> {
            object : BiometricGate {
                override suspend fun isAvailable() = false
                override suspend fun authenticate(reason: String) = false
            }
        }
        single<SecureKeyStore> {
            object : SecureKeyStore {
                override suspend fun saveVaultKey(key: com.kwazzar.lockruna.domain.session.VaultKey) = Unit
                override suspend fun loadVaultKey(): com.kwazzar.lockruna.domain.session.VaultKey? = null
                override suspend fun clear() = Unit
            }
        }
        single { com.kwazzar.lockruna.domain.auth.DevicePlatform.IOS }
    }
}
