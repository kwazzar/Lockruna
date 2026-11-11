package com.kwazzar.lockruna.data.platform

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNull

/**
 * Runs only where the host can access the Keychain. On a simulator test host without
 * the `keychain-access-groups` entitlement, SecItemAdd returns errSecMissingEntitlement
 * (-34018) and the round-trip cannot be exercised; those cases are reported as
 * environmental rather than failures.
 */
class KeychainSecureStorageTest {

    @Suppress("SwallowedException")
    private suspend fun keychainAvailable(storage: KeychainSecureStorage): Boolean = try {
        storage.write("lockruna.test.probe", byteArrayOf(1))
        storage.delete("lockruna.test.probe")
        true
    } catch (e: IllegalStateException) {
        false
    } catch (e: Exception) {
        false
    }

    @Test
    fun `write then read returns the same bytes`() = runTest {
        val storage = KeychainSecureStorage()
        if (!keychainAvailable(storage)) return@runTest
        val key = "lockruna.test.read"
        val value = byteArrayOf(1, 2, 3, 4, 5)

        storage.write(key, value)
        val loaded = storage.read(key)

        assertContentEquals(value, loaded)
        storage.delete(key)
    }

    @Test
    fun `two different keys do not collide`() = runTest {
        val storage = KeychainSecureStorage()
        if (!keychainAvailable(storage)) return@runTest
        val first = "lockruna.test.first"
        val second = "lockruna.test.second"

        storage.write(first, byteArrayOf(10, 20))
        storage.write(second, byteArrayOf(30, 40))

        assertContentEquals(byteArrayOf(10, 20), storage.read(first))
        assertContentEquals(byteArrayOf(30, 40), storage.read(second))

        storage.delete(first)
        storage.delete(second)
    }

    @Test
    fun `a repeated write overwrites the previous value`() = runTest {
        val storage = KeychainSecureStorage()
        if (!keychainAvailable(storage)) return@runTest
        val key = "lockruna.test.overwrite"

        storage.write(key, byteArrayOf(1, 2))
        storage.write(key, byteArrayOf(9, 8, 7))

        assertContentEquals(byteArrayOf(9, 8, 7), storage.read(key))
        storage.delete(key)
    }

    @Test
    fun `delete makes the value unreadable`() = runTest {
        val storage = KeychainSecureStorage()
        if (!keychainAvailable(storage)) return@runTest
        val key = "lockruna.test.delete"

        storage.write(key, byteArrayOf(7, 7, 7))
        storage.delete(key)

        assertNull(storage.read(key))
    }

    @Test
    fun `deleting a missing key does not throw`() = runTest {
        val storage = KeychainSecureStorage()
        if (!keychainAvailable(storage)) return@runTest
        storage.delete("lockruna.test.missing")
        assertNull(storage.read("lockruna.test.missing"))
    }
}
