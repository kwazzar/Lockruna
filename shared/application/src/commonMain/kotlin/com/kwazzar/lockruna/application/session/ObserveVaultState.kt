package com.kwazzar.lockruna.application.session

import com.kwazzar.lockruna.domain.session.VaultSession
import kotlinx.coroutines.flow.Flow

/** Reports lock/unlock transitions so the vault index can follow the session. */
class ObserveVaultState(private val session: VaultSession) {
    operator fun invoke(): Flow<VaultSession.State> = session.stateFlow
}
