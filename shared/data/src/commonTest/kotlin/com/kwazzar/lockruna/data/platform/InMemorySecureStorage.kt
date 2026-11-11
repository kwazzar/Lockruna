package com.kwazzar.lockruna.data.platform

/** Test double for the byte storage the platform secure storages implement. */
class InMemorySecureStorage(initial: Map<String, ByteArray> = emptyMap()) : SecureStorage {
    private val values = initial.toMutableMap()

    val keys: Set<String> get() = values.keys

    override suspend fun read(key: String): ByteArray? = values[key]?.copyOf()

    override suspend fun write(key: String, value: ByteArray) {
        values[key] = value.copyOf()
    }

    override suspend fun delete(key: String) {
        values.remove(key)
    }
}
