package com.kwazzar.lockruna.presentation.session

import com.kwazzar.lockruna.application.session.ApplyAutoLockPolicy
import com.kwazzar.lockruna.application.session.LockVault
import com.kwazzar.lockruna.application.session.ObserveVaultState
import com.kwazzar.lockruna.domain.session.VaultSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SessionStore(
    private val observeVaultState: ObserveVaultState,
    private val lockVault: LockVault,
    private val applyAutoLockPolicy: ApplyAutoLockPolicy,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(SessionState())
    private val _effects = MutableSharedFlow<SessionEffect>(extraBufferCapacity = 0, replay = 0)

    val state = _state.asStateFlow()
    val effects = _effects.asSharedFlow()

    init {
        scope.launch {
            observeVaultState().collect { vaultState ->
                _state.value = SessionState(isLocked = vaultState is VaultSession.State.Locked)
            }
        }
    }

    /** Stops following the vault state; call it when the screen is disposed. */
    fun close() {
        scope.cancel()
    }

    suspend fun dispatch(intent: SessionIntent) {
        when (intent) {
            SessionIntent.Lock -> {
                lockVault()
                _effects.emit(SessionEffect.Locked)
            }

            is SessionIntent.Resumed -> {
                if (applyAutoLockPolicy(intent.backgroundedAtMonotonicMillis)) {
                    _effects.emit(SessionEffect.Locked)
                }
            }
        }
    }
}

data class SessionState(val isLocked: Boolean = true)

sealed interface SessionIntent {
    data object Lock : SessionIntent

    data class Resumed(val backgroundedAtMonotonicMillis: Long) : SessionIntent
}

sealed interface SessionEffect {
    data object Locked : SessionEffect
}
