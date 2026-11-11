package com.kwazzar.lockruna

import android.app.Application
import com.kwazzar.lockruna.data.di.androidPlatformModule
import com.kwazzar.lockruna.data.di.authModule
import com.kwazzar.lockruna.data.di.coreModule
import com.kwazzar.lockruna.data.di.syncModule
import com.kwazzar.lockruna.data.platform.platformHttpClient
import com.kwazzar.lockruna.platform.CurrentActivityProvider
import com.kwazzar.lockruna.presentation.di.presentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class LockrunaApplication : Application() {
    val activityProvider = CurrentActivityProvider()

    override fun onCreate() {
        super.onCreate()
        val http = platformHttpClient()
        startKoin {
            androidContext(this@LockrunaApplication)
            modules(
                coreModule(),
                androidPlatformModule(this@LockrunaApplication, activityProvider),
                authModule(http, BuildConfig.BASE_URL),
                syncModule(http, BuildConfig.BASE_URL),
                presentationModule,
            )
        }
    }
}
