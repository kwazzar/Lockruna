package com.kwazzar.lockruna.contract

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class KdfParamsDtoTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun `serializes field names required by the API contract`() {
        val encoded = json.encodeToString(
            KdfParamsDto.serializer(),
            KdfParamsDto(memoryKiB = 65536, iterations = 3),
        )

        assertEquals("""{"alg":"argon2id","m":65536,"t":3,"p":1}""", encoded)
    }

    @Test
    fun `deserializes the contract payload`() {
        val decoded = json.decodeFromString(
            KdfParamsDto.serializer(),
            """{"alg":"argon2id","m":65536,"t":3,"p":1}""",
        )

        assertEquals(65536, decoded.memoryKiB)
        assertEquals(3, decoded.iterations)
        assertEquals(1, decoded.parallelism)
    }
}
