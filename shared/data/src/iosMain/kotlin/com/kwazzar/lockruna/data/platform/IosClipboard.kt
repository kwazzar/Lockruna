@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.kwazzar.lockruna.data.platform

import com.kwazzar.lockruna.domain.core.Result
import com.kwazzar.lockruna.domain.error.AppError
import com.kwazzar.lockruna.domain.vault.Clipboard
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.UIKit.UIPasteboard
import platform.UIKit.UIPasteboardOptionExpirationDate
import platform.UIKit.UIPasteboardOptionLocalOnly
import platform.UIKit.UIPasteboardTypeAutomatic

class IosClipboard : Clipboard {

    override suspend fun copySecret(value: String): Result<Unit, AppError> {
        val pasteboard = UIPasteboard.generalPasteboard
        val expiration = NSDate(
            timeIntervalSinceReferenceDate =
            NSDate().timeIntervalSinceReferenceDate + EXPIRATION_SECONDS,
        )
        pasteboard.setItems(
            listOf(mapOf(UIPasteboardTypeAutomatic to value)),
            mapOf(
                UIPasteboardOptionExpirationDate to expiration,
                UIPasteboardOptionLocalOnly to true,
            ),
        )
        return Result.Success(Unit)
    }

    private companion object {
        const val EXPIRATION_SECONDS = 30.0
    }
}
