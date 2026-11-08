package com.kwazzar.lockruna.server.platform.di

import com.kwazzar.lockruna.server.platform.config.ServerSettings
import org.koin.core.module.Module
import org.koin.dsl.module

fun serverModule(settings: ServerSettings): Module = module {
    single { settings }
}
