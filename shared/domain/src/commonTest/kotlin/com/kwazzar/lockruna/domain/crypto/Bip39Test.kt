package com.kwazzar.lockruna.domain.crypto

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** SHA-256 stands in for the platform digest here: the codec must match BIP39's spec, not a fake. */
private class Sha256Crypto : CryptoService {
    override fun hkdfSha256(inputKey: ByteArray, salt: ByteArray?, info: String, length: Int): ByteArray =
        ByteArray(length)
    override fun encryptGcm(key: ByteArray, plaintext: ByteArray, aad: ByteArray): GcmCiphertext =
        GcmCiphertext(ciphertext = plaintext, nonce = ByteArray(12), tag = ByteArray(16))
    override fun decryptGcm(key: ByteArray, ciphertext: GcmCiphertext, aad: ByteArray): ByteArray =
        ciphertext.ciphertext
    override fun randomBytes(size: Int): ByteArray = ByteArray(size)
    override fun sha256(input: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(input)
}

class Bip39Test {
    private val crypto = Sha256Crypto()

    @Test
    fun `the all-zero entropy encodes to the known BIP39 vector`() {
        val words = Bip39.encode(ByteArray(Bip39.ENTROPY_BYTES), crypto)

        assertEquals(Bip39.WORD_COUNT, words.size)
        assertEquals("abandon", words.first())
        // 16 zero bytes plus a zero checksum: the last 11 bits are the index of "about".
        assertEquals("about", words.last())
        assertTrue(words.drop(1).dropLast(1).all { it == "abandon" })
    }

    @Test
    fun `the all-ff entropy encodes to the known BIP39 vector`() {
        val words = Bip39.encode(ByteArray(Bip39.ENTROPY_BYTES) { 0x7f }, crypto)

        assertEquals(
            "legal winner thank year wave sausage worth useful legal winner thank yellow",
            words.joinToString(" "),
        )
    }

    @Test
    fun `encode and decode round-trip arbitrary entropy`() {
        val entropy = ByteArray(Bip39.ENTROPY_BYTES) { (it * 17 + 3).toByte() }

        val decoded = Bip39.decode(Bip39.encode(entropy, crypto), crypto)

        assertTrue(decoded is Result.Success)
        assertContentEquals(entropy, (decoded as Result.Success).value)
    }

    @Test
    fun `a tampered word fails the checksum`() {
        val words = Bip39.encode(ByteArray(Bip39.ENTROPY_BYTES) { (it * 5).toByte() }, crypto)
        val tampered = words.toMutableList()
        tampered[0] = if (words[0] == "abandon") "ability" else "abandon"

        val decoded = Bip39.decode(tampered, crypto)

        assertTrue(decoded is Result.Failure)
        assertTrue((decoded as Result.Failure).error is AppError.InvalidRecoveryKey)
    }

    @Test
    fun `a word outside the list is rejected`() {
        val words = Bip39.encode(ByteArray(Bip39.ENTROPY_BYTES), crypto)
        val tampered = words.toMutableList()
        tampered[3] = "notaword"

        val decoded = Bip39.decode(tampered, crypto)

        assertTrue(decoded is Result.Failure)
        assertTrue((decoded as Result.Failure).error is AppError.InvalidRecoveryKey)
    }

    @Test
    fun `a wrong word count is rejected`() {
        val decoded = Bip39.decode(List(Bip39.WORD_COUNT + 1) { "abandon" }, crypto)

        assertTrue(decoded is Result.Failure)
        assertTrue((decoded as Result.Failure).error is AppError.InvalidRecoveryKey)
    }
}
