package com.kwazzar.lockruna.data.platform

/**
 * Byte storage with the platform's protection (Android Keystore, iOS Keychain).
 * A detail of `data`; the domain only sees the ports built on top of it.
 */
interface SecureStorage {
    suspend fun read(key: String): ByteArray?
    suspend fun write(key: String, value: ByteArray)
    suspend fun delete(key: String)
}
