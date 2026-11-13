@file:Suppress("FunctionNaming", "LongMethod")

package com.kwazzar.lockruna.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kwazzar.lockruna.presentation.vault.ItemUi
import com.kwazzar.lockruna.presentation.vault.VaultListIntent
import com.kwazzar.lockruna.presentation.vault.VaultListStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultListScreen(
    store: VaultListStore,
    onOpenItem: (String) -> Unit,
    onNewItem: () -> Unit,
    onLock: () -> Unit,
    onDevices: () -> Unit,
    onSettings: () -> Unit,
    onSync: () -> Unit,
    syncMessage: String?,
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
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Vault",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    TextButton(
                        onClick = onSync,
                        colors = ButtonDefaults.textButtonColors(contentColor = DarkZenPalette.TextSecondary),
                    ) {
                        Text("Sync", color = DarkZenPalette.TextSecondary)
                    }
                    TextButton(
                        onClick = onDevices,
                        colors = ButtonDefaults.textButtonColors(contentColor = DarkZenPalette.TextSecondary),
                    ) {
                        Text("Devices", color = DarkZenPalette.TextSecondary)
                    }
                    TextButton(
                        onClick = onSettings,
                        colors = ButtonDefaults.textButtonColors(contentColor = DarkZenPalette.TextSecondary),
                    ) {
                        Text("Settings", color = DarkZenPalette.TextSecondary)
                    }
                    IconButton(onClick = { scope.launch { store.dispatch(VaultListIntent.Lock) } }) {
                        Text("Lock", color = DarkZenPalette.TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewItem,
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .border(1.dp, DarkZenPalette.Divider, RoundedCornerShape(16.dp))
                    .semantics { contentDescription = "Add item" },
                shape = RoundedCornerShape(16.dp),
                containerColor = DarkZenPalette.Card,
                contentColor = DarkZenPalette.TextPrimary,
            ) {
                Text("+", fontWeight = FontWeight.Bold)
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            DarkZenTextField(
                value = state.searchQuery ?: "",
                onValueChange = { scope.launch { store.dispatch(VaultListIntent.Search(it)) } },
                label = "Search",
                modifier = Modifier.fillMaxWidth().padding(8.dp),
            )
            if (syncMessage != null) {
                Text(
                    text = syncMessage,
                    color = DarkZenPalette.TextSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
            LazyColumn {
                items(state.items) { item -> ItemRow(item, store) }
            }
        }
    }
}

@Composable
private fun ItemRow(item: ItemUi, store: VaultListStore) {
    val scope = rememberCoroutineScope()
    DarkZenCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable {
                scope.launch { store.dispatch(VaultListIntent.ItemClicked(item.id)) }
            },
    ) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = item.username ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = DarkZenPalette.TextSecondary,
        )
    }
}
