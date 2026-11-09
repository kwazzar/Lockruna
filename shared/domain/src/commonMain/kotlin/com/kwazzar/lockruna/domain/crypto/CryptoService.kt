package com.kwazzar.lockruna.domain.crypto

data class GcmCiphertext(val ciphertext: ByteArray, val nonce: ByteArray, val tag: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as GcmCiphertext
        return ciphertext.contentEquals(other.ciphertext) &&
            nonce.contentEquals(other.nonce) &&
            tag.contentEquals(other.tag)
    }

    override fun hashCode(): Int {
        var result = ciphertext.contentHashCode()
        result = 31 * result + nonce.contentHashCode()
        result = 31 * result + tag.contentHashCode()
        return result
    }

    override fun toString(): String =
        "GcmCiphertext(ciphertextLength=${ciphertext.size}, nonceLength=${nonce.size}, tagLength=${tag.size})"
}

interface CryptoService {
    /** HKDF-SHA256 (RFC 5869); [info] is the domain label, encoded as UTF-8. */
    fun hkdfSha256(inputKey: ByteArray, salt: ByteArray?, info: String, length: Int = 32): ByteArray
    fun encryptGcm(key: ByteArray, plaintext: ByteArray, aad: ByteArray): GcmCiphertext
    fun decryptGcm(key: ByteArray, ciphertext: GcmCiphertext, aad: ByteArray): ByteArray
    fun randomBytes(size: Int): ByteArray
}
