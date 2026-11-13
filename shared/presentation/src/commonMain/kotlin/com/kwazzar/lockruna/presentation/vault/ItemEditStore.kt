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
import kotlinx.coroutines.sync.Mutex

class ItemEditStore(
    private val createItem: CreateItem,
    private val updateItem: UpdateItem,
    private val getItem: GetItem,
) {
    private val _state = MutableStateFlow(ItemEditState())
    private val _effects = MutableSharedFlow<ItemEditEffect>(extraBufferCapacity = 0, replay = 0)
    private var draftSecret: String = ""
    private var secretDraftChanged: Boolean = false
    private var draftCardNumber: String = ""
    private var draftCardExpiry: String = ""
    private var draftCardCvv: String = ""
    private var draftNoteBody: String = ""
    private val saveMutex = Mutex()

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
                _state.value = ItemEditState(existingId = existingId)
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
                type = item.type,
                title = item.title.value,
                username = item.username.orEmpty(),
                url = item.url?.value.orEmpty(),
                folderId = item.folderId?.value,
            )
        }
    }

    suspend fun dispatch(intent: ItemEditIntent) {
        when (intent) {
            is ItemEditIntent.TypeChanged -> {
                updateState { copy(type = intent.type) }
                // Reset typed drafts when type changes
                draftCardNumber = ""
                draftCardExpiry = ""
                draftCardCvv = ""
                draftNoteBody = ""
            }
            is ItemEditIntent.TitleChanged -> updateState { copy(title = intent.value) }
            is ItemEditIntent.UsernameChanged -> updateState { copy(username = intent.value) }
            is ItemEditIntent.SecretChanged -> {
                draftSecret = intent.value
                secretDraftChanged = true
                updateState { copy(hasSecretDraft = intent.value.isNotEmpty()) }
            }
            is ItemEditIntent.CardNumberChanged -> {
                draftCardNumber = intent.value
                updateState { copy(hasCardNumberDraft = intent.value.isNotEmpty()) }
            }
            is ItemEditIntent.CardExpiryChanged -> {
                draftCardExpiry = intent.value
            }
            is ItemEditIntent.CardCvvChanged -> {
                draftCardCvv = intent.value
                updateState { copy(hasCardCvvDraft = intent.value.isNotEmpty()) }
            }
            is ItemEditIntent.NoteBodyChanged -> {
                draftNoteBody = intent.value
                updateState { copy(hasNoteBodyDraft = intent.value.isNotEmpty()) }
            }
            is ItemEditIntent.UrlChanged -> updateState { copy(url = intent.value) }
            is ItemEditIntent.FolderSelected -> updateState { copy(folderId = intent.folderId) }
            ItemEditIntent.Save -> save()
        }
    }

    private suspend fun save() {
        if (!saveMutex.tryLock()) return
        try {
            _state.value = _state.value.copy(isSaving = true)
            val result = when (val existingId = _state.value.existingId) {
                null -> create()
                else -> update(existingId)
            }
            when (result) {
                is Result.Success -> {
                    draftSecret = ""
                    secretDraftChanged = false
                    _state.value = _state.value.copy(isSaving = false, hasSecretDraft = false)
                    _effects.emit(ItemEditEffect.Saved(result.value.value))
                }

                is Result.Failure -> {
                    _state.value = _state.value.copy(isSaving = false)
                    _effects.emit(ItemEditEffect.ValidationError(result.error))
                }
            }
        } finally {
            saveMutex.unlock()
        }
    }

    private suspend fun create(): Result<ItemId, AppError> {
        val result = createItem(
            CreateItemCommand(
                type = _state.value.type,
                title = _state.value.title,
                username = _state.value.username.takeIf { it.isNotBlank() },
                secret = if (secretDraftChanged) draftSecret else null,
                url = _state.value.url.takeIf { it.isNotBlank() },
                folderId = parseFolderId(),
                cardNumber = if (_state.value.type == VaultItemType.CARD) draftCardNumber.takeIf { it.isNotBlank() } else null,
                cardExpiry = if (_state.value.type == VaultItemType.CARD) draftCardExpiry.takeIf { it.isNotBlank() } else null,
                cardCvv = if (_state.value.type == VaultItemType.CARD) draftCardCvv.takeIf { it.isNotBlank() } else null,
                noteBody = if (_state.value.type == VaultItemType.NOTE) draftNoteBody.takeIf { it.isNotBlank() } else null,
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
            cardNumber = if (_state.value.type == VaultItemType.CARD) draftCardNumber.takeIf { it.isNotBlank() } else null,
            cardExpiry = if (_state.value.type == VaultItemType.CARD) draftCardExpiry.takeIf { it.isNotBlank() } else null,
            cardCvv = if (_state.value.type == VaultItemType.CARD) draftCardCvv.takeIf { it.isNotBlank() } else null,
            noteBody = if (_state.value.type == VaultItemType.NOTE) draftNoteBody.takeIf { it.isNotBlank() } else null,
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
        draftCardNumber = ""
        draftCardExpiry = ""
        draftCardCvv = ""
        draftNoteBody = ""
        _state.value = ItemEditState()
    }
}

data class ItemEditState(
    val existingId: String? = null,
    val type: VaultItemType = VaultItemType.LOGIN,
    val title: String = "",
    val username: String = "",
    val url: String = "",
    val folderId: String? = null,
    val hasSecretDraft: Boolean = false,
    val hasCardNumberDraft: Boolean = false,
    val hasCardCvvDraft: Boolean = false,
    val hasNoteBodyDraft: Boolean = false,
    val isSaving: Boolean = false,
)

sealed interface ItemEditIntent {
    data class TypeChanged(val type: VaultItemType) : ItemEditIntent

    data class TitleChanged(val value: String) : ItemEditIntent

    data class UsernameChanged(val value: String) : ItemEditIntent

    data class SecretChanged(val value: String) : ItemEditIntent {
        override fun toString(): String = "SecretChanged(value=<redacted>)"
    }

    data class CardNumberChanged(val value: String) : ItemEditIntent {
        override fun toString(): String = "CardNumberChanged(value=<redacted>)"
    }

    data class CardExpiryChanged(val value: String) : ItemEditIntent

    data class CardCvvChanged(val value: String) : ItemEditIntent {
        override fun toString(): String = "CardCvvChanged(value=<redacted>)"
    }

    data class NoteBodyChanged(val value: String) : ItemEditIntent {
        override fun toString(): String = "NoteBodyChanged(value=<redacted>)"
    }

    data class UrlChanged(val value: String) : ItemEditIntent

    data class FolderSelected(val folderId: String?) : ItemEditIntent

    data object Save : ItemEditIntent
}

sealed interface ItemEditEffect {
    data class Saved(val id: String) : ItemEditEffect

    data class ValidationError(val error: AppError) : ItemEditEffect
}
