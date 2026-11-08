package com.kwazzar.lockruna.server.platform.db

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

internal fun String.toUuid(): UUID = UUID.fromString(this)

internal fun Instant.toColumn(): OffsetDateTime = OffsetDateTime.ofInstant(this, ZoneOffset.UTC)
