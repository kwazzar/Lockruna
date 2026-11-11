package com.kwazzar.lockruna.data.di

import org.koin.core.module.Module

actual fun platformModule(): Module =
    error("platformModule() is not available on Android; use androidPlatformModule(context, activityProvider)")
