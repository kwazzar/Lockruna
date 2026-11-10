package com.kwazzar.lockruna.data.auth

import com.kwazzar.lockruna.domain.auth.AuthApi

expect fun createAuthApi(httpClient: io.ktor.client.HttpClient, baseUrl: String): AuthApi
