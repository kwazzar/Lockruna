package com.kwazzar.lockruna.domain.auth

/**
 * Canonical email form — the same one the server stores and compares: trimmed and lowercased.
 * The AAD of the wrapped vault key is built from these bytes, so registration and login
 * have to agree on them.
 */
fun canonicalEmail(raw: String): String = raw.trim().lowercase()
