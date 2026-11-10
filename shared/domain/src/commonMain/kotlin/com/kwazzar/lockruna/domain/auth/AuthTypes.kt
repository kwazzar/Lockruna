package com.kwazzar.lockruna.domain.auth

import com.kwazzar.lockruna.domain.crypto.KdfParams as DomainKdfParams

import com.kwazzar.lockruna.domain.crypto.KdfParams

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Int
) {
    override fun toString() = "AuthTokens(redacted)"
}

data class AuthenticatedUser(
    val userId: String,
    val deviceId: String,
    val wrappedVaultKey: ByteArray,
    val keyNonce: ByteArray,
    val kdfSalt: ByteArray,
    val kdfParams: DomainKdfParams,
    val tokens: AuthTokens
) {
    override fun toString() = "AuthenticatedUser(userId=$userId)"
}

data class PreLoginData(
    val kdfSalt: ByteArray,
    val kdfParams: DomainKdfParams
)

/**
 * Wire format for wrapped keys: nonce(12) || ciphertext || tag (AES-GCM 128-bit tag).
 */
data class SealedBlob(val nonce: ByteArray, val payload: ByteArray) {
    companion object {
        fun pack(nonce: ByteArray, ciphertext: ByteArray): ByteArray {
            require(nonce.size == 12) { "nonce must be 12 bytes" }
            return nonce + ciphertext
        }

        fun unpack(bytes: ByteArray): SealedBlob {
            require(bytes.size >= 12) { "blob too short" }
            return SealedBlob(
                nonce = bytes.copyOfRange(0, 12),
                payload = bytes.copyOfRange(12, bytes.size)
            )
        }
    }
}