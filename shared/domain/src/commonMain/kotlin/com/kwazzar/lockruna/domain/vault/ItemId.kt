package com.kwazzar.lockruna.domain.vault

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import kotlin.jvm.JvmInline

/**
 * Unique identifier for a vault item.
 * Must be a valid UUID v4 string (36 chars, 4 dashes at positions 8,13,18,23, hex only).
 */
@JvmInline
value class ItemId private constructor(val value: String) {
    override fun toString(): String = "ItemId(len=${value.length})"

    companion object {
        private const val UUID_LENGTH = 36
        private const val DASH_POSITION_1 = 8
        private const val DASH_POSITION_2 = 13
        private const val DASH_POSITION_3 = 18
        private const val DASH_POSITION_4 = 23
        private val DASH_POSITIONS = setOf(DASH_POSITION_1, DASH_POSITION_2, DASH_POSITION_3, DASH_POSITION_4)
        private const val VERSION_POSITION = 14
        private const val VARIANT_POSITION = 19
        private const val VERSION_4_CHAR = '4'
        private val VALID_VARIANT_CHARS = setOf('8', '9', 'a', 'b')
        private const val HEX_CHARS = "0123456789abcdefABCDEF"

        fun of(raw: String): Result<ItemId, AppError> = if (!isValidUuidV4(raw)) {
            Result.Failure(AppError.InvalidItem("ItemId must be a valid UUID v4"))
        } else {
            Result.Success(ItemId(raw))
        }

        private fun isValidUuidV4(s: String): Boolean = s.length == UUID_LENGTH &&
            hasValidDashes(s) &&
            hasValidHexChars(s) &&
            hasValidVersion(s) &&
            hasValidVariant(s)

        private fun hasValidDashes(s: String): Boolean = s[DASH_POSITION_1] == '-' &&
            s[DASH_POSITION_2] == '-' &&
            s[DASH_POSITION_3] == '-' &&
            s[DASH_POSITION_4] == '-'

        private fun hasValidHexChars(s: String): Boolean {
            for (i in 0 until UUID_LENGTH) {
                if (i in DASH_POSITIONS) continue
                if (s[i] !in HEX_CHARS) return false
            }
            return true
        }

        private fun hasValidVersion(s: String): Boolean = s[VERSION_POSITION] == VERSION_4_CHAR

        private fun hasValidVariant(s: String): Boolean {
            val variantChar = s[VARIANT_POSITION].lowercaseChar()
            return variantChar in VALID_VARIANT_CHARS
        }
    }
}
