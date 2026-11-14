@file:Suppress("FunctionNaming", "LongMethod")

package com.kwazzar.lockruna.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kwazzar.lockruna.presentation.auth.LoginIntent
import com.kwazzar.lockruna.presentation.auth.LoginStore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(store: LoginStore, onLoggedIn: () -> Unit, onRegister: () -> Unit, onRecovery: () -> Unit) {
    val state by store.state.collectAsState()
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        store.effects.collectLatest { effect ->
            when (effect) {
                com.kwazzar.lockruna.presentation.auth.LoginEffect.LoggedIn -> onLoggedIn()
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Sign in",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        DarkZenTextField(
            value = state.email,
            onValueChange = {
                scope.launch { store.dispatch(LoginIntent.EmailChanged(it)) }
            },
            label = "Email",
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        DarkZenTextField(
            value = password,
            onValueChange = {
                scope.launch {
                    password = it
                    store.dispatch(LoginIntent.PasswordChanged(it))
                }
            },
            label = "Password",
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
        )
        Spacer(Modifier.height(8.dp))
        DarkZenTextField(
            value = state.deviceName,
            onValueChange = {
                scope.launch { store.dispatch(LoginIntent.DeviceNameChanged(it)) }
            },
            label = "Device name",
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        if (state.error != null) {
            Text("Login failed", color = DarkZenPalette.Danger)
        }
        Spacer(Modifier.height(8.dp))
        if (state.isSubmitting) {
            CircularProgressIndicator(color = DarkZenPalette.Accent)
        } else {
            DarkZenPrimaryButton(
                onClick = {
                    scope.launch { store.dispatch(LoginIntent.Submit) }
                },
            ) {
                Text("Sign in")
            }
        }
        TextButton(onClick = onRegister) {
            Text("Create account", color = DarkZenPalette.TextSecondary)
        }
        TextButton(onClick = onRecovery) {
            Text("Lost access", color = DarkZenPalette.TextSecondary)
        }
    }
}
