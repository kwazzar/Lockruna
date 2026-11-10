package com.kwazzar.lockruna.application.session

import com.kwazzar.lockruna.domain.session.VaultSession

/** Locks the vault: drops the in-memory key and moves to the locked state. */
class LockVault(private val session: VaultSession) {
    operator fun invoke() {
        session.lock()
    }
}
