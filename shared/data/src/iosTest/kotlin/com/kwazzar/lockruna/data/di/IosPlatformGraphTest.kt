package com.kwazzar.lockruna.data.di

import com.kwazzar.lockruna.application.auth.CompleteRecovery
import com.kwazzar.lockruna.application.auth.DeleteAccount
import com.kwazzar.lockruna.application.auth.EnableBiometrics
import com.kwazzar.lockruna.application.auth.ListDevices
import com.kwazzar.lockruna.application.auth.Login
import com.kwazzar.lockruna.application.auth.Register
import com.kwazzar.lockruna.application.auth.RevokeDevice
import com.kwazzar.lockruna.application.auth.UnlockWithBiometrics
import com.kwazzar.lockruna.domain.auth.AccountApi
import com.kwazzar.lockruna.domain.auth.AuthApi
import com.kwazzar.lockruna.domain.auth.DeviceApi
import com.kwazzar.lockruna.domain.auth.RecoveryApi
import com.kwazzar.lockruna.domain.sync.AccessTokenProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * The iOS platform bindings, assembled with the real modules. A binding the platform
 * module forgot — a missing `BiometricPolicyStore` for instance — has to fail here
 * instead of at app startup on a device.
 */
class IosPlatformGraphTest {

    @Test
    fun `the ios graph resolves the whole auth surface`() = runTest {
        val application = graph()

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
            assertNotNull(koin.get<UnlockWithBiometrics>())
            assertNotNull(koin.get<CompleteRecovery>())
            assertNotNull(koin.get<DeleteAccount>())
            assertNotNull(koin.get<ListDevices>())
            assertNotNull(koin.get<RevokeDevice>())
        } finally {
            application.close()
        }
    }

    private fun graph(): KoinApplication {
        val client = HttpClient(MockEngine) { engine { addHandler { respond("", HttpStatusCode.OK) } } }
        return koinApplication {
            modules(
                coreModule(),
                iosPlatformModule(),
                authModule(client, "https://api.example.com"),
                syncModule(client, "https://api.example.com"),
            )
        }
    }
}
