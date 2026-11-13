package com.kwazzar.lockruna.contract

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthDtosTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `RecoveryStartResponseDto round-trips`() {
        val original = RecoveryStartResponseDto(
            userId = "12345678-1234-1234-1234-123456789abc",
            recoveryWrappedVaultKey = "dGVzdA==",
        )
        val encoded = json.encodeToString(RecoveryStartResponseDto.serializer(), original)
        val decoded = json.decodeFromString(RecoveryStartResponseDto.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `RecoveryCompleteRequest round-trips`() {
        val original = RecoveryCompleteRequest(
            email = "test@example.com",
            recoveryAuthKey = "cmVjb3Zlcnk=",
            newAuthKey = "bmV3YXV0aA==",
            newKdfSalt = "c2FsdA==",
            newKdfParams = KdfParamsDto(alg = "argon2id", memoryKiB = 65536, iterations = 3, parallelism = 1),
            newWrappedVaultKey = "d3JhcHBlZA==",
            device = DeviceInfoDto(name = "test-device", platform = "android"),
        )
        val encoded = json.encodeToString(RecoveryCompleteRequest.serializer(), original)
        val decoded = json.decodeFromString(RecoveryCompleteRequest.serializer(), encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `DeleteAccountRequest round-trips`() {
        val original = DeleteAccountRequest(authKey = "YXV0aA==")
        val encoded = json.encodeToString(DeleteAccountRequest.serializer(), original)
        val decoded = json.decodeFromString(DeleteAccountRequest.serializer(), encoded)
        assertEquals(original, decoded)
    }
}