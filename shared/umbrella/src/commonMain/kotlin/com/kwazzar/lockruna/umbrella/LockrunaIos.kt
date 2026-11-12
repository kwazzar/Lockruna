package com.kwazzar.lockruna.umbrella

import com.kwazzar.lockruna.application.sync.SyncNow
import com.kwazzar.lockruna.data.di.authModule
import com.kwazzar.lockruna.data.di.coreModule
import com.kwazzar.lockruna.data.di.platformModule
import com.kwazzar.lockruna.data.di.syncModule
import com.kwazzar.lockruna.data.platform.platformHttpClient
import com.kwazzar.lockruna.presentation.auth.LoginStore
import com.kwazzar.lockruna.presentation.auth.RecoveryStore
import com.kwazzar.lockruna.presentation.auth.RegisterStore
import com.kwazzar.lockruna.presentation.auth.UnlockStore
import com.kwazzar.lockruna.presentation.di.presentationModule
import com.kwazzar.lockruna.presentation.session.SessionStore
import com.kwazzar.lockruna.presentation.vault.DevicesStore
import com.kwazzar.lockruna.presentation.vault.ItemDetailStore
import com.kwazzar.lockruna.presentation.vault.ItemEditStore
import com.kwazzar.lockruna.presentation.vault.SettingsStore
import com.kwazzar.lockruna.presentation.vault.VaultListStore
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

object LockrunaIos {
    private var started = false
    private var sessionStoreRef: SessionStore? = null
    private var loginStoreRef: LoginStore? = null
    private var unlockStoreRef: UnlockStore? = null
    private var registerStoreRef: RegisterStore? = null
    private var recoveryStoreRef: RecoveryStore? = null
    private var vaultListStoreRef: VaultListStore? = null
    private var itemDetailStoreRef: ItemDetailStore? = null
    private var itemEditStoreRef: ItemEditStore? = null
    private var devicesStoreRef: DevicesStore? = null
    private var settingsStoreRef: SettingsStore? = null
    private var syncNowRef: SyncNow? = null

    fun start(baseUrl: String = "http://localhost:8080") {
        if (started) return
        val http = platformHttpClient()
        val koin = startKoin {
            modules(
                coreModule(),
                platformModule(),
                authModule(http, baseUrl),
                syncModule(http, baseUrl),
                presentationModule,
            )
        }.koin
        sessionStoreRef = koin.get()
        loginStoreRef = koin.get()
        unlockStoreRef = koin.get()
        registerStoreRef = koin.get()
        recoveryStoreRef = koin.get()
        vaultListStoreRef = koin.get()
        itemDetailStoreRef = koin.get()
        itemEditStoreRef = koin.get()
        devicesStoreRef = koin.get()
        settingsStoreRef = koin.get()
        syncNowRef = koin.get()
        started = true
    }

    fun stop() {
        if (!started) return
        stopKoin()
        sessionStoreRef = null
        loginStoreRef = null
        unlockStoreRef = null
        registerStoreRef = null
        recoveryStoreRef = null
        vaultListStoreRef = null
        itemDetailStoreRef = null
        itemEditStoreRef = null
        devicesStoreRef = null
        settingsStoreRef = null
        syncNowRef = null
        started = false
    }

    fun sessionStore(): SessionStore = sessionStoreRef ?: error("LockrunaIos not started")
    fun loginStore(): LoginStore = loginStoreRef ?: error("LockrunaIos not started")
    fun unlockStore(): UnlockStore = unlockStoreRef ?: error("LockrunaIos not started")
    fun registerStore(): RegisterStore = registerStoreRef ?: error("LockrunaIos not started")
    fun recoveryStore(): RecoveryStore = recoveryStoreRef ?: error("LockrunaIos not started")
    fun vaultListStore(): VaultListStore = vaultListStoreRef ?: error("LockrunaIos not started")
    fun itemDetailStore(): ItemDetailStore = itemDetailStoreRef ?: error("LockrunaIos not started")
    fun itemEditStore(): ItemEditStore = itemEditStoreRef ?: error("LockrunaIos not started")
    fun devicesStore(): DevicesStore = devicesStoreRef ?: error("LockrunaIos not started")
    fun settingsStore(): SettingsStore = settingsStoreRef ?: error("LockrunaIos not started")

    /** The sync engine (US-09): the UI calls it on unlock, resume, after a save and by hand. */
    suspend fun sync(): com.kwazzar.lockruna.domain.core.Result<Unit, com.kwazzar.lockruna.domain.error.AppError> =
        syncNowRef?.invoke() ?: error("LockrunaIos not started")
}
