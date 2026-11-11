package com.kwazzar.lockruna.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kwazzar.lockruna.domain.auth.AuthStateStore
import com.kwazzar.lockruna.presentation.app.AppScreen
import com.kwazzar.lockruna.presentation.app.startScreen
import com.kwazzar.lockruna.presentation.auth.LoginStore
import com.kwazzar.lockruna.presentation.auth.UnlockStore
import com.kwazzar.lockruna.presentation.session.SessionStore
import com.kwazzar.lockruna.presentation.vault.ItemDetailStore
import com.kwazzar.lockruna.presentation.vault.ItemEditStore
import com.kwazzar.lockruna.presentation.vault.VaultListStore
import kotlinx.coroutines.launch
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
                onOpenItem = { id -> selectedId = id; route = "detail" },
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
