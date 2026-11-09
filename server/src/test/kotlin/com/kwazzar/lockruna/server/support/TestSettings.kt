package com.kwazzar.lockruna.server.support

import com.kwazzar.lockruna.server.platform.config.ServerSettings

/**
 * Settings for tests: every value is a throwaway fixture, never a real secret.
 */
fun testSettings(databaseUrl: String = "jdbc:postgresql://localhost:5432/passkeeper_test"): ServerSettings =
    ServerSettings(
        host = "127.0.0.1",
        port = 0,
        databaseUrl = databaseUrl,
        redisUrl = "redis://localhost:6379",
        jwtIssuer = "passkeeper-test",
        jwtAudience = "passkeeper-test-clients",
        jwtActiveKey = "test-active-key-0123456789abcdef",
        jwtPreviousKey = null,
        pepper = ByteArray(32) { 0xab.toByte() },
        authFakeSecret = "test-fake-secret-0123456789abcdef".toByteArray(),
    )
