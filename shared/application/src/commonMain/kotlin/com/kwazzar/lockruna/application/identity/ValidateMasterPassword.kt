package com.kwazzar.lockruna.application.identity

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.identity.MasterPassword

/** Validates the master password against the domain policy. */
class ValidateMasterPassword {
    operator fun invoke(password: String): Result<MasterPassword, AppError> =
        MasterPassword.of(password)
}
