package com.kwazzar.lockruna.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DatabaseDriverFactory {
    actual fun create(): SqlDriver =
        NativeSqliteDriver(PassKeeperDatabase.Schema, "passkeeper.db")
}
