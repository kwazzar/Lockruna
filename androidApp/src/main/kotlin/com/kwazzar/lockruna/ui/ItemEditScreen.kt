@file:Suppress("FunctionNaming", "LongMethod")

package com.kwazzar.lockruna.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kwazzar.lockruna.domain.vault.VaultItemType
import com.kwazzar.lockruna.presentation.vault.ItemEditIntent
import com.kwazzar.lockruna.presentation.vault.ItemEditStore
import kotlinx.coroutines.launch

/**
 * The item form. LOGIN uses username/password/url, CARD a holder and a number,
 * NOTE a body — all on the generic item fields (US-04, US-05).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditScreen(
    store: ItemEditStore,
    folders: List<Pair<String, String>>,
    onSaved: () -> Unit,
    onBack: () -> Unit,
) {
    val state by store.state.collectAsState()
    val scope = rememberCoroutineScope()
    var title by remember(state.existingId) { mutableStateOf(state.title) }
    var username by remember(state.existingId) { mutableStateOf(state.username) }
    var secret by remember(state.existingId) { mutableStateOf("") }
    var url by remember(state.existingId) { mutableStateOf(state.url) }
    val editing = state.existingId != null

    LaunchedEffect(Unit) {
        store.effects.collect { effect ->
            when (effect) {
                is com.kwazzar.lockruna.presentation.vault.ItemEditEffect.Saved -> onSaved()
                is com.kwazzar.lockruna.presentation.vault.ItemEditEffect.ValidationError -> {}
            }
        }
    }
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Item", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        if (!editing) {
            var typeMenuOpen by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = typeMenuOpen,
                onExpandedChange = { typeMenuOpen = it },
            ) {
                OutlinedTextField(
                    value = typeLabel(state.type),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuOpen) },
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                ExposedDropdownMenu(expanded = typeMenuOpen, onDismissRequest = { typeMenuOpen = false }) {
                    listOf(VaultItemType.LOGIN, VaultItemType.CARD, VaultItemType.NOTE).forEach { type ->
                        DropdownMenuItem(
                            text = { Text(typeLabel(type)) },
                            onClick = {
                                typeMenuOpen = false
                                scope.launch { store.dispatch(ItemEditIntent.TypeChanged(type)) }
                            },
                        )
                    }
                }
            }
        }

        OutlinedTextField(value = title, onValueChange = {
            scope.launch {
                title = it
                store.dispatch(ItemEditIntent.TitleChanged(it))
            }
        }, label = { Text(titleLabel(state.type)) })
        if (state.type != VaultItemType.NOTE) {
            OutlinedTextField(value = username, onValueChange = {
                scope.launch {
                    username = it
                    store.dispatch(ItemEditIntent.UsernameChanged(it))
                }
            }, label = { Text(usernameLabel(state.type)) })
        }
        OutlinedTextField(
            value = secret,
            onValueChange = {
                scope.launch {
                    secret = it
                    store.dispatch(ItemEditIntent.SecretChanged(it))
                }
            },
            label = { Text(secretLabel(state.type)) },
            visualTransformation = if (state.type == VaultItemType.NOTE) {
                androidx.compose.ui.text.input.VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
        )
        if (state.type == VaultItemType.LOGIN) {
            OutlinedTextField(value = url, onValueChange = {
                scope.launch {
                    url = it
                    store.dispatch(ItemEditIntent.UrlChanged(it))
                }
            }, label = { Text("URL") })
        }

        Spacer(Modifier.height(16.dp))
        Text("Folder", style = MaterialTheme.typography.titleSmall)
        if (folders.isEmpty()) {
            Text("No folders yet", style = MaterialTheme.typography.bodySmall)
        }
        folders.forEach { (folderId, folderName) ->
            OutlinedButton(onClick = {
                scope.launch { store.dispatch(ItemEditIntent.FolderSelected(folderId)) }
            }) { Text(folderName) }
        }
        if (state.folderId != null) {
            OutlinedButton(onClick = {
                scope.launch { store.dispatch(ItemEditIntent.FolderSelected(null)) }
            }) { Text("No folder") }
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = { scope.launch { store.dispatch(ItemEditIntent.Save) } }) { Text("Save") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBack) { Text("Back") }
    }
}

private fun typeLabel(type: VaultItemType): String = when (type) {
    VaultItemType.LOGIN -> "Login"
    VaultItemType.CARD -> "Card"
    VaultItemType.NOTE -> "Note"
    VaultItemType.FOLDER -> "Folder"
}

private fun titleLabel(type: VaultItemType): String = when (type) {
    VaultItemType.CARD -> "Card name"
    else -> "Title"
}

private fun usernameLabel(type: VaultItemType): String = when (type) {
    VaultItemType.CARD -> "Card holder"
    else -> "Username"
}

private fun secretLabel(type: VaultItemType): String = when (type) {
    VaultItemType.CARD -> "Card number"
    VaultItemType.NOTE -> "Note"
    else -> "Password"
}
