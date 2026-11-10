package com.kwazzar.lockruna.application.session

import com.kwazzar.lockruna.domain.session.AutoLockPolicy
import com.kwazzar.lockruna.domain.session.Clock
import com.kwazzar.lockruna.domain.session.VaultSession

/** Returns true when the auto-lock policy expired and the session was locked. */
class ApplyAutoLockPolicy(
    private val session: VaultSession,
    private val clock: Clock,
    private val policy: AutoLockPolicy,
) {
    operator fun invoke(backgroundedAtMonotonicMillis: Long): Boolean {
        val elapsed = clock.monotonicMillis() - backgroundedAtMonotonicMillis
        if (elapsed >= policy.timeoutMillis) {
            session.lock()
            return true
        }
        return false
    }
}
