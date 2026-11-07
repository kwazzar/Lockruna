package com.kwazzar.lockruna.presentation.vault

import com.kwazzar.lockruna.domain.vault.VaultItemType

/** Item projection for the UI. Never carries secrets. */
data class ItemUi(
    val id: String,
    val type: VaultItemType,
    val title: String,
    val folderId: String?,
    val hasSecret: Boolean,
)
