package com.kwazzar.lockruna.presentation.vault

import com.kwazzar.lockruna.domain.vault.VaultItem

fun VaultItem.toItemUi(): ItemUi = ItemUi(
    id = id.value,
    type = type,
    title = title.value,
    username = username,
    folderId = folderId?.value,
    updatedAtMillis = updatedAtMillis,
    hasSecret = secret != null,
)
