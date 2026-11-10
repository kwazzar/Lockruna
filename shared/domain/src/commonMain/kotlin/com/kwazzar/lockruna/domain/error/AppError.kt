package com.kwazzar.lockruna.domain.error

/** Sealed error hierarchy, one branch per context. */
sealed interface AppError {
    data object VaultLocked : AppError

    data object InvalidCredentials : AppError

    data object Unauthorized : AppError

    data object NetworkUnavailable : AppError

    data object ItemCorrupted : AppError

    data object ItemNotFound : AppError

    data class InvalidItem(val reason: String) : AppError

    data class WeakKdfParams(val actual: String) : AppError

    data class RateLimited(val retryAfterSeconds: Int) : AppError

    data class WeakMasterPassword(val reason: WeakPasswordReason) : AppError
}

enum class WeakPasswordReason {
    TOO_SHORT,
    TOO_WEAK,
}
