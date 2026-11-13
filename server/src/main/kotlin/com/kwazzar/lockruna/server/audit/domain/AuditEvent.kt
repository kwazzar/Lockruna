package com.kwazzar.lockruna.server.audit.domain

import kotlinx.serialization.Serializable

/**
 * Audit actions per ADR-004 MongoDB schema:
 * register | login | login.failed | recovery | device.revoke | token.reuse | sync.push | account.delete
 */
enum class AuditAction {
    REGISTER,
    LOGIN,
    LOGIN_FAILED,
    RECOVERY,
    DEVICE_REVOKE,
    TOKEN_REUSE,
    SYNC_PUSH,
    ACCOUNT_DELETE
}

@Serializable
data class AuditEvent(
    val userId: String?,
    val deviceId: String?,
    val action: AuditAction,
    val ip: String?,
    val ts: Long,
    val meta: Map<String, String> = emptyMap(),
)