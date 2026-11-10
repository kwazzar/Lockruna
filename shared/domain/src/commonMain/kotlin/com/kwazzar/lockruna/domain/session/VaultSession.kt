package com.kwazzar.lockruna.domain.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Vault session state — holds the vault key in memory when unlocked.
 * Mutable state (not an aggregate) because it represents the unlock/lock lifecycle.
 */
class VaultSession {
    sealed interface State {
        data object Locked : State
        data object Unlocked : State
    }

    private val stateValue = MutableStateFlow<State>(State.Locked)
    private var vaultKeyValue: VaultKey? = null

    val state: State
        get() = stateValue.value

    /** Emits on every lock/unlock transition so the vault index can follow the session. */
    val stateFlow: StateFlow<State> = stateValue.asStateFlow()

    fun unlock(vaultKey: VaultKey) {
        vaultKeyValue = vaultKey
        stateValue.value = State.Unlocked
    }

    fun lock() {
        vaultKeyValue?.zeroize()
        vaultKeyValue = null
        stateValue.value = State.Locked
    }

    fun currentVaultKey(): VaultKey? = vaultKeyValue
}
