package com.kwazzar.lockruna.data.di

import com.kwazzar.lockruna.data.auth.SecureAuthStateStore
import com.kwazzar.lockruna.data.db.DatabaseDriverFactory
import com.kwazzar.lockruna.data.platform.IosBiometricVault
import com.kwazzar.lockruna.data.platform.IosClipboard
import com.kwazzar.lockruna.data.platform.KeychainSecureStorage
import com.kwazzar.lockruna.data.platform.SecureStorage
import com.kwazzar.lockruna.data.session.SecureAutoLockSettingsStore
import com.kwazzar.lockruna.domain.auth.AuthStateStore
import com.kwazzar.lockruna.domain.auth.DevicePlatform
import com.kwazzar.lockruna.domain.session.AutoLockSettingsStore
import com.kwazzar.lockruna.domain.session.BiometricGate
import com.kwazzar.lockruna.domain.session.SecureKeyStore
import com.kwazzar.lockruna.domain.vault.Clipboard
import org.koin.core.module.Module
import org.koin.dsl.module

fun iosPlatformModule(): Module = module {
    single { DatabaseDriverFactory() }
    single<SecureStorage> { KeychainSecureStorage() }
    single<AuthStateStore> { SecureAuthStateStore(get()) }
    single<AutoLockSettingsStore> { SecureAutoLockSettingsStore(get()) }
    single<Clipboard> { IosClipboard() }
    single { IosBiometricVault(get()) }
    single<BiometricGate> { get<IosBiometricVault>() }
    single<SecureKeyStore> { get<IosBiometricVault>() }
    single { DevicePlatform.IOS }
}
