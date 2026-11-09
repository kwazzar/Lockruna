package com.kwazzar.lockruna.domain.vault

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError

/**
 * URL for a vault item. Must start with http:// or https://.
 */
@JvmInline
value class Url private constructor(val value: String) {
    override fun toString(): String = "Url(len=${value.length})"

    companion object {
        fun of(raw: String): Result<Url, AppError> {
            val trimmed = raw.trim()
            if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                return Result.Failure(AppError.InvalidItem("Url must start with http:// or https://"))
            }
            return Result.Success(Url(trimmed))
        }
    }
}