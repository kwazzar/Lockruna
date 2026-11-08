package com.kwazzar.lockruna.data.crypto

import com.kwazzar.lockruna.domain.crypto.KdfParams
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals

class Argon2VectorsTest {
    @Test
    fun `argon2id matches the reference vectors`() = runTest {
        val argon2 = Argon2Factory.create()
        CryptoVectors.argon2Cases.forEach { case ->
            val actual = argon2.hash(case.password, case.salt, KdfParams())
            assertContentEquals(case.expected, actual, "mismatch for salt=${case.salt.toHex()}")
        }
    }
}
