package com.kwazzar.lockruna.presentation.vault

import com.kwazzar.lockruna.application.vault.CopySecret
import com.kwazzar.lockruna.application.vault.DeleteItem
import com.kwazzar.lockruna.application.vault.GetItem
import com.kwazzar.lockruna.application.vault.RevealSecret
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.vault.ItemId
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class ItemDetailStore(
    private val getItem: GetItem,
    private val revealSecret: RevealSecret,
    private val copySecret: CopySecret,
    private val deleteItem: DeleteItem,
) {
    private val _state = MutableStateFlow(ItemDetailState(item = null, secretVisible = false))
    private val _effects = MutableSharedFlow<ItemDetailEffect>(extraBufferCapacity = 0, replay = 0)
    private var revealedSecret: String? = null

    val state = _state.asStateFlow()
    val effects = _effects.asSharedFlow()

    suspend fun open(id: String) {
        val itemId = when (val parsed = ItemId.of(id)) {
            is Result.Success -> parsed.value
            is Result.Failure -> {
                clearItem()
                return
            }
        }
        val item = when (val result = getItem(itemId)) {
            is Result.Success -> result.value
            is Result.Failure -> null
        }
        revealedSecret = null
        _state.value = ItemDetailState(item = item?.toItemUi(), secretVisible = false)
    }

    suspend fun dispatch(intent: ItemDetailIntent) {
        when (intent) {
            ItemDetailIntent.Reveal -> reveal()
            ItemDetailIntent.Hide -> hide()
            ItemDetailIntent.Copy -> copy()
            ItemDetailIntent.Edit -> edit()
            ItemDetailIntent.DeleteConfirmed -> delete()
        }
    }

    private suspend fun reveal() {
        val id = currentItemId() ?: return
        val secret = when (val result = revealSecret(id)) {
            is Result.Success -> result.value ?: return
            is Result.Failure -> return
        }
        revealedSecret = secret
        _state.value = _state.value.copy(secretVisible = true)
        _effects.emit(ItemDetailEffect.SecretRevealed(secret))
    }

    private fun hide() {
        revealedSecret = null
        _state.value = _state.value.copy(secretVisible = false)
    }

    private suspend fun copy() {
        val id = currentItemId() ?: return
        if (copySecret(id) is Result.Success) {
            _effects.emit(ItemDetailEffect.SecretCopied)
        }
    }

    private suspend fun edit() {
        val id = currentItemId() ?: return
        _effects.emit(ItemDetailEffect.NavigateToEdit(id.value))
    }

    private suspend fun delete() {
        val id = currentItemId() ?: return
        if (deleteItem(id) is Result.Success) {
            clearItem()
            _effects.emit(ItemDetailEffect.Closed)
        }
    }

    private fun currentItemId(): ItemId? {
        val rawId = _state.value.item?.id ?: return null
        return when (val parsed = ItemId.of(rawId)) {
            is Result.Success -> parsed.value
            is Result.Failure -> null
        }
    }

    private fun clearItem() {
        revealedSecret = null
        _state.value = ItemDetailState(item = null, secretVisible = false)
    }
}

data class ItemDetailState(val item: ItemUi?, val secretVisible: Boolean)

sealed interface ItemDetailIntent {
    data object Reveal : ItemDetailIntent

    data object Hide : ItemDetailIntent

    data object Copy : ItemDetailIntent

    data object Edit : ItemDetailIntent

    data object DeleteConfirmed : ItemDetailIntent
}

sealed interface ItemDetailEffect {
    data class NavigateToEdit(val id: String) : ItemDetailEffect

    data object Closed : ItemDetailEffect

    data class SecretRevealed(val value: String) : ItemDetailEffect {
        override fun toString(): String = "SecretRevealed(value=<redacted>)"
    }

    data object SecretCopied : ItemDetailEffect
}
