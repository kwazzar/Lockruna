package com.kwazzar.lockruna.data.platform

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.vault.Clipboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Copies a secret to the system clipboard and wipes it after [CLEAR_AFTER_MILLIS].
 * The clipboard is marked sensitive so that the system does not show a preview.
 */
class AndroidClipboard(private val context: Context) : Clipboard {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var clearJob: Job? = null

    override suspend fun copySecret(value: String): Result<Unit, AppError> {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return Result.Failure(AppError.ClipboardUnavailable)

        val clip = ClipData.newPlainText(CLIP_LABEL, value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        manager.setPrimaryClip(clip)

        clearJob?.cancel()
        clearJob = scope.launch {
            delay(CLEAR_AFTER_MILLIS)
            manager.clearPrimaryClip()
        }
        return Result.Success(Unit)
    }

    private companion object {
        const val CLIP_LABEL = "PassKeeper"
        const val CLEAR_AFTER_MILLIS = 30_000L
    }
}
