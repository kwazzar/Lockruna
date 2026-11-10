package com.kwazzar.lockruna.domain.session

import com.kwazzar.lockruna.domain.crypto.FakeCryptoService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class VaultSessionTest {

    private val cryptoService = FakeCryptoService()

    @Test
    fun `starts locked`() {
        assertEquals(VaultSession.State.Locked, VaultSession().state)
    }

    @Test
    fun `unlock opens the session and keeps the key`() {
        val session = VaultSession()
        val key = VaultKey.random(cryptoService)

        session.unlock(key)

        assertEquals(VaultSession.State.Unlocked, session.state)
        assertNotNull(session.currentVaultKey())
    }

    @Test
    fun `state flow reports lock transitions`() {
        val session = VaultSession()
        assertEquals(VaultSession.State.Locked, session.stateFlow.value)

        session.unlock(VaultKey.random(cryptoService))
        assertEquals(VaultSession.State.Unlocked, session.stateFlow.value)

        session.lock()
        assertEquals(VaultSession.State.Locked, session.stateFlow.value)
    }

    @Test
    fun `lock closes the session and drops the key`() {
        val session = VaultSession()
        session.unlock(VaultKey.random(cryptoService))

        session.lock()

        assertEquals(VaultSession.State.Locked, session.state)
        assertNull(session.currentVaultKey())
    }
}
