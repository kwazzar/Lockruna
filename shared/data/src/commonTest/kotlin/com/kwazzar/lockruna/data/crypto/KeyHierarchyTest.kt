package com.kwazzar.lockruna.data.crypto

import com.kwazzar.lockruna.domain.crypto.HkdfLabels
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class KeyHierarchyTest {
    private val crypto = PlatformCrypto()

    @Test
    fun `vault key survives a wrap and unwrap round trip`() {
        val masterKey = crypto.hkdfSha256(ByteArray(32) { 1 }, null, "master", 32)
        val kek = crypto.hkdfSha256(masterKey, null, HkdfLabels.KEK, 32)
        val authKey = crypto.hkdfSha256(masterKey, null, HkdfLabels.AUTH, 32)
        val vaultKey = crypto.randomBytes(32)
        val userId = "user-1"

        val wrapped = crypto.encryptGcm(kek, vaultKey, userId.encodeToByteArray())
        val unwrapped = crypto.decryptGcm(kek, wrapped, userId.encodeToByteArray())

        assertContentEquals(vaultKey, unwrapped)
        assertFalse(authKey.contentEquals(kek))
    }

    @Test
    fun `unwrap with a wrong user id fails`() {
        val kek = crypto.hkdfSha256(ByteArray(32) { 1 }, null, HkdfLabels.KEK, 32)
        val vaultKey = crypto.randomBytes(32)
        val wrapped = crypto.encryptGcm(kek, vaultKey, "user-1".encodeToByteArray())

        assertFailsWith<IllegalArgumentException> {
            crypto.decryptGcm(kek, wrapped, "user-2".encodeToByteArray())
        }
    }
}
