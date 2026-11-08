@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.kwazzar.lockruna.data.crypto

import com.kwazzar.lockruna.domain.crypto.PasswordNormalizer
import icu.unorm2_getNFKCInstance
import icu.unorm2_normalize
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned

private const val U_ZERO_ERROR: Int = 0
private const val U_BUFFER_OVERFLOW_ERROR: Int = -15

/**
 * NFKC via the system ICU4C normalizer, so the result matches the Android
 * implementation byte for byte.
 */
actual class PlatformNormalizer actual constructor() : PasswordNormalizer {
    actual override fun normalize(raw: String): String {
        if (raw.isEmpty()) return raw
        val source = UShortArray(raw.length) { raw[it].code.toUShort() }
        var capacity = source.size * 3 + 8
        while (true) {
            val target = UShortArray(capacity)
            memScoped {
                val status = allocArray<IntVar>(1)
                val normalizer = unorm2_getNFKCInstance(status)
                check(status[0] == U_ZERO_ERROR) { "ICU NFKC instance is unavailable" }
                val length = source.usePinned { sourcePointer ->
                    target.usePinned { targetPointer ->
                        unorm2_normalize(
                            normalizer,
                            sourcePointer.addressOf(0),
                            source.size,
                            targetPointer.addressOf(0),
                            capacity,
                            status,
                        )
                    }
                }
                when (status[0]) {
                    U_ZERO_ERROR -> return buildString {
                        for (index in 0 until length) append(target[index].toInt().toChar())
                    }
                    U_BUFFER_OVERFLOW_ERROR -> capacity = length
                    else -> error("ICU NFKC normalization failed with code ${status[0]}")
                }
            }
        }
    }
}
