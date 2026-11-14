package com.kwazzar.lockruna.server.support

import com.mongodb.kotlin.client.coroutine.MongoClient
import kotlinx.coroutines.runBlocking
import org.testcontainers.mongodb.MongoDBContainer
import org.testcontainers.utility.DockerImageName

/**
 * MongoDB Testcontainers helper for integration tests.
 * Usage: MongoTest.mongo { client -> ... }
 * Set TEST_MONGODB_URI to use a local MongoDB instead of testcontainers.
 */
object MongoTest {
    private val client: MongoClient by lazy {
        val uri = System.getenv("TEST_MONGODB_URI")
            ?: MongoDBContainer(DockerImageName.parse("mongo:7-jammy"))
                .withReuse(true)
                .apply { start() }
                .connectionString
        MongoClient.create(uri)
    }

    suspend fun mongo(block: suspend (MongoClient) -> Unit) = runBlocking { block(client) }
}
