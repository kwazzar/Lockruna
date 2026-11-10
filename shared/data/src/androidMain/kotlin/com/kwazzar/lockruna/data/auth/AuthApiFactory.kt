package com.kwazzar.lockruna.data.auth

import com.kwazzar.lockruna.domain.auth.AuthApi
import io.ktor.client.HttpClient

actual fun createAuthApi(httpClient: HttpClient, baseUrl: String): AuthApi = KtorAuthApi(httpClient, baseUrl)
