package com.kwazzar.lockruna.domain.session

import com.kwazzar.lockruna.domain.crypto.CryptoService
import com.kwazzar.lockruna.domain.vault.ItemId

/**
 * Clock providing both wall time and monotonic time.
 * Wall time is used for timestamps (updatedAt).
 * Monotonic time is used for auto-lock policy (not affected by system clock changes).
 */
interface Clock {
    /** Wall clock, epoch millis — для updatedAt. */
    fun nowMillis(): Long

    /** Монотонний час — для автоблокування; не залежить від налаштування системного годинника. */
    fun monotonicMillis(): Long
}

/**
 * Generates unique identifiers.
 */
interface IdGenerator {
    fun newItemId(): ItemId
    fun newUuid(): String
}

/**
 * Secure storage for the vault key (Keychain on iOS, Keystore on Android).
 */
interface SecureKeyStore {
    suspend fun saveVaultKey(key: VaultKey)
    suspend fun loadVaultKey(): VaultKey?
    suspend fun clear()
}

/**
 * Biometric authentication gate (Face ID, Touch ID, BiometricPrompt).
 */
interface BiometricGate {
    suspend fun isAvailable(): Boolean
    suspend fun authenticate(reason: String): Boolean
}

/**
 * In-memory vault key. Zeroized on lock.
 */
@JvmInline
value class VaultKey private constructor(val bytes: ByteArray) {
    fun zeroize() {
        bytes.fill(0)
    }

    companion object {
        private const val VAULT_KEY_SIZE = 32
        fun random(cryptoService: CryptoService): VaultKey = VaultKey(cryptoService.randomBytes(VAULT_KEY_SIZE))
        fun from(bytes: ByteArray): VaultKey = VaultKey(bytes.copyOf())
    }
}
