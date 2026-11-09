package com.kwazzar.lockruna.server.identity.infrastructure

import com.kwazzar.lockruna.server.identity.domain.DeviceRecord
import com.kwazzar.lockruna.server.identity.domain.RefreshTokenRecord
import com.kwazzar.lockruna.server.identity.domain.UserRecord
import com.kwazzar.lockruna.server.support.PostgresTest
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RefreshTokenRepositoryTest {
    private val users = ExposedUserRepository(PostgresTest.transactions)
    private val devices = ExposedDeviceRepository(PostgresTest.transactions)
    private val tokens = ExposedRefreshTokenRepository(PostgresTest.transactions)

    @BeforeTest
    fun setUp() {
        PostgresTest.migrate()
        PostgresTest.reset()
    }

    @Test
    fun `spending a token twice fails the second time`() = runBlocking {
        val user = users.create(userRecord())
        val device = devices.create(deviceRecord(user.id))
        val token = tokens.insert(tokenRecord(device.id, user.id))
        val now = Instant.now()

        assertTrue(tokens.markUsed(user.id, token.id, now))
        assertFalse(tokens.markUsed(user.id, token.id, now.plusSeconds(1)))
    }

    @Test
    fun `spending a revoked token fails`() = runBlocking {
        val user = users.create(userRecord())
        val device = devices.create(deviceRecord(user.id))
        val token = tokens.insert(tokenRecord(device.id, user.id))
        tokens.revokeDeviceTokens(user.id, device.id, Instant.now())

        assertFalse(tokens.markUsed(user.id, token.id, Instant.now()))
    }

    @Test
    fun `a token of another user cannot be spent`() = runBlocking {
        val user = users.create(userRecord())
        val device = devices.create(deviceRecord(user.id))
        val token = tokens.insert(tokenRecord(device.id, user.id))

        assertFalse(tokens.markUsed("00000000-0000-0000-0000-000000000000", token.id, Instant.now()))
    }

    private fun userRecord() = UserRecord(
        id = UUID.randomUUID().toString(),
        email = "user@example.com",
        authHash = ByteArray(32) { 0x01 },
        recoveryAuthHash = ByteArray(32) { 0x02 },
        kdfSalt = ByteArray(16) { 0x03 },
        kdfParams = """{"alg":"argon2id","m":65536,"t":3,"p":1}""",
        wrappedVaultKey = ByteArray(32) { 0x04 },
        recoveryWrappedVaultKey = ByteArray(32) { 0x05 },
        syncSeq = 0,
    )

    private fun deviceRecord(userId: String) = DeviceRecord(
        id = UUID.randomUUID().toString(),
        userId = userId,
        name = "Test device",
        platform = "ios",
        createdAt = Instant.now(),
        lastSeen = null,
        revokedAt = null,
    )

    private fun tokenRecord(deviceId: String, userId: String) = RefreshTokenRecord(
        id = UUID.randomUUID().toString(),
        deviceId = deviceId,
        userId = userId,
        familyId = UUID.randomUUID().toString(),
        tokenHash = ByteArray(32) { 0x06 },
        expiresAt = Instant.now().plusSeconds(3600),
        familyCreatedAt = Instant.now(),
        usedAt = null,
        revokedAt = null,
    )
}
