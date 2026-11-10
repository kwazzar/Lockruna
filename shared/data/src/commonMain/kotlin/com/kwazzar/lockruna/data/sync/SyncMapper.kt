package com.kwazzar.lockruna.data.sync

import com.kwazzar.lockruna.contract.ChangeDto
import com.kwazzar.lockruna.domain.sync.PendingChange
import com.kwazzar.lockruna.domain.vault.VaultItemType
import kotlin.io.encoding.Base64

/** Wire mapping for sync types: the server speaks lowercase type names and base64 payloads. */
internal fun PendingChange.toDto() = ChangeDto(
    id = id,
    type = type.name.lowercase(),
    ciphertext = ciphertext?.toBase64(),
    nonce = nonce?.toBase64(),
    baseVersion = baseVersion,
    deleted = deleted,
)

internal fun ChangeDto.toDomain() = PendingChange(
    id = id,
    type = type.toItemType(),
    ciphertext = ciphertext?.fromBase64(),
    nonce = nonce?.fromBase64(),
    baseVersion = baseVersion,
    deleted = deleted,
)

internal fun String.toItemType(): VaultItemType =
    VaultItemType.entries.firstOrNull { it.name.equals(this, ignoreCase = true) }
        ?: throw IllegalArgumentException("Unknown item type: $this")

private fun ByteArray.toBase64(): String = Base64.encode(this)

private fun String.fromBase64(): ByteArray = Base64.decode(this)
