package com.kwazzar.lockruna.server.platform.logging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SafeCallLoggingTest {
    @Test
    fun `logs only method, path, status and duration`() {
        val line = SafeCallLogging.format("POST", "/auth/login", 401, 12)

        assertEquals("POST /auth/login 401 12ms", line)
    }

    @Test
    fun `never includes request or response bodies`() {
        val line = SafeCallLogging.format("POST", "/auth/login", 401, 12)

        assertFalse(line.contains("authKey"))
        assertFalse(line.contains("body"))
    }
}
