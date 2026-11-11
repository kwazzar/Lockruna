package com.kwazzar.lockruna.presentation.app

enum class AppScreen { UNLOCK, LOGIN, VAULT }

/** Locked with a stored session → unlock; locked without one → sign in. */
fun startScreen(isLocked: Boolean, hasSession: Boolean): AppScreen = when {
    isLocked && hasSession -> AppScreen.UNLOCK
    isLocked -> AppScreen.LOGIN
    else -> AppScreen.VAULT
}
