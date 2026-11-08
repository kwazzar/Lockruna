package com.kwazzar.lockruna.domain.crypto

interface PasswordNormalizer {
    /** NFKC, потім UTF-8. */
    fun normalize(raw: String): ByteArray
}
