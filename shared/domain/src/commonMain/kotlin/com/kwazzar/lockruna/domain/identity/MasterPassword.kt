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

        fun of(raw: String): Result<MasterPassword, AppError> = if (raw.length < MIN_LENGTH) {
            Result.Failure(AppError.WeakMasterPassword(WeakPasswordReason.TOO_SHORT))
        } else {
            Result.Success(MasterPassword(raw))
        }

        fun strength(raw: String): PasswordStrength {
            val warnings = mutableListOf<String>()
            var score = 0

            if (raw.length < MIN_LENGTH) {
                warnings += "too short"
            } else {
                score += 1
                if (raw.length >= 16) score += 1
            }

            val hasDigit = raw.any { it.isDigit() }
            if (!hasDigit) warnings += "no digit" else score += 1

            val hasSymbol = raw.any { !it.isLetterOrDigit() }
            if (!hasSymbol) warnings += "no symbol" else score += 1

            val hasUpper = raw.any { it.isUpperCase() }
            val hasLower = raw.any { it.isLowerCase() }
            if (hasUpper && hasLower) score += 1 else {
                if (!hasUpper) warnings += "no uppercase"
                if (!hasLower) warnings += "no lowercase"
            }

            if (isCommonPattern(raw)) {
                warnings += "common pattern"
                score = score.coerceAtMost(1)
            }

            val level = if (raw.length < MIN_LENGTH) 0 else score.coerceIn(0, 4)
            return PasswordStrength(level, warnings)
        }

        private fun isCommonPattern(raw: String): Boolean {
            if (raw.isEmpty()) return true
            val lower = raw.lowercase()
            if (raw.toSet().size == 1) return true
            val mostCommonCount = raw.groupingBy { it }.eachCount().values.maxOrNull() ?: 0
            if (raw.length >= 4 && mostCommonCount > raw.length / 2) return true
            val common = listOf(
                "password", "qwerty", "123456", "12345678", "letmein", "admin",
                "welcome", "iloveyou", "abc123", "monkey", "dragon",
            )
            if (common.any { lower.contains(it) }) return true
            if (isSequential(lower)) return true
            return false
        }

        private fun isSequential(s: String): Boolean {
            if (s.length < 4) return false
            var asc = true
            var desc = true
            for (i in 1 until s.length) {
                if (s[i].code != s[i - 1].code + 1) asc = false
                if (s[i].code != s[i - 1].code - 1) desc = false
            }
            return asc || desc
        }
    }
}
