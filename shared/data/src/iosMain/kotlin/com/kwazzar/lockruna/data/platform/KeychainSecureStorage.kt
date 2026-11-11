@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
@file:Suppress("ktlint:standard:no-wildcard-imports", "WildcardImport")

package com.kwazzar.lockruna.data.platform

import com.kwazzar.lockruna.data.platform.SecureStorage
import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Security.*

class KeychainSecureStorage : SecureStorage {

    override suspend fun read(key: String): ByteArray? {
        val service = key.toCFString()
        val dict = CFDictionaryCreateMutable(null, 0L, null, null)!!
        CFDictionarySetValue(dict, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(dict, kSecAttrService, service)
        CFDictionarySetValue(dict, kSecAttrSynchronizable, kCFBooleanFalse)
        CFDictionarySetValue(dict, kSecReturnData, kCFBooleanTrue)
        CFDictionarySetValue(dict, kSecMatchLimit, kSecMatchLimitOne)

        val bytes = memScoped {
            val result = alloc<CPointerVarOf<CFTypeRef>>()
            val status = SecItemCopyMatching(dict, result.ptr)
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
        CFRelease(dict)
        CFRelease(service)
        return bytes
    }

    override suspend fun write(key: String, value: ByteArray) {
        val service = key.toCFString()
        val data = value.toCFData()

        val attributes = CFDictionaryCreateMutable(null, 0L, null, null)!!
        CFDictionarySetValue(attributes, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(attributes, kSecAttrService, service)
        CFDictionarySetValue(attributes, kSecValueData, data)
        CFDictionarySetValue(attributes, kSecAttrAccessible, kSecAttrAccessibleWhenUnlockedThisDeviceOnly)
        CFDictionarySetValue(attributes, kSecAttrSynchronizable, kCFBooleanFalse)

        val addStatus = SecItemAdd(attributes, null)
        if (addStatus != errSecSuccess && addStatus != errSecDuplicateItem) {
            error("SecItemAdd failed: $addStatus")
        }
        if (addStatus == errSecDuplicateItem) {
            val query = CFDictionaryCreateMutable(null, 0L, null, null)!!
            CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionarySetValue(query, kSecAttrService, service)
            CFDictionarySetValue(query, kSecAttrSynchronizable, kCFBooleanFalse)

            val update = CFDictionaryCreateMutable(null, 0L, null, null)!!
            CFDictionarySetValue(update, kSecValueData, data)

            SecItemUpdate(query, update)

            CFRelease(update)
            CFRelease(query)
        }

        CFRelease(attributes)
        CFRelease(data)
        CFRelease(service)
    }

    override suspend fun delete(key: String) {
        val service = key.toCFString()
        val query = CFDictionaryCreateMutable(null, 0L, null, null)!!
        CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(query, kSecAttrService, service)
        CFDictionarySetValue(query, kSecAttrSynchronizable, kCFBooleanFalse)

        SecItemDelete(query)

        CFRelease(query)
        CFRelease(service)
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
}
