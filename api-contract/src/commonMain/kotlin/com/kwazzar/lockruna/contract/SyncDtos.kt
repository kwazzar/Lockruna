package com.kwazzar.lockruna.contract

import kotlinx.serialization.Serializable

/** A vault item as `openapi.yaml` describes it: ciphertext only. */
@Serializable
data class EncryptedItemDto(
    val id: String,
    val type: String,
    val ciphertext: String? = null,
    val nonce: String? = null,
    val version: Long,
    val deletedAt: String? = null,
    val updatedAt: String,
)

/** A local change the client wants to apply. */
@Serializable
data class ChangeDto(
    val id: String,
    val type: String,
    val ciphertext: String? = null,
    val nonce: String? = null,
    val baseVersion: Long,
    val deleted: Boolean = false,
)

@Serializable
data class PushRequestDto(val changes: List<ChangeDto>)

@Serializable
data class AppliedChangeDto(val id: String, val version: Long, val updatedAt: String)

@Serializable
data class ConflictDto(val id: String, val server: EncryptedItemDto)

@Serializable
data class PushResultDto(val applied: List<AppliedChangeDto>, val conflicts: List<ConflictDto>)

@Serializable
data class SyncPageDto(val items: List<EncryptedItemDto>, val nextCursor: String, val hasMore: Boolean)
