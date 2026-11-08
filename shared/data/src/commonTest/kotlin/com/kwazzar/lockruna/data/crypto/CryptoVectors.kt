package com.kwazzar.lockruna.data.crypto

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
}
