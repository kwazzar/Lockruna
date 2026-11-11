package com.kwazzar.lockruna.presentation.auth

import com.kwazzar.lockruna.application.auth.CredentialsCommand
import com.kwazzar.lockruna.application.auth.Login
import com.kwazzar.lockruna.domain.auth.DevicePlatform
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex

class LoginStore(private val login: Login, private val platform: DevicePlatform) {
    private val _state = MutableStateFlow(LoginState())
    private val _effects = MutableSharedFlow<LoginEffect>(extraBufferCapacity = 0, replay = 0)
    private var draftPassword: String = ""
    private val submitMutex = Mutex()

    val state = _state.asStateFlow()
    val effects = _effects.asSharedFlow()

    /** The screen is gone: drop the draft password. The store starts no background work. */
    fun close() {
        draftPassword = ""
        _state.value = _state.value.copy(hasPassword = false)
    }

    suspend fun dispatch(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.EmailChanged -> _state.value = _state.value.copy(email = intent.value)
            is LoginIntent.PasswordChanged -> {
                draftPassword = intent.value
                _state.value = _state.value.copy(hasPassword = intent.value.isNotEmpty())
            }

            is LoginIntent.DeviceNameChanged -> _state.value = _state.value.copy(deviceName = intent.value)
            LoginIntent.Submit -> submit()
            LoginIntent.ErrorDismissed -> _state.value = _state.value.copy(error = null)
        }
    }

    private suspend fun submit() {
        if (!submitMutex.tryLock()) return
        try {
            _state.value = _state.value.copy(isSubmitting = true, error = null)
            val result = login(
                CredentialsCommand(
                    email = _state.value.email,
                    password = draftPassword,
                    deviceName = _state.value.deviceName,
                    deviceId = null,
                    platform = platform,
                ),
            )
            when (result) {
                is Result.Success -> {
                    draftPassword = ""
                    _state.value = _state.value.copy(isSubmitting = false, hasPassword = false)
                    _effects.emit(LoginEffect.LoggedIn)
                }

                is Result.Failure -> {
                    _state.value = _state.value.copy(isSubmitting = false, error = result.error)
                }
            }
        } finally {
            submitMutex.unlock()
        }
    }
}

data class LoginState(
    val email: String = "",
    val deviceName: String = "",
    val hasPassword: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: AppError? = null,
)

sealed interface LoginIntent {
    data class EmailChanged(val value: String) : LoginIntent

    data class PasswordChanged(val value: String) : LoginIntent {
        override fun toString(): String = "PasswordChanged(value=<redacted>)"
    }

    data class DeviceNameChanged(val value: String) : LoginIntent

    data object Submit : LoginIntent

    data object ErrorDismissed : LoginIntent
}

sealed interface LoginEffect {
    data object LoggedIn : LoginEffect
}
