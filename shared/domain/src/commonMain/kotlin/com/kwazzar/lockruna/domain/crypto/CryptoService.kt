package com.kwazzar.lockruna.domain.crypto

/**
 * GCM ciphertext with nonce and authentication tag.
 * Note: when stored/transmitted, ciphertext and tag are concatenated (ciphertext || tag).
 * This class keeps them separate for API clarity.
 */
data class GcmCiphertext(
    val ciphertext: ByteArray,
    val nonce: ByteArray,
    val tag: ByteArray
)

/**
 * Cryptographic operations port.
 * Implementation provided by crypto-core plan (Argon2, AES-GCM, HKDF, random).
 */
interface CryptoService {
    fun hkdfSha256(inputKey: ByteArray, salt: ByteArray?, info: ByteArray, length: Int = 32): ByteArray
    fun encryptGcm(key: ByteArray, plaintext: ByteArray, aad: ByteArray): GcmCiphertext
    fun decryptGcm(key: ByteArray, ciphertext: GcmCiphertext, aad: ByteArray): ByteArray
    fun randomBytes(size: Int): ByteArray
}