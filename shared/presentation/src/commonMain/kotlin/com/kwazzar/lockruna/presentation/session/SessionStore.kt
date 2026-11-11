package com.kwazzar.lockruna.presentation.session

import com.kwazzar.lockruna.application.session.ApplyAutoLockPolicy
import com.kwazzar.lockruna.application.session.LockVault
import com.kwazzar.lockruna.application.session.ObserveVaultState
import com.kwazzar.lockruna.domain.session.VaultSession
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

class SessionStore(
    private val observeVaultState: ObserveVaultState,
    private val lockVault: LockVault,
    private val applyAutoLockPolicy: ApplyAutoLockPolicy,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(SessionState())
    private val _effects = Channel<SessionEffect>(Channel.BUFFERED)

    val state = _state.asStateFlow()
    val effects: Flow<SessionEffect> = _effects.receiveAsFlow()

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
                _effects.send(SessionEffect.Locked)
            }

            is SessionIntent.Resumed -> {
                val backgroundedAt = intent.backgroundedAtMonotonicMillis ?: return
                if (applyAutoLockPolicy(backgroundedAt)) {
                    _effects.send(SessionEffect.Locked)
                }
            }
        }
    }
}

data class SessionState(val isLocked: Boolean = true)

sealed interface SessionIntent {
    data object Lock : SessionIntent

    /** [backgroundedAtMonotonicMillis] is null when the app was never in the background. */
    data class Resumed(val backgroundedAtMonotonicMillis: Long?) : SessionIntent
}

sealed interface SessionEffect {
    data object Locked : SessionEffect
}
