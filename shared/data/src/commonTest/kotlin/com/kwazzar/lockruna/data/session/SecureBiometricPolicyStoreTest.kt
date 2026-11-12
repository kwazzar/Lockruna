package com.kwazzar.lockruna.data.session

import com.kwazzar.lockruna.data.platform.InMemorySecureStorage
import com.kwazzar.lockruna.data.session.SecureBiometricPolicyStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The biometric policy: the failure count survives a restart, the unlock does not (US-07). */
class SecureBiometricPolicyStoreTest {

    @Test
    fun `a fresh store has no failures and no unlock`() = runTest {
        val store = SecureBiometricPolicyStore(InMemorySecureStorage())

        assertEquals(0, store.failures())
        assertNull(store.lastSuccessfulUnlock())
    }

    @Test
    fun `failures accumulate and reset`() = runTest {
        val storage = InMemorySecureStorage()
        val store = SecureBiometricPolicyStore(storage)

        store.recordFailure()
        store.recordFailure()
        assertEquals(2, store.failures())

        store.resetFailures()
        assertEquals(0, store.failures())
    }

    @Test
    fun `the failure count survives a restart`() = runTest {
        val storage = InMemorySecureStorage()
        val store = SecureBiometricPolicyStore(storage)
        store.recordFailure()

        val restarted = SecureBiometricPolicyStore(storage)

        assertEquals(1, restarted.failures())
    }

    @Test
    fun `the last successful unlock does not survive a restart`() = runTest {
        val storage = InMemorySecureStorage()
        val store = SecureBiometricPolicyStore(storage)
        store.setLastSuccessfulUnlock(1_700_000_000_000L)

        val restarted = SecureBiometricPolicyStore(storage)

        // US-07: the master password is required after a restart, so a stored timestamp
        // must not open the biometric path on its own.
        assertNull(restarted.lastSuccessfulUnlock())
    }

    @Test
    fun `a corrupted value reads as a clean slate`() = runTest {
        val storage = InMemorySecureStorage()
        storage.write("biometric_failures", ByteArray(4) { 9 })

        val store = SecureBiometricPolicyStore(storage)

        assertEquals(0, store.failures())
        assertNull(store.lastSuccessfulUnlock())
    }
}
