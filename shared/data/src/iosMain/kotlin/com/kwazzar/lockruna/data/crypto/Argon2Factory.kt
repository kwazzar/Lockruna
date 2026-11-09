@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.kwazzar.lockruna.data.crypto

import com.kwazzar.lockruna.domain.crypto.Argon2
import com.kwazzar.lockruna.domain.crypto.KdfParams
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import libsodium.crypto_pwhash
import libsodium.crypto_pwhash_ALG_ARGON2ID13

actual object Argon2Factory {
    actual fun create(): Argon2 = Argon2Libsodium()
}

private class Argon2Libsodium : Argon2 {
    override suspend fun hash(password: String, salt: ByteArray, params: KdfParams): ByteArray =
        withContext(Dispatchers.Default) {
            require(params == KdfParams.SUPPORTED) { "Only the Argon2id parameters fixed by ADR-003 are supported" }
            require(salt.size == params.saltLength) { "Salt must be exactly ${params.saltLength} bytes" }
            val passwordBytes = password.encodeToByteArray()
            val output = UByteArray(params.outputLength)
            val status = withCBytes(salt) { saltPointer ->
                output.usePinned { outputPointer ->
                    crypto_pwhash(
                        outputPointer.addressOf(0),
                        params.outputLength.toULong(),
                        password,
                        passwordBytes.size.toULong(),
                        saltPointer,
                        params.iterations.toULong(),
                        (params.memoryKiB * 1024).toULong(),
                        crypto_pwhash_ALG_ARGON2ID13,
                    )
                }
            }
            check(status == 0) { "Argon2id hashing failed" }
            return@withContext output.asByteArray()
        }
}
