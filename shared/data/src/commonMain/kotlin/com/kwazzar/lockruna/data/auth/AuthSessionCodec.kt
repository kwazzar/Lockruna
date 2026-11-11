package com.kwazzar.lockruna.data.auth

import com.kwazzar.lockruna.domain.auth.AuthTokens
import com.kwazzar.lockruna.domain.auth.AuthenticatedUser
import com.kwazzar.lockruna.domain.crypto.KdfParams
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

/** JSON wire format of [AuthenticatedUser] for the platform secure storage; bytes travel as Base64. */
internal object AuthSessionCodec {

    private val json = Json { ignoreUnknownKeys = true }

    fun encode(user: AuthenticatedUser): ByteArray = json.encodeToString(SessionRecord.from(user)).encodeToByteArray()

    /** Returns null for anything that is not a record this build understands. */
    fun decode(bytes: ByteArray): AuthenticatedUser? = try {
        json.decodeFromString<SessionRecord>(bytes.decodeToString()).toDomain()
    } catch (ignored: SerializationException) {
        null
    } catch (ignored: IllegalArgumentException) {
        null
    }
}

@Serializable
private data class SessionRecord(
    val userId: String,
    val deviceId: String,
    val wrappedVaultKey: String,
    val keyNonce: String,
    val kdfSalt: String,
    val kdfParams: KdfParamsRecord,
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Int,
) {
    fun toDomain() = AuthenticatedUser(
        userId = userId,
        deviceId = deviceId,
        wrappedVaultKey = Base64.decode(wrappedVaultKey),
        keyNonce = Base64.decode(keyNonce),
        kdfSalt = Base64.decode(kdfSalt),
        kdfParams = kdfParams.toDomain(),
        tokens = AuthTokens(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresInSeconds = expiresInSeconds,
        ),
    )

    companion object {
        fun from(user: AuthenticatedUser) = SessionRecord(
            userId = user.userId,
            deviceId = user.deviceId,
            wrappedVaultKey = Base64.encode(user.wrappedVaultKey),
            keyNonce = Base64.encode(user.keyNonce),
            kdfSalt = Base64.encode(user.kdfSalt),
            kdfParams = KdfParamsRecord.from(user.kdfParams),
            accessToken = user.tokens.accessToken,
            refreshToken = user.tokens.refreshToken,
            expiresInSeconds = user.tokens.expiresInSeconds,
        )
    }
}

@Serializable
private data class KdfParamsRecord(
    val alg: String,
    val memoryKiB: Int,
    val iterations: Int,
    val parallelism: Int,
    val saltLength: Int,
    val outputLength: Int,
) {
    fun toDomain() = KdfParams(
        alg = alg,
        memoryKiB = memoryKiB,
        iterations = iterations,
        parallelism = parallelism,
        saltLength = saltLength,
        outputLength = outputLength,
    )

    companion object {
        fun from(params: KdfParams) = KdfParamsRecord(
            alg = params.alg,
            memoryKiB = params.memoryKiB,
            iterations = params.iterations,
            parallelism = params.parallelism,
            saltLength = params.saltLength,
            outputLength = params.outputLength,
        )
    }
}
