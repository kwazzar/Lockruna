package com.kwazzar.lockruna.server.support

import com.kwazzar.lockruna.server.platform.db.DatabaseFactory
import com.kwazzar.lockruna.server.platform.db.ExposedTransactionRunner
import com.kwazzar.lockruna.server.platform.db.TransactionRunner
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.jdbc.Database
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * Postgres for integration tests: Testcontainers by default, a local database when `TEST_DATABASE_URL` is set.
 */
object PostgresTest {
    private val container: PostgreSQLContainer by lazy {
        PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
    }

    val dataSource: HikariDataSource by lazy {
        val url = System.getenv("TEST_DATABASE_URL") ?: container.apply { start() }.jdbcUrl
        val user = System.getenv("TEST_DATABASE_USER") ?: container.username
        val password = System.getenv("TEST_DATABASE_PASSWORD") ?: container.password
        DatabaseFactory.createDataSource(url, user, password)
    }

    val transactions: TransactionRunner by lazy { ExposedTransactionRunner(connect()) }

    fun migrate() {
        DatabaseFactory.migrate(dataSource)
    }

    fun reset() {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("TRUNCATE TABLE users CASCADE")
            }
        }
    }

    fun tableNames(): List<String> = dataSource.connection.use { connection ->
        connection.metaData.getTables(null, "public", "%", arrayOf("TABLE")).use { rows ->
            buildList {
                while (rows.next()) add(rows.getString("TABLE_NAME"))
            }
        }
    }

    fun indexNames(): List<String> = dataSource.connection.use { connection ->
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT indexname FROM pg_indexes WHERE schemaname = 'public'").use { rows ->
                buildList {
                    while (rows.next()) add(rows.getString("indexname"))
                }
            }
        }
    }

    private fun connect(): Database {
        migrate()
        return Database.connect(dataSource)
    }
}
