package com.kwazzar.lockruna.server.identity.infrastructure

import com.kwazzar.lockruna.server.identity.domain.EmailAlreadyRegisteredException
import com.kwazzar.lockruna.server.identity.domain.UserRecord
import com.kwazzar.lockruna.server.support.PostgresTest
import kotlinx.coroutines.runBlocking
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UserRepositoryTest {
    private val repository = ExposedUserRepository(PostgresTest.transactions)

    @BeforeTest
    fun setUp() {
        PostgresTest.migrate()
        PostgresTest.reset()
    }

    @Test
    fun `finds a user by email regardless of case`() = runBlocking {
        val created = repository.create(userRecord())

        val found = repository.findByEmail("USER@Example.COM")

        assertNotNull(found)
        assertEquals(created.id, found.id)
        assertEquals("user@example.com", found.email)
        assertContentEquals(created.authHash, found.authHash)
    }

    @Test
    fun `returns null for an unknown email`() = runBlocking {
        assertNull(repository.findByEmail("nobody@example.com"))
    }

    @Test
    fun `creating a duplicate email is reported as already registered`() = runBlocking {
        repository.create(userRecord())

        assertFailsWith<EmailAlreadyRegisteredException> {
            repository.create(userRecord())
        }
        Unit
    }

    @Test
    fun `finds a user by id`() = runBlocking {
        val created = repository.create(userRecord())

        val found = repository.findById(created.id)

        assertNotNull(found)
        assertEquals(created.id, found.id)
    }

    private fun userRecord() = UserRecord(
        id = UUID.randomUUID().toString(),
        email = "user@example.com",
        authHash = ByteArray(32) { 0x01 },
        recoveryAuthHash = ByteArray(32) { 0x02 },
        kdfSalt = ByteArray(16) { 0x03 },
        kdfParams = """{"alg":"argon2id","m":65536,"t":3,"p":1}""",
        wrappedVaultKey = ByteArray(32) { 0x04 },
        recoveryWrappedVaultKey = ByteArray(32) { 0x05 },
        syncSeq = 0,
    )
}
