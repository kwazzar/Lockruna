package com.kwazzar.lockruna.contract

import kotlinx.serialization.Serializable

@Serializable
data class PreloginRequest(val email: String)

@Serializable
data class PreloginResponse(val kdfSalt: String, val kdfParams: KdfParamsDto)

@Serializable
data class DeviceInfoDto(val deviceId: String? = null, val name: String, val platform: String)

@Serializable
data class RegisterRequest(
    val email: String,
    val authKey: String,
    val recoveryAuthKey: String,
    val kdfSalt: String,
    val kdfParams: KdfParamsDto,
    val wrappedVaultKey: String,
    val recoveryWrappedVaultKey: String,
    val device: DeviceInfoDto,
)

@Serializable
data class LoginRequest(val email: String, val authKey: String, val device: DeviceInfoDto)

@Serializable
data class TokenPairDto(val accessToken: String, val refreshToken: String, val expiresIn: Int)

@Serializable
data class AuthResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int,
    val userId: String,
    val deviceId: String,
    val kdfSalt: String,
    val kdfParams: KdfParamsDto,
    val wrappedVaultKey: String,
)
