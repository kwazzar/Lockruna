package com.kwazzar.lockruna.domain.identity

data class PasswordStrength(
    val level: Int,
    val warnings: List<String>,
)
