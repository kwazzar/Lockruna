package com.kwazzar.lockruna.ui

import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kwazzar.lockruna.presentation.vault.VaultListIntent
import com.kwazzar.lockruna.presentation.vault.VaultListStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultListScreen(
    store: VaultListStore,
    onOpenItem: (String) -> Unit,
    onNewItem: () -> Unit,
    onLock: () -> Unit,
) {
    val state by store.state.collectAsState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        store.effects.collect { effect ->
            when (effect) {
                is com.kwazzar.lockruna.presentation.vault.VaultListEffect.OpenItem -> onOpenItem(effect.id)
                is com.kwazzar.lockruna.presentation.vault.VaultListEffect.SecretRevealed -> {}
                com.kwazzar.lockruna.presentation.vault.VaultListEffect.SecretCopied -> {}
            }
        }
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Vault") }, actions = { IconButton(onClick = { scope.launch { store.dispatch(VaultListIntent.Lock) } }) { Text("Lock") } }) },
        floatingActionButton = { FloatingActionButton(onClick = onNewItem) { Text("+") } },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.searchQuery ?: "",
                onValueChange = { scope.launch { store.dispatch(VaultListIntent.Search(it)) } },
                label = { Text("Search") },
                modifier = Modifier.padding(8.dp),
            )
            LazyColumn {
                items(state.items) { item ->
                    Row(Modifier.clickable { scope.launch { store.dispatch(VaultListIntent.ItemClicked(item.id)) } }.padding(12.dp)) {
                        Column {
                            Text(item.title)
                            Text(item.username ?: "")
                        }
                    }
                }
            }
        }
    }
}
