package com.kwazzar.lockruna.application.auth

import com.kwazzar.lockruna.domain.auth.AuthApi
import com.kwazzar.lockruna.domain.auth.AuthStateStore
import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.session.VaultSession

class Logout(
    private val authApi: AuthApi,
    private val authStateStore: AuthStateStore,
    private val session: VaultSession,
) {
    suspend operator fun invoke(): Result<Unit, AppError> {
        val logoutResult = authApi.logout()
        if (logoutResult is Result.Failure) return logoutResult

        authStateStore.clear()
        session.lock()

        return Result.Success(Unit)
    }
}
