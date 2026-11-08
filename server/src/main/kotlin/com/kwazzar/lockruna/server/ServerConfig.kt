package com.kwazzar.lockruna.server

/**
 * Runtime configuration. Every secret (pepper, JWT key, database password) comes from the
 * environment and is never stored in the repository.
 */
data class ServerConfig(val host: String, val port: Int) {
    companion object {
        fun fromEnv(env: Map<String, String> = System.getenv()): ServerConfig = ServerConfig(
            host = env["HOST"] ?: "0.0.0.0",
            port = env["PORT"]?.toIntOrNull() ?: 8080,
        )
    }
}
