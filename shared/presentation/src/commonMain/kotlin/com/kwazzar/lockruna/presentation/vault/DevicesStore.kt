package com.kwazzar.lockruna.presentation.vault

import com.kwazzar.lockruna.application.auth.ListDevices
import com.kwazzar.lockruna.application.auth.RevokeDevice
import com.kwazzar.lockruna.domain.auth.DeviceInfo
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.sync.Mutex

/** The devices of the account, so another device can be revoked (US-10). */
class DevicesStore(private val listDevices: ListDevices, private val revokeDevice: RevokeDevice) {
    private val _state = MutableStateFlow(DevicesState(isLoading = true))
    private val _effects = Channel<DevicesEffect>(Channel.BUFFERED)
    private val loadMutex = Mutex()
    private val revokeMutex = Mutex()

    val state = _state.asStateFlow()
    val effects: Flow<DevicesEffect> = _effects.receiveAsFlow()

    suspend fun dispatch(intent: DevicesIntent) {
        when (intent) {
            DevicesIntent.Load -> load()
            is DevicesIntent.Revoke -> revoke(intent.deviceId)
            DevicesIntent.ErrorDismissed -> _state.value = _state.value.copy(error = null)
        }
    }

    private suspend fun load() {
        if (!loadMutex.tryLock()) return
        try {
            _state.value = _state.value.copy(isLoading = true, error = null)
            _state.value = when (val result = listDevices()) {
                is Result.Failure -> _state.value.copy(isLoading = false, error = result.error)
                is Result.Success -> _state.value.copy(
                    isLoading = false,
                    devices = result.value,
                    revokingDeviceId = null,
                )
            }
        } finally {
            loadMutex.unlock()
        }
    }

    private suspend fun revoke(deviceId: String) {
        if (!revokeMutex.tryLock()) return
        try {
            _state.value = _state.value.copy(revokingDeviceId = deviceId, error = null)
            when (val result = revokeDevice(deviceId)) {
                is Result.Failure -> _state.value = _state.value.copy(
                    revokingDeviceId = null,
                    error = result.error,
                )

                is Result.Success -> {
                    _effects.send(DevicesEffect.DeviceRevoked)
                    // The server is the truth: ask it again instead of dropping the row locally.
                    load()
                }
            }
        } finally {
            revokeMutex.unlock()
        }
    }
}

data class DevicesState(
    val devices: List<DeviceInfo> = emptyList(),
    val isLoading: Boolean = false,
    val revokingDeviceId: String? = null,
    val error: AppError? = null,
)

sealed interface DevicesIntent {
    data object Load : DevicesIntent

    data class Revoke(val deviceId: String) : DevicesIntent

    data object ErrorDismissed : DevicesIntent
}

sealed interface DevicesEffect {
    data object DeviceRevoked : DevicesEffect
}
