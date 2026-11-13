package com.kwazzar.lockruna.data.crypto

import com.kwazzar.lockruna.domain.crypto.KdfParams
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.ExperimentalKotlinTestApi
import kotlin.OptIn

class Argon2PerformanceTest {

    @OptIn(ExperimentalKotlinTestApi::class)
    @Test
    fun `Argon2id derive is within acceptable bounds`() = runTest {
        val argon2 = Argon2Factory.create()
        val params = KdfParams.SUPPORTED
        val password = "test-password-12345678"
        val salt = ByteArray(params.saltLength) { 42 }

        val times = mutableListOf<Long>()
        repeat(5) {
            val start = System.nanoTime()
            argon2.hash(password, salt, params)
            val elapsedMs = (System.nanoTime() - start) / 1_000_000
            times.add(elapsedMs)
        }

        times.sort()
        val median = times[times.size / 2]
        val min = times.first()
        val max = times.last()

        println("Argon2id benchmark: alg=${params.alg}, m=${params.memoryKiB} KiB, t=${params.iterations}, p=${params.parallelism}, salt=${params.saltLength}, output=${params.outputLength}; samples=$times; min=$min ms, median=$median ms, max=$max ms")

        // Generous ceiling so the test does not flake on slower CI runners.
        // Real NFR is ~1.5 s on a mid-range device; here we just guard against regression.
        assertTrue(median < 5_000) { "Median derive time $median ms exceeds 5 s ceiling" }
    }
}
