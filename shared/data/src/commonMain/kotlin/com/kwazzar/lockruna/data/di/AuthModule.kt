package com.kwazzar.lockruna.data.di

import com.kwazzar.lockruna.application.auth.EnableBiometrics
import com.kwazzar.lockruna.application.auth.HasStoredSession
import com.kwazzar.lockruna.application.auth.Login
import com.kwazzar.lockruna.application.auth.Logout
import com.kwazzar.lockruna.application.auth.Register
import com.kwazzar.lockruna.application.auth.UnlockWithBiometrics
import com.kwazzar.lockruna.application.auth.UnlockWithPassword
import com.kwazzar.lockruna.data.auth.SessionAccessTokenProvider
import com.kwazzar.lockruna.data.auth.createAuthApi
import com.kwazzar.lockruna.domain.auth.AuthApi
import com.kwazzar.lockruna.domain.sync.AccessTokenProvider
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.dsl.module

fun authModule(httpClient: HttpClient, baseUrl: String): Module = module {
    single { createAuthApi(httpClient, baseUrl) }
    single<AuthApi> { get() }
    single<AccessTokenProvider> { SessionAccessTokenProvider(get()) }

    includes(authUseCasesModule)
}

/** Application use cases of the auth flow. */
private val authUseCasesModule = module {
    single { Login(get(), get(), get(), get(), get(), get()) }
    single { Logout(get(), get(), get()) }
    single { UnlockWithPassword(get(), get(), get(), get(), get()) }
    single { Register(get(), get(), get(), get(), get(), get(), get()) }
    single { EnableBiometrics(get(), get(), get()) }
    single { UnlockWithBiometrics(get(), get(), get(), get(), get()) }
    single { HasStoredSession(get()) }
}
