package com.kwazzar.lockruna.domain.vault

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import kotlin.jvm.JvmInline

/**
 * Secret value (password, card number, etc.). Must not be empty.
 * Never logged or included in toString.
 */
@JvmInline
value class Secret private constructor(val value: String) {
    override fun toString(): String = "Secret(len=${value.length})"

    companion object {
        fun of(raw: String): Result<Secret, AppError> {
            if (raw.isEmpty()) {
                return Result.Failure(AppError.InvalidItem("Secret must not be empty"))
            }
            return Result.Success(Secret(raw))
        }
    }
}
