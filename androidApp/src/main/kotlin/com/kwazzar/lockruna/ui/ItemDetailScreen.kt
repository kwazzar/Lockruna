@file:Suppress("FunctionNaming", "LongMethod")

package com.kwazzar.lockruna.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kwazzar.lockruna.presentation.vault.ItemDetailIntent
import com.kwazzar.lockruna.presentation.vault.ItemDetailStore
import kotlinx.coroutines.launch

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
                is com.kwazzar.lockruna.presentation.vault.ItemDetailEffect.SecretRevealed -> {
                    revealed = effect.value
                }
                com.kwazzar.lockruna.presentation.vault.ItemDetailEffect.SecretCopied -> {}
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        TextButton(
            onClick = onBack,
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Text("Back", color = DarkZenPalette.TextSecondary)
        }
        DarkZenCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = state.item?.title ?: "",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = state.item?.username ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = DarkZenPalette.TextSecondary,
            )
        }
        if (state.secretVisible && revealed != null) {
            Spacer(Modifier.height(12.dp))
            DarkZenCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Secret: $revealed",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = DarkZenPalette.TextSecondary,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        DarkZenPrimaryButton(
            onClick = { scope.launch { store.dispatch(ItemDetailIntent.Reveal) } },
        ) {
            Text("Reveal")
        }
        Spacer(Modifier.height(8.dp))
        DarkZenSecondaryButton(
            onClick = { scope.launch { store.dispatch(ItemDetailIntent.Hide) } },
        ) {
            Text("Hide")
        }
        Spacer(Modifier.height(8.dp))
        DarkZenPrimaryButton(
            onClick = { scope.launch { store.dispatch(ItemDetailIntent.Copy) } },
        ) {
            Text("Copy")
        }
        Spacer(Modifier.height(8.dp))
        DarkZenSecondaryButton(
            onClick = { scope.launch { store.dispatch(ItemDetailIntent.Edit) } },
        ) {
            Text("Edit")
        }
        Spacer(Modifier.height(8.dp))
        DarkZenDangerButton(
            onClick = { scope.launch { store.dispatch(ItemDetailIntent.DeleteConfirmed) } },
        ) {
            Text("Delete")
        }
    }
}
