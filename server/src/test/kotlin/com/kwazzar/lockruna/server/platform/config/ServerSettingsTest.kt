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
                "JWT_PREVIOUS_KEY" to "previous-key",
            ),
        )

        assertEquals("127.0.0.1", settings.host)
        assertEquals(9000, settings.port)
        assertEquals("jdbc:postgresql://localhost:5432/passkeeper", settings.databaseUrl)
        assertEquals("redis://localhost:6379", settings.redisUrl)
        assertEquals("passkeeper", settings.jwtIssuer)
        assertEquals("passkeeper-clients", settings.jwtAudience)
        assertEquals("active-key", settings.jwtActiveKey)
        assertEquals("previous-key", settings.jwtPreviousKey)
        assertContentEquals(pepperBytes, settings.pepper)
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
        listOf("DATABASE_URL", "REDIS_URL", "JWT_ISSUER", "JWT_AUDIENCE", "JWT_ACTIVE_KEY", "PEPPER").forEach { key ->
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

    private companion object {
        val pepperBytes = ByteArray(32) { 0xab.toByte() }
        val pepperHex = pepperBytes.joinToString("") { "%02x".format(it) }
        val requiredEnvironment = mapOf(
            "DATABASE_URL" to "jdbc:postgresql://localhost:5432/passkeeper",
            "REDIS_URL" to "redis://localhost:6379",
            "JWT_ISSUER" to "passkeeper",
            "JWT_AUDIENCE" to "passkeeper-clients",
            "JWT_ACTIVE_KEY" to "active-key",
            "PEPPER" to pepperHex,
        )
    }
}
