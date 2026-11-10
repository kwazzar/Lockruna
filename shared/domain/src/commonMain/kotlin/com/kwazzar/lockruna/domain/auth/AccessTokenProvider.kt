package com.kwazzar.lockruna.domain.auth

interface AccessTokenProvider {
    suspend fun token(): String?
}
