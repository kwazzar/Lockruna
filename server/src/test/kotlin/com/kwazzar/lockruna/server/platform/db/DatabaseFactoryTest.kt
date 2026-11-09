package com.kwazzar.lockruna.server.platform.db

import kotlin.test.Test
import kotlin.test.assertNull

class DatabaseFactoryTest {
    @Test
    fun `leaves credentials unset when they come from the url`() {
        val config = DatabaseFactory.hikariConfig("jdbc:postgresql://localhost:5432/passkeeper")

        assertNull(config.username)
        assertNull(config.password)
    }
}
