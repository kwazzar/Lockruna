package com.kwazzar.lockruna.domain.session

/**
 * Vault session state — holds the vault key in memory when unlocked.
 * Mutable state (not an aggregate) because it represents the unlock/lock lifecycle.
 */
class VaultSession {
    sealed interface State {
        data object Locked : State
        data object Unlocked : State
    }

    private var stateValue: State = State.Locked
    private var vaultKeyValue: VaultKey? = null

    val state: State
        get() = stateValue

    fun unlock(vaultKey: VaultKey) {
        vaultKeyValue = vaultKey
        stateValue = State.Unlocked
    }

    fun lock() {
        vaultKeyValue?.zeroize()
        vaultKeyValue = null
        stateValue = State.Locked
    }

    fun currentVaultKey(): VaultKey? = vaultKeyValue
}
