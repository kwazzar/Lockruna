package com.kwazzar.lockruna.data.crypto

import com.kwazzar.lockruna.domain.crypto.Argon2
import com.kwazzar.lockruna.domain.crypto.KdfParams
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters

actual object Argon2Factory {
    actual fun create(): Argon2 = Argon2BouncyCastle()
}

private class Argon2BouncyCastle : Argon2 {
    override suspend fun hash(password: String, salt: ByteArray, params: KdfParams): ByteArray {
        require(salt.size == params.saltLength) { "Salt must be exactly ${params.saltLength} bytes" }
        val generator = Argon2BytesGenerator()
        generator.init(
            Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withSalt(salt)
                .withMemoryAsKB(params.memoryKiB)
                .withIterations(params.iterations)
                .withParallelism(params.parallelism)
                .build(),
        )
        return ByteArray(params.outputLength).also { generator.generateBytes(password.encodeToByteArray(), it) }
    }
}
