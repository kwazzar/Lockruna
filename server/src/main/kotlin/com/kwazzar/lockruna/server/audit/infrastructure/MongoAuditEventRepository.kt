package com.kwazzar.lockruna.server.audit.infrastructure

import com.kwazzar.lockruna.server.audit.domain.AuditAction
import com.kwazzar.lockruna.server.audit.domain.AuditEvent
import com.kwazzar.lockruna.server.audit.domain.AuditEventRepository
import com.kwazzar.lockruna.server.platform.config.ServerSettings
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.IndexOptions
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.bson.Document

/**
 * MongoDB audit event repository with async bounded queue and fail-open semantics.
 * Never stores secrets, plaintext, ciphertext, nonce, or key material.
 */
class MongoAuditEventRepository(
    private val mongoClient: MongoClient,
    private val settings: ServerSettings,
    private val registry: MeterRegistry = SimpleMeterRegistry(),
) : AuditEventRepository {

    private val collection: MongoCollection<Document> = mongoClient
        .getDatabase("lockruna_audit")
        .getCollection<Document>("audit_events")
        .withCodecRegistry(com.mongodb.MongoClientSettings.getDefaultCodecRegistry())

    private val queue = Channel<AuditEvent>(1000)
    private val scope = CoroutineScope(SupervisorJob())
    private val failures = Counter.builder("lockruna.audit.write.failures").register(registry)

    init {
        scope.launch { processQueue() }
    }

    override suspend fun record(event: AuditEvent) {
        queue.trySend(event) ?: failures.increment()
    }

    private suspend fun processQueue() {
        for (event in queue) {
            try {
                val doc = eventToDoc(event)
                collection.insertOne(doc) // coroutine driver returns InsertOneResult directly
            } catch (e: Exception) {
                failures.increment()
            }
        }
    }

    private fun eventToDoc(e: AuditEvent): Document {
        val metaDoc = Document()
        e.meta.forEach { (k, v) ->
            metaDoc.append(k, v)
        }
        return Document()
            .append("userId", e.userId)
            .append("deviceId", e.deviceId)
            .append("action", e.action.name)
            .append("ip", e.ip)
            .append("ts", e.ts)
            .append("meta", metaDoc)
    }

    companion object {
        /** Provisions required indexes: compound {userId: 1, ts: -1} and TTL on ts (365 days). */
        suspend fun provisionIndexes(mongoClient: MongoClient) {
            val coll = mongoClient
                .getDatabase("lockruna_audit")
                .getCollection<Document>("audit_events")
            // Compound index for query patterns
            coll.createIndex(Indexes.ascending("userId", "ts"))
            // TTL index: 365 days
            coll.createIndex(
                Indexes.ascending("ts"),
                IndexOptions().expireAfter(365L * 24 * 60 * 60, TimeUnit.SECONDS),
            )
        }
    }
}