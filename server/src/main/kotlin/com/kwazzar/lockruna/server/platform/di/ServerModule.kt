package com.kwazzar.lockruna.server.platform.di

import com.kwazzar.lockruna.server.identity.application.AuthenticateSession
import com.kwazzar.lockruna.server.identity.application.ListDevices
import com.kwazzar.lockruna.server.identity.application.LoginUser
import com.kwazzar.lockruna.server.identity.application.LogoutSession
import com.kwazzar.lockruna.server.identity.application.PreloginUser
import com.kwazzar.lockruna.server.identity.application.RefreshSession
import com.kwazzar.lockruna.server.identity.application.RegisterUser
import com.kwazzar.lockruna.server.identity.application.RevokeDevice
import com.kwazzar.lockruna.server.identity.domain.DeviceDenylist
import com.kwazzar.lockruna.server.identity.domain.DeviceRepository
import com.kwazzar.lockruna.server.identity.domain.RateLimiter
import com.kwazzar.lockruna.server.identity.domain.RefreshTokenRepository
import com.kwazzar.lockruna.server.identity.domain.TokenService
import com.kwazzar.lockruna.server.identity.domain.UserRepository
import com.kwazzar.lockruna.server.identity.infrastructure.ExposedDeviceRepository
import com.kwazzar.lockruna.server.identity.infrastructure.ExposedRefreshTokenRepository
import com.kwazzar.lockruna.server.identity.infrastructure.ExposedUserRepository
import com.kwazzar.lockruna.server.identity.infrastructure.JwtTokenService
import com.kwazzar.lockruna.server.platform.config.ServerSettings
import com.kwazzar.lockruna.server.platform.db.DatabaseFactory
import com.kwazzar.lockruna.server.platform.db.ExposedTransactionRunner
import com.kwazzar.lockruna.server.platform.db.TransactionRunner
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.core.module.Module
import org.koin.dsl.module
import javax.sql.DataSource

fun serverModule(
    settings: ServerSettings,
    dataSource: DataSource,
    rateLimiter: RateLimiter,
    denylist: DeviceDenylist,
): Module = module {
    single { settings }
    single { dataSource }
    single<TransactionRunner> {
        DatabaseFactory.migrate(dataSource)
        ExposedTransactionRunner(Database.connect(dataSource))
    }
    single<UserRepository> { ExposedUserRepository(get()) }
    single<DeviceRepository> { ExposedDeviceRepository(get()) }
    single<RefreshTokenRepository> { ExposedRefreshTokenRepository(get()) }
    single<RateLimiter> { rateLimiter }
    single<DeviceDenylist> { denylist }
    single<TokenService> { JwtTokenService(get(), settings) }
    single { PreloginUser(get(), settings.authFakeSecret) }
    single { RegisterUser(get(), get(), get(), get(), settings) }
    single { LoginUser(get(), get(), get(), get(), settings) }
    single { AuthenticateSession(get(), get()) }
    single { RefreshSession(get(), get()) }
    single { RevokeDevice(get(), get(), get()) }
    single { ListDevices(get()) }
    single { LogoutSession(get()) }
}
