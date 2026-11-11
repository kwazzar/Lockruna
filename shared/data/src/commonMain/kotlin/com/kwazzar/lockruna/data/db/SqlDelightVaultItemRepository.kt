package com.kwazzar.lockruna.data.db

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.crypto.CryptoService
import com.kwazzar.lockruna.domain.crypto.GcmCiphertext
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.Clock
import com.kwazzar.lockruna.domain.session.VaultKey
import com.kwazzar.lockruna.domain.session.VaultSession
import com.kwazzar.lockruna.domain.vault.ItemId
import com.kwazzar.lockruna.domain.vault.Secret
import com.kwazzar.lockruna.domain.vault.Title
import com.kwazzar.lockruna.domain.vault.Url
import com.kwazzar.lockruna.domain.vault.VaultItem
import com.kwazzar.lockruna.domain.vault.VaultItemRepository
import com.kwazzar.lockruna.domain.vault.VaultItemType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.coroutines.EmptyCoroutineContext

private const val GCM_TAG_SIZE = 16
private const val HISTORY_SIZE = 10L

/** Keeps only sealed records on disk; encrypts on save, decrypts on read (ADR-005). */
class SqlDelightVaultItemRepository(
    private val database: PassKeeperDatabase,
    private val cryptoService: CryptoService,
    private val session: VaultSession,
    private val clock: Clock,
) : VaultItemRepository {

    override suspend fun save(item: VaultItem): Result<VaultItem, AppError> {
        val vaultKey = session.currentVaultKey() ?: return Result.Failure(AppError.VaultLocked)
        val row = database.passKeeperDatabaseQueries.getVaultItem(item.id.value).executeAsOneOrNull()
        val now = clock.nowMillis()

        val stored = encryptRow(item, vaultKey) ?: return Result.Failure(AppError.ItemCorrupted)

        if (row != null && row.ciphertext != null && row.nonce != null) {
            // shortcut: history rows drop the dirty flag, so the sealed AAD version (dirty +1)
            // cannot be recovered; define history decryption together with the sync wave.
            database.passKeeperDatabaseQueries.insertItemHistory(
                item_id = item.id.value,
                version = row.version,
                ciphertext = row.ciphertext,
                nonce = row.nonce,
                updated_at = row.updated_at,
            )
            database.passKeeperDatabaseQueries.deleteOldHistory(item.id.value, HISTORY_SIZE)
        }

        database.passKeeperDatabaseQueries.upsertVaultItem(
            id = item.id.value,
            type = item.type.name,
            ciphertext = stored.ciphertext + stored.tag,
            nonce = stored.nonce,
            version = item.version,
            sync_seq = 0,
            deleted = item.deleted,
            dirty = item.dirty,
            max_seen_version = maxOf(item.version, row?.max_seen_version ?: 0),
            updated_at = now,
        )

        return Result.Success(item.copy(updatedAtMillis = now))
    }

    override suspend fun get(id: ItemId): Result<VaultItem?, AppError> {
        val vaultKey = session.currentVaultKey() ?: return Result.Failure(AppError.VaultLocked)
        val row = database.passKeeperDatabaseQueries.getVaultItem(id.value).executeAsOneOrNull()
            ?: return Result.Success(null)
        return decryptRow(row, vaultKey)
    }

    override fun observeAll(): Flow<List<VaultItem>> {
        val vaultKey = session.currentVaultKey() ?: return flowOf(emptyList())
        return database.passKeeperDatabaseQueries.getAllNonDeletedVaultItems()
            .asFlow()
            .mapToList(EmptyCoroutineContext)
            .map { rows -> rows.mapNotNull { row -> (decryptRow(row, vaultKey) as? Result.Success)?.value } }
    }

    private fun aad(id: String, type: String, version: Long, dirty: Boolean): ByteArray {
        // ADR-003: AAD version is the value the record has after push, so an unpushed
        // local version is sealed for version + 1 and a synced one for its own version.
        val aadVersion = if (dirty) version + 1 else version
        return "$id|$type|$aadVersion".encodeToByteArray()
    }

    private fun encryptRow(item: VaultItem, vaultKey: VaultKey): GcmCiphertext? {
        val plaintext = VaultItemRowMapper.serializeContent(VaultItemRowMapper.toContent(item)).encodeToByteArray()
        val aad = aad(item.id.value, item.type.name, item.version, item.dirty)
        return attempt { cryptoService.encryptGcm(vaultKey.bytes, plaintext, aad) }
    }

    private fun decryptRow(row: Vault_item, vaultKey: VaultKey): Result<VaultItem?, AppError> {
        val stored = row.ciphertext
        val nonce = row.nonce
        if (stored == null || nonce == null || stored.size < GCM_TAG_SIZE) {
            return Result.Failure(AppError.ItemCorrupted)
        }

        val ciphertext = GcmCiphertext(
            ciphertext = stored.copyOf(stored.size - GCM_TAG_SIZE),
            nonce = nonce,
            tag = stored.copyOfRange(stored.size - GCM_TAG_SIZE, stored.size),
        )
        val plaintext = attempt {
            cryptoService.decryptGcm(vaultKey.bytes, ciphertext, aad(row.id, row.type, row.version, row.dirty))
        } ?: return Result.Failure(AppError.ItemCorrupted)

        return buildItem(row, plaintext)
    }

    private fun buildItem(row: Vault_item, plaintext: ByteArray): Result<VaultItem, AppError> {
        val content = (VaultItemRowMapper.deserializeContent(plaintext.decodeToString()) as? Result.Success)?.value
            ?: return Result.Failure(AppError.ItemCorrupted)
        val item = attempt {
            VaultItem(
                id = requireNotNull(ItemId.of(row.id) as? Result.Success).value,
                type = VaultItemType.valueOf(row.type),
                title = requireNotNull(Title.of(content.title) as? Result.Success).value,
                username = content.username,
                secret = content.secret?.let { (Secret.of(it) as? Result.Success)?.value },
                url = content.url?.let { (Url.of(it) as? Result.Success)?.value },
                folderId = content.folderId?.let { (ItemId.of(it) as? Result.Success)?.value },
                version = row.version,
                dirty = row.dirty,
                deleted = row.deleted,
                updatedAtMillis = row.updated_at,
            )
        } ?: return Result.Failure(AppError.ItemCorrupted)
        return Result.Success(item)
    }

    private inline fun <T> attempt(block: () -> T): T? = try {
        block()
    } catch (ignored: IllegalArgumentException) {
        null
    } catch (ignored: IllegalStateException) {
        null
    }
}
