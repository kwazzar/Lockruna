package com.kwazzar.lockruna.data.di

import org.koin.core.module.Module

/** Platform-specific Koin module (database driver, storage, biometrics, clipboard). */
expect fun platformModule(): Module
