package com.kwazzar.lockruna.domain.crypto

class FakeCryptoService : CryptoService {
    override fun hkdfSha256(inputKey: ByteArray, salt: ByteArray?, info: String, length: Int): ByteArray =
        ByteArray(length)

    override fun encryptGcm(key: ByteArray, plaintext: ByteArray, aad: ByteArray): GcmCiphertext =
        GcmCiphertext(ciphertext = plaintext, nonce = ByteArray(12), tag = ByteArray(16))

    override fun decryptGcm(key: ByteArray, ciphertext: GcmCiphertext, aad: ByteArray): ByteArray =
        ciphertext.ciphertext

    override fun randomBytes(size: Int): ByteArray = ByteArray(size)

    override fun sha256(input: ByteArray): ByteArray = ByteArray(32)
}
