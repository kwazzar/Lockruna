package com.kwazzar.lockruna.server.platform.di

import com.kwazzar.lockruna.server.audit.domain.AuditEventRepository
import com.kwazzar.lockruna.server.audit.infrastructure.MongoAuditEventRepository
import com.kwazzar.lockruna.server.identity.application.AuthenticateSession
import com.kwazzar.lockruna.server.identity.application.CompleteRecovery
import com.kwazzar.lockruna.server.identity.application.DeleteAccount
import com.kwazzar.lockruna.server.identity.application.ListDevices
import com.kwazzar.lockruna.server.identity.application.LoginUser
import com.kwazzar.lockruna.server.identity.application.LogoutSession
import com.kwazzar.lockruna.server.identity.application.PreloginUser
import com.kwazzar.lockruna.server.identity.application.RefreshSession
import com.kwazzar.lockruna.server.identity.application.RegisterUser
import com.kwazzar.lockruna.server.identity.application.RevokeDevice
import com.kwazzar.lockruna.server.identity.application.StartRecovery
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
import com.kwazzar.lockruna.server.sync.application.PullChanges
import com.kwazzar.lockruna.server.sync.application.PushChanges
import com.kwazzar.lockruna.server.sync.domain.IdempotencyRepository
import com.kwazzar.lockruna.server.sync.domain.VaultItemRepository
import com.kwazzar.lockruna.server.sync.infrastructure.ExposedIdempotencyRepository
import com.kwazzar.lockruna.server.sync.infrastructure.ExposedVaultItemRepository
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.core.module.Module
import org.koin.dsl.module
import com.mongodb.kotlin.client.coroutine.MongoClient
import javax.sql.DataSource

fun serverModule(
    settings: ServerSettings,
    dataSource: DataSource,
    rateLimiter: RateLimiter,
    denylist: DeviceDenylist,
    mongoClient: MongoClient,
): Module = module {
    single { settings }
    single { dataSource }
    single { mongoClient }
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
    single { PreloginUser(get(), get(), settings.authFakeSecret) }
    single { StartRecovery(get(), get(), settings.authFakeSecret) }
    single { RegisterUser(get(), get(), get(), get(), get(), settings) }
    single { CompleteRecovery(get(), get(), get(), get(), get(), get(), settings) }
    single { DeleteAccount(get(), get(), get(), settings) }
    single { LoginUser(get(), get(), get(), get(), settings) }
    single { AuthenticateSession(get(), get()) }
    single { RefreshSession(get(), get(), get()) }
    single { RevokeDevice(get(), get(), get()) }
    single { ListDevices(get()) }
    single { LogoutSession(get()) }
    single<VaultItemRepository> { ExposedVaultItemRepository(get()) }
    single<IdempotencyRepository> { ExposedIdempotencyRepository(get()) }
    single<AuditEventRepository> { MongoAuditEventRepository(get(), get()) }
    single { PullChanges(get()) }
    single { PushChanges(get(), get(), get(), get()) }
}
