package com.kwazzar.lockruna.data.di

import android.content.Context
import com.kwazzar.lockruna.data.auth.SecureAuthStateStore
import com.kwazzar.lockruna.data.db.DatabaseDriverFactory
import com.kwazzar.lockruna.data.platform.ActivityProvider
import com.kwazzar.lockruna.data.platform.AndroidBiometricVault
import com.kwazzar.lockruna.data.platform.AndroidClipboard
import com.kwazzar.lockruna.data.platform.KeystoreSecureStorage
import com.kwazzar.lockruna.data.platform.SecureStorage
import com.kwazzar.lockruna.data.session.SecureAutoLockSettingsStore
import com.kwazzar.lockruna.data.session.SecureBiometricPolicyStore
import com.kwazzar.lockruna.domain.auth.AuthStateStore
import com.kwazzar.lockruna.domain.auth.BiometricPolicyStore
import com.kwazzar.lockruna.domain.auth.DevicePlatform
import com.kwazzar.lockruna.domain.session.AutoLockSettingsStore
import com.kwazzar.lockruna.domain.session.BiometricGate
import com.kwazzar.lockruna.domain.session.SecureKeyStore
import com.kwazzar.lockruna.domain.vault.Clipboard
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * The Android bindings: the real database driver, the keystore storage, the
 * biometric vault and the sensitive clipboard.
 */
fun androidPlatformModule(context: Context, activityProvider: ActivityProvider): Module = module {
    single {
        DatabaseDriverFactory().apply { initialize(context) }
    }
    single<SecureStorage> { KeystoreSecureStorage(context) }
    single<AuthStateStore> { SecureAuthStateStore(get()) }
    single<AutoLockSettingsStore> { SecureAutoLockSettingsStore(get()) }
    single<BiometricPolicyStore> { SecureBiometricPolicyStore(get()) }
    single<Clipboard> { AndroidClipboard(context) }
    single {
        AndroidBiometricVault(context, activityProvider, get())
    }
    single<BiometricGate> { get<AndroidBiometricVault>() }
    single<SecureKeyStore> { get<AndroidBiometricVault>() }
    single { DevicePlatform.ANDROID }
}
