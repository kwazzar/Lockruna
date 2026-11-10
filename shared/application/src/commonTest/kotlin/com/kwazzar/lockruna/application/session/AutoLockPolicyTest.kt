package com.kwazzar.lockruna.application.session

import com.kwazzar.lockruna.application.support.FakeClock
import com.kwazzar.lockruna.application.support.FakeCryptoService
import com.kwazzar.lockruna.domain.session.AutoLockPolicy
import com.kwazzar.lockruna.domain.session.VaultKey
import com.kwazzar.lockruna.domain.session.VaultSession
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AutoLockPolicyTest {

    private fun newPolicy(timeout: AutoLockPolicy): Triple<ApplyAutoLockPolicy, VaultSession, FakeClock> {
        val session = VaultSession()
        val clock = FakeClock(wallMillis = 1_000_000, monotonicMillis = 1_000_000)
        return Triple(ApplyAutoLockPolicy(session, clock, timeout), session, clock)
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
}
