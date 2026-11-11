package com.kwazzar.lockruna.presentation.auth

import com.kwazzar.lockruna.application.auth.EnableBiometrics
import com.kwazzar.lockruna.application.auth.UnlockWithBiometrics
import com.kwazzar.lockruna.application.auth.UnlockWithPassword
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.BiometricGate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

class UnlockStore(
    private val unlockWithPassword: UnlockWithPassword,
    private val unlockWithBiometrics: UnlockWithBiometrics,
    private val enableBiometrics: EnableBiometrics,
    private val biometricGate: BiometricGate,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(UnlockState())
    private val _effects = Channel<UnlockEffect>(Channel.BUFFERED)
    private var draftPassword: String = ""
    private val unlockMutex = Mutex()

    val state = _state.asStateFlow()
    val effects: Flow<UnlockEffect> = _effects.receiveAsFlow()

    init {
        scope.launch {
            val available = biometricGate.isAvailable()
            _state.value = _state.value.copy(biometricAvailable = available)
        }
    }

    /** Stops the availability probe and drops the draft password. */
    fun close() {
        scope.cancel()
        draftPassword = ""
        _state.value = _state.value.copy(hasPassword = false)
    }

    suspend fun dispatch(intent: UnlockIntent) {
        when (intent) {
            is UnlockIntent.EmailChanged -> _state.value = _state.value.copy(email = intent.value)
            is UnlockIntent.PasswordChanged -> {
                draftPassword = intent.value
                _state.value = _state.value.copy(hasPassword = intent.value.isNotEmpty())
            }

            UnlockIntent.SubmitPassword -> unlockByPassword()
            UnlockIntent.SubmitBiometrics -> unlockByBiometrics()
            UnlockIntent.EnableBiometrics -> enableBiometricsNow()
            UnlockIntent.ErrorDismissed -> _state.value = _state.value.copy(error = null)
        }
    }

    private suspend fun unlockByPassword() {
        if (!unlockMutex.tryLock()) return
        try {
            _state.value = _state.value.copy(isUnlocking = true, error = null)
            when (unlockWithPassword(_state.value.email, draftPassword)) {
                is Result.Success -> {
                    draftPassword = ""
                    _state.value = _state.value.copy(isUnlocking = false, hasPassword = false)
                    _effects.send(UnlockEffect.Unlocked)
                }

                is Result.Failure -> {
                    // Every failed attempt looks the same: no hint about the stored account.
                    _state.value = _state.value.copy(isUnlocking = false, error = AppError.InvalidCredentials)
                }
            }
        } finally {
            unlockMutex.unlock()
        }
    }

    private suspend fun unlockByBiometrics() {
        if (!unlockMutex.tryLock()) return
        try {
            _state.value = _state.value.copy(isUnlocking = true, error = null)
            when (unlockWithBiometrics(UNLOCK_REASON)) {
                is Result.Success -> {
                    draftPassword = ""
                    _state.value = _state.value.copy(isUnlocking = false, hasPassword = false)
                    _effects.send(UnlockEffect.Unlocked)
                }

                is Result.Failure -> {
                    _state.value = _state.value.copy(isUnlocking = false, error = AppError.InvalidCredentials)
                }
            }
        } finally {
            unlockMutex.unlock()
        }
    }

    private suspend fun enableBiometricsNow() {
        if (!unlockMutex.tryLock()) return
        try {
            _state.value = _state.value.copy(error = null)
            val result = enableBiometrics(ENABLE_REASON)
            when (result) {
                is Result.Success -> _effects.send(UnlockEffect.BiometricsEnabled)
                is Result.Failure -> _state.value = _state.value.copy(error = result.error)
            }
        } finally {
            unlockMutex.unlock()
        }
    }

    private companion object {
        private const val UNLOCK_REASON = "Unlock the vault"
        private const val ENABLE_REASON = "Enable biometric unlock"
    }
}

data class UnlockState(
    val email: String = "",
    val hasPassword: Boolean = false,
    val isUnlocking: Boolean = false,
    val biometricAvailable: Boolean = false,
    val error: AppError? = null,
)

sealed interface UnlockIntent {
    data class EmailChanged(val value: String) : UnlockIntent

    data class PasswordChanged(val value: String) : UnlockIntent {
        override fun toString(): String = "PasswordChanged(value=<redacted>)"
    }

    data object SubmitPassword : UnlockIntent

    data object SubmitBiometrics : UnlockIntent

    data object EnableBiometrics : UnlockIntent

    data object ErrorDismissed : UnlockIntent
}

sealed interface UnlockEffect {
    data object Unlocked : UnlockEffect

    data object BiometricsEnabled : UnlockEffect
}
