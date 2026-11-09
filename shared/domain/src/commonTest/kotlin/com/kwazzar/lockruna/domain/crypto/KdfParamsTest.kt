package com.kwazzar.lockruna.domain.crypto

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KdfParamsTest {
    @Test
    fun `default params are acceptable`() {
        assertTrue(KdfParams.isAcceptable(KdfParams()))
    }

    @Test
    fun `params below the built-in minimum are rejected`() {
        assertFalse(KdfParams.isAcceptable(KdfParams(memoryKiB = 32768)))
        assertFalse(KdfParams.isAcceptable(KdfParams(iterations = 2)))
        assertFalse(KdfParams.isAcceptable(KdfParams(parallelism = 0)))
        assertFalse(KdfParams.isAcceptable(KdfParams(saltLength = 8)))
        assertFalse(KdfParams.isAcceptable(KdfParams(outputLength = 16)))
        assertFalse(KdfParams.isAcceptable(KdfParams(alg = "argon2i")))
    }
}
