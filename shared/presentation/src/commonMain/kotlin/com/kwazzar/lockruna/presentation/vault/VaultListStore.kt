package com.kwazzar.lockruna.presentation.vault

import com.kwazzar.lockruna.application.session.LockVault
import com.kwazzar.lockruna.application.session.ObserveVaultState
import com.kwazzar.lockruna.application.vault.CopySecret
import com.kwazzar.lockruna.application.vault.ObserveItems
import com.kwazzar.lockruna.application.vault.RevealSecret
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.session.VaultSession
import com.kwazzar.lockruna.domain.vault.ItemId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VaultListStore(
    private val observeItems: ObserveItems,
    observeVaultState: ObserveVaultState,
    private val lockVault: LockVault,
    private val revealSecret: RevealSecret,
    private val copySecret: CopySecret,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val observedItems = MutableStateFlow<List<ItemUi>>(emptyList())
    private val _state = MutableStateFlow(VaultListState(items = emptyList(), searchQuery = null, isLoading = true))
    private val _effects = MutableSharedFlow<VaultListEffect>(extraBufferCapacity = 0, replay = 0)
    private var itemsJob: Job? = null

    val state = _state.asStateFlow()
    val effects = _effects.asSharedFlow()

    init {
        scope.launch {
            observeVaultState().collect { vaultState ->
                when (vaultState) {
                    is VaultSession.State.Unlocked -> startObservingItems()
                    is VaultSession.State.Locked -> clearItems()
                }
            }
        }
    }

    /** Stops the observation; call it when the screen is disposed. */
    fun close() {
        scope.cancel()
    }

    suspend fun dispatch(intent: VaultListIntent) {
        when (intent) {
            is VaultListIntent.Search -> {
                _state.value = _state.value.copy(searchQuery = intent.query)
                updateItems()
            }

            is VaultListIntent.ItemClicked -> _effects.emit(VaultListEffect.OpenItem(intent.id))

            is VaultListIntent.RevealSecret -> reveal(intent.id)

            is VaultListIntent.CopySecret -> copy(intent.id)

            VaultListIntent.Lock -> lockVault()
        }
    }

    private suspend fun reveal(rawId: String) {
        val id = when (val parsed = ItemId.of(rawId)) {
            is Result.Success -> parsed.value
            is Result.Failure -> return
        }
        val secret = when (val result = revealSecret(id)) {
            is Result.Success -> result.value
            is Result.Failure -> return
        }
        _effects.emit(VaultListEffect.SecretRevealed(secret))
    }

    private suspend fun copy(rawId: String) {
        val id = when (val parsed = ItemId.of(rawId)) {
            is Result.Success -> parsed.value
            is Result.Failure -> return
        }
        if (copySecret(id) is Result.Success) {
            _effects.emit(VaultListEffect.SecretCopied)
        }
    }

    private fun startObservingItems() {
        itemsJob?.cancel()
        _state.value = _state.value.copy(isLoading = true)
        itemsJob = scope.launch {
            observeItems().collect { items ->
                observedItems.value = items.map { it.toItemUi() }
                updateItems()
            }
        }
    }

    private fun clearItems() {
        itemsJob?.cancel()
        itemsJob = null
        observedItems.value = emptyList()
        _state.value = _state.value.copy(items = emptyList(), isLoading = false)
    }

    private fun updateItems() {
        val query = _state.value.searchQuery?.trim()?.lowercase().orEmpty()
        val items = if (query.isEmpty()) {
            observedItems.value
        } else {
            observedItems.value.filter { item ->
                item.title.lowercase().contains(query) ||
                    item.username?.lowercase()?.contains(query) == true
            }
        }
        _state.value = _state.value.copy(items = items, isLoading = false)
    }
}

data class VaultListState(val items: List<ItemUi>, val searchQuery: String?, val isLoading: Boolean)

sealed interface VaultListIntent {
    data class Search(val query: String?) : VaultListIntent

    data class ItemClicked(val id: String) : VaultListIntent

    data class RevealSecret(val id: String) : VaultListIntent

    data class CopySecret(val id: String) : VaultListIntent

    data object Lock : VaultListIntent
}

sealed interface VaultListEffect {
    data class OpenItem(val id: String) : VaultListEffect

    data class SecretRevealed(val value: String) : VaultListEffect {
        override fun toString(): String = "SecretRevealed(value=<redacted>)"
    }

    data object SecretCopied : VaultListEffect
}
