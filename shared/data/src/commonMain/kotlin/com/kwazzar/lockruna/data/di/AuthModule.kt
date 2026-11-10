package com.kwazzar.lockruna.data.di

import com.kwazzar.lockruna.data.auth.InMemoryAuthStateStore
import com.kwazzar.lockruna.data.auth.SessionAccessTokenProvider
import com.kwazzar.lockruna.data.auth.createAuthApi
import com.kwazzar.lockruna.domain.auth.AuthApi
import com.kwazzar.lockruna.domain.auth.AuthStateStore
import com.kwazzar.lockruna.domain.sync.AccessTokenProvider
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.dsl.module

fun authModule(httpClient: HttpClient, baseUrl: String): Module = module {
    single { createAuthApi(httpClient, baseUrl) }
    single<AuthApi> { get() }
    single { InMemoryAuthStateStore() }
    single<AuthStateStore> { get<InMemoryAuthStateStore>() }
    single<AccessTokenProvider> { SessionAccessTokenProvider(get()) }

    // Application use cases
    single {
        com.kwazzar.lockruna.application.auth.Login(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }
    single {
        com.kwazzar.lockruna.application.auth.Logout(
            get(),
            get(),
            get(),
        )
    }
    single {
        com.kwazzar.lockruna.application.auth.UnlockWithPassword(
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }
    single {
        com.kwazzar.lockruna.application.auth.Register(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }
    single {
        com.kwazzar.lockruna.application.auth.EnableBiometrics(
            get(),
            get(),
            get(),
        )
    }
    single {
        com.kwazzar.lockruna.application.auth.UnlockWithBiometrics(
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }
}
