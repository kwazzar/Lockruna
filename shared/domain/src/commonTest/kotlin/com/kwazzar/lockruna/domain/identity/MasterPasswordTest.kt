package com.kwazzar.lockruna.domain.identity

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.error.WeakPasswordReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MasterPasswordTest {
    @Test
    fun `accepts a password of at least the minimum length`() {
        val result = MasterPassword.of("a".repeat(MasterPassword.MIN_LENGTH))

        assertIs<Result.Success<MasterPassword>>(result)
    }

    @Test
    fun `rejects a password shorter than the minimum length`() {
        val result = MasterPassword.of("a".repeat(MasterPassword.MIN_LENGTH - 1))

        val error = assertIs<Result.Failure<AppError>>(result).error
        assertEquals(AppError.WeakMasterPassword(WeakPasswordReason.TOO_SHORT), error)
    }
}
