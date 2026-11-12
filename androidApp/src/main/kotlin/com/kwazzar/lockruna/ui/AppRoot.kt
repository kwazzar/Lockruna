@file:Suppress("FunctionNaming", "LongMethod")

package com.kwazzar.lockruna.ui

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kwazzar.lockruna.application.sync.SyncNow
import com.kwazzar.lockruna.domain.auth.AuthStateStore
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.vault.VaultItemType
import com.kwazzar.lockruna.presentation.app.AppScreen
import com.kwazzar.lockruna.presentation.app.startScreen
import com.kwazzar.lockruna.presentation.auth.LoginStore
import com.kwazzar.lockruna.presentation.auth.RecoveryStore
import com.kwazzar.lockruna.presentation.auth.RegisterStore
import com.kwazzar.lockruna.presentation.auth.UnlockStore
import com.kwazzar.lockruna.presentation.session.SessionEffect
import com.kwazzar.lockruna.presentation.session.SessionIntent
import com.kwazzar.lockruna.presentation.session.SessionStore
import com.kwazzar.lockruna.presentation.vault.DevicesStore
import com.kwazzar.lockruna.presentation.vault.ItemDetailStore
import com.kwazzar.lockruna.presentation.vault.ItemEditStore
import com.kwazzar.lockruna.presentation.vault.SettingsStore
import com.kwazzar.lockruna.presentation.vault.VaultListStore
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

@Composable
fun AppRoot() {
    val koin = GlobalContext.get()
    val authStateStore = koin.get<AuthStateStore>()
    val sessionStore = remember { koin.get<SessionStore>() }
    val unlockStore = remember { koin.get<UnlockStore>() }
    val loginStore = remember { koin.get<LoginStore>() }
    val registerStore = remember { koin.get<RegisterStore>() }
    val recoveryStore = remember { koin.get<RecoveryStore>() }
    val vaultListStore = remember { koin.get<VaultListStore>() }
    val itemDetailStore = remember { koin.get<ItemDetailStore>() }
    val itemEditStore = remember { koin.get<ItemEditStore>() }
    val devicesStore = remember { koin.get<DevicesStore>() }
    val settingsStore = remember { koin.get<SettingsStore>() }
    val syncNow = remember { koin.get<SyncNow>() }
    val scope = rememberCoroutineScope()

    var screen by remember { mutableStateOf(AppScreen.UNLOCK) }
    var route by remember { mutableStateOf("list") }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var syncJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    fun runSync() {
        syncJob?.cancel()
        syncJob = scope.launch {
            syncMessage = when (val result = syncNow()) {
                is Result.Success -> null
                is Result.Failure -> "Offline"
            }
        }
    }

    /** A local save waits for the next keystroke to be over before it hits the network. */
    fun scheduleSync() {
        syncJob?.cancel()
        syncJob = scope.launch {
            kotlinx.coroutines.delay(SYNC_DEBOUNCE_MILLIS)
            syncMessage = when (val result = syncNow()) {
                is Result.Success -> null
                is Result.Failure -> "Offline"
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { syncJob?.cancel() }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    AppLifecycleEffects(sessionStore = sessionStore, onResume = { runSync() }, owner = lifecycleOwner)
    AppNetworkSync(onAvailable = { runSync() })
    LaunchedEffect(Unit) {
        val hasSession = authStateStore.loadSession() != null
        screen = startScreen(isLocked = true, hasSession = hasSession)
        sessionStore.effects.collect { effect ->
            when (effect) {
                is SessionEffect.Locked -> {
                    screen = startScreen(isLocked = true, hasSession = authStateStore.loadSession() != null)
                }
            }
        }
    }

    AppScreens(
        screen = screen,
        route = route,
        selectedId = selectedId,
        syncMessage = syncMessage,
        unlockStore = unlockStore,
        loginStore = loginStore,
        registerStore = registerStore,
        recoveryStore = recoveryStore,
        vaultListStore = vaultListStore,
        itemDetailStore = itemDetailStore,
        itemEditStore = itemEditStore,
        devicesStore = devicesStore,
        settingsStore = settingsStore,
        onUnlocked = {
            screen = AppScreen.VAULT
            route = "list"
            runSync()
        },
        onLoggedIn = {
            screen = AppScreen.VAULT
            route = "list"
        },
        onRegister = { screen = AppScreen.REGISTER },
        onRecovery = { screen = AppScreen.RECOVERY },
        onRegistered = {
            screen = AppScreen.VAULT
            route = "list"
        },
        onRecovered = {
            screen = AppScreen.VAULT
            route = "list"
        },
        onAccountDeleted = { screen = startScreen(isLocked = true, hasSession = false) },
        onOpenItem = { id ->
            selectedId = id
            route = "detail"
        },
        onNewItem = { route = "edit" },
        onDevices = { screen = AppScreen.DEVICES },
        onSettings = { screen = AppScreen.SETTINGS },
        onSync = { runSync() },
        onSaved = {
            route = "list"
            scheduleSync()
        },
        onBack = { route = "list" },
    )
}

@Composable
private fun AppScreens(
    screen: AppScreen,
    route: String,
    selectedId: String?,
    syncMessage: String?,
    unlockStore: UnlockStore,
    loginStore: LoginStore,
    registerStore: RegisterStore,
    recoveryStore: RecoveryStore,
    vaultListStore: VaultListStore,
    itemDetailStore: ItemDetailStore,
    itemEditStore: ItemEditStore,
    devicesStore: DevicesStore,
    settingsStore: SettingsStore,
    onUnlocked: () -> Unit,
    onLoggedIn: () -> Unit,
    onRegister: () -> Unit,
    onRecovery: () -> Unit,
    onRegistered: () -> Unit,
    onRecovered: () -> Unit,
    onAccountDeleted: () -> Unit,
    onOpenItem: (String) -> Unit,
    onNewItem: () -> Unit,
    onDevices: () -> Unit,
    onSettings: () -> Unit,
    onSync: () -> Unit,
    onSaved: () -> Unit,
    onBack: () -> Unit,
) {
    when (screen) {
        AppScreen.UNLOCK -> UnlockScreen(unlockStore) { onUnlocked() }
        AppScreen.LOGIN -> LoginScreen(
            store = loginStore,
            onLoggedIn = onLoggedIn,
            onRegister = onRegister,
            onRecovery = onRecovery,
        )
        AppScreen.REGISTER -> RegisterScreen(registerStore) { onRegistered() }
        AppScreen.RECOVERY -> RecoveryScreen(recoveryStore) { onRecovered() }
        AppScreen.DEVICES -> DevicesScreen(devicesStore)
        AppScreen.SETTINGS -> SettingsScreen(settingsStore) { onAccountDeleted() }
        AppScreen.VAULT -> VaultRoutes(
            route = route,
            selectedId = selectedId,
            vaultListStore = vaultListStore,
            itemDetailStore = itemDetailStore,
            itemEditStore = itemEditStore,
            onOpenItem = onOpenItem,
            onNewItem = onNewItem,
            onDevices = onDevices,
            onSettings = onSettings,
            onSync = onSync,
            syncMessage = syncMessage,
            onSaved = onSaved,
            onBack = onBack,
        )
    }
}

/** Sync when the network comes back (US-09, foreground only in this wave). */
@Composable
private fun AppNetworkSync(onAvailable: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    DisposableEffect(Unit) {
        val manager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE)
            as android.net.ConnectivityManager
        val callback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                onAvailable()
            }
        }
        manager.registerDefaultNetworkCallback(callback)
        onDispose { manager.unregisterNetworkCallback(callback) }
    }
}

