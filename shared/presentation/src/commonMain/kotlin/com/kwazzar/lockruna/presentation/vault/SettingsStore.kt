package com.kwazzar.lockruna.presentation.vault

import com.kwazzar.lockruna.application.auth.DeleteAccount
import com.kwazzar.lockruna.application.auth.DisableBiometrics
import com.kwazzar.lockruna.application.auth.EnableBiometrics
import com.kwazzar.lockruna.application.auth.ObserveBiometricUnlock
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.AutoLockPolicy
import com.kwazzar.lockruna.domain.session.AutoLockSettingsStore
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.sync.Mutex

/** The settings screen: the auto-lock timeout (US-08), biometric unlock (US-07), account deletion (US-11). */
class SettingsStore(
    private val autoLockSettingsStore: AutoLockSettingsStore,
    private val observeBiometricUnlock: ObserveBiometricUnlock,
    private val enableBiometrics: EnableBiometrics,
    private val disableBiometrics: DisableBiometrics,
    private val deleteAccount: DeleteAccount,
) {
    private val _state = MutableStateFlow(SettingsState(isLoading = true))
    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    private val loadMutex = Mutex()
    private val biometricsMutex = Mutex()
    private val deleteMutex = Mutex()
    private var draftPassword: String = ""

    val state = _state.asStateFlow()
    val effects: Flow<SettingsEffect> = _effects.receiveAsFlow()

    /** The screen is gone: drop the typed master password. */
    fun close() {
        draftPassword = ""
        _state.value = _state.value.copy(hasPasswordForDeletion = false)
    }

    suspend fun dispatch(intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.Load -> load()
            is SettingsIntent.PolicyChanged -> changePolicy(intent.policy)
            is SettingsIntent.BiometricsToggled -> toggleBiometrics(intent.enabled)
            is SettingsIntent.DeletionPasswordChanged -> {
                draftPassword = intent.value
                _state.value = _state.value.copy(hasPasswordForDeletion = intent.value.isNotEmpty())
            }

            SettingsIntent.DeleteAccount -> deleteAccountNow()
            SettingsIntent.ErrorDismissed -> _state.value = _state.value.copy(error = null)
        }
    }

    private suspend fun load() {
        if (!loadMutex.tryLock()) return
        try {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val policy = autoLockSettingsStore.load()
            val biometrics = observeBiometricUnlock()
            _state.value = _state.value.copy(
                isLoading = false,
                policy = policy,
                choices = AutoLockPolicy.ALL,
                biometricsAvailable = biometrics.available,
                biometricsEnabled = biometrics.enabled,
            )
        } finally {
            loadMutex.unlock()
        }
    }

    private suspend fun changePolicy(policy: AutoLockPolicy) {
        autoLockSettingsStore.save(policy)
        _state.value = _state.value.copy(policy = policy)
        _effects.send(SettingsEffect.PolicySaved)
    }

    private suspend fun toggleBiometrics(enabled: Boolean) {
        if (!biometricsMutex.tryLock()) return
        try {
            val result = when (enabled) {
                true -> enableBiometrics(BIOMETRIC_PROMPT_REASON)
                false -> disableBiometrics()
            }
            when (result) {
                is Result.Failure -> _state.value = _state.value.copy(error = result.error)

                is Result.Success -> {
                    val biometrics = observeBiometricUnlock()
                    _state.value = _state.value.copy(
                        biometricsAvailable = biometrics.available,
                        biometricsEnabled = biometrics.enabled,
                    )
                    _effects.send(SettingsEffect.BiometricsChanged(biometrics.enabled))
                }
            }
        } finally {
            biometricsMutex.unlock()
        }
    }

    private suspend fun deleteAccountNow() {
        if (!deleteMutex.tryLock()) return
        try {
            _state.value = _state.value.copy(isDeleting = true, error = null)
            val result = deleteAccount(draftPassword)
            when (result) {
                is Result.Failure -> {
                    _state.value = _state.value.copy(isDeleting = false, error = result.error)
                }

                is Result.Success -> {
                    draftPassword = ""
                    _state.value = _state.value.copy(isDeleting = false, hasPasswordForDeletion = false)
                    _effects.send(SettingsEffect.AccountDeleted)
                }
            }
        } finally {
            deleteMutex.unlock()
        }
    }

    private companion object {
        const val BIOMETRIC_PROMPT_REASON = "Unlock the vault"
    }
}

data class SettingsState(
    val policy: AutoLockPolicy = AutoLockPolicy.ONE_MINUTE,
    val choices: List<AutoLockPolicy> = emptyList(),
    val biometricsAvailable: Boolean = false,
    val biometricsEnabled: Boolean = false,
    val hasPasswordForDeletion: Boolean = false,
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val error: AppError? = null,
)

sealed interface SettingsIntent {
    data object Load : SettingsIntent

    data class PolicyChanged(val policy: AutoLockPolicy) : SettingsIntent

    data class BiometricsToggled(val enabled: Boolean) : SettingsIntent

    data class DeletionPasswordChanged(val value: String) : SettingsIntent {
        override fun toString(): String = "DeletionPasswordChanged(value=<redacted>)"
    }

    data object DeleteAccount : SettingsIntent

    data object ErrorDismissed : SettingsIntent
}

sealed interface SettingsEffect {
    data object PolicySaved : SettingsEffect

    data class BiometricsChanged(val enabled: Boolean) : SettingsEffect

    data object AccountDeleted : SettingsEffect
}
