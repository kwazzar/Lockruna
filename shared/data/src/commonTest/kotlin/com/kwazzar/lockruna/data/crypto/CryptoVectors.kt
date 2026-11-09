package com.kwazzar.lockruna.data.crypto

/** One Argon2id reference case: the password, the salt and the expected hash. */
data class Argon2Case(val password: String, val salt: ByteArray, val expected: ByteArray)

/** One NFKC reference case: the raw input and the expected normalized output. */
data class NormalizationCase(val raw: String, val expected: String)

fun String.hexToByteArray(): ByteArray {
    check(length % 2 == 0) { "Hex string must have an even length" }
    return ByteArray(length / 2) { i ->
        val index = i * 2
        this.substring(index, index + 2).toInt(16).toByte()
    }
}

fun ByteArray.toHex(): String = joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

object CryptoVectors {
    // RFC 5869 Test Case 3 (salt not provided, info empty)
    val rfc5869Case3Ikm: ByteArray = ByteArray(22) { 0x0b }
    const val RFC5869_CASE3_INFO: String = ""
    const val RFC5869_CASE3_L: Int = 42
    val rfc5869Case3Okm: ByteArray =
        "8da4e775a563c18f715f802a063c5a31b8a11f5c5ee1879ec3454e5f3c738d2d9d201395faa4b61a96c8".hexToByteArray()

    // HKDF-SHA256 with a domain label, computed with an independent RFC 5869 implementation
    val hkdfLabelIkm: ByteArray = ByteArray(22) { 0x0b }
    val hkdfLabelSalt: ByteArray = "000102030405060708090a0b0c".hexToByteArray()
    const val HKDF_LABEL_INFO: String = "passkeeper/auth/v1"
    const val HKDF_LABEL_L: Int = 42
    val hkdfLabelOkm: ByteArray =
        "3b8405d337146f50e28e609fa61c62df0c1361089de771952bbb77285f70269e408deaa3fca7817553aa".hexToByteArray()

    // Non-ASCII label: pins the UTF-8 encoding of the info parameter
    val hkdfCyrillicIkm: ByteArray = ByteArray(22) { 0x0b }
    val hkdfCyrillicSalt: ByteArray = "000102030405060708090a0b0c".hexToByteArray()
    const val HKDF_CYRILLIC_INFO: String = "ключ"
    const val HKDF_CYRILLIC_L: Int = 32
    val hkdfCyrillicOkm: ByteArray =
        "07efb15469b42533fc8d30956f1781eebab2bd00d585fa54e15610c62779626e".hexToByteArray()

    // AES-256-GCM with an all-zero key, IV and plaintext (NIST CAVP)
    val gcmAes256ZeroKey: ByteArray = ByteArray(32)
    val gcmAes256ZeroIv: ByteArray = ByteArray(12)
    val gcmAes256ZeroPlaintext: ByteArray = ByteArray(16)
    val gcmAes256ZeroAad: ByteArray = ByteArray(0)
    val gcmAes256ZeroCiphertext: ByteArray = "cea7403d4d606b6e074ec5d3baf39d18".hexToByteArray()
    val gcmAes256ZeroTag: ByteArray = "d0d1c8a799996bf0265b98b5d48ab919".hexToByteArray()

    // Argon2id (m = 65536 KiB, t = 3, p = 1, salt 16 bytes, output 32 bytes),
    // generated with the reference C implementation (argon2-cffi).
    val argon2Cases: List<Argon2Case> = listOf(
        Argon2Case(
            password = "correct horse battery staple",
            salt = "000102030405060708090a0b0c0d0e0f".hexToByteArray(),
            expected = "0d1a3c6523c8f06e4e0af9c515aa5b5448cfebd6838f2d52c3d8b6ef8ddc3c2e".hexToByteArray(),
        ),
        Argon2Case(
            password = "correct horse battery staple",
            salt = "101112131415161718191a1b1c1d1e1f".hexToByteArray(),
            expected = "1a563099620be88a73c7bd53da0b313e98060493b58ba22254a065c3d7db26c3".hexToByteArray(),
        ),
        Argon2Case(
            password = "correct horse battery staple",
            salt = "a1b2c3d4e5f60718293a4b5c6d7e8f90".hexToByteArray(),
            expected = "79c663db87eebd2abf49fb5435689f6a9a87e4cde90de3fa375b529b05ffc7f6".hexToByteArray(),
        ),
        // Non-ASCII password: pins the UTF-8 encoding of the password on both platforms.
        Argon2Case(
            password = "пароль-测试",
            salt = "000102030405060708090a0b0c0d0e0f".hexToByteArray(),
            expected = "1ff0681253591582cd2fd999184800ed9a6bb8d8ab879168352f8008d08b1c57".hexToByteArray(),
        ),
    )

    // NFKC, per the Unicode standard.
    val normalizationCases: List<NormalizationCase> = listOf(
        NormalizationCase(raw = "пароль", expected = "пароль"),
        NormalizationCase(raw = "e\u0301", expected = "é"),
        NormalizationCase(raw = "ﬁ", expected = "fi"),
        NormalizationCase(raw = "Ａ", expected = "A"),
        // U+1E9E has no compatibility decomposition, so NFKC keeps it as is.
        NormalizationCase(raw = "ẞ", expected = "ẞ"),
        NormalizationCase(raw = "１２３", expected = "123"),
        NormalizationCase(raw = "①", expected = "1"),
        NormalizationCase(raw = "ｶﾞ", expected = "ガ"),
        // U+FDFA expands to 18 code units, so it overflows the initial ICU buffer.
        NormalizationCase(
            raw = "\uFDFA",
            expected = "صلى الله عليه وسلم",
        ),
    )
}
