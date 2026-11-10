package com.kwazzar.lockruna.application.support

import com.kwazzar.lockruna.domain.crypto.CryptoService
import com.kwazzar.lockruna.domain.crypto.GcmCiphertext

class FakeCryptoService : CryptoService {
    var hkdfResults: Map<String, ByteArray> = emptyMap()
    var decryptGcmResult: ByteArray = ByteArray(32)

    override fun hkdfSha256(inputKey: ByteArray, salt: ByteArray?, info: String, length: Int): ByteArray =
        hkdfResults[info] ?: ByteArray(length)

    override fun encryptGcm(key: ByteArray, plaintext: ByteArray, aad: ByteArray): GcmCiphertext =
        GcmCiphertext(ciphertext = plaintext, nonce = ByteArray(12), tag = ByteArray(16))

    override fun decryptGcm(key: ByteArray, ciphertext: GcmCiphertext, aad: ByteArray): ByteArray =
        decryptGcmResult

    override fun randomBytes(size: Int): ByteArray = ByteArray(size)
}
