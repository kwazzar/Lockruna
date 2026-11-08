package com.kwazzar.lockruna.data.crypto

import com.kwazzar.lockruna.domain.crypto.PasswordNormalizer
import java.text.Normalizer

actual class PlatformNormalizer actual constructor() : PasswordNormalizer {
    override fun normalize(raw: String): ByteArray =
        Normalizer.normalize(raw, Normalizer.Form.NFKC).toByteArray(Charsets.UTF_8)
}
