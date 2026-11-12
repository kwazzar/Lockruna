package com.kwazzar.lockruna.data.crypto

import com.kwazzar.lockruna.domain.auth.SealedBlob
import com.kwazzar.lockruna.domain.crypto.GcmCiphertext
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

/**
 * The wire layout of a wrapped key, exercised with the platform's real cipher.
 * A missing or mismatched nonce makes the GCM tag fail, so the pack and the unpack
 * must agree on the nonce the cipher actually used.
 */
class SealedBlobTest {
    private val crypto = PlatformCrypto()
    private val key = crypto.hkdfSha256(ByteArray(32) { 2 }, null, "kek", 32)
    private val vaultKey = crypto.randomBytes(32)

    @Test
    fun `pack and unpack round trip the vault key`() {
        val blob = pack()

        val sealed = SealedBlob.unpack(blob)
        val unwrapped = crypto.decryptGcm(key, sealed.toGcm(), "user@example.com".encodeToByteArray())

        assertContentEquals(vaultKey, unwrapped)
    }

    @Test
    fun `unpack with the wrong aad fails`() {
        val blob = pack()

        val sealed = SealedBlob.unpack(blob)

        assertFailsWith<IllegalArgumentException> {
            crypto.decryptGcm(key, sealed.toGcm(), "other@example.com".encodeToByteArray())
        }
    }

    @Test
    fun `a blob whose nonce was replaced no longer opens`() {
        val blob = pack()
        val tampered = crypto.randomBytes(12) + blob.copyOfRange(12, blob.size)

        val sealed = SealedBlob.unpack(tampered)

        assertFailsWith<IllegalArgumentException> {
            crypto.decryptGcm(key, sealed.toGcm(), "user@example.com".encodeToByteArray())
        }
    }

    private fun pack(): ByteArray {
        val ciphertext = crypto.encryptGcm(key, vaultKey, "user@example.com".encodeToByteArray())
        return SealedBlob.pack(ciphertext.nonce, ciphertext.ciphertext + ciphertext.tag)
    }

    private fun SealedBlob.toGcm(): GcmCiphertext = GcmCiphertext(
        ciphertext = payload.copyOfRange(0, payload.size - 16),
        nonce = nonce,
        tag = payload.copyOfRange(payload.size - 16, payload.size),
    )
}
