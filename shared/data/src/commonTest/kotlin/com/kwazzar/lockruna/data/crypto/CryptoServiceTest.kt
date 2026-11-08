package com.kwazzar.lockruna.data.crypto

import com.kwazzar.lockruna.domain.crypto.GcmCiphertext
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CryptoServiceTest {
    private val crypto = PlatformCrypto()

    @Test
    fun `hkdfSha256 matches the reference vector with a domain label`() {
        val okm = crypto.hkdfSha256(
            inputKey = CryptoVectors.hkdfLabelIkm,
            salt = CryptoVectors.hkdfLabelSalt,
            info = CryptoVectors.HKDF_LABEL_INFO,
            length = CryptoVectors.HKDF_LABEL_L,
        )
        assertContentEquals(CryptoVectors.hkdfLabelOkm, okm)
    }

    @Test
    fun `hkdfSha256 matches the reference vector with a non-ascii label`() {
        val okm = crypto.hkdfSha256(
            inputKey = CryptoVectors.hkdfCyrillicIkm,
            salt = CryptoVectors.hkdfCyrillicSalt,
            info = CryptoVectors.HKDF_CYRILLIC_INFO,
            length = CryptoVectors.HKDF_CYRILLIC_L,
        )
        assertContentEquals(CryptoVectors.hkdfCyrillicOkm, okm)
    }

    @Test
    fun `hkdfSha256 with null salt matches RFC 5869 test case 3`() {
        val okm = crypto.hkdfSha256(
            inputKey = CryptoVectors.rfc5869Case3Ikm,
            salt = null,
            info = CryptoVectors.RFC5869_CASE3_INFO,
            length = CryptoVectors.RFC5869_CASE3_L,
        )
        assertContentEquals(CryptoVectors.rfc5869Case3Okm, okm)
    }

    @Test
    fun `decryptGcm matches the AES-256 zero-key vector`() {
        val ciphertext = GcmCiphertext(
            ciphertext = CryptoVectors.gcmAes256ZeroCiphertext,
            nonce = CryptoVectors.gcmAes256ZeroIv,
            tag = CryptoVectors.gcmAes256ZeroTag,
        )
        val decrypted = crypto.decryptGcm(
            key = CryptoVectors.gcmAes256ZeroKey,
            ciphertext = ciphertext,
            aad = CryptoVectors.gcmAes256ZeroAad,
        )
        assertContentEquals(CryptoVectors.gcmAes256ZeroPlaintext, decrypted)
    }

    @Test
    fun `gcm rejects a key that is not 256 bits`() {
        assertFailsWith<IllegalArgumentException> {
            crypto.encryptGcm(ByteArray(16), ByteArray(0), ByteArray(0))
        }
    }

    @Test
    fun `encryptGcm and decryptGcm round trip succeeds`() {
        val key = crypto.randomBytes(32)
        val plaintext = "sensitive payload".encodeToByteArray()
        val aad = "record-42".encodeToByteArray()

        val encrypted = crypto.encryptGcm(key, plaintext, aad)
        assertEquals(12, encrypted.nonce.size)
        assertEquals(16, encrypted.tag.size)

        val decrypted = crypto.decryptGcm(key, encrypted, aad)
        assertContentEquals(plaintext, decrypted)
    }

    @Test
    fun `decryptGcm fails with modified aad`() {
        val key = crypto.randomBytes(32)
        val plaintext = "hello".encodeToByteArray()
        val aad = "good-aad".encodeToByteArray()

        val encrypted = crypto.encryptGcm(key, plaintext, aad)
        assertFailsWith<IllegalArgumentException> {
            crypto.decryptGcm(key, encrypted, "bad-aad".encodeToByteArray())
        }
    }

    @Test
    fun `decryptGcm fails with tampered ciphertext`() {
        val key = crypto.randomBytes(32)
        val plaintext = "hello".encodeToByteArray()
        val aad = "aad".encodeToByteArray()

        val encrypted = crypto.encryptGcm(key, plaintext, aad)
        val tamperedCiphertext = encrypted.ciphertext.copyOf()
        tamperedCiphertext[0] = (tamperedCiphertext[0].toInt() xor 0xFF).toByte()

        assertFailsWith<IllegalArgumentException> {
            crypto.decryptGcm(key, encrypted.copy(ciphertext = tamperedCiphertext), aad)
        }
    }

    @Test
    fun `decryptGcm fails with tampered tag`() {
        val key = crypto.randomBytes(32)
        val plaintext = "hello".encodeToByteArray()
        val aad = "aad".encodeToByteArray()

        val encrypted = crypto.encryptGcm(key, plaintext, aad)
        val tamperedTag = encrypted.tag.copyOf()
        tamperedTag[0] = (tamperedTag[0].toInt() xor 0xFF).toByte()

        assertFailsWith<IllegalArgumentException> {
            crypto.decryptGcm(key, encrypted.copy(tag = tamperedTag), aad)
        }
    }
}
