package com.kwazzar.lockruna.domain.vault

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError

/**
 * Title of a vault item. Must not be blank.
 */
@JvmInline
value class Title private constructor(val value: String) {
    override fun toString(): String = "Title(len=${value.length})"

    companion object {
        fun of(raw: String): Result<Title, AppError> {
            if (raw.isBlank()) {
                return Result.Failure(AppError.InvalidItem("Title must not be blank"))
            }
            return Result.Success(Title(raw.trim()))
        }
    }
}
