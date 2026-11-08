@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.kwazzar.lockruna.data.crypto

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import libsodium.crypto_aead_aes256gcm_decrypt
import libsodium.crypto_aead_aes256gcm_encrypt
import libsodium.crypto_kdf_hkdf_sha256_expand
import libsodium.crypto_kdf_hkdf_sha256_extract
import libsodium.crypto_pwhash
import libsodium.crypto_pwhash_ALG_ARGON2ID13
import libsodium.randombytes_buf
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies that the ionspin bindings expose the primitives the crypto layer needs
 * and that they agree with the published vectors.
 */
class IonspinProbeTest {
    @Test
    fun `crypto_pwhash derives an argon2id hash`() {
        val salt = ByteArray(16) { it.toByte() }
        val first = argon2id("correct horse battery staple", salt)
        val second = argon2id("correct horse battery staple", salt)
        val other = argon2id("correct horse battery staple", ByteArray(16) { (it + 1).toByte() })

        assertEquals(32, first.size)
        assertContentEquals(first, second)
        assertTrue(!first.contentEquals(other))
    }

    @Test
    fun `crypto_kdf_hkdf_sha256 matches the reference vector`() {
        val prk = UByteArray(32)
        val salt = CryptoVectors.hkdfCyrillicSalt
        val ikm = CryptoVectors.hkdfCyrillicIkm
        val info = CryptoVectors.HKDF_CYRILLIC_INFO

        val extractStatus = prk.usePinned { prkPointer ->
            salt.asUByteArray().usePinned { saltPointer ->
                ikm.asUByteArray().usePinned { ikmPointer ->
                    crypto_kdf_hkdf_sha256_extract(
                        prkPointer.addressOf(0),
                        saltPointer.addressOf(0),
                        salt.size.toULong(),
                        ikmPointer.addressOf(0),
                        ikm.size.toULong(),
                    )
                }
            }
        }
        assertEquals(0, extractStatus)

        val output = UByteArray(CryptoVectors.HKDF_CYRILLIC_L)
        val infoSize = info.encodeToByteArray().size
        val expandStatus = prk.usePinned { prkPointer ->
            output.usePinned { outputPointer ->
                crypto_kdf_hkdf_sha256_expand(
                    outputPointer.addressOf(0),
                    output.size.toULong(),
                    info,
                    infoSize.toULong(),
                    prkPointer.addressOf(0),
                )
            }
        }
        assertEquals(0, expandStatus)

        assertContentEquals(CryptoVectors.hkdfCyrillicOkm, output.asByteArray())
    }

    @Test
    fun `crypto_aead_aes256gcm matches the zero key vector`() {
        val output = UByteArray(CryptoVectors.gcmAes256ZeroPlaintext.size + 16)
        val status = CryptoVectors.gcmAes256ZeroPlaintext.asUByteArray().usePinned { messagePointer ->
            CryptoVectors.gcmAes256ZeroKey.asUByteArray().usePinned { keyPointer ->
                CryptoVectors.gcmAes256ZeroIv.asUByteArray().usePinned { noncePointer ->
                    output.usePinned { outputPointer ->
                        crypto_aead_aes256gcm_encrypt(
                            outputPointer.addressOf(0),
                            null,
                            messagePointer.addressOf(0),
                            CryptoVectors.gcmAes256ZeroPlaintext.size.toULong(),
                            null,
                            0uL,
                            null,
                            noncePointer.addressOf(0),
                            keyPointer.addressOf(0),
                        )
                    }
                }
            }
        }
        assertEquals(0, status)

        val bytes = output.asByteArray()
        assertContentEquals(
            CryptoVectors.gcmAes256ZeroCiphertext,
            bytes.copyOf(CryptoVectors.gcmAes256ZeroCiphertext.size),
        )
        assertContentEquals(CryptoVectors.gcmAes256ZeroTag, bytes.copyOfRange(16, 32))
    }

    @Test
    fun `crypto_aead_aes256gcm decrypts what it encrypted`() {
        val key = ByteArray(32) { it.toByte() }
        val nonce = ByteArray(12) { (it + 3).toByte() }
        val plaintext = "sensitive payload".encodeToByteArray()
        val aad = "record-42".encodeToByteArray()

        val output = UByteArray(plaintext.size + 16)
        val encryptStatus = plaintext.asUByteArray().usePinned { messagePointer ->
            aad.asUByteArray().usePinned { aadPointer ->
                key.asUByteArray().usePinned { keyPointer ->
                    nonce.asUByteArray().usePinned { noncePointer ->
                        output.usePinned { outputPointer ->
                            crypto_aead_aes256gcm_encrypt(
                                outputPointer.addressOf(0),
                                null,
                                messagePointer.addressOf(0),
                                plaintext.size.toULong(),
                                aadPointer.addressOf(0),
                                aad.size.toULong(),
                                null,
                                noncePointer.addressOf(0),
                                keyPointer.addressOf(0),
                            )
                        }
                    }
                }
            }
        }
        assertEquals(0, encryptStatus)

        val decrypted = UByteArray(plaintext.size)
        val decryptStatus = output.usePinned { combinedPointer ->
            aad.asUByteArray().usePinned { aadPointer ->
                key.asUByteArray().usePinned { keyPointer ->
                    nonce.asUByteArray().usePinned { noncePointer ->
                        decrypted.usePinned { decryptedPointer ->
                            crypto_aead_aes256gcm_decrypt(
                                decryptedPointer.addressOf(0),
                                null,
                                null,
                                combinedPointer.addressOf(0),
                                output.size.toULong(),
                                aadPointer.addressOf(0),
                                aad.size.toULong(),
                                noncePointer.addressOf(0),
                                keyPointer.addressOf(0),
                            )
                        }
                    }
                }
            }
        }
        assertEquals(0, decryptStatus)

        assertContentEquals(plaintext, decrypted.asByteArray())
    }

    @Test
    fun `randombytes_buf fills the buffer`() {
        val bytes = ByteArray(32)
        bytes.asUByteArray().usePinned { pointer ->
            randombytes_buf(pointer.addressOf(0), bytes.size.toULong())
        }
        assertTrue(bytes.any { it != 0.toByte() })
    }

    private fun argon2id(password: String, salt: ByteArray): ByteArray {
        val output = UByteArray(32)
        val passwordBytes = password.encodeToByteArray()
        val status = salt.asUByteArray().usePinned { saltPointer ->
            output.usePinned { outputPointer ->
                crypto_pwhash(
                    outputPointer.addressOf(0),
                    output.size.toULong(),
                    password,
                    passwordBytes.size.toULong(),
                    saltPointer.addressOf(0),
                    3uL,
                    (65536 * 1024).toULong(),
                    crypto_pwhash_ALG_ARGON2ID13,
                )
            }
        }
        assertEquals(0, status)
        return output.asByteArray()
    }
}
