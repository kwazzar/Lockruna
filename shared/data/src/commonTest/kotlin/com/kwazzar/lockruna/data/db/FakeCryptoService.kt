package com.kwazzar.lockruna.data.db

import com.kwazzar.lockruna.domain.crypto.CryptoService
import com.kwazzar.lockruna.domain.crypto.GcmCiphertext

private const val FAKE_NONCE_SIZE = 12
private const val FAKE_TAG_SIZE = 16
private const val FAKE_KEY_HASH_SIZE = 8

/** Deterministic XOR "cipher" that still binds ciphertext to the key and AAD, like real GCM. Test only. */
class FakeCryptoService : CryptoService {
    private val testKey = "test-key-32-bytes-long--------".encodeToByteArray()
    private var randomCounter = 0

    override fun hkdfSha256(inputKey: ByteArray, salt: ByteArray?, info: String, length: Int): ByteArray {
        val infoBytes = info.encodeToByteArray()
        val combined = ByteArray(length)
        for (i in 0 until length) {
            combined[i] = (inputKey[i % inputKey.size].toInt() xor testKey[i % testKey.size].toInt()).toByte()
        }
        salt?.forEachIndexed { i, byte ->
            combined[i % combined.size] =
                (combined[i % combined.size].toInt() xor byte.toInt()).toByte()
        }
        infoBytes.forEachIndexed { i, byte ->
            combined[i % combined.size] =
                (combined[i % combined.size].toInt() xor byte.toInt()).toByte()
        }
        return combined
    }

    override fun encryptGcm(key: ByteArray, plaintext: ByteArray, aad: ByteArray): GcmCiphertext {
        val nonce = nonceFrom(aad)
        return GcmCiphertext(
            ciphertext = xor(plaintext, key),
            nonce = nonce,
            tag = tagOf(key, nonce),
        )
    }

    override fun decryptGcm(key: ByteArray, ciphertext: GcmCiphertext, aad: ByteArray): ByteArray {
        val expectedNonce = nonceFrom(aad)
        if (!ciphertext.nonce.contentEquals(expectedNonce)) {
            throw IllegalArgumentException("GCM authentication failed: AAD mismatch")
        }
        if (!ciphertext.tag.copyOf(
                FAKE_KEY_HASH_SIZE,
            ).contentEquals(tagOf(key, ciphertext.nonce).copyOf(FAKE_KEY_HASH_SIZE))
        ) {
            throw IllegalArgumentException("GCM authentication failed: key mismatch")
        }
        return xor(ciphertext.ciphertext, key)
    }

    override fun randomBytes(size: Int): ByteArray = ByteArray(size) { i -> ((randomCounter + i) * 31 + 7).toByte() }
        .also { randomCounter += size }

    // The nonce derives from the AAD, so a decrypt with the wrong AAD is detected byte for byte.
    private fun nonceFrom(aad: ByteArray): ByteArray = ByteArray(FAKE_NONCE_SIZE) { i -> aad[i % aad.size] }

    private fun tagOf(key: ByteArray, nonce: ByteArray): ByteArray {
        val tag = ByteArray(FAKE_TAG_SIZE)
        key.forEachIndexed { i, byte ->
            tag[i % FAKE_KEY_HASH_SIZE] =
                (tag[i % FAKE_KEY_HASH_SIZE].toInt() xor byte.toInt()).toByte()
        }
        nonce.forEachIndexed { i, byte ->
            tag[FAKE_KEY_HASH_SIZE + i % FAKE_KEY_HASH_SIZE] =
                (tag[FAKE_KEY_HASH_SIZE + i % FAKE_KEY_HASH_SIZE].toInt() xor byte.toInt()).toByte()
        }
        return tag
    }

    private fun xor(data: ByteArray, key: ByteArray): ByteArray =
        ByteArray(data.size) { i -> (data[i].toInt() xor key[i % key.size].toInt()).toByte() }
}
