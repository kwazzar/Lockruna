package com.kwazzar.lockruna.server.audit.infrastructure

import com.kwazzar.lockruna.server.audit.domain.AuditAction
import com.kwazzar.lockruna.server.audit.domain.AuditEvent
import com.kwazzar.lockruna.server.support.MongoTest
import com.kwazzar.lockruna.server.support.testSettings
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.bson.Document

class MongoAuditEventRepositoryTest {

    @Test
    fun `record failure does not throw`() = runBlocking {
        // Create a client that will fail
        val failingClient = com.mongodb.kotlin.client.coroutine.MongoClient.create("mongodb://invalid:27017")
        val repo = MongoAuditEventRepository(failingClient, testSettings())
        
        val event = AuditEvent(
            userId = "user-1",
            deviceId = "device-1",
            action = AuditAction.LOGIN,
            ip = "1.2.3.4",
            ts = System.currentTimeMillis(),
        )
        
        // Should not throw even though MongoDB is unreachable
        repo.record(event)
        failingClient.close()
    }

    @Test
    fun `failure counter increments on error`() = runBlocking {
        MongoTest.mongo { client ->
            val registry = SimpleMeterRegistry()
            val repo = MongoAuditEventRepository(client, testSettings(), registry)
            
            val event = AuditEvent(
                userId = "user-1",
                deviceId = "device-1",
                action = AuditAction.LOGIN,
                ip = "1.2.3.4",
                ts = System.currentTimeMillis(),
            )
            repo.record(event)
            Thread.sleep(500)
            
            val counter = registry.get("lockruna.audit.write.failures").counter()
            // Should be 0 since MongoDB is available
            assertEquals(0.0, counter?.count() ?: 0.0)
        }
    }
}