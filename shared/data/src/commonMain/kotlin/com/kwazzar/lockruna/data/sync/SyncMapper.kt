package com.kwazzar.lockruna.data.sync

import com.kwazzar.lockruna.contract.AppliedChangeDto
import com.kwazzar.lockruna.contract.ChangeDto
import com.kwazzar.lockruna.contract.ConflictDto
import com.kwazzar.lockruna.contract.EncryptedItemDto
import com.kwazzar.lockruna.contract.PushResultDto
import com.kwazzar.lockruna.contract.SyncPageDto
import com.kwazzar.lockruna.domain.sync.AppliedChange
import com.kwazzar.lockruna.domain.sync.Conflict
import com.kwazzar.lockruna.domain.sync.PendingChange
import com.kwazzar.lockruna.domain.sync.PushResult
import com.kwazzar.lockruna.domain.sync.RemoteItem
import com.kwazzar.lockruna.domain.sync.SyncPage
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

internal fun SyncPageDto.toDomain() = SyncPage(
    items = items.map { it.toDomain() },
    nextCursor = nextCursor,
    hasMore = hasMore,
)

internal fun EncryptedItemDto.toDomain() = RemoteItem(
    id = id,
    type = type.toItemType(),
    ciphertext = ciphertext?.fromBase64(),
    nonce = nonce?.fromBase64(),
    version = version,
    deleted = ciphertext == null || deletedAt != null,
    updatedAtMillis = parseInstantMillis(updatedAt),
)

internal fun PushResultDto.toDomain() = PushResult(
    applied = applied.map { it.toDomain() },
    conflicts = conflicts.map { it.toDomain() },
)

internal fun AppliedChangeDto.toDomain() = AppliedChange(
    id = id,
    newVersion = version,
    updatedAtMillis = parseInstantMillis(updatedAt),
)

internal fun ConflictDto.toDomain() = Conflict(id = id, server = server.toDomain())

internal fun String.toItemType(): VaultItemType =
    VaultItemType.entries.firstOrNull { it.name.equals(this, ignoreCase = true) }
        ?: throw IllegalArgumentException("Unknown item type: $this")

/**
 * Parses the ISO-8601 UTC instant the server sends (`2026-11-10T10:00:00[.123456789]Z`) into epoch millis.
 */
internal fun parseInstantMillis(text: String): Long {
    require(text.endsWith("Z")) { "Instant is not UTC: $text" }
    val dateTime = text.removeSuffix("Z")
    val date = dateTime.substringBefore('T').split('-')
    val time = dateTime.substringAfter('T').split(':')
    require(date.size == INSTANT_PARTS && time.size == INSTANT_PARTS) { "Invalid instant: $text" }

    val year = date[0].toIntOrNull() ?: invalidInstant(text)
    val month = date[1].toIntOrNull() ?: invalidInstant(text)
    val day = date[2].toIntOrNull() ?: invalidInstant(text)
    val hour = time[0].toIntOrNull() ?: invalidInstant(text)
    val minute = time[1].toIntOrNull() ?: invalidInstant(text)
    val secondText = time[2]
    val second = secondText.substringBefore('.').toIntOrNull() ?: invalidInstant(text)
    val millis = if (secondText.contains('.')) {
        secondText.substringAfter('.').take(3).padEnd(3, '0').toIntOrNull() ?: invalidInstant(text)
    } else {
        0
    }

    val days = daysFromCivil(year, month, day)
    return days * MILLIS_PER_DAY + hour * MILLIS_PER_HOUR + minute * MILLIS_PER_MINUTE +
        second * MILLIS_PER_SECOND + millis
}

private const val INSTANT_PARTS = 3
private const val MILLIS_PER_SECOND = 1_000L
private const val MILLIS_PER_MINUTE = 60_000L
private const val MILLIS_PER_HOUR = 3_600_000L
private const val MILLIS_PER_DAY = 86_400_000L
private const val DAYS_PER_ERA = 146_097L
private const val ERA_YEARS = 400
private const val ERA_BIAS = 399
private const val DAYS_PER_YEAR = 365
private const val LEAP_YEAR_CYCLE = 4
private const val CENTURY_CYCLE = 100
private const val MONTH_SHIFT = 153
private const val MONTH_SHIFT_DIVISOR = 5
private const val MONTHS_BEFORE_MARCH = 3
private const val MONTHS_FROM_MARCH = 9
private const val EPOCH_DAY_SHIFT = 719_468L

private fun invalidInstant(text: String): Nothing = throw IllegalArgumentException("Invalid instant: $text")

/** Days between the epoch and the given civil date (Howard Hinnant's algorithm). */
private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
    val shiftedYear = if (month <= 2) year - 1 else year
    val era = (if (shiftedYear >= 0) shiftedYear else shiftedYear - ERA_BIAS) / ERA_YEARS
    val yearOfEra = shiftedYear - era * ERA_YEARS
    val monthIndex = if (month > 2) month - MONTHS_BEFORE_MARCH else month + MONTHS_FROM_MARCH
    val dayOfYear = (MONTH_SHIFT * monthIndex + 2) / MONTH_SHIFT_DIVISOR + day - 1
    val dayOfEra = yearOfEra * DAYS_PER_YEAR + yearOfEra / LEAP_YEAR_CYCLE -
        yearOfEra / CENTURY_CYCLE + dayOfYear
    return era.toLong() * DAYS_PER_ERA + dayOfEra - EPOCH_DAY_SHIFT
}

private fun ByteArray.toBase64(): String = Base64.encode(this)

private fun String.fromBase64(): ByteArray = Base64.decode(this)
