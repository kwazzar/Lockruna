package com.kwazzar.lockruna.server.identity.application

import com.kwazzar.lockruna.contract.KdfParamsDto

/**
 * The credentials of a session: what the client needs to talk to the API and to unwrap the vault key.
 */
data class AuthSession(
    val userId: String,
    val deviceId: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int,
    val kdfSalt: ByteArray,
    val kdfParams: KdfParamsDto,
    val wrappedVaultKey: ByteArray,
)
