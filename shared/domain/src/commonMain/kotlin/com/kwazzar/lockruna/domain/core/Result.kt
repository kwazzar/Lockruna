package com.kwazzar.lockruna.domain.core

/**
 * Outcome of an operation that can fail with a domain error.
 *
 * Exceptions never cross the Swift boundary, so every use case returns this type.
 */
sealed interface Result<out T, out E> {
    data class Success<out T>(val value: T) : Result<T, Nothing>

    data class Failure<out E>(val error: E) : Result<Nothing, E>
}
