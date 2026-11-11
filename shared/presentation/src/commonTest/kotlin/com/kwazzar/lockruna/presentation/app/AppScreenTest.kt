package com.kwazzar.lockruna.presentation.app

import kotlin.test.Test
import kotlin.test.assertEquals

class AppScreenTest {

    @Test
    fun `a locked vault with a stored session asks for the unlock screen`() {
        assertEquals(AppScreen.UNLOCK, startScreen(isLocked = true, hasSession = true))
    }

    @Test
    fun `a locked vault without a stored session asks for the login screen`() {
        assertEquals(AppScreen.LOGIN, startScreen(isLocked = true, hasSession = false))
    }

    @Test
    fun `an unlocked vault opens the vault`() {
        assertEquals(AppScreen.VAULT, startScreen(isLocked = false, hasSession = false))
    }
}
