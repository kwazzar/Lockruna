package com.kwazzar.lockruna.server.audit.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuditEventTest {

    @Test
    fun `AuditAction enum has 8 ADR-004 actions`() {
        val actions = AuditAction.values()
        assertEquals(8, actions.size)
        assertTrue(actions.any { it == AuditAction.REGISTER })
        assertTrue(actions.any { it == AuditAction.LOGIN })
        assertTrue(actions.any { it == AuditAction.LOGIN_FAILED })
        assertTrue(actions.any { it == AuditAction.RECOVERY })
        assertTrue(actions.any { it == AuditAction.DEVICE_REVOKE })
        assertTrue(actions.any { it == AuditAction.TOKEN_REUSE })
        assertTrue(actions.any { it == AuditAction.SYNC_PUSH })
        assertTrue(actions.any { it == AuditAction.ACCOUNT_DELETE })
    }

    @Test
    fun `AuditEvent holds all required fields`() {
        val event = AuditEvent(
            userId = "user-123",
            deviceId = "device-456",
            action = AuditAction.LOGIN,
            ip = "192.168.1.1",
            ts = 1_700_000_000_000L,
            meta = mapOf("key" to "value"),
        )
        assertEquals("user-123", event.userId)
        assertEquals("device-456", event.deviceId)
        assertEquals(AuditAction.LOGIN, event.action)
        assertEquals("192.168.1.1", event.ip)
        assertEquals(1_700_000_000_000L, event.ts)
        assertEquals(mapOf("key" to "value"), event.meta)
    }
}