/**
 * The auto-lock and sync lifecycle wiring: the monotonic time of the background is
 * captured on stop, and the resume applies the user's timeout (US-08) and syncs (US-09).
 */
@Composable
private fun AppLifecycleEffects(
    sessionStore: SessionStore,
    onResume: () -> Unit,
    owner: androidx.lifecycle.LifecycleOwner,
) {
    val scope = rememberCoroutineScope()
    var backgroundedAt by remember { mutableStateOf<Long?>(null) }
    val observer = remember(owner) {
        LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> backgroundedAt = SystemClock.elapsedRealtime()
                Lifecycle.Event.ON_START -> {
                    val ts = backgroundedAt
                    backgroundedAt = null
                    scope.launch { sessionStore.dispatch(SessionIntent.Resumed(ts)) }
                    onResume()
                }
                else -> {}
            }
        }
    }
    DisposableEffect(owner) {
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}

@Composable
private fun VaultRoutes(
    route: String,
    selectedId: String?,
    vaultListStore: VaultListStore,
    itemDetailStore: ItemDetailStore,
    itemEditStore: ItemEditStore,
    onOpenItem: (String) -> Unit,
    onNewItem: () -> Unit,
    onDevices: () -> Unit,
    onSettings: () -> Unit,
    onSync: () -> Unit,
    syncMessage: String?,
    onSaved: () -> Unit,
    onBack: () -> Unit,
) {
    when (route) {
        "list" -> VaultListScreen(
            store = vaultListStore,
            onOpenItem = onOpenItem,
            onNewItem = onNewItem,
            onLock = { },
            onDevices = onDevices,
            onSettings = onSettings,
            onSync = onSync,
            syncMessage = syncMessage,
        )
        "detail" -> {
            LaunchedEffect(selectedId) { selectedId?.let { itemDetailStore.open(it) } }
            ItemDetailScreen(store = itemDetailStore, onBack = onBack, onEdit = { onNewItem() })
        }
        else -> {
            LaunchedEffect(selectedId) { itemEditStore.begin(selectedId) }
            ItemEditScreen(
                store = itemEditStore,
                folders = vaultListStore.state.value.items
                    .filter { it.type == VaultItemType.FOLDER }
                    .map { it.id to it.title },
                onSaved = onSaved,
                onBack = onBack,
            )
        }
    }
}

private const val SYNC_DEBOUNCE_MILLIS = 2_000L
