package com.kwazzar.lockruna.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kwazzar.lockruna.application.sync.SyncNow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SyncWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params),
    KoinComponent {

    private val syncNow: SyncNow by inject()

    override suspend fun doWork(): Result = try {
        syncNow()
        Result.success()
    } catch (failure: Exception) {
        Log.w("SyncWorker", "Background sync failed", failure)
        Result.retry()
    }
}
