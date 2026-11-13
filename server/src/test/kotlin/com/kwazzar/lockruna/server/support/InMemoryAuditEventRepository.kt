package com.kwazzar.lockruna.server.support

import com.kwazzar.lockruna.server.audit.domain.AuditEvent
import com.kwazzar.lockruna.server.audit.domain.AuditEventRepository
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * In-memory audit event repository fake for unit tests.
 * Not thread-safe for concurrent modifications of the same queue, but adequate for single-threaded tests.
 */
class InMemoryAuditEventRepository : AuditEventRepository {
    private val events = ConcurrentLinkedQueue<AuditEvent>()

    override suspend fun record(event: AuditEvent) {
        events.add(event)
    }

    fun getEvents(): List<AuditEvent> = events.toList()

    fun clear() {
        events.clear()
    }
}
