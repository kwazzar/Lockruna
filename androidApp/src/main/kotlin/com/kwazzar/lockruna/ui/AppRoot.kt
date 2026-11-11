package com.kwazzar.lockruna.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kwazzar.lockruna.domain.auth.AuthStateStore
import com.kwazzar.lockruna.presentation.app.AppScreen
import com.kwazzar.lockruna.presentation.app.startScreen
import org.koin.core.context.GlobalContext

@Composable
fun AppRoot() {
    val authStateStore = GlobalContext.get().get<AuthStateStore>()
    var screen by remember { mutableStateOf(AppScreen.UNLOCK) }
    LaunchedEffect(Unit) {
        val hasSession = authStateStore.loadSession() != null
        screen = startScreen(isLocked = true, hasSession = hasSession)
    }
    when (screen) {
        AppScreen.UNLOCK -> Text("Unlock")
        AppScreen.LOGIN -> Text("Login")
        AppScreen.VAULT -> Text("Vault")
    }
}
