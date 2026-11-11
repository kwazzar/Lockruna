package com.kwazzar.lockruna.presentation.support

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.crypto.CryptoService
import com.kwazzar.lockruna.domain.crypto.GcmCiphertext
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.Clock
import com.kwazzar.lockruna.domain.vault.Clipboard

class RecordingClipboard : Clipboard {
    val values = mutableListOf<String>()

    override suspend fun copySecret(value: String): Result<Unit, AppError> {
        values += value
        return Result.Success(Unit)
    }
}

class FakeClock(var wallMillis: Long = 0, var monotonicMillis: Long = 0) : Clock {
    override fun nowMillis(): Long = wallMillis

    override fun monotonicMillis(): Long = monotonicMillis
}

class FakeCryptoService : CryptoService {
    var decryptGcmFails: Boolean = false

    override fun hkdfSha256(inputKey: ByteArray, salt: ByteArray?, info: String, length: Int): ByteArray =
        ByteArray(length)

    override fun encryptGcm(key: ByteArray, plaintext: ByteArray, aad: ByteArray): GcmCiphertext =
        GcmCiphertext(ciphertext = plaintext, nonce = ByteArray(12), tag = ByteArray(16))

    override fun decryptGcm(key: ByteArray, ciphertext: GcmCiphertext, aad: ByteArray): ByteArray {
        if (decryptGcmFails) throw IllegalArgumentException("GCM authentication failed")
        return ciphertext.ciphertext
    }

    override fun randomBytes(size: Int): ByteArray = ByteArray(size)

    override fun sha256(input: ByteArray): ByteArray = ByteArray(32)
}
