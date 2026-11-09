package com.kwazzar.lockruna.data.crypto

import com.kwazzar.lockruna.domain.crypto.CryptoService
import com.kwazzar.lockruna.domain.crypto.GcmCiphertext
import org.bouncycastle.crypto.InvalidCipherTextException
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.modes.GCMBlockCipher
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.crypto.params.KeyParameter
import java.security.SecureRandom

private const val GCM_KEY_SIZE: Int = 32
private const val GCM_NONCE_SIZE: Int = 12
private const val GCM_TAG_BITS: Int = 128
private const val GCM_TAG_SIZE: Int = GCM_TAG_BITS / Byte.SIZE_BITS

actual class PlatformCrypto actual constructor() : CryptoService {
    private val secureRandom = SecureRandom()

    actual override fun hkdfSha256(inputKey: ByteArray, salt: ByteArray?, info: String, length: Int): ByteArray {
        val generator = HKDFBytesGenerator(SHA256Digest())
        generator.init(HKDFParameters(inputKey, salt, info.encodeToByteArray()))
        return ByteArray(length).also { generator.generateBytes(it, 0, length) }
    }

    actual override fun encryptGcm(key: ByteArray, plaintext: ByteArray, aad: ByteArray): GcmCiphertext {
        require(key.size == GCM_KEY_SIZE) { "AES-256-GCM key must be $GCM_KEY_SIZE bytes" }
        val nonce = randomBytes(GCM_NONCE_SIZE)
        val cipher = GCMBlockCipher.newInstance(AESEngine.newInstance())
        cipher.init(true, AEADParameters(KeyParameter(key), GCM_TAG_BITS, nonce, aad))
        val buffer = ByteArray(cipher.getOutputSize(plaintext.size))
        val produced = cipher.processBytes(plaintext, 0, plaintext.size, buffer, 0)
        // BouncyCastle buffers the last block, so doFinal emits the remaining
        // ciphertext followed by the authentication tag.
        val flushed = cipher.doFinal(buffer, produced)
        val ciphertextSize = produced + flushed - GCM_TAG_SIZE
        return GcmCiphertext(
            ciphertext = buffer.copyOf(ciphertextSize),
            nonce = nonce,
            tag = buffer.copyOfRange(ciphertextSize, ciphertextSize + GCM_TAG_SIZE),
        )
    }

    actual override fun decryptGcm(key: ByteArray, ciphertext: GcmCiphertext, aad: ByteArray): ByteArray {
        require(key.size == GCM_KEY_SIZE) { "AES-256-GCM key must be $GCM_KEY_SIZE bytes" }
        val cipher = GCMBlockCipher.newInstance(AESEngine.newInstance())
        cipher.init(false, AEADParameters(KeyParameter(key), GCM_TAG_BITS, ciphertext.nonce, aad))
        // BouncyCastle verifies the tag in doFinal, so the tag travels with the ciphertext.
        val combined = ciphertext.ciphertext + ciphertext.tag
        val buffer = ByteArray(cipher.getOutputSize(combined.size))
        return try {
            val produced = cipher.processBytes(combined, 0, combined.size, buffer, 0)
            val flushed = cipher.doFinal(buffer, produced)
            buffer.copyOf(produced + flushed)
        } catch (e: InvalidCipherTextException) {
            buffer.fill(0)
            throw IllegalArgumentException("GCM authentication failed", e)
        }
    }

    actual override fun randomBytes(size: Int): ByteArray = ByteArray(size).also(secureRandom::nextBytes)
}
