package com.kwazzar.lockruna.data.di

import com.kwazzar.lockruna.application.sync.SyncNow
import com.kwazzar.lockruna.data.sync.KtorSyncApi
import com.kwazzar.lockruna.data.sync.SqlDelightSyncStore
import com.kwazzar.lockruna.domain.sync.SyncApi
import com.kwazzar.lockruna.domain.sync.SyncStore
import io.ktor.client.HttpClient
import org.koin.dsl.module

/**
 * Koin bindings of the sync feature.
 *
 * The database, the id generator and the clock come from the modules that own them.
 * The access token provider comes from the auth module.
 */
fun syncModule(httpClient: HttpClient, baseUrl: String) = module {
    single { KtorSyncApi(httpClient, baseUrl, get()) }
    single<SyncApi> { get<KtorSyncApi>() }
    single<SyncStore> { SqlDelightSyncStore(get()) }
    single { SyncNow(get(), get(), get(), get(), get()) }
}
