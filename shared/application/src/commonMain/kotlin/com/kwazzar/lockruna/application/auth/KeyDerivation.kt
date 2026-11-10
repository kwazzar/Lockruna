package com.kwazzar.lockruna.application.auth

import com.kwazzar.lockruna.domain.auth.SealedBlob
import com.kwazzar.lockruna.domain.crypto.CryptoService
import com.kwazzar.lockruna.domain.crypto.GcmCiphertext
import com.kwazzar.lockruna.domain.crypto.HkdfLabels
import com.kwazzar.lockruna.domain.crypto.KdfParams

/**
 * Centralized key derivation logic. Internal - only for tests.
 */
internal object KeyDerivation {

    private const val HKDF_OUTPUT_LENGTH = 32

    suspend fun normalizeAndDerive(
        password: String,
        salt: ByteArray,
        argon2: com.kwazzar.lockruna.domain.crypto.Argon2,
        normalizer: com.kwazzar.lockruna.domain.crypto.PasswordNormalizer,
    ): ByteArray = argon2.hash(normalizer.normalize(password), salt, KdfParams.SUPPORTED)

    fun authKey(masterKey: ByteArray, crypto: CryptoService): ByteArray =
        crypto.hkdfSha256(masterKey, null, HkdfLabels.AUTH, HKDF_OUTPUT_LENGTH)

    fun kek(masterKey: ByteArray, crypto: CryptoService): ByteArray =
        crypto.hkdfSha256(masterKey, null, HkdfLabels.KEK, HKDF_OUTPUT_LENGTH)

    fun unwrapVaultKey(kek: ByteArray, blob: ByteArray, aadEmail: String, crypto: CryptoService): ByteArray {
        val sealed = SealedBlob.unpack(blob)
        val ciphertext = GcmCiphertext(
            ciphertext = sealed.payload.copyOfRange(0, sealed.payload.size - 16),
            nonce = sealed.nonce,
            tag = sealed.payload.copyOfRange(sealed.payload.size - 16, sealed.payload.size),
        )
        return crypto.decryptGcm(kek, ciphertext, aadEmail.toByteArray())
    }
}
