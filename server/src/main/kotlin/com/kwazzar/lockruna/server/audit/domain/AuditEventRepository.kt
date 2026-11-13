package com.kwazzar.lockruna.server.audit.domain

interface AuditEventRepository {
    suspend fun record(event: AuditEvent)
}