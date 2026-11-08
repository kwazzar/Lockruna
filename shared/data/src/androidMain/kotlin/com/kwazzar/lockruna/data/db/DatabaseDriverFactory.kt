package com.kwazzar.lockruna.data.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DatabaseDriverFactory {
    private lateinit var context: Context

    fun initialize(context: Context) {
        this.context = context
    }

    actual fun create(): SqlDriver = AndroidSqliteDriver(PassKeeperDatabase.Schema, context, "passkeeper.db")
}
