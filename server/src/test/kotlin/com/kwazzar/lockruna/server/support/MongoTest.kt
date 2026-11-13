package com.kwazzar.lockruna.server.support

import com.mongodb.kotlin.client.coroutine.MongoClient
import kotlinx.coroutines.runBlocking
import org.testcontainers.mongodb.MongoDBContainer
import org.testcontainers.utility.DockerImageName

/**
 * MongoDB Testcontainers helper for integration tests.
 * Usage: MongoTest.mongo { client -> ... }
 */
object MongoTest {
    private val container: MongoDBContainer by lazy {
        MongoDBContainer(DockerImageName.parse("mongo:7-jammy"))
            .withReuse(true)
    }

    private val client: MongoClient by lazy {
        container.apply { start() }
        // Use the connection string provided by testcontainers
        val uri = container.getConnectionString()
        MongoClient.create(uri)
    }

    suspend fun mongo(block: suspend (MongoClient) -> Unit) = runBlocking { block(client) }
}