package com.kwazzar.lockruna.application.session

import com.kwazzar.lockruna.domain.session.AutoLockSettingsStore
import com.kwazzar.lockruna.domain.session.Clock
import com.kwazzar.lockruna.domain.session.VaultSession

/**
 * Returns true when the auto-lock policy expired and the session was locked.
 * The timeout is the one the user chose, read at the moment the app comes back.
 */
class ApplyAutoLockPolicy(
    private val session: VaultSession,
    private val clock: Clock,
    private val settingsStore: AutoLockSettingsStore,
) {
    suspend operator fun invoke(backgroundedAtMonotonicMillis: Long): Boolean {
        val elapsed = clock.monotonicMillis() - backgroundedAtMonotonicMillis
        if (elapsed >= settingsStore.load().timeoutMillis) {
            session.lock()
            return true
        }
        return false
    }
}
