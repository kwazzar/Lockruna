package com.kwazzar.lockruna.domain.crypto

interface PasswordNormalizer {
    /** NFKC. The UTF-8 encoding happens in the KDF. */
    fun normalize(raw: String): String
}
