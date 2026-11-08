@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.kwazzar.lockruna.data.crypto

import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned

/**
 * Pins [bytes] and passes a native pointer to [block]. Empty arrays get a valid
 * placeholder pointer, because libsodium reads zero bytes from them anyway.
 */
internal inline fun <T> withCBytes(bytes: ByteArray, block: (CValuesRef<UByteVar>) -> T): T {
    val source = bytes.asUByteArray()
    if (source.isEmpty()) {
        val placeholder = UByteArray(1)
        return placeholder.usePinned { block(it.addressOf(0)) }
    }
    return source.usePinned { block(it.addressOf(0)) }
}
