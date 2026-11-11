package com.kwazzar.lockruna.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import com.kwazzar.lockruna.presentation.vault.ItemEditIntent
import com.kwazzar.lockruna.presentation.vault.ItemEditStore
import kotlinx.coroutines.launch

@Composable
fun ItemEditScreen(store: ItemEditStore, onSaved: () -> Unit, onBack: () -> Unit) {
    val state by store.state.collectAsState()
    val scope = rememberCoroutineScope()
    var title by remember(state.existingId) { mutableStateOf(state.title) }
    var username by remember(state.existingId) { mutableStateOf(state.username) }
    var secret by remember(state.existingId) { mutableStateOf("") }
    var url by remember(state.existingId) { mutableStateOf(state.url) }
    LaunchedEffect(Unit) {
        store.effects.collect { effect ->
            when (effect) {
                is com.kwazzar.lockruna.presentation.vault.ItemEditEffect.Saved -> onSaved()
                is com.kwazzar.lockruna.presentation.vault.ItemEditEffect.ValidationError -> {}
            }
        }
    }
    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(value = title, onValueChange = {
            scope.launch {
                title = it
                store.dispatch(ItemEditIntent.TitleChanged(it))
            }
        }, label = { Text("Title") })
        OutlinedTextField(value = username, onValueChange = {
            scope.launch {
                username = it
                store.dispatch(ItemEditIntent.UsernameChanged(it))
            }
        }, label = { Text("Username") })
        OutlinedTextField(value = secret, onValueChange = {
            scope.launch {
                secret = it
                store.dispatch(ItemEditIntent.SecretChanged(it))
            }
        }, label = { Text("Secret") }, visualTransformation = PasswordVisualTransformation())
        OutlinedTextField(value = url, onValueChange = {
            scope.launch {
                url = it
                store.dispatch(ItemEditIntent.UrlChanged(it))
            }
        }, label = { Text("URL") })
        Button(onClick = { scope.launch { store.dispatch(ItemEditIntent.Save) } }) { Text("Save") }
    }
}
