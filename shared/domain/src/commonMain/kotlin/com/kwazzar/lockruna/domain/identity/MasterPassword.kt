package com.kwazzar.lockruna.domain.identity

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.error.WeakPasswordReason
import kotlin.jvm.JvmInline

/**
 * Master password with its validity rule baked in.
 *
 * The raw value never leaves the device and must never be logged.
 */
@JvmInline
value class MasterPassword private constructor(val value: String) {
    companion object {
        const val MIN_LENGTH: Int = 12

        fun of(raw: String): Result<MasterPassword, AppError> =
            if (raw.length < MIN_LENGTH) {
                Result.Failure(AppError.WeakMasterPassword(WeakPasswordReason.TOO_SHORT))
            } else {
                Result.Success(MasterPassword(raw))
            }
    }
}
