package com.kwazzar.lockruna.application.support

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.Clock
import com.kwazzar.lockruna.domain.session.IdGenerator
import com.kwazzar.lockruna.domain.vault.ItemId
import com.kwazzar.lockruna.domain.vault.VaultItem
import com.kwazzar.lockruna.domain.vault.VaultItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryVaultItemRepository : VaultItemRepository {
    private val store = mutableMapOf<String, VaultItem>()
    private val stateFlow = MutableStateFlow(emptyList<VaultItem>())

    override suspend fun save(item: VaultItem): Result<VaultItem, AppError> {
        store[item.id.value] = item
        stateFlow.value = store.values.toList()
        return Result.Success(item)
    }

    override suspend fun get(id: ItemId): Result<VaultItem?, AppError> {
        return Result.Success(store[id.value])
    }

    override fun observeAll() = stateFlow.asStateFlow()
}

class FakeClock(
    var wallMillis: Long = 0,
    var monotonicMillis: Long = 0
) : Clock {
    override fun nowMillis(): Long = wallMillis
    override fun monotonicMillis(): Long = monotonicMillis
}

class FakeIdGenerator : IdGenerator {
    private var counter = 1

    override fun newItemId(): ItemId {
        val uuid = String.format("%032x", counter).let { s ->
            "${s.substring(0, 8)}-${s.substring(8, 12)}-4${s.substring(13, 16)}-8${s.substring(17, 20)}-${s.substring(20, 32)}"
        }
        counter++
        val result = ItemId.of(uuid)
        return when (result) {
            is Result.Success -> result.value
            is Result.Failure -> throw IllegalStateException("Invalid generated UUID: ${result.error}")
        }
    }

    override fun newUuid(): String {
        val uuid = String.format("%032x", counter).let { s ->
            "${s.substring(0, 8)}-${s.substring(8, 12)}-4${s.substring(13, 16)}-8${s.substring(17, 20)}-${s.substring(20, 32)}"
        }
        counter++
        return uuid
    }
}