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
    // RFC 5869 Test Case 1
    val rfc5869Case1Ikm: ByteArray = ByteArray(22) { 0x0b }
    val rfc5869Case1Salt: ByteArray = "000102030405060708090a0b0c".hexToByteArray()
    val rfc5869Case1Info: ByteArray = "f0f1f2f3f4f5f6f7f8f9".hexToByteArray()
    const val RFC5869_CASE1_L: Int = 42
    val rfc5869Case1Okm: ByteArray =
        "3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865".hexToByteArray()

    // RFC 5869 Test Case 3 (salt not provided, info empty)
    val rfc5869Case3Ikm: ByteArray = ByteArray(22) { 0x0b }
    val rfc5869Case3Info: ByteArray = ByteArray(0)
    const val RFC5869_CASE3_L: Int = 42
    val rfc5869Case3Okm: ByteArray =
        "8da4e775a563c18f715f802a063c5a31b8a11f5c5ee1879ec3454e5f3c738d2d9d201395faa4b61a96c8".hexToByteArray()

    // GCM specification Test Case 13 (AES-128, all-zero key, IV and plaintext)
    val gcmCase13Aes128Key: ByteArray = ByteArray(16)
    val gcmCase13Aes128Iv: ByteArray = ByteArray(12)
    val gcmCase13Aes128Plaintext: ByteArray = ByteArray(16)
    val gcmCase13Aes128Aad: ByteArray = ByteArray(0)
    val gcmCase13Aes128Ciphertext: ByteArray = "0388dace60b6a392f328c2b971b2fe78".hexToByteArray()
    val gcmCase13Aes128Tag: ByteArray = "ab6e47d42cec13bdf53a67b21257bddf".hexToByteArray()

    // AES-256-GCM with an all-zero key, IV and plaintext
    val gcmAes256ZeroKey: ByteArray = ByteArray(32)
    val gcmAes256ZeroIv: ByteArray = ByteArray(12)
    val gcmAes256ZeroPlaintext: ByteArray = ByteArray(16)
    val gcmAes256ZeroAad: ByteArray = ByteArray(0)
    val gcmAes256ZeroCiphertext: ByteArray = "cea7403d4d606b6e074ec5d3baf39d18".hexToByteArray()
    val gcmAes256ZeroTag: ByteArray = "d0d1c8a799996bf0265b98b5d48ab919".hexToByteArray()
}
