package com.kwazzar.lockruna.domain.auth

enum class DevicePlatform { IOS, ANDROID }

data class DeviceIdentity(
    val deviceId: String?,
    val name: String,
    val platform: DevicePlatform
) {
    override fun toString() = "DeviceIdentity(name=$name)"
}