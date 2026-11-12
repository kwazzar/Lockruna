package com.kwazzar.lockruna.application.support

import com.kwazzar.lockruna.domain.crypto.CryptoService
import com.kwazzar.lockruna.domain.crypto.GcmCiphertext

class FakeCryptoService : CryptoService {
    var hkdfResults: Map<String, ByteArray> = emptyMap()
    var decryptGcmResult: ByteArray = ByteArray(32)
    var decryptGcmFails: Boolean = false
    var lastAad: ByteArray? = null
    var randomBytesResults: Map<Int, ByteArray> = emptyMap()

    override fun hkdfSha256(inputKey: ByteArray, salt: ByteArray?, info: String, length: Int): ByteArray =
        hkdfResults[info] ?: ByteArray(length)

    override fun encryptGcm(key: ByteArray, plaintext: ByteArray, aad: ByteArray): GcmCiphertext =
        GcmCiphertext(ciphertext = plaintext, nonce = ByteArray(12), tag = ByteArray(16))

    override fun decryptGcm(key: ByteArray, ciphertext: GcmCiphertext, aad: ByteArray): ByteArray {
        lastAad = aad
        if (decryptGcmFails) throw IllegalArgumentException("GCM authentication failed")
        return decryptGcmResult
    }

    override fun randomBytes(size: Int): ByteArray = randomBytesResults[size] ?: ByteArray(size)

    override fun sha256(input: ByteArray): ByteArray = ByteArray(32)
}

