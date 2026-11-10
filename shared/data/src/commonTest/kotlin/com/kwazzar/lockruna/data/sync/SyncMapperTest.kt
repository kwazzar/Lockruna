package com.kwazzar.lockruna.data.sync

import com.kwazzar.lockruna.domain.sync.PendingChange
import com.kwazzar.lockruna.domain.vault.VaultItemType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SyncMapperTest {

    @Test
    fun `parses server instants into epoch millis`() {
        assertEquals(0L, parseInstantMillis("1970-01-01T00:00:00Z"))
        assertEquals(1_794_304_800_000L, parseInstantMillis("2026-11-10T10:00:00Z"))
        assertEquals(1_794_304_800_123L, parseInstantMillis("2026-11-10T10:00:00.123Z"))
        assertEquals(1_709_164_800_000L, parseInstantMillis("2024-02-29T00:00:00Z"))
        assertEquals(1_798_761_599_000L, parseInstantMillis("2026-12-31T23:59:59Z"))
        assertEquals(951_914_096_000L, parseInstantMillis("2000-03-01T12:34:56Z"))
    }

    @Test
    fun `keeps millisecond precision and drops finer digits`() {
        assertEquals(1_794_304_800_123L, parseInstantMillis("2026-11-10T10:00:00.123456789Z"))
    }

    @Test
    fun `rejects an instant the client cannot read`() {
        assertFailsWith<IllegalArgumentException> { parseInstantMillis("2026-11-10 10:00:00") }
        assertFailsWith<IllegalArgumentException> { parseInstantMillis("not-a-date") }
    }

    @Test
    fun `maps a pending change to the wire format`() {
        val change = PendingChange(
            id = "item-1",
            type = VaultItemType.LOGIN,
            ciphertext = byteArrayOf(1, 2, 3),
            nonce = byteArrayOf(4),
            baseVersion = 7,
            deleted = true,
        )

        val dto = change.toDto()

        assertEquals("item-1", dto.id)
        assertEquals("login", dto.type)
        assertEquals("AQID", dto.ciphertext)
        assertEquals("BA==", dto.nonce)
        assertEquals(7L, dto.baseVersion)
        assertEquals(true, dto.deleted)
        assertEquals(change, dto.toDomain())
    }
}
