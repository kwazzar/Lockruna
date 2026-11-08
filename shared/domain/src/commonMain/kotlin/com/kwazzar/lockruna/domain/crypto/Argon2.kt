package com.kwazzar.lockruna.domain.crypto

data class KdfParams(
    val alg: String = DEFAULT_ALG,
    val memoryKiB: Int = DEFAULT_MEMORY_KIB,
    val iterations: Int = DEFAULT_ITERATIONS,
    val parallelism: Int = DEFAULT_PARALLELISM,
    val saltLength: Int = DEFAULT_SALT_LENGTH,
    val outputLength: Int = DEFAULT_OUTPUT_LENGTH,
) {
    companion object {
        const val DEFAULT_ALG: String = "argon2id"
        const val DEFAULT_MEMORY_KIB: Int = 65536
        const val DEFAULT_ITERATIONS: Int = 3
        const val DEFAULT_PARALLELISM: Int = 1
        const val DEFAULT_SALT_LENGTH: Int = 16
        const val DEFAULT_OUTPUT_LENGTH: Int = 32

        const val MIN_MEMORY_KIB: Int = 65536
        const val MIN_ITERATIONS: Int = 3
        const val MIN_PARALLELISM: Int = 1

        fun isAcceptable(params: KdfParams): Boolean = params.alg == DEFAULT_ALG &&
            params.memoryKiB >= MIN_MEMORY_KIB &&
            params.iterations >= MIN_ITERATIONS &&
            params.parallelism >= MIN_PARALLELISM &&
            params.saltLength == DEFAULT_SALT_LENGTH &&
            params.outputLength == DEFAULT_OUTPUT_LENGTH
    }
}

interface Argon2 {
    suspend fun hash(password: ByteArray, salt: ByteArray, params: KdfParams): ByteArray
}
