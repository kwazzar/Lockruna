package com.kwazzar.lockruna.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kwazzar.lockruna.presentation.vault.ItemDetailIntent
import com.kwazzar.lockruna.presentation.vault.ItemDetailStore

@Composable
fun ItemDetailScreen(store: ItemDetailStore, onBack: () -> Unit, onEdit: () -> Unit) {
    val state by store.state.collectAsState()
    val scope = rememberCoroutineScope()
    var revealed by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        store.effects.collect { effect ->
            when (effect) {
                is com.kwazzar.lockruna.presentation.vault.ItemDetailEffect.NavigateToEdit -> onEdit()
                com.kwazzar.lockruna.presentation.vault.ItemDetailEffect.Closed -> onBack()
                is com.kwazzar.lockruna.presentation.vault.ItemDetailEffect.SecretRevealed -> { revealed = effect.value }
                com.kwazzar.lockruna.presentation.vault.ItemDetailEffect.SecretCopied -> {}
            }
        }
    }
    Column(Modifier.padding(16.dp)) {
        Text(state.item?.title ?: "")
        Text(state.item?.username ?: "")
        if (state.secretVisible && revealed != null) Text("Secret: $revealed")
        Button(onClick = { scope.launch { store.dispatch(ItemDetailIntent.Reveal) } }) { Text("Reveal") }
        TextButton(onClick = { scope.launch { store.dispatch(ItemDetailIntent.Hide) } }) { Text("Hide") }
        Button(onClick = { scope.launch { store.dispatch(ItemDetailIntent.Copy) } }) { Text("Copy") }
        Button(onClick = { scope.launch { store.dispatch(ItemDetailIntent.Edit) } }) { Text("Edit") }
        Button(onClick = { scope.launch { store.dispatch(ItemDetailIntent.DeleteConfirmed) } }) { Text("Delete") }
    }
}
