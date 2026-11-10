package com.kwazzar.lockruna.data.di

import com.kwazzar.lockruna.application.sync.SyncNow
import com.kwazzar.lockruna.data.sync.KtorSyncApi
import com.kwazzar.lockruna.data.sync.SqlDelightSyncStore
import com.kwazzar.lockruna.domain.sync.AccessTokenProvider
import com.kwazzar.lockruna.domain.sync.SyncApi
import com.kwazzar.lockruna.domain.sync.SyncStore
import io.ktor.client.HttpClient
import org.koin.dsl.module

/**
 * Koin bindings of the sync feature.
 *
 * The database, the id generator and the clock come from the modules that own them.
 * The access token provider is passed in: the auth wave owns its implementation.
 */
fun syncModule(httpClient: HttpClient, baseUrl: String, accessTokenProvider: AccessTokenProvider) = module {
    single { KtorSyncApi(httpClient, baseUrl, accessTokenProvider) }
    single<SyncApi> { get<KtorSyncApi>() }
    single<SyncStore> { SqlDelightSyncStore(get()) }
    single { SyncNow(get(), get(), accessTokenProvider, get(), get()) }
}
