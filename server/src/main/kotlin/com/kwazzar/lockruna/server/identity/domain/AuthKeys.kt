package com.kwazzar.lockruna.server.identity.domain

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Key handling for the zero-knowledge flow: the server stores only `HMAC(pepper, authKey)`
 * and compares it in constant time.
 */
object AuthKeys {
    private const val HMAC_SHA256 = "HmacSHA256"
    private const val SHA_256 = "SHA-256"

    fun hash(pepper: ByteArray, key: ByteArray): ByteArray {
        val mac = Mac.getInstance(HMAC_SHA256)
        mac.init(SecretKeySpec(pepper, HMAC_SHA256))
        return mac.doFinal(key)
    }

    fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean = MessageDigest.isEqual(a, b)

    /** Rate limiter keys carry a hash of the email, never the address itself. */
    fun emailKey(email: String): String = MessageDigest.getInstance(SHA_256)
        .digest(email.lowercase().toByteArray())
        .joinToString("") { "%02x".format(it) }
}
