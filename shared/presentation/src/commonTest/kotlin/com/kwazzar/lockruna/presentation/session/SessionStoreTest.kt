package com.kwazzar.lockruna.presentation.session

import com.kwazzar.lockruna.application.session.ApplyAutoLockPolicy
import com.kwazzar.lockruna.application.session.LockVault
import com.kwazzar.lockruna.application.session.ObserveVaultState
import com.kwazzar.lockruna.domain.session.VaultKey
import com.kwazzar.lockruna.domain.session.VaultSession
import com.kwazzar.lockruna.presentation.support.FakeClock
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionStoreTest {

    private val clock = FakeClock(monotonicMillis = 100_000)
    private val session = VaultSession()
    private val store = SessionStore(
        observeVaultState = ObserveVaultState(session),
        lockVault = LockVault(session),
        applyAutoLockPolicy = ApplyAutoLockPolicy(session, clock, com.kwazzar.lockruna.domain.session.AutoLockPolicy.ONE_MINUTE),
    )

    @Test
    fun `the vault starts locked`() = runTest {
        assertTrue(store.state.value.isLocked)
    }

    @Test
    fun `unlocking the vault clears the locked flag`() = runTest {
        session.unlock(VaultKey.from(ByteArray(32) { 1 }))

        awaitLocked(false)

        assertFalse(store.state.value.isLocked)
    }

    @Test
    fun `resuming after the auto-lock timeout locks the vault again`() = runTest {
        session.unlock(VaultKey.from(ByteArray(32) { 1 }))
        awaitLocked(false)
        val effect = async(start = CoroutineStart.UNDISPATCHED) { store.effects.first() }

        store.dispatch(SessionIntent.Resumed(backgroundedAtMonotonicMillis = 100_000 - 61_000))

        assertEquals(SessionEffect.Locked, effect.await())
        assertNull(session.currentVaultKey())
    }

    @Test
    fun `a resume without a background period keeps the just unlocked session`() = runTest {
        session.unlock(VaultKey.from(ByteArray(32) { 1 }))
        awaitLocked(false)

        store.dispatch(SessionIntent.Resumed(backgroundedAtMonotonicMillis = 100_000 - 10))

        assertTrue(store.state.value.isLocked.not())
        assertTrue(session.currentVaultKey() != null)
    }

    @Test
    fun `the lock intent locks the vault`() = runTest {
        session.unlock(VaultKey.from(ByteArray(32) { 1 }))
        awaitLocked(false)
        val effect = async(start = CoroutineStart.UNDISPATCHED) { store.effects.first() }

        store.dispatch(SessionIntent.Lock)

        assertEquals(SessionEffect.Locked, effect.await())
        assertNull(session.currentVaultKey())
    }

    private suspend fun awaitLocked(expected: Boolean) = withContext(Dispatchers.Default) {
        withTimeout(5_000) {
            while (store.state.value.isLocked != expected) delay(10)
        }
    }
}
