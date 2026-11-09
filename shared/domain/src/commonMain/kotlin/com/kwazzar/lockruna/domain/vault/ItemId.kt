package com.kwazzar.lockruna.domain.vault

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError

/**
 * Unique identifier for a vault item.
 * Must be a valid UUID v4 string (36 chars, 4 dashes at positions 8,13,18,23, hex only).
 */
@JvmInline
value class ItemId private constructor(val value: String) {
    override fun toString(): String = "ItemId(len=${value.length})"

    companion object {
        fun of(raw: String): Result<ItemId, AppError> {
            if (!isValidUuidV4(raw)) {
                return Result.Failure(AppError.InvalidItem("ItemId must be a valid UUID v4"))
            }
            return Result.Success(ItemId(raw))
        }

        private fun isValidUuidV4(s: String): Boolean {
            if (s.length != 36) return false
            if (s[8] != '-' || s[13] != '-' || s[18] != '-' || s[23] != '-') return false
            val hexChars = "0123456789abcdefABCDEF"
            for (i in 0 until 36) {
                if (i in setOf(8, 13, 18, 23)) continue
                if (s[i] !in hexChars) return false
            }
            // Version 4: 13th char (index 14) must be '4'
            if (s[14] != '4') return false
            // Variant: 17th char (index 19) must be 8, 9, a, or b
            val variantChar = s[19].lowercaseChar()
            if (variantChar !in setOf('8', '9', 'a', 'b')) return false
            return true
        }
    }
}