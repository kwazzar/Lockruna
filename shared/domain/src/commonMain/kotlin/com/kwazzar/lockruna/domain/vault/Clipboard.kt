package com.kwazzar.lockruna.domain.vault

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError

/** Copies sensitive content; native implementations clear it after 30 seconds. */
interface Clipboard {
    suspend fun copySecret(value: String): Result<Unit, AppError>
}
