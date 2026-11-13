package com.kwazzar.lockruna.server.platform.db

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.json.jsonb

object Users : Table("users") {
    val id = javaUUID("id")
    val email = text("email")
    val authHash = binary("auth_hash")
    val recoveryAuthHash = binary("recovery_auth_hash")
    val kdfSalt = binary("kdf_salt")
    val kdfParams = jsonb("kdf_params", { value: String -> value }, { value: String -> value })
    val wrappedVaultKey = binary("wrapped_vault_key")
    val recoveryWrappedVaultKey = binary("recovery_wrapped_vault_key")
    val syncSeq = long("sync_seq")
    val createdAt = timestampWithTimeZone("created_at")
}

object Devices : Table("devices") {
    val id = javaUUID("id")
    val userId = javaUUID("user_id")
    val name = text("name")
    val platform = text("platform")
    val createdAt = timestampWithTimeZone("created_at")
    val lastSeen = timestampWithTimeZone("last_seen").nullable()
    val revokedAt = timestampWithTimeZone("revoked_at").nullable()
}

object RefreshTokens : Table("refresh_tokens") {
    val id = javaUUID("id")
    val deviceId = javaUUID("device_id")
    val familyId = javaUUID("family_id")
    val tokenHash = binary("token_hash")
    val expiresAt = timestampWithTimeZone("expires_at")
    val familyCreatedAt = timestampWithTimeZone("family_created_at")
    val usedAt = timestampWithTimeZone("used_at").nullable()
    val revokedAt = timestampWithTimeZone("revoked_at").nullable()
}

object VaultItems : Table("vault_items") {
    val userId = javaUUID("user_id")
    val id = javaUUID("id")
    val type = text("type")
    val ciphertext = binary("ciphertext").nullable()
    val nonce = binary("nonce").nullable()
    val version = long("version")
    val syncSeq = long("sync_seq")
    val deletedAt = timestampWithTimeZone("deleted_at").nullable()
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(userId, id)
}

object IdempotencyKeys : Table("idempotency_keys") {
    val userId = javaUUID("user_id")
    val idempotencyKey = javaUUID("idempotency_key")
    val bodyHash = binary("body_hash")
    val response = jsonb("response", { value: String -> value }, { value: String -> value })
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(userId, idempotencyKey)
}

object ItemHistory : Table("item_history") {
    val userId = javaUUID("user_id")
    val itemId = javaUUID("item_id")
    val type = text("type")
    val version = long("version")
    val ciphertext = binary("ciphertext")
    val nonce = binary("nonce")
    val updatedAt = timestampWithTimeZone("updated_at")

    override val primaryKey = PrimaryKey(userId, itemId, version)
}
