package com.kwazzar.lockruna.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

actual class DatabaseDriverFactory {
    actual fun create(): SqlDriver =
        JdbcSqliteDriver("jdbc:sqlite:passkeeper.db")
}
