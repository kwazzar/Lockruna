@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
@file:Suppress("ktlint:standard:no-wildcard-imports", "WildcardImport")

package com.kwazzar.lockruna.data.platform

import com.kwazzar.lockruna.domain.session.BiometricGate
import com.kwazzar.lockruna.domain.session.SecureKeyStore
import com.kwazzar.lockruna.domain.session.VaultKey
import kotlinx.cinterop.*
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreFoundation.*
import platform.Foundation.*
import platform.LocalAuthentication.*
import platform.Security.*
import kotlin.coroutines.resume

class IosBiometricVault(private val storage: SecureStorage) :
    BiometricGate,
    SecureKeyStore {

    private var authContext: LAContext? = null

    override suspend fun isAvailable(): Boolean {
        val ctx = LAContext()
        return ctx.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, null)
    }

    override suspend fun authenticate(reason: String): Boolean {
        val ctx = LAContext()
        ctx.localizedReason = reason
        return suspendCancellableCoroutine { continuation ->
            ctx.evaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, reason) { success, _ ->
                if (success) {
                    authContext = ctx
                    continuation.resume(true)
                } else {
                    continuation.resume(false)
                }
            }
        }
    }

    override suspend fun saveVaultKey(key: VaultKey) {
        val accessControl = SecAccessControlCreateWithFlags(
            null,
            kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
            kSecAccessControlBiometryCurrentSet,
            null,
        )!!
        val service = VAULT_SERVICE.toCFString()
        val account = VAULT_ACCOUNT.toCFString()
        val data = key.bytes.toCFData()

        val dict = CFDictionaryCreateMutable(null, 0L, null, null)!!
        CFDictionarySetValue(dict, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(dict, kSecAttrService, service)
        CFDictionarySetValue(dict, kSecAttrAccount, account)
        CFDictionarySetValue(dict, kSecValueData, data)
        CFDictionarySetValue(dict, kSecAttrAccessControl, accessControl)
        CFDictionarySetValue(dict, kSecAttrSynchronizable, kCFBooleanFalse)

        // Replace any existing item first.
        val deleteQuery = CFDictionaryCreateMutable(null, 0L, null, null)!!
        CFDictionarySetValue(deleteQuery, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(deleteQuery, kSecAttrService, service)
        CFDictionarySetValue(deleteQuery, kSecAttrAccount, account)
        SecItemDelete(deleteQuery)

        SecItemAdd(dict, null)

        CFRelease(deleteQuery)
        CFRelease(dict)
        CFRelease(data)
        CFRelease(account)
        CFRelease(service)
        CFRelease(accessControl)
    }

    override suspend fun loadVaultKey(): VaultKey? {
        val service = VAULT_SERVICE.toCFString()
        val account = VAULT_ACCOUNT.toCFString()

        val query = CFDictionaryCreateMutable(null, 0L, null, null)!!
        CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(query, kSecAttrService, service)
        CFDictionarySetValue(query, kSecAttrAccount, account)
        CFDictionarySetValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionarySetValue(query, kSecMatchLimit, kSecMatchLimitOne)
        CFDictionarySetValue(query, kSecAttrSynchronizable, kCFBooleanFalse)

        val ctx = authContext
        if (ctx != null) {
            val bridged = CFBridgingRetain(ctx)
            CFDictionarySetValue(query, kSecUseAuthenticationContext, bridged)
            CFRelease(bridged)
        } else {
            val reason = "Unlock vault".toCFString()
            CFDictionarySetValue(query, kSecUseOperationPrompt, reason)
            CFRelease(reason)
        }

        val resultBytes = memScoped {
            val result = alloc<CPointerVarOf<CFTypeRef>>()
            val status = SecItemCopyMatching(query, result.ptr)
            if (status != errSecSuccess) {
                null
            } else {
                val data = result.value as? CFDataRef
                if (data == null) {
                    null
                } else {
                    val length = CFDataGetLength(data).toInt()
                    if (length == 0) {
                        ByteArray(0)
                    } else {
                        val ptr = CFDataGetBytePtr(data)!!
                        ByteArray(length) { i -> ptr[i].toByte() }
                    }
                }
            }
        }

        CFRelease(query)
        CFRelease(account)
        CFRelease(service)

        return resultBytes?.let { VaultKey.from(it) }
    }

    override suspend fun clear() {
        val service = VAULT_SERVICE.toCFString()
        val account = VAULT_ACCOUNT.toCFString()

        val query = CFDictionaryCreateMutable(null, 0L, null, null)!!
        CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(query, kSecAttrService, service)
        CFDictionarySetValue(query, kSecAttrAccount, account)

        SecItemDelete(query)

        CFRelease(query)
        CFRelease(account)
        CFRelease(service)
        authContext = null
    }

    private fun String.toCFString(): CFStringRef = this.encodeToByteArray().usePinned { ptr ->
        CFStringCreateWithCString(null, ptr.addressOf(0), kCFStringEncodingUTF8)!!
    }

    private fun ByteArray.toCFData(): CFDataRef = if (isEmpty()) {
        CFDataCreate(null, null, 0)!!
    } else {
        this.toUByteArray().usePinned { ptr ->
            CFDataCreate(null, ptr.addressOf(0), size.toLong())!!
        }
    }

    companion object {
        private const val VAULT_SERVICE = "com.kwazzar.lockruna.vault"
        private const val VAULT_ACCOUNT = "vault_key"
    }
}
