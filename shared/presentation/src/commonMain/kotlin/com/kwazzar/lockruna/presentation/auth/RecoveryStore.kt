package com.kwazzar.lockruna.presentation.auth

import com.kwazzar.lockruna.application.auth.CompleteRecovery
import com.kwazzar.lockruna.application.auth.RecoverCommand
import com.kwazzar.lockruna.domain.auth.DevicePlatform
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.identity.MasterPassword
import com.kwazzar.lockruna.domain.identity.PasswordStrength
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.sync.Mutex

/**
 * Restoring access with the recovery key and a new master password (US-03).
 *
 * The recovery words and the new password never reach the observable state.
 */
class RecoveryStore(private val completeRecovery: CompleteRecovery, private val platform: DevicePlatform) {
    private val _state = MutableStateFlow(RecoveryState())
    private val _effects = Channel<RecoveryEffect>(Channel.BUFFERED)
    private var draftRecoveryWords: List<String> = emptyList()
    private var draftPassword: String = ""
    private val submitMutex = Mutex()

    val state = _state.asStateFlow()
    val effects: Flow<RecoveryEffect> = _effects.receiveAsFlow()

    fun close() {
        draftRecoveryWords = emptyList()
        draftPassword = ""
        _state.value = _state.value.copy(hasRecoveryWords = false, hasPassword = false, passwordStrength = null, strength = null)
    }

    suspend fun dispatch(intent: RecoveryIntent) {
        when (intent) {
            is RecoveryIntent.EmailChanged -> update { copy(email = intent.value) }
            is RecoveryIntent.DeviceNameChanged -> update { copy(deviceName = intent.value) }
            is RecoveryIntent.RecoveryWordsChanged -> {
                draftRecoveryWords = intent.words
                update { copy(hasRecoveryWords = intent.words.isNotEmpty()) }
            }

            is RecoveryIntent.NewPasswordChanged -> {
                draftPassword = intent.value
                update {
                    copy(
                        hasPassword = intent.value.isNotEmpty(),
                        passwordStrength = MasterPassword.strength(intent.value),
                        strength = MasterPassword.strength(intent.value),
                    )
                }
            }

            RecoveryIntent.Submit -> submit()
            RecoveryIntent.ErrorDismissed -> update { copy(error = null) }
        }
    }

    private suspend fun submit() {
        if (!submitMutex.tryLock()) return
        try {
            update { copy(isSubmitting = true, error = null) }
            val result = completeRecovery(
                RecoverCommand(
                    email = _state.value.email,
                    recoveryWords = draftRecoveryWords,
                    newPassword = draftPassword,
                    deviceName = _state.value.deviceName,
                    deviceId = null,
                    platform = platform,
                ),
            )
            when (result) {
                is Result.Failure -> {
                    update { copy(isSubmitting = false, error = result.error) }
                }

                is Result.Success -> {
                    draftRecoveryWords = emptyList()
                    draftPassword = ""
                    _state.value = _state.value.copy(
                        isSubmitting = false,
                        hasRecoveryWords = false,
                        hasPassword = false,
                        passwordStrength = null,
                        strength = null,
                    )
                    _effects.send(RecoveryEffect.Recovered)
                }
            }
        } finally {
            submitMutex.unlock()
        }
    }

    private fun update(transform: RecoveryState.() -> RecoveryState) {
        _state.value = _state.value.transform()
    }
}

data class RecoveryState(
    val email: String = "",
    val deviceName: String = "",
    val hasRecoveryWords: Boolean = false,
    val hasPassword: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: AppError? = null,
    val passwordStrength: PasswordStrength? = null,
    val strength: PasswordStrength? = null,
)

sealed interface RecoveryIntent {
    data class EmailChanged(val value: String) : RecoveryIntent

    data class DeviceNameChanged(val value: String) : RecoveryIntent

    /** The 12 words the user types in; they never reach the observable state. */
    data class RecoveryWordsChanged(val words: List<String>) : RecoveryIntent {
        override fun toString(): String = "RecoveryWordsChanged(count=${words.size})"
    }

    data class NewPasswordChanged(val value: String) : RecoveryIntent {
        override fun toString(): String = "NewPasswordChanged(value=<redacted>)"
    }

    data object Submit : RecoveryIntent

    data object ErrorDismissed : RecoveryIntent
}

sealed interface RecoveryEffect {
    data object Recovered : RecoveryEffect
}
