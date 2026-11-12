package com.kwazzar.lockruna.presentation.auth

import com.kwazzar.lockruna.application.auth.Register
import com.kwazzar.lockruna.application.auth.RegisterCommand
import com.kwazzar.lockruna.domain.auth.DevicePlatform
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.sync.Mutex

/**
 * Registration and the one-time recovery key walk-through (US-01, US-03).
 *
 * The master password and the recovery words stay in private fields: they must never
 * reach the observable state, its `toString()` or a log.
 */
class RegisterStore(private val register: Register, private val platform: DevicePlatform) {
    private val _state = MutableStateFlow(RegisterState())
    private val _effects = Channel<RegisterEffect>(Channel.BUFFERED)
    private var draftPassword: String = ""
    private var draftConfirmation: String = ""
    private var recoveryWords: List<String> = emptyList()
    private val submitMutex = Mutex()

    val state = _state.asStateFlow()
    val effects: Flow<RegisterEffect> = _effects.receiveAsFlow()

    /** The screen is gone: drop the draft password and the words. */
    fun close() {
        draftPassword = ""
        draftConfirmation = ""
        recoveryWords = emptyList()
        _state.value = _state.value.copy(hasPassword = false, hasRecoveryWords = false)
    }

    suspend fun dispatch(intent: RegisterIntent) {
        when (intent) {
            is RegisterIntent.EmailChanged -> update { copy(email = intent.value) }
            is RegisterIntent.PasswordChanged -> {
                draftPassword = intent.value
                update { copy(hasPassword = intent.value.isNotEmpty()) }
            }

            is RegisterIntent.DeviceNameChanged -> update { copy(deviceName = intent.value) }
            is RegisterIntent.ConfirmationChanged -> draftConfirmation = intent.value
            RegisterIntent.Submit -> submit()
            RegisterIntent.ConfirmRecoveryWords -> confirmRecoveryWords()
            RegisterIntent.ErrorDismissed -> update { copy(error = null) }
        }
    }

    private suspend fun submit() {
        if (!submitMutex.tryLock()) return
        try {
            update { copy(isSubmitting = true, error = null) }
            val result = register(
                RegisterCommand(
                    email = _state.value.email,
                    password = draftPassword,
                    deviceName = _state.value.deviceName,
                    deviceId = null,
                    platform = platform,
                ),
            )
            when (result) {
                is Result.Failure -> update { copy(isSubmitting = false, error = result.error) }

                is Result.Success -> {
                    recoveryWords = result.value.recoveryWords
                    draftPassword = ""
                    _state.value = _state.value.copy(
                        isSubmitting = false,
                        hasPassword = false,
                        hasRecoveryWords = true,
                    )
                    _effects.send(RegisterEffect.RecoveryKeyReady(recoveryWords))
                }
            }
        } finally {
            submitMutex.unlock()
        }
    }

    private suspend fun confirmRecoveryWords() {
        if (recoveryWords.isEmpty()) return
        val typed = draftConfirmation.lowercase().split(" ").filter { it.isNotBlank() }
        if (typed != recoveryWords) {
            update { copy(error = AppError.InvalidRecoveryKey("the typed words do not match")) }
            return
        }
        // The user proved they kept the key: the store drops it and the screen moves on.
        draftConfirmation = ""
        recoveryWords = emptyList()
        _state.value = _state.value.copy(hasRecoveryWords = false)
        _effects.send(RegisterEffect.Registered)
    }

    private fun update(transform: RegisterState.() -> RegisterState) {
        _state.value = _state.value.transform()
    }
}

data class RegisterState(
    val email: String = "",
    val deviceName: String = "",
    val hasPassword: Boolean = false,
    val hasRecoveryWords: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: AppError? = null,
)

sealed interface RegisterIntent {
    data class EmailChanged(val value: String) : RegisterIntent

    data class PasswordChanged(val value: String) : RegisterIntent {
        override fun toString(): String = "PasswordChanged(value=<redacted>)"
    }

    data class DeviceNameChanged(val value: String) : RegisterIntent

    data class ConfirmationChanged(val value: String) : RegisterIntent {
        override fun toString(): String = "ConfirmationChanged(value=<redacted>)"
    }

    data object Submit : RegisterIntent

    data object ConfirmRecoveryWords : RegisterIntent

    data object ErrorDismissed : RegisterIntent
}

sealed interface RegisterEffect {
    /** The 12 words, shown once. The user proves they kept them by typing them back (US-03). */
    data class RecoveryKeyReady(val words: List<String>) : RegisterEffect {
        /** The words are the recovery key: never log, print or snapshot them. */
        override fun toString(): String = "RecoveryKeyReady(size=${words.size})"
    }

    data object Registered : RegisterEffect
}
