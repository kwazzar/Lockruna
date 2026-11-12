package com.kwazzar.lockruna.domain.session

import com.kwazzar.lockruna.domain.session.AutoLockPolicy

/** The user's auto-lock choice (US-08). Stored, so it survives a restart. */
interface AutoLockSettingsStore {
    suspend fun load(): AutoLockPolicy

    suspend fun save(policy: AutoLockPolicy)
}
