@file:Suppress("FunctionNaming", "LongMethod")

package com.kwazzar.lockruna.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kwazzar.lockruna.domain.session.AutoLockPolicy
import com.kwazzar.lockruna.presentation.vault.DevicesIntent
import com.kwazzar.lockruna.presentation.vault.DevicesStore
import com.kwazzar.lockruna.presentation.vault.SettingsEffect
import com.kwazzar.lockruna.presentation.vault.SettingsIntent
import com.kwazzar.lockruna.presentation.vault.SettingsStore
import kotlinx.coroutines.launch

/** The devices of the account, with revocation (US-10). */
@Composable
fun DevicesScreen(store: DevicesStore) {
    val state by store.state.collectAsState()
    val scope = rememberCoroutineScope()
    var revoking by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { store.dispatch(DevicesIntent.Load) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Devices", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        if (state.isLoading) {
            CircularProgressIndicator()
        } else {
            LazyColumn {
                items(state.devices) { device ->
                    Row(
                        Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(device.name)
                            Text(device.platform, style = MaterialTheme.typography.bodySmall)
                            if (device.current) {
                                Text("This device", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (!device.current) {
                            TextButton(onClick = { revoking = device.id }) { Text("Revoke") }
                        }
                    }
                }
            }
        }
        if (state.error != null) {
            Spacer(Modifier.height(8.dp))
            Text("Could not load the devices", color = MaterialTheme.colorScheme.error)
        }
    }

    val pending = revoking
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { revoking = null },
            title = { Text("Revoke this device?") },
            text = { Text("Its tokens stop working right away.") },
            confirmButton = {
                TextButton(onClick = {
                    revoking = null
                    scope.launch { store.dispatch(DevicesIntent.Revoke(pending)) }
                }) { Text("Revoke") }
            },
            dismissButton = { TextButton(onClick = { revoking = null }) { Text("Cancel") } },
        )
    }
}

/** Auto-lock timeout (US-08), biometric unlock (US-07) and account deletion (US-11). */
@Composable
fun SettingsScreen(store: SettingsStore, onAccountDeleted: () -> Unit) {
    val state by store.state.collectAsState()
    val scope = rememberCoroutineScope()
    var deletionPassword by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { store.dispatch(SettingsIntent.Load) }
    LaunchedEffect(Unit) {
        store.effects.collect { effect ->
            if (effect is SettingsEffect.AccountDeleted) {
                deletionPassword = ""
                confirmDelete = false
                onAccountDeleted()
            }
        }
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)

        Text("Auto-lock", style = MaterialTheme.typography.titleMedium)
        state.choices.forEach { policy ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = state.policy == policy,
                    onClick = { scope.launch { store.dispatch(SettingsIntent.PolicyChanged(policy)) } },
                )
                Text(autoLockLabel(policy))
            }
        }

        Text("Biometric unlock", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = state.biometricsEnabled,
                enabled = state.biometricsAvailable,
                onCheckedChange = { enabled ->
                    scope.launch { store.dispatch(SettingsIntent.BiometricsToggled(enabled)) }
                },
            )
            Text(if (state.biometricsAvailable) "Unlock with biometrics" else "Not available on this device")
        }

        Spacer(Modifier.height(16.dp))
        Text("Delete account", style = MaterialTheme.typography.titleMedium)
        Text("The account and every record are erased. This cannot be undone.")
        Button(onClick = { confirmDelete = true }) { Text("Delete account") }
        if (state.error != null) {
            Text("Something went wrong", color = MaterialTheme.colorScheme.error)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this account?") },
            text = {
                Column {
                    Text("Type your master password to confirm.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = deletionPassword,
                        onValueChange = { deletionPassword = it },
                        label = { Text("Master password") },
                        visualTransformation = PasswordVisualTransformation(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val password = deletionPassword
                    confirmDelete = false
                    deletionPassword = ""
                    scope.launch {
                        store.dispatch(SettingsIntent.DeletionPasswordChanged(password))
                        store.dispatch(SettingsIntent.DeleteAccount)
                    }
                }) { Text("Delete forever") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

private fun autoLockLabel(policy: AutoLockPolicy): String = when (policy.timeoutMillis) {
    AutoLockPolicy.IMMEDIATE_MILLIS -> "Immediately"
    AutoLockPolicy.ONE_MINUTE_MILLIS -> "After 1 minute"
    AutoLockPolicy.FIVE_MINUTES_MILLIS -> "After 5 minutes"
    AutoLockPolicy.FIFTEEN_MINUTES_MILLIS -> "After 15 minutes"
    else -> "After ${policy.timeoutMillis / MILLIS_PER_SECOND} seconds"
}

private const val MILLIS_PER_SECOND = 1_000L
