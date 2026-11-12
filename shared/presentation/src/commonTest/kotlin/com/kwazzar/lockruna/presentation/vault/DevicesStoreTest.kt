package com.kwazzar.lockruna.presentation.vault

import com.kwazzar.lockruna.application.auth.ListDevices
import com.kwazzar.lockruna.application.auth.RevokeDevice
import com.kwazzar.lockruna.domain.auth.DeviceApi
import com.kwazzar.lockruna.domain.auth.DeviceInfo
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DevicesStoreTest {

    private val deviceApi = FakeDeviceApi()
    private val store = DevicesStore(ListDevices(deviceApi), RevokeDevice(deviceApi))

    @Test
    fun `loading shows the devices`() = runTest {
        store.dispatch(DevicesIntent.Load)
        awaitIdle()

        assertEquals(2, store.state.value.devices.size)
        assertEquals("device-1", store.state.value.devices.first().id)
        assertTrue(store.state.value.devices.first().current)
        assertFalse(store.state.value.isLoading)
        assertNull(store.state.value.error)
    }

    @Test
    fun `revoking a device reloads the list from the server`() = runTest {
        store.dispatch(DevicesIntent.Load)
        awaitIdle()
        val effect = async(start = CoroutineStart.UNDISPATCHED) { store.effects.first() }

        store.dispatch(DevicesIntent.Revoke("device-2"))

        assertEquals(DevicesEffect.DeviceRevoked, effect.await())
        awaitIdle()
        assertEquals("device-2", deviceApi.lastRevokedDeviceId)
        // The revoked device is gone from what the server reports.
        assertEquals(listOf("device-1"), store.state.value.devices.map { it.id })
    }

    @Test
    fun `a failing list is reported`() = runTest {
        deviceApi.listResult = Result.Failure(AppError.NetworkUnavailable)

        store.dispatch(DevicesIntent.Load)
        awaitIdle()

        assertEquals(AppError.NetworkUnavailable, store.state.value.error)
        assertTrue(store.state.value.devices.isEmpty())
    }

    @Test
    fun `a failing revoke keeps the device in the list`() = runTest {
        store.dispatch(DevicesIntent.Load)
        awaitIdle()
        deviceApi.revokeResult = Result.Failure(AppError.NetworkUnavailable)

        store.dispatch(DevicesIntent.Revoke("device-2"))
        awaitIdle()

        assertEquals(AppError.NetworkUnavailable, store.state.value.error)
        assertEquals(2, store.state.value.devices.size)
    }

    private suspend fun awaitIdle() = withContext(Dispatchers.Default) {
        withTimeout(5_000) {
            while (store.state.value.isLoading) delay(10)
        }
    }
}

private class FakeDeviceApi : DeviceApi {
    private val devices = mutableListOf(
        DeviceInfo(id = "device-1", name = "Phone", platform = "ios", lastSeen = null, current = true),
        DeviceInfo(id = "device-2", name = "Tablet", platform = "android", lastSeen = null, current = false),
    )
    var listResult: Result<List<DeviceInfo>, AppError> = Result.Success(devices.toList())
    var revokeResult: Result<Unit, AppError> = Result.Success(Unit)
    var lastRevokedDeviceId: String? = null

    override suspend fun list(): Result<List<DeviceInfo>, AppError> = listResult

    override suspend fun revoke(deviceId: String): Result<Unit, AppError> {
        lastRevokedDeviceId = deviceId
        if (revokeResult is Result.Success) {
            devices.removeAll { it.id == deviceId }
            listResult = Result.Success(devices.toList())
        }
        return revokeResult
    }
}
