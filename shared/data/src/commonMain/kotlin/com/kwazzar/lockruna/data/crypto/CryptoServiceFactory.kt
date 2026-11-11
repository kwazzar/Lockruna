package com.kwazzar.lockruna.data.crypto

import com.kwazzar.lockruna.domain.crypto.Argon2
import com.kwazzar.lockruna.domain.crypto.CryptoService
import com.kwazzar.lockruna.domain.crypto.GcmCiphertext
import com.kwazzar.lockruna.domain.crypto.PasswordNormalizer

expect class PlatformCrypto() : CryptoService {
    override fun hkdfSha256(inputKey: ByteArray, salt: ByteArray?, info: String, length: Int): ByteArray
    override fun encryptGcm(key: ByteArray, plaintext: ByteArray, aad: ByteArray): GcmCiphertext
    override fun decryptGcm(key: ByteArray, ciphertext: GcmCiphertext, aad: ByteArray): ByteArray
    override fun randomBytes(size: Int): ByteArray
    override fun sha256(input: ByteArray): ByteArray
}

expect class PlatformNormalizer() : PasswordNormalizer {
    override fun normalize(raw: String): String
}

expect object Argon2Factory {
    fun create(): Argon2
}
