package com.kwazzar.lockruna.domain.crypto

object HkdfLabels {
    const val AUTH: String = "passkeeper/auth/v1"
    const val KEK: String = "passkeeper/kek/v1"
    const val RECOVERY_WRAP: String = "passkeeper/recovery-wrap/v1"
    const val RECOVERY_AUTH: String = "passkeeper/recovery-auth/v1"
}
