package com.kwazzar.lockruna.data.session

import com.kwazzar.lockruna.data.platform.SecureStorage
import com.kwazzar.lockruna.domain.session.AutoLockPolicy
import com.kwazzar.lockruna.domain.session.AutoLockSettingsStore

/**
 * The auto-lock timeout in the platform's protected storage, so the choice survives a restart.
 */
class SecureAutoLockSettingsStore(private val storage: SecureStorage) : AutoLockSettingsStore {
    override suspend fun load(): AutoLockPolicy {
        val stored = storage.read(KEY)?.decodeToString()?.toLongOrNull() ?: return AutoLockPolicy.ONE_MINUTE
        return AutoLockPolicy.ALL.firstOrNull { it.timeoutMillis == stored } ?: AutoLockPolicy.ONE_MINUTE
    }

    override suspend fun save(policy: AutoLockPolicy) {
        storage.write(KEY, policy.timeoutMillis.toString().encodeToByteArray())
    }

    private companion object {
        const val KEY = "auto_lock_policy"
    }
}

/** A store with a fixed policy, used where no platform storage exists (tests, tools). */
class FixedAutoLockSettingsStore(private val policy: AutoLockPolicy = AutoLockPolicy.ONE_MINUTE) :
    AutoLockSettingsStore {
    override suspend fun load(): AutoLockPolicy = policy

    override suspend fun save(policy: AutoLockPolicy) = Unit
}
