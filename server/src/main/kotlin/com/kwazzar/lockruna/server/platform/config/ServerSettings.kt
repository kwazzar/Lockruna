package com.kwazzar.lockruna.server.platform.config

/**
 * Runtime configuration. Every secret (pepper, JWT key, database password) comes from the
 * environment and is never stored in the repository.
 */
data class ServerSettings(
    val host: String,
    val port: Int,
    val databaseUrl: String,
    val redisUrl: String,
    val jwtIssuer: String,
    val jwtAudience: String,
    val jwtActiveKey: String,
    val jwtPreviousKey: String?,
    val pepper: ByteArray,
    val authFakeSecret: ByteArray,
) {
    companion object {
        private const val PEPPER_BYTES = 32
        private const val HEX_SHIFT = 4
        private const val JWT_KEY_MIN_BYTES = 32
        private const val DEFAULT_HOST = "0.0.0.0"
        private const val DEFAULT_PORT = 8080

        fun fromEnv(env: Map<String, String> = System.getenv()): ServerSettings = ServerSettings(
            host = env["HOST"] ?: DEFAULT_HOST,
            port = env["PORT"]?.toIntOrNull() ?: DEFAULT_PORT,
            databaseUrl = env.required("DATABASE_URL"),
            redisUrl = env.required("REDIS_URL"),
            jwtIssuer = env.required("JWT_ISSUER"),
            jwtAudience = env.required("JWT_AUDIENCE"),
            jwtActiveKey = env.required("JWT_ACTIVE_KEY").toJwtKey(),
            jwtPreviousKey = env["JWT_PREVIOUS_KEY"]?.takeIf { it.isNotBlank() }?.toJwtKey(),
            pepper = env.required("PEPPER").toPepper(),
            authFakeSecret = env.required("AUTH_FAKE_SECRET").toByteArray(),
        )

        private fun Map<String, String>.required(name: String): String =
            this[name]?.takeIf { it.isNotBlank() } ?: error("$name is required")

        private fun String.toPepper(): ByteArray {
            if (length != PEPPER_BYTES * 2) error("PEPPER must be ${PEPPER_BYTES * 2} hex characters")
            val bytes = ByteArray(PEPPER_BYTES)
            for (index in 0 until PEPPER_BYTES) {
                val high = Character.digit(this[index * 2], 16)
                val low = Character.digit(this[index * 2 + 1], 16)
                if (high < 0 || low < 0) error("PEPPER must be a hex string")
                bytes[index] = ((high shl HEX_SHIFT) or low).toByte()
            }
            return bytes
        }

        private fun String.toJwtKey(): String {
            if (encodeToByteArray().size < JWT_KEY_MIN_BYTES) {
                error("JWT key must be at least $JWT_KEY_MIN_BYTES bytes")
            }
            return this
        }
    }
}
