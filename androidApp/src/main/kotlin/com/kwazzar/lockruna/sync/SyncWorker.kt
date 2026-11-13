package com.kwazzar.lockruna.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kwazzar.lockruna.application.sync.SyncNow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val syncNow: SyncNow by inject()

    override suspend fun doWork(): Result {
        return try {
            syncNow()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}