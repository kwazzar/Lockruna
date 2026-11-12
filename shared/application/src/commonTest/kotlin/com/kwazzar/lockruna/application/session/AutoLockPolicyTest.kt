package com.kwazzar.lockruna.application.session

import com.kwazzar.lockruna.application.support.FakeClock
import com.kwazzar.lockruna.application.support.FakeCryptoService
import com.kwazzar.lockruna.domain.session.AutoLockPolicy
import com.kwazzar.lockruna.domain.session.AutoLockSettingsStore
import com.kwazzar.lockruna.domain.session.VaultKey
import com.kwazzar.lockruna.domain.session.VaultSession
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** The user's choice, as the settings screen left it. */
private class FakeAutoLockSettingsStore(var policy: AutoLockPolicy) : AutoLockSettingsStore {
    override suspend fun load(): AutoLockPolicy = policy
    override suspend fun save(policy: AutoLockPolicy) {
        this.policy = policy
    }
}

class AutoLockPolicyTest {

    private fun newPolicy(timeout: AutoLockPolicy): Triple<ApplyAutoLockPolicy, VaultSession, FakeClock> {
        val session = VaultSession()
        val clock = FakeClock(wallMillis = 1_000_000, monotonicMillis = 1_000_000)
        val store = FakeAutoLockSettingsStore(timeout)
        return Triple(ApplyAutoLockPolicy(session, clock, store), session, clock)
    }

    @Test
    fun `stays unlocked while within the timeout`() = runTest {
        val (policy, session, clock) = newPolicy(AutoLockPolicy.ONE_MINUTE)
        session.unlock(VaultKey.random(FakeCryptoService()))
        clock.monotonicMillis = 1_000_000

        val locked = policy(1_000_000)

        assertEquals(false, locked)
        assertEquals(VaultSession.State.Unlocked, session.state)
    }

    @Test
    fun `locks once the timeout elapsed`() = runTest {
        val (policy, session, clock) = newPolicy(AutoLockPolicy.ONE_MINUTE)
        session.unlock(VaultKey.random(FakeCryptoService()))
        clock.monotonicMillis = 120_000

        val locked = policy(60_000)

        assertEquals(true, locked)
        assertEquals(VaultSession.State.Locked, session.state)
    }

    @Test
    fun `wall clock changes do not trigger the lock`() = runTest {
        val (policy, session, clock) = newPolicy(AutoLockPolicy.ONE_MINUTE)
        session.unlock(VaultKey.random(FakeCryptoService()))
        clock.monotonicMillis = 1_000_000

        clock.wallMillis = 4_000_000
        val locked = policy(970_000)

        assertEquals(false, locked)
        assertEquals(VaultSession.State.Unlocked, session.state)
    }

    @Test
    fun `the timeout the user chose is read when the app comes back`() = runTest {
        val store = FakeAutoLockSettingsStore(AutoLockPolicy.FIVE_MINUTES)
        val session = VaultSession()
        val clock = FakeClock(wallMillis = 1_000_000, monotonicMillis = 1_000_000)
        session.unlock(VaultKey.random(FakeCryptoService()))
        val policy = ApplyAutoLockPolicy(session, clock, store)

        // Two minutes in the background: still within the five-minute choice.
        clock.monotonicMillis = 121_000
        val locked = policy(1_000_000)

        assertEquals(false, locked)
        assertEquals(VaultSession.State.Unlocked, session.state)
    }

    @Test
    fun `a changed choice takes effect on the next resume`() = runTest {
        val store = FakeAutoLockSettingsStore(AutoLockPolicy.ONE_MINUTE)
        val session = VaultSession()
        val clock = FakeClock(wallMillis = 1_000_000, monotonicMillis = 1_000_000)
        session.unlock(VaultKey.random(FakeCryptoService()))
        val policy = ApplyAutoLockPolicy(session, clock, store)
        store.save(AutoLockPolicy.IMMEDIATE)

        val locked = policy(999_999)

        assertEquals(true, locked)
        assertEquals(VaultSession.State.Locked, session.state)
    }
}
