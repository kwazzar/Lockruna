package com.kwazzar.lockruna.application.support

import com.kwazzar.lockruna.domain.crypto.CryptoService
import com.kwazzar.lockruna.domain.crypto.GcmCiphertext

class FakeCryptoService : CryptoService {
    override fun hkdfSha256(inputKey: ByteArray, salt: ByteArray?, info: String, length: Int): ByteArray =
        ByteArray(length)

    override fun encryptGcm(key: ByteArray, plaintext: ByteArray, aad: ByteArray): GcmCiphertext =
        GcmCiphertext(ciphertext = plaintext, nonce = ByteArray(12), tag = ByteArray(16))

    override fun decryptGcm(key: ByteArray, ciphertext: GcmCiphertext, aad: ByteArray): ByteArray =
        ciphertext.ciphertext

    override fun randomBytes(size: Int): ByteArray = ByteArray(size)
}
