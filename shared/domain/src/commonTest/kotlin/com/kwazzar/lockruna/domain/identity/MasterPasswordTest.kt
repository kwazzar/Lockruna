package com.kwazzar.lockruna.domain.identity

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.error.WeakPasswordReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

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

    @Test
    fun `strength is zero for a short password`() {
        val s = MasterPassword.strength("short")
        assertEquals(0, s.level)
        assertTrue(s.warnings.contains("too short"))
    }

    @Test
    fun `strength warns when no digit or symbol`() {
        val s = MasterPassword.strength("a".repeat(MasterPassword.MIN_LENGTH))
        assertTrue(s.warnings.contains("no digit"))
        assertTrue(s.warnings.contains("no symbol"))
    }

    @Test
    fun `strength reaches top for a strong password`() {
        val s = MasterPassword.strength("Abcdef1234!@#xyz")
        assertEquals(4, s.level)
        assertTrue(s.warnings.isEmpty())
    }

    @Test
    fun `strength flags common patterns`() {
        val s = MasterPassword.strength("passwordpassword1!")
        assertTrue(s.warnings.contains("common pattern"))
    }
}
