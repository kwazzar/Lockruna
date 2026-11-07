package com.kwazzar.lockruna.data.db

import app.cash.sqldelight.db.SqlDriver

/** Creates the local database driver for the current platform. */
expect class DatabaseDriverFactory {
    fun create(): SqlDriver
}
