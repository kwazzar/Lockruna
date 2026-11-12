package com.kwazzar.lockruna.data.session

import com.kwazzar.lockruna.data.platform.InMemorySecureStorage
import com.kwazzar.lockruna.data.session.SecureAutoLockSettingsStore
import com.kwazzar.lockruna.domain.session.AutoLockPolicy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The user's auto-lock choice survives a restart (US-08). */
class SecureAutoLockSettingsStoreTest {

    @Test
    fun `defaults to one minute when nothing is stored`() = runTest {
        val store = SecureAutoLockSettingsStore(InMemorySecureStorage())

        assertEquals(AutoLockPolicy.ONE_MINUTE, store.load())
    }

    @Test
    fun `a saved choice survives a restart`() = runTest {
        val storage = InMemorySecureStorage()
        val store = SecureAutoLockSettingsStore(storage)

        store.save(AutoLockPolicy.FIFTEEN_MINUTES)

        assertEquals(AutoLockPolicy.FIFTEEN_MINUTES, SecureAutoLockSettingsStore(storage).load())
    }

    @Test
    fun `an unknown stored value falls back to the default`() = runTest {
        val storage = InMemorySecureStorage()
        storage.write("auto_lock_policy", ByteArray(3) { 7 })

        assertEquals(AutoLockPolicy.ONE_MINUTE, SecureAutoLockSettingsStore(storage).load())
    }

    @Test
    fun `clearing leaves the default`() = runTest {
        val storage = InMemorySecureStorage()
        val store = SecureAutoLockSettingsStore(storage)
        store.save(AutoLockPolicy.IMMEDIATE)

        storage.delete("auto_lock_policy")

        assertEquals(AutoLockPolicy.ONE_MINUTE, store.load())
        assertNull(storage.read("auto_lock_policy"))
    }
}
