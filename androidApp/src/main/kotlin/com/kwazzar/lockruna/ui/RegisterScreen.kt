@file:Suppress("FunctionNaming", "LongMethod")

package com.kwazzar.lockruna.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.kwazzar.lockruna.presentation.auth.RecoveryEffect
import com.kwazzar.lockruna.presentation.auth.RecoveryIntent
import com.kwazzar.lockruna.presentation.auth.RecoveryStore
import com.kwazzar.lockruna.presentation.auth.RegisterEffect
import com.kwazzar.lockruna.presentation.auth.RegisterIntent
import com.kwazzar.lockruna.presentation.auth.RegisterStore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Registration and the one-time recovery key (US-01, US-03).
 *
 * The 12 words live in a UI-local state only: they are shown once, the user types them back,
 * and both are dropped when the screen leaves composition.
 */
@Composable
fun RegisterScreen(store: RegisterStore, onRegistered: () -> Unit) {
    val state by store.state.collectAsState()
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var recoveryWords by remember { mutableStateOf<List<String>?>(null) }
    var confirmation by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        store.effects.collectLatest { effect ->
            when (effect) {
                is RegisterEffect.RecoveryKeyReady -> recoveryWords = effect.words
                RegisterEffect.Registered -> {
                    recoveryWords = null
                    confirmation = ""
                    onRegistered()
                }
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            recoveryWords = null
            confirmation = ""
            store.close()
        }
    }

    val words = recoveryWords
    if (words == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Create account",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            DarkZenTextField(
                value = state.email,
                onValueChange = {
                    scope.launch { store.dispatch(RegisterIntent.EmailChanged(it)) }
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
                        store.dispatch(RegisterIntent.PasswordChanged(it))
                    }
                },
                label = "Master password",
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    Text(
                        text = "At least 12 characters",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkZenPalette.TextSecondary,
                    )
                },
                visualTransformation = PasswordVisualTransformation(),
            )
            val strength = state.strength ?: state.passwordStrength
            if (strength != null && password.isNotEmpty()) {
                Text(
                    text = "Strength: ${strength.level}/4",
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkZenPalette.TextSecondary,
                )
                if (strength.warnings.isNotEmpty()) {
                    Text(
                        text = strength.warnings.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkZenPalette.Warning,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            DarkZenTextField(
                value = state.deviceName,
                onValueChange = {
                    scope.launch { store.dispatch(RegisterIntent.DeviceNameChanged(it)) }
                },
                label = "Device name",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            if (state.error != null) {
                Text("Registration failed", color = DarkZenPalette.Danger)
            }
            Spacer(Modifier.height(8.dp))
            if (state.isSubmitting) {
                CircularProgressIndicator(color = DarkZenPalette.Accent)
            } else {
                DarkZenPrimaryButton(
                    onClick = { scope.launch { store.dispatch(RegisterIntent.Submit) } },
                ) {
                    Text("Create account")
                }
            }
        }
        return
    }

    AlertDialog(
        onDismissRequest = { },
        containerColor = DarkZenPalette.Background,
        titleContentColor = DarkZenPalette.TextPrimary,
        textContentColor = DarkZenPalette.TextSecondary,
        title = { Text("Your recovery key") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
            ) {
                Text("Write these 12 words down. Without them a forgotten password cannot be restored.")
                Spacer(Modifier.height(12.dp))
                words.forEachIndexed { index, word ->
                    DarkZenCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "${index + 1}. $word",
                            color = DarkZenPalette.TextPrimary,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                DarkZenTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it },
                    label = "Type the 12 words back",
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.error != null) {
                    Text(
                        text = "The words do not match",
                        color = DarkZenPalette.Danger,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    store.dispatch(RegisterIntent.ConfirmationChanged(confirmation))
                    store.dispatch(RegisterIntent.ConfirmRecoveryWords)
                }
            }) {
                Text("I saved them", color = DarkZenPalette.Accent)
            }
        },
    )
}

/** Restoring access with the recovery key and a new master password (US-03). */
@Composable
fun RecoveryScreen(store: RecoveryStore, onRecovered: () -> Unit) {
    val state by store.state.collectAsState()
    val scope = rememberCoroutineScope()
    var words by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        store.effects.collectLatest { effect ->
            when (effect) {
                RecoveryEffect.Recovered -> {
                    words = ""
                    password = ""
                    onRecovered()
                }
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            words = ""
            password = ""
            store.close()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Recover access",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        DarkZenTextField(
            value = state.email,
            onValueChange = {
                scope.launch { store.dispatch(RecoveryIntent.EmailChanged(it)) }
            },
            label = "Email",
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        DarkZenTextField(
            value = words,
            onValueChange = {
                scope.launch {
                    words = it
                    store.dispatch(
                        RecoveryIntent.RecoveryWordsChanged(
                            it.trim().split(' ').filter { word -> word.isNotBlank() },
                        ),
                    )
                }
            },
            label = "Recovery key (12 words)",
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )
        Spacer(Modifier.height(8.dp))
        DarkZenTextField(
            value = password,
            onValueChange = {
                scope.launch {
                    password = it
                    store.dispatch(RecoveryIntent.NewPasswordChanged(it))
                }
            },
            label = "New master password",
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
        )
        val recoveryStrength = state.strength ?: state.passwordStrength
        if (recoveryStrength != null && password.isNotEmpty()) {
            Text(
                text = "Strength: ${recoveryStrength.level}/4",
                style = MaterialTheme.typography.bodySmall,
                color = DarkZenPalette.TextSecondary,
            )
            if (recoveryStrength.warnings.isNotEmpty()) {
                Text(
                    text = recoveryStrength.warnings.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = DarkZenPalette.Warning,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        DarkZenTextField(
            value = state.deviceName,
            onValueChange = {
                scope.launch {
                    store.dispatch(RecoveryIntent.DeviceNameChanged(it))
                }
            },
            label = "Device name",
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        if (state.error != null) {
            Text("Recovery failed", color = DarkZenPalette.Danger)
        }
        Spacer(Modifier.height(8.dp))
        if (state.isSubmitting) {
            CircularProgressIndicator(color = DarkZenPalette.Accent)
        } else {
            DarkZenPrimaryButton(
                onClick = { scope.launch { store.dispatch(RecoveryIntent.Submit) } },
            ) {
                Text("Restore access")
            }
        }
    }
}
