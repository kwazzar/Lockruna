package com.kwazzar.lockruna.presentation.vault

import com.kwazzar.lockruna.application.vault.CreateItem
import com.kwazzar.lockruna.application.vault.CreateItemCommand
import com.kwazzar.lockruna.application.vault.GetItem
import com.kwazzar.lockruna.application.vault.UpdateItem
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.vault.ItemId
import com.kwazzar.lockruna.domain.vault.VaultItemType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class ItemEditStore(
    private val createItem: CreateItem,
    private val updateItem: UpdateItem,
    private val getItem: GetItem,
) {
    private val _state = MutableStateFlow(ItemEditState())
    private val _effects = MutableSharedFlow<ItemEditEffect>(extraBufferCapacity = 0, replay = 0)
    private var draftSecret: String = ""
    private var secretDraftChanged: Boolean = false

    val state = _state.asStateFlow()
    val effects = _effects.asSharedFlow()

    suspend fun begin(existingId: String?) {
        if (existingId == null) {
            clearDraft()
            return
        }
        val itemId = when (val parsed = ItemId.of(existingId)) {
            is Result.Success -> parsed.value
            is Result.Failure -> {
                clearDraft()
                return
            }
        }
        val item = when (val result = getItem(itemId)) {
            is Result.Success -> result.value
            is Result.Failure -> null
        }
        clearDraft()
        _state.value = if (item == null) {
            ItemEditState(existingId = existingId)
        } else {
            ItemEditState(
                existingId = item.id.value,
                title = item.title.value,
                username = item.username.orEmpty(),
                url = item.url?.value.orEmpty(),
                folderId = item.folderId?.value,
            )
        }
    }

    suspend fun dispatch(intent: ItemEditIntent) {
        when (intent) {
            is ItemEditIntent.TitleChanged -> updateState { copy(title = intent.value) }
            is ItemEditIntent.UsernameChanged -> updateState { copy(username = intent.value) }
            is ItemEditIntent.SecretChanged -> {
                draftSecret = intent.value
                secretDraftChanged = true
                updateState { copy(hasSecretDraft = intent.value.isNotEmpty()) }
            }

            is ItemEditIntent.UrlChanged -> updateState { copy(url = intent.value) }
            is ItemEditIntent.FolderSelected -> updateState { copy(folderId = intent.folderId) }
            ItemEditIntent.Save -> save()
        }
    }

    private suspend fun save() {
        if (_state.value.isSaving) return
        _state.value = _state.value.copy(isSaving = true)
        val result = when (val existingId = _state.value.existingId) {
            null -> create()
            else -> update(existingId)
        }
        when (result) {
            is Result.Success -> {
                _state.value = _state.value.copy(isSaving = false)
                _effects.emit(ItemEditEffect.Saved(result.value.value))
            }

            is Result.Failure -> {
                _state.value = _state.value.copy(isSaving = false)
                _effects.emit(ItemEditEffect.ValidationError(result.error))
            }
        }
    }

    private suspend fun create(): Result<ItemId, AppError> {
        val result = createItem(
            CreateItemCommand(
                type = VaultItemType.LOGIN,
                title = _state.value.title,
                username = _state.value.username.takeIf { it.isNotBlank() },
                secret = if (secretDraftChanged) draftSecret else null,
                url = _state.value.url.takeIf { it.isNotBlank() },
                folderId = parseFolderId(),
            ),
        )
        return when (result) {
            is Result.Success -> Result.Success(result.value.id)
            is Result.Failure -> Result.Failure(result.error)
        }
    }

    private suspend fun update(existingId: String): Result<ItemId, AppError> {
        val itemId = when (val parsed = ItemId.of(existingId)) {
            is Result.Success -> parsed.value
            is Result.Failure -> return Result.Failure(parsed.error)
        }
        val result = updateItem(
            id = itemId,
            title = _state.value.title,
            username = _state.value.username.takeIf { it.isNotBlank() },
            secret = if (secretDraftChanged) draftSecret else null,
            url = _state.value.url.takeIf { it.isNotBlank() },
            folderId = parseFolderId(),
        )
        return when (result) {
            is Result.Success -> Result.Success(result.value.id)
            is Result.Failure -> Result.Failure(result.error)
        }
    }

    private fun parseFolderId(): ItemId? {
        val rawId = _state.value.folderId ?: return null
        return when (val parsed = ItemId.of(rawId)) {
            is Result.Success -> parsed.value
            is Result.Failure -> null
        }
    }

    private fun updateState(transform: ItemEditState.() -> ItemEditState) {
        _state.value = _state.value.transform()
    }

    private fun clearDraft() {
        draftSecret = ""
        secretDraftChanged = false
        _state.value = ItemEditState()
    }
}

data class ItemEditState(
    val existingId: String? = null,
    val title: String = "",
    val username: String = "",
    val url: String = "",
    val folderId: String? = null,
    val hasSecretDraft: Boolean = false,
    val isSaving: Boolean = false,
)

sealed interface ItemEditIntent {
    data class TitleChanged(val value: String) : ItemEditIntent

    data class UsernameChanged(val value: String) : ItemEditIntent

    data class SecretChanged(val value: String) : ItemEditIntent

    data class UrlChanged(val value: String) : ItemEditIntent

    data class FolderSelected(val folderId: String?) : ItemEditIntent

    data object Save : ItemEditIntent
}

sealed interface ItemEditEffect {
    data class Saved(val id: String) : ItemEditEffect

    data class ValidationError(val error: AppError) : ItemEditEffect
}
