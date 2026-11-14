@file:Suppress("FunctionNaming", "LongMethod")

package com.kwazzar.lockruna.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp),
    ) {
        Text(
            text = "Devices",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        if (state.isLoading) {
            CircularProgressIndicator(color = DarkZenPalette.Accent)
        } else {
            LazyColumn {
                items(state.devices) { device ->
                    DarkZenCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = device.name,
                                    color = DarkZenPalette.TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = device.platform,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DarkZenPalette.TextSecondary,
                                )
                                if (device.current) {
                                    Text(
                                        text = "This device",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DarkZenPalette.TextSecondary,
                                    )
                                }
                            }
                            if (!device.current) {
                                TextButton(
                                    onClick = { revoking = device.id },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = DarkZenPalette.Danger,
                                    ),
                                ) { Text("Revoke") }
                            }
                        }
                    }
                }
            }
        }
        if (state.error != null) {
            Spacer(Modifier.height(8.dp))
            DarkZenCard(modifier = Modifier.fillMaxWidth()) {
                Text("Could not load the devices", color = DarkZenPalette.Danger)
            }
        }
    }

    val pending = revoking
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { revoking = null },
            containerColor = DarkZenPalette.Background,
            title = { Text("Revoke this device?", color = DarkZenPalette.TextPrimary) },
            text = {
                Text(
                    "Its tokens stop working right away.",
                    color = DarkZenPalette.TextSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        revoking = null
                        scope.launch { store.dispatch(DevicesIntent.Revoke(pending)) }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = DarkZenPalette.Danger),
                ) { Text("Revoke") }
            },
            dismissButton = {
                TextButton(
                    onClick = { revoking = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = DarkZenPalette.TextSecondary),
                ) { Text("Cancel") }
            },
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
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        SettingsSection {
            Text(
                text = "Auto-lock",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            state.choices.forEach { policy ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = state.policy == policy,
                        onClick = { scope.launch { store.dispatch(SettingsIntent.PolicyChanged(policy)) } },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = DarkZenPalette.Accent,
                            unselectedColor = DarkZenPalette.TextSecondary,
                        ),
                    )
                    Text(autoLockLabel(policy))
                }
            }
        }

        SettingsSection {
            Text(
                text = "Biometric unlock",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = state.biometricsEnabled,
                    enabled = state.biometricsAvailable,
                    onCheckedChange = { enabled ->
                        scope.launch { store.dispatch(SettingsIntent.BiometricsToggled(enabled)) }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DarkZenPalette.TextPrimary,
                        checkedTrackColor = DarkZenPalette.Accent,
                        checkedBorderColor = DarkZenPalette.Accent,
                        uncheckedThumbColor = DarkZenPalette.TextSecondary,
                        uncheckedTrackColor = DarkZenPalette.Panel,
                        uncheckedBorderColor = DarkZenPalette.TextSecondary,
                    ),
                )
                Text(
                    text = if (state.biometricsAvailable) {
                        "Unlock with biometrics"
                    } else {
                        "Not available on this device"
                    },
                    color = DarkZenPalette.TextSecondary,
                )
            }
        }

        SettingsSection {
            Text(
                text = "Delete account",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = DarkZenPalette.Danger,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "The account and every record are erased. This cannot be undone.",
                color = DarkZenPalette.Danger,
            )
            Spacer(Modifier.height(12.dp))
            DarkZenDangerButton(onClick = { confirmDelete = true }) { Text("Delete account") }
            if (state.error != null) {
                Text("Something went wrong", color = DarkZenPalette.Danger)
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = DarkZenPalette.Background,
            title = { Text("Delete this account?", color = DarkZenPalette.TextPrimary) },
            text = {
                Column {
                    Text(
                        "Type your master password to confirm.",
                        color = DarkZenPalette.TextSecondary,
                    )
                    Spacer(Modifier.height(8.dp))
                    DarkZenTextField(
                        value = deletionPassword,
                        onValueChange = { deletionPassword = it },
                        label = "Master password",
                        visualTransformation = PasswordVisualTransformation(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val password = deletionPassword
                        confirmDelete = false
                        deletionPassword = ""
                        scope.launch {
                            store.dispatch(SettingsIntent.DeletionPasswordChanged(password))
                            store.dispatch(SettingsIntent.DeleteAccount)
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = DarkZenPalette.Danger),
                ) { Text("Delete forever") }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmDelete = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = DarkZenPalette.TextSecondary),
                ) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SettingsSection(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkZenPalette.Panel,
        contentColor = DarkZenPalette.TextPrimary,
        border = BorderStroke(1.dp, DarkZenPalette.Divider),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
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
