package com.kwazzar.lockruna.domain.vault

/** Copies sensitive content; native implementations clear it after 30 seconds. */
interface Clipboard {
    suspend fun copySecret(value: String)
}
