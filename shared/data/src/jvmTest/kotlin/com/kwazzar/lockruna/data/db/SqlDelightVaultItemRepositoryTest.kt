package com.kwazzar.lockruna.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.VaultKey
import com.kwazzar.lockruna.domain.session.VaultSession
import com.kwazzar.lockruna.domain.vault.ItemId
import com.kwazzar.lockruna.domain.vault.Secret
import com.kwazzar.lockruna.domain.vault.Title
import com.kwazzar.lockruna.domain.vault.Url
import com.kwazzar.lockruna.domain.vault.VaultItem
import com.kwazzar.lockruna.domain.vault.VaultItemType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private const val BASE_MILLIS = 1_000_000L

class SqlDelightVaultItemRepositoryTest {

    private fun createRepo(session: VaultSession = VaultSession()): Pair<SqlDelightVaultItemRepository, SqlDriver> {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        PassKeeperDatabase.Schema.create(driver)
        val repo = SqlDelightVaultItemRepository(
            database = PassKeeperDatabase(driver),
            cryptoService = FakeCryptoService(),
            session = session,
            clock = FakeClock(wallMillis = BASE_MILLIS, monotonicMillis = 500_000),
        )
        return repo to driver
    }

    private fun createItem(id: String, type: VaultItemType = VaultItemType.LOGIN): VaultItem = VaultItem(
        id = (ItemId.of(id) as Result.Success).value,
        type = type,
        title = (Title.of("Test Item") as Result.Success).value,
        username = "testuser",
        secret = (Secret.of("secret-value") as Result.Success).value,
        url = (Url.of("https://example.com") as Result.Success).value,
        folderId = null,
        version = 0,
        dirty = true,
        deleted = false,
        updatedAtMillis = BASE_MILLIS,
    )

    private fun unlockedSession(cryptoService: FakeCryptoService): VaultSession =
        VaultSession().apply { unlock(VaultKey.random(cryptoService)) }

    @Test
    fun `save then get returns the same item`() = runTest {
        val (repo, _) = createRepo(unlockedSession(FakeCryptoService()))
        val item = createItem("00000000-0000-4000-8000-000000000001")

        val saved = repo.save(item)

        val retrieved = assertIs<Result.Success<VaultItem?>>(repo.get(item.id)).value
        assertNotNull(retrieved)
        assertEquals(item.id, retrieved.id)
        assertEquals(item.title, retrieved.title)
        assertEquals(item.username, retrieved.username)
        assertEquals(item.secret, retrieved.secret)
        assertEquals(item.url, retrieved.url)
        assertEquals(item.version, retrieved.version)
        assertEquals(item.dirty, retrieved.dirty)
        assertEquals(BASE_MILLIS, retrieved.updatedAtMillis)
        assertIs<Result.Success<VaultItem>>(saved)
    }

    @Test
    fun `only sealed columns are filled`() = runTest {
        val (repo, driver) = createRepo(unlockedSession(FakeCryptoService()))
        repo.save(createItem("00000000-0000-4000-8000-000000000001"))

        val row = PassKeeperDatabase(driver)
            .passKeeperDatabaseQueries
            .getVaultItem("00000000-0000-4000-8000-000000000001")
            .executeAsOne()

        assertNotNull(row.ciphertext)
        assertNotNull(row.nonce)
    }

    @Test
    fun `get with a wrong key returns ItemCorrupted`() = runTest {
        val cryptoService = FakeCryptoService()
        val session = unlockedSession(cryptoService)
        val (repo, driver) = createRepo(session)
        val item = createItem("00000000-0000-4000-8000-000000000001")
        assertIs<Result.Success<VaultItem>>(repo.save(item))

        session.lock()
        session.unlock(VaultKey.random(cryptoService))
        val otherRepo = SqlDelightVaultItemRepository(
            PassKeeperDatabase(driver),
            cryptoService,
            session,
            FakeClock(wallMillis = BASE_MILLIS, monotonicMillis = 500_000),
        )

        val result = otherRepo.get(item.id)
        assertIs<Result.Failure<*>>(result)
        assertEquals(AppError.ItemCorrupted, result.error)
    }

