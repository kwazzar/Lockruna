package com.kwazzar.lockruna

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.kwazzar.lockruna.data.di.androidPlatformModule
import com.kwazzar.lockruna.data.di.authModule
import com.kwazzar.lockruna.data.di.coreModule
import com.kwazzar.lockruna.data.di.syncModule
import com.kwazzar.lockruna.data.platform.platformHttpClient
import com.kwazzar.lockruna.platform.CurrentActivityProvider
import com.kwazzar.lockruna.presentation.di.presentationModule
import com.kwazzar.lockruna.sync.SyncWorker
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import java.util.concurrent.TimeUnit
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
        schedulePeriodicSync()
    }

    private fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "lockruna-sync",
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
