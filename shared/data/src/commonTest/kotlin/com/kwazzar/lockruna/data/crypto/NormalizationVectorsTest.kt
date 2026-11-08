package com.kwazzar.lockruna.data.crypto

import kotlin.test.Test
import kotlin.test.assertEquals

class NormalizationVectorsTest {
    @Test
    fun `normalization matches the reference vectors`() {
        val normalizer = PlatformNormalizer()
        CryptoVectors.normalizationCases.forEach { case ->
            assertEquals(case.expected, normalizer.normalize(case.raw), "mismatch for input=${case.raw}")
        }
    }
}
