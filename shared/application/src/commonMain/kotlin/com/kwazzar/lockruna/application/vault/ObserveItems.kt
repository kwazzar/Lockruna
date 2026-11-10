package com.kwazzar.lockruna.application.vault

import com.kwazzar.lockruna.domain.vault.VaultItem
import com.kwazzar.lockruna.domain.vault.VaultItemRepository
import kotlinx.coroutines.flow.Flow

class ObserveItems(private val repository: VaultItemRepository) {
    operator fun invoke(): Flow<List<VaultItem>> = repository.observeAll()
}
