package com.kwazzar.lockruna.contract

import kotlinx.serialization.Serializable

/** A device of the user as `openapi.yaml` describes it. */
@Serializable
data class DeviceDto(
    val id: String,
    val name: String,
    val platform: String,
    val lastSeen: String? = null,
    val current: Boolean,
)
