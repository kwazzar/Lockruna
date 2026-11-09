package com.kwazzar.lockruna.data.crypto

import com.kwazzar.lockruna.domain.crypto.KdfParams
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class Argon2VectorsTest {
    @Test
    fun `argon2id matches the reference vectors`() = runTest {
        val argon2 = Argon2Factory.create()
        CryptoVectors.argon2Cases.forEach { case ->
            val actual = argon2.hash(case.password, case.salt, KdfParams())
            assertContentEquals(case.expected, actual, "mismatch for salt=${case.salt.toHex()}")
        }
    }

    @Test
    fun `argon2id rejects parameter sets that are not supported on every platform`() = runTest {
        val argon2 = Argon2Factory.create()
        val salt = ByteArray(KdfParams.DEFAULT_SALT_LENGTH)

        assertFailsWith<IllegalArgumentException> {
            argon2.hash("password", salt, KdfParams(memoryKiB = 32768))
        }
        assertFailsWith<IllegalArgumentException> {
            argon2.hash("password", salt, KdfParams(parallelism = 2))
        }
        assertFailsWith<IllegalArgumentException> {
            argon2.hash("password", salt, KdfParams(alg = "argon2i"))
        }
    }
}
