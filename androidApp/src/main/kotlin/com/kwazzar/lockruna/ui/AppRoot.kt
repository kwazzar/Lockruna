package com.kwazzar.lockruna.ui

import android.os.SystemClock
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kwazzar.lockruna.domain.auth.AuthStateStore
import com.kwazzar.lockruna.presentation.app.AppScreen
import com.kwazzar.lockruna.presentation.app.startScreen
import com.kwazzar.lockruna.presentation.auth.LoginStore
import com.kwazzar.lockruna.presentation.auth.UnlockStore
import com.kwazzar.lockruna.presentation.session.SessionStore
import com.kwazzar.lockruna.presentation.vault.ItemDetailStore
import com.kwazzar.lockruna.presentation.vault.ItemEditStore
import com.kwazzar.lockruna.presentation.vault.VaultListStore
import org.koin.core.context.GlobalContext

@Composable
fun AppRoot() {
    val authStateStore = GlobalContext.get().get<AuthStateStore>()
    val sessionStore = remember { GlobalContext.get().get<SessionStore>() }
    val unlockStore = remember { GlobalContext.get().get<UnlockStore>() }
    val loginStore = remember { GlobalContext.get().get<LoginStore>() }
    val vaultListStore = remember { GlobalContext.get().get<VaultListStore>() }
    val itemDetailStore = remember { GlobalContext.get().get<ItemDetailStore>() }
    val itemEditStore = remember { GlobalContext.get().get<ItemEditStore>() }

    var screen by remember { mutableStateOf(AppScreen.UNLOCK) }
    var route by remember { mutableStateOf("list") }
    var selectedId by remember { mutableStateOf<String?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    var backgroundedAt by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> backgroundedAt = SystemClock.elapsedRealtime()
                Lifecycle.Event.ON_START -> {
                    val ts = backgroundedAt
                    backgroundedAt = null
                    kotlinx.coroutines.runBlocking {
                        sessionStore.dispatch(com.kwazzar.lockruna.presentation.session.SessionIntent.Resumed(ts))
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }
    LaunchedEffect(Unit) {
        val hasSession = authStateStore.loadSession() != null
        screen = startScreen(isLocked = true, hasSession = hasSession)
        sessionStore.effects.collect { effect ->
            when (effect) {
                com.kwazzar.lockruna.presentation.session.SessionEffect.Locked -> {
                    screen = startScreen(isLocked = true, hasSession = authStateStore.loadSession() != null)
                }
            }
        }
    }

    when (screen) {
        AppScreen.UNLOCK -> UnlockScreen(unlockStore) { screen = AppScreen.VAULT }
        AppScreen.LOGIN -> LoginScreen(loginStore) { screen = AppScreen.VAULT }
        AppScreen.VAULT -> when (route) {
            "list" -> VaultListScreen(
                store = vaultListStore,
                onOpenItem = { id ->
                    selectedId = id
                    route = "detail"
                },
                onNewItem = { route = "edit" },
                onLock = { },
            )
            "detail" -> {
                LaunchedEffect(selectedId) { selectedId?.let { itemDetailStore.open(it) } }
                ItemDetailScreen(
                    store = itemDetailStore,
                    onBack = { route = "list" },
                    onEdit = { route = "edit" },
                )
            }
            "edit" -> {
                LaunchedEffect(selectedId) { itemEditStore.begin(selectedId) }
                ItemEditScreen(
                    store = itemEditStore,
                    onSaved = { route = "list" },
                    onBack = { route = "list" },
                )
            }
            else -> Text("Vault")
        }
    }
}
