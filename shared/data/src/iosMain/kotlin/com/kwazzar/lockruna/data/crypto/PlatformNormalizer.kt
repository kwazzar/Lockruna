package com.kwazzar.lockruna.data.crypto

import com.kwazzar.lockruna.domain.crypto.PasswordNormalizer
import platform.Foundation.NSString
import platform.Foundation.precomposedStringWithCompatibilityMapping

actual class PlatformNormalizer actual constructor() : PasswordNormalizer {
    override fun normalize(raw: String): String =
        (raw as NSString).precomposedStringWithCompatibilityMapping() as String
}
