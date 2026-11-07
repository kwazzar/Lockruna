package com.kwazzar.lockruna.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** KDF parameters as defined by `openapi.yaml` (`KdfParams`). */
@Serializable
data class KdfParamsDto(
    @SerialName("alg") val alg: String = "argon2id",
    @SerialName("m") val memoryKiB: Int,
    @SerialName("t") val iterations: Int,
    @SerialName("p") val parallelism: Int = 1,
)
