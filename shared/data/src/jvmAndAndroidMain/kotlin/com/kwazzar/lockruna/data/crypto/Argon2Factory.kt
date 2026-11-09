package com.kwazzar.lockruna.data.crypto

import com.kwazzar.lockruna.domain.crypto.Argon2
import com.kwazzar.lockruna.domain.crypto.KdfParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters

actual object Argon2Factory {
    actual fun create(): Argon2 = Argon2BouncyCastle()
}

private class Argon2BouncyCastle : Argon2 {
    override suspend fun hash(password: String, salt: ByteArray, params: KdfParams): ByteArray =
        withContext(Dispatchers.Default) {
            require(params == KdfParams.SUPPORTED) { "Only the Argon2id parameters fixed by ADR-003 are supported" }
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
            return@withContext ByteArray(params.outputLength).also {
                generator.generateBytes(password.encodeToByteArray(), it)
            }
        }
}
