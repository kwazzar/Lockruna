@file:Suppress("FunctionNaming", "LongMethod")

package com.kwazzar.lockruna.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
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
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OutlinedTextField(value = state.email, onValueChange = {
                scope.launch { store.dispatch(RegisterIntent.EmailChanged(it)) }
            }, label = { Text("Email") })
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = {
                    scope.launch {
                        password = it
                        store.dispatch(RegisterIntent.PasswordChanged(it))
                    }
                },
                label = { Text("Master password") },
                supportingText = { Text("At least 12 characters") },
                visualTransformation = PasswordVisualTransformation(),
            )
            val strength = state.strength ?: state.passwordStrength
            if (strength != null && password.isNotEmpty()) {
                Text("Strength: ${strength.level}/4", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                if (strength.warnings.isNotEmpty()) {
                    Text(strength.warnings.joinToString(", "), color = androidx.compose.material3.MaterialTheme.colorScheme.error, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = state.deviceName, onValueChange = {
                scope.launch { store.dispatch(RegisterIntent.DeviceNameChanged(it)) }
            }, label = { Text("Device name") })
            Spacer(Modifier.height(8.dp))
            if (state.error != null) {
                Text("Registration failed", color = androidx.compose.material3.MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(8.dp))
            if (state.isSubmitting) {
                CircularProgressIndicator()
            } else {
                Button(onClick = { scope.launch { store.dispatch(RegisterIntent.Submit) } }) { Text("Create account") }
            }
        }
        return
    }

    AlertDialog(
        onDismissRequest = { },
        title = { Text("Your recovery key") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Write these 12 words down. Without them a forgotten password cannot be restored.")
                Spacer(Modifier.height(12.dp))
                words.forEachIndexed { index, word ->
                    Text("${index + 1}. $word")
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it },
                    label = { Text("Type the 12 words back") },
                )
                if (state.error != null) {
                    Text(
                        "The words do not match",
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
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
            }) { Text("I saved them") }
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
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Recover access", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = state.email, onValueChange = {
            scope.launch { store.dispatch(RecoveryIntent.EmailChanged(it)) }
        }, label = { Text("Email") })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
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
            label = { Text("Recovery key (12 words)") },
            minLines = 3,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = {
                scope.launch {
                    password = it
                    store.dispatch(RecoveryIntent.NewPasswordChanged(it))
                }
            },
            label = { Text("New master password") },
            visualTransformation = PasswordVisualTransformation(),
        )
        val recoveryStrength = state.strength ?: state.passwordStrength
        if (recoveryStrength != null && password.isNotEmpty()) {
            Text("Strength: ${recoveryStrength.level}/4", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            if (recoveryStrength.warnings.isNotEmpty()) {
                Text(recoveryStrength.warnings.joinToString(", "), color = androidx.compose.material3.MaterialTheme.colorScheme.error, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = state.deviceName, onValueChange = {
            scope.launch {
                store.dispatch(RecoveryIntent.DeviceNameChanged(it))
            }
        }, label = { Text("Device name") })
        Spacer(Modifier.height(8.dp))
        if (state.error != null) {
            Text("Recovery failed", color = androidx.compose.material3.MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(8.dp))
        Row {
            if (state.isSubmitting) {
                CircularProgressIndicator()
            } else {
                Button(onClick = { scope.launch { store.dispatch(RecoveryIntent.Submit) } }) {
                    Text("Restore access")
                }
            }
        }
    }
}
