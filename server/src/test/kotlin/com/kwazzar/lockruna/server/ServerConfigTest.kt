package com.kwazzar.lockruna.server

import kotlin.test.Test
import kotlin.test.assertEquals

class ServerConfigTest {
    @Test
    fun `falls back to defaults when the environment is empty`() {
        val config = ServerConfig.fromEnv(emptyMap())

        assertEquals("0.0.0.0", config.host)
        assertEquals(8080, config.port)
    }

    @Test
    fun `reads host and port from the environment`() {
        val config = ServerConfig.fromEnv(mapOf("HOST" to "127.0.0.1", "PORT" to "9000"))

        assertEquals("127.0.0.1", config.host)
        assertEquals(9000, config.port)
    }
}
