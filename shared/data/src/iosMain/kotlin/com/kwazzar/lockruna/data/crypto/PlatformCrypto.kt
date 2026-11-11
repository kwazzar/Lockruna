@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.kwazzar.lockruna.data.crypto

import com.kwazzar.lockruna.domain.crypto.CryptoService
import com.kwazzar.lockruna.domain.crypto.GcmCiphertext
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import libsodium.crypto_aead_aes256gcm_decrypt
import libsodium.crypto_aead_aes256gcm_encrypt
import libsodium.crypto_hash_sha256
import libsodium.crypto_kdf_hkdf_sha256_expand
import libsodium.crypto_kdf_hkdf_sha256_extract
import libsodium.randombytes_buf

private const val GCM_KEY_SIZE: Int = 32
private const val GCM_NONCE_SIZE: Int = 12
private const val GCM_TAG_SIZE: Int = 16
private const val HKDF_PRF_SIZE: Int = 32

actual class PlatformCrypto actual constructor() : CryptoService {
    actual override fun hkdfSha256(inputKey: ByteArray, salt: ByteArray?, info: String, length: Int): ByteArray {
        val prk = UByteArray(HKDF_PRF_SIZE)
        val saltSize = salt?.size ?: 0
        val extractResult = withCBytes(inputKey) { keyPointer ->
            withCBytes(salt ?: ByteArray(0)) { saltPointer ->
                prk.usePinned { prkPointer ->
                    crypto_kdf_hkdf_sha256_extract(
                        prkPointer.addressOf(0),
                        saltPointer,
                        saltSize.toULong(),
                        keyPointer,
                        inputKey.size.toULong(),
                    )
                }
            }
        }
        check(extractResult == 0) { "HKDF extract failed" }

        val infoSize = info.encodeToByteArray().size
        val output = UByteArray(length)
        val expandResult = prk.usePinned { prkPointer ->
            output.usePinned { outputPointer ->
                crypto_kdf_hkdf_sha256_expand(
                    outputPointer.addressOf(0),
                    length.toULong(),
                    info,
                    infoSize.toULong(),
                    prkPointer.addressOf(0),
                )
            }
        }
        check(expandResult == 0) { "HKDF expand failed" }
        return output.asByteArray()
    }

    actual override fun encryptGcm(key: ByteArray, plaintext: ByteArray, aad: ByteArray): GcmCiphertext {
        require(key.size == GCM_KEY_SIZE) { "AES-256-GCM key must be $GCM_KEY_SIZE bytes" }
        val nonce = randomBytes(GCM_NONCE_SIZE)
        val output = UByteArray(plaintext.size + GCM_TAG_SIZE)
        val status = withCBytes(key) { keyPointer ->
            withCBytes(nonce) { noncePointer ->
                withCBytes(aad) { aadPointer ->
                    withCBytes(plaintext) { messagePointer ->
                        output.usePinned { outputPointer ->
                            crypto_aead_aes256gcm_encrypt(
                                outputPointer.addressOf(0),
                                null,
                                messagePointer,
                                plaintext.size.toULong(),
                                aadPointer,
                                aad.size.toULong(),
                                null,
                                noncePointer,
                                keyPointer,
                            )
                        }
                    }
                }
            }
        }
        check(status == 0) { "AES-256-GCM encryption failed" }
        val bytes = output.asByteArray()
        return GcmCiphertext(
            ciphertext = bytes.copyOf(plaintext.size),
            nonce = nonce,
            tag = bytes.copyOfRange(plaintext.size, plaintext.size + GCM_TAG_SIZE),
        )
    }

    actual override fun decryptGcm(key: ByteArray, ciphertext: GcmCiphertext, aad: ByteArray): ByteArray {
        require(key.size == GCM_KEY_SIZE) { "AES-256-GCM key must be $GCM_KEY_SIZE bytes" }
        val combined = ciphertext.ciphertext + ciphertext.tag
        val output = UByteArray(ciphertext.ciphertext.size)
        val status = withCBytes(key) { keyPointer ->
            withCBytes(ciphertext.nonce) { noncePointer ->
                withCBytes(aad) { aadPointer ->
                    withCBytes(combined) { combinedPointer ->
                        output.usePinned { outputPointer ->
                            crypto_aead_aes256gcm_decrypt(
                                outputPointer.addressOf(0),
                                null,
                                null,
                                combinedPointer,
                                combined.size.toULong(),
                                aadPointer,
                                aad.size.toULong(),
                                noncePointer,
                                keyPointer,
                            )
                        }
                    }
                }
            }
        }
        if (status != 0) {
            throw IllegalArgumentException("GCM authentication failed")
        }
        return output.asByteArray()
    }

    actual override fun randomBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        withCBytes(bytes) { pointer ->
            randombytes_buf(pointer, size.toULong())
        }
        return bytes
    }

    actual override fun sha256(input: ByteArray): ByteArray {
        val out = ByteArray(32)
        val rc = withCBytes(input) { inPointer ->
            out.asUByteArray().usePinned { outPinned ->
                crypto_hash_sha256(outPinned.addressOf(0), inPointer, input.size.toULong())
            }
        }
        check(rc == 0) { "crypto_hash_sha256 failed" }
        return out
    }
}
