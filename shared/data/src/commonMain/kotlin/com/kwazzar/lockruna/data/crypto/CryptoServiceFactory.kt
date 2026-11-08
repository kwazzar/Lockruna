package com.kwazzar.lockruna.data.crypto

import com.kwazzar.lockruna.domain.crypto.Argon2
import com.kwazzar.lockruna.domain.crypto.CryptoService
import com.kwazzar.lockruna.domain.crypto.PasswordNormalizer

expect class PlatformCrypto() : CryptoService

expect class PlatformNormalizer() : PasswordNormalizer

expect object Argon2Factory {
    fun create(): Argon2
}
