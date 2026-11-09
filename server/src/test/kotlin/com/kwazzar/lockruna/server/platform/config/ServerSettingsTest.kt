package com.kwazzar.lockruna.server.platform.config

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ServerSettingsTest {
    @Test
    fun `reads every setting from the environment`() {
        val settings = ServerSettings.fromEnv(
            requiredEnvironment + mapOf(
                "HOST" to "127.0.0.1",
                "PORT" to "9000",
                "JWT_PREVIOUS_KEY" to PREVIOUS_KEY,
            ),
        )

        assertEquals("127.0.0.1", settings.host)
        assertEquals(9000, settings.port)
        assertEquals("jdbc:postgresql://localhost:5432/passkeeper", settings.databaseUrl)
        assertEquals("redis://localhost:6379", settings.redisUrl)
        assertEquals("passkeeper", settings.jwtIssuer)
        assertEquals("passkeeper-clients", settings.jwtAudience)
        assertEquals(ACTIVE_KEY, settings.jwtActiveKey)
        assertEquals(PREVIOUS_KEY, settings.jwtPreviousKey)
        assertContentEquals(pepperBytes, settings.pepper)
        assertContentEquals(FAKE_SECRET.toByteArray(), settings.authFakeSecret)
    }

    @Test
    fun `falls back to the default host and port`() {
        val settings = ServerSettings.fromEnv(requiredEnvironment)

        assertEquals("0.0.0.0", settings.host)
        assertEquals(8080, settings.port)
    }

    @Test
    fun `previous jwt key is optional`() {
        val settings = ServerSettings.fromEnv(requiredEnvironment)

        assertNull(settings.jwtPreviousKey)
    }

    @Test
    fun `refuses to start without a required secret`() {
        listOf(
            "DATABASE_URL",
            "REDIS_URL",
            "JWT_ISSUER",
            "JWT_AUDIENCE",
            "JWT_ACTIVE_KEY",
            "AUTH_FAKE_SECRET",
            "PEPPER",
        ).forEach { key ->
            assertFailsWith<IllegalStateException>("missing $key must stop the server") {
                ServerSettings.fromEnv(requiredEnvironment - key)
            }
        }
    }

    @Test
    fun `refuses to start when the pepper is not 32 hex bytes`() {
        assertFailsWith<IllegalStateException> {
            ServerSettings.fromEnv(requiredEnvironment + ("PEPPER" to "00".repeat(16)))
        }
    }

    @Test
    fun `refuses to start when the fake secret is shorter than 32 bytes`() {
        assertFailsWith<IllegalStateException> {
            ServerSettings.fromEnv(requiredEnvironment + ("AUTH_FAKE_SECRET" to "short-secret"))
        }
    }

    @Test
    fun `refuses to start when a jwt key is shorter than 32 bytes`() {
        assertFailsWith<IllegalStateException> {
            ServerSettings.fromEnv(requiredEnvironment + ("JWT_ACTIVE_KEY" to "short-key"))
        }
        assertFailsWith<IllegalStateException> {
            ServerSettings.fromEnv(requiredEnvironment + ("JWT_PREVIOUS_KEY" to "short-key"))
        }
    }

    private companion object {
        val pepperBytes = ByteArray(32) { 0xab.toByte() }
        val pepperHex = pepperBytes.joinToString("") { "%02x".format(it) }
        const val ACTIVE_KEY = "active-key-0123456789abcdef0123456789"
        const val PREVIOUS_KEY = "previous-key-0123456789abcdef0123456"
        const val FAKE_SECRET = "fake-secret-0123456789abcdef012345"
        val requiredEnvironment = mapOf(
            "DATABASE_URL" to "jdbc:postgresql://localhost:5432/passkeeper",
            "REDIS_URL" to "redis://localhost:6379",
            "JWT_ISSUER" to "passkeeper",
            "JWT_AUDIENCE" to "passkeeper-clients",
            "JWT_ACTIVE_KEY" to ACTIVE_KEY,
            "AUTH_FAKE_SECRET" to FAKE_SECRET,
            "PEPPER" to pepperHex,
        )
    }
}
