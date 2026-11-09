package com.kwazzar.lockruna.server.platform.db

import com.kwazzar.lockruna.server.platform.config.ServerSettings
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import javax.sql.DataSource

/**
 * Postgres wiring: connection pool, schema migrations and the Exposed database handle.
 */
object DatabaseFactory {
    private const val POOL_SIZE = 10

    fun createDataSource(url: String, user: String? = null, password: String? = null): HikariDataSource =
        HikariDataSource(hikariConfig(url, user, password))

    fun hikariConfig(url: String, user: String? = null, password: String? = null): HikariConfig = HikariConfig().apply {
        jdbcUrl = url
        if (user != null) username = user
        if (password != null) this.password = password
        maximumPoolSize = POOL_SIZE
        poolName = "lockruna-server"
    }

    fun migrate(dataSource: DataSource) {
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
            .migrate()
    }

    fun connect(settings: ServerSettings): Database {
        val dataSource = createDataSource(settings.databaseUrl)
        migrate(dataSource)
        return Database.connect(dataSource)
    }
}