    @Test
    fun `save on a locked session returns VaultLocked`() = runTest {
        val (repo, _) = createRepo(VaultSession())

        val result = repo.save(createItem("00000000-0000-4000-8000-000000000001"))

        assertIs<Result.Failure<*>>(result)
        assertEquals(AppError.VaultLocked, result.error)
    }

    @Test
    fun `history keeps only the last ten versions`() = runTest {
        val (repo, driver) = createRepo(unlockedSession(FakeCryptoService()))
        var item = createItem("00000000-0000-4000-8000-000000000001")
        assertIs<Result.Success<VaultItem>>(repo.save(item))

        for (i in 1..12) {
            item = assertIs<Result.Success<VaultItem>>(
                item.update(
                    (Title.of("Updated $i") as Result.Success).value,
                    item.username,
                    (Secret.of("secret-$i") as Result.Success).value,
                    (Url.of("https://example.com") as Result.Success).value,
                    null,
                    BASE_MILLIS + i,
                ),
            ).value
            assertIs<Result.Success<VaultItem>>(repo.save(item))
        }

        val history = PassKeeperDatabase(driver).passKeeperDatabaseQueries.getItemHistory(item.id.value).executeAsList()
        assertEquals(10, history.size)
        assertEquals((2L..11L).toList(), history.map { it.version }.sorted())
    }

    @Test
    fun `max_seen_version never decreases`() = runTest {
        val (repo, driver) = createRepo(unlockedSession(FakeCryptoService()))
        var item = createItem("00000000-0000-4000-8000-000000000001")
        assertIs<Result.Success<VaultItem>>(repo.save(item))

        for (i in 1..5) {
            item = assertIs<Result.Success<VaultItem>>(
                item.update(
                    (Title.of("Updated $i") as Result.Success).value,
                    item.username,
                    (Secret.of("secret-$i") as Result.Success).value,
                    (Url.of("https://example.com") as Result.Success).value,
                    null,
                    BASE_MILLIS + i,
                ),
            ).value
            assertIs<Result.Success<VaultItem>>(repo.save(item))
        }

        val queries = PassKeeperDatabase(driver).passKeeperDatabaseQueries
        assertEquals(5, queries.getVaultItem(item.id.value).executeAsOne().max_seen_version)

        // A stale version arriving later must not pull the guard down.
        queries.updateMaxSeenVersion(3, item.id.value)
        assertEquals(5, queries.getVaultItem(item.id.value).executeAsOne().max_seen_version)
    }

    @Test
    fun `observeAll emits saves and hides deleted items`() = runTest {
        val (repo, _) = createRepo(unlockedSession(FakeCryptoService()))

        assertEquals(emptyList(), repo.observeAll().first())

        val item = createItem("00000000-0000-4000-8000-000000000001")
        assertIs<Result.Success<VaultItem>>(repo.save(item))
        assertEquals(listOf(item.id), repo.observeAll().first().map { it.id })

        val deleted = assertIs<Result.Success<VaultItem>>(item.delete(BASE_MILLIS + 1)).value
        assertIs<Result.Success<VaultItem>>(repo.save(deleted))
        assertEquals(emptyList(), repo.observeAll().first())
    }

    @Test
    fun `observeAll is empty while locked`() = runTest {
        val (repo, _) = createRepo(VaultSession())

        assertEquals(emptyList(), repo.observeAll().first())
    }

    @Test
    fun `get of a missing item returns null`() = runTest {
        val (repo, _) = createRepo(unlockedSession(FakeCryptoService()))

        val result = repo.get((ItemId.of("00000000-0000-4000-8000-000000000009") as Result.Success).value)

        assertNull(assertIs<Result.Success<VaultItem?>>(result).value)
    }
}
