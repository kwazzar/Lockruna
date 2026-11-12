package com.kwazzar.lockruna.domain.crypto

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError

/**
 * BIP39 recovery-key codec (ADR-003): 128 bits of entropy become 12 words,
 * where the last word carries a 4-bit SHA-256 checksum.
 *
 * The recovery key is generated once during registration, shown to the user and never stored
 * on the device. Only keys derived from it reach the server.
 */
object Bip39 {
    const val ENTROPY_BYTES: Int = 16
    const val WORD_COUNT: Int = 12

    private const val BITS_PER_WORD = 11
    private const val ENTROPY_BITS = ENTROPY_BYTES * Byte.SIZE_BITS
    private const val CHECKSUM_BITS = 4
    private const val TOTAL_BITS = ENTROPY_BITS + CHECKSUM_BITS

    /** Encodes [entropy] (exactly 16 bytes) as 12 words. */
    fun encode(entropy: ByteArray, crypto: CryptoService): List<String> {
        require(entropy.size == ENTROPY_BYTES) {
            "BIP39 entropy must be $ENTROPY_BYTES bytes, was ${entropy.size}"
        }
        val bits = entropyBits(entropy, crypto.sha256(entropy))
        return (0 until WORD_COUNT).map { index ->
            BIP39_ENGLISH_WORDS[bits.word(index * BITS_PER_WORD)]
        }
    }

    /**
     * Decodes 12 words back into 16 bytes of entropy.
     * Unknown words, a wrong word count and a checksum mismatch all yield [AppError.InvalidRecoveryKey].
     */
    fun decode(words: List<String>, crypto: CryptoService): Result<ByteArray, AppError> {
        if (words.size != WORD_COUNT) {
            return Result.Failure(AppError.InvalidRecoveryKey("expected $WORD_COUNT words, was ${words.size}"))
        }
        val indices = IntArray(WORD_COUNT)
        for (index in words.indices) {
            val position = BIP39_ENGLISH_WORDS.indexOf(words[index].trim().lowercase())
            if (position < 0) return Result.Failure(AppError.InvalidRecoveryKey("unknown word"))
            indices[index] = position
        }

        val entropy = ByteArray(ENTROPY_BYTES)
        val checksum = ByteArray(CHECKSUM_BITS)
        for (index in 0 until TOTAL_BITS) {
            val word = indices[index / BITS_PER_WORD]
            val bit = (word shr (BITS_PER_WORD - 1 - index % BITS_PER_WORD)) and 1 == 1
            if (index < ENTROPY_BITS) {
                val byteIndex = index / Byte.SIZE_BITS
                if (bit) {
                    entropy[byteIndex] =
                        (entropy[byteIndex].toInt() or (1 shl (Byte.SIZE_BITS - 1 - index % Byte.SIZE_BITS))).toByte()
                }
            } else if (bit) {
                checksum[index - ENTROPY_BITS] = 1
            }
        }

        val expected = checksumBits(crypto.sha256(entropy))
        if (!checksum.contentEquals(expected)) {
            return Result.Failure(AppError.InvalidRecoveryKey("checksum mismatch"))
        }
        return Result.Success(entropy)
    }

    private fun entropyBits(entropy: ByteArray, digest: ByteArray): IntArray {
        val bits = IntArray(TOTAL_BITS)
        for (index in 0 until ENTROPY_BITS) {
            val byte = entropy[index / Byte.SIZE_BITS].toInt()
            if ((byte shr (Byte.SIZE_BITS - 1 - index % Byte.SIZE_BITS)) and 1 == 1) bits[index] = 1
        }
        for (index in 0 until CHECKSUM_BITS) {
            val byte = digest[index / Byte.SIZE_BITS].toInt()
            if ((byte shr (Byte.SIZE_BITS - 1 - index % Byte.SIZE_BITS)) and 1 == 1) bits[ENTROPY_BITS + index] = 1
        }
        return bits
    }

    private fun IntArray.word(from: Int): Int {
        var value = 0
        for (bit in 0 until BITS_PER_WORD) value = (value shl 1) or this[from + bit]
        return value
    }

    private fun checksumBits(digest: ByteArray): ByteArray {
        val checksum = ByteArray(CHECKSUM_BITS)
        for (index in checksum.indices) {
            val byte = digest[index / Byte.SIZE_BITS].toInt()
            if ((byte shr (Byte.SIZE_BITS - 1 - index % Byte.SIZE_BITS)) and 1 == 1) checksum[index] = 1
        }
        return checksum
    }
}
