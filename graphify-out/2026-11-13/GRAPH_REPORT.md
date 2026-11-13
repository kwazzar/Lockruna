# Graph Report - Lockruna  (2026-11-13)

## Corpus Check
- 333 files · ~104,714 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 2716 nodes · 4852 edges · 206 communities (150 shown, 56 thin omitted)
- Extraction: 86% EXTRACTED · 14% INFERRED · 0% AMBIGUOUS · INFERRED: 687 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `ca186141`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- AppError
- Рішення
- PassKeeper: модель загроз (STRIDE)
- opencode.json
- Рішення
- PassKeeper: вимоги до MVP
- command
- Рішення
- DDD audit — Lockruna
- AGENTS.md
- Adding an aggregate — Lockruna
- ContentView.swift
- ItemTypeDto
- KdfParamsDtoTest
- LayerDependencyTest
- DatabaseDriverFactory
- ADR-001: KMP-ядро + нативний UI
- ADR-002: Clean Architecture + DDD-lite, MVI у presentation
- VaultItemType
- MainActivity
- gradlew
- ServerConfigTest
- DatabaseDriverFactory
- DatabaseDriverFactory
- DatabaseDriverFactory
- MasterPasswordTest
- graphify.js
- ServerConfig.kt
- verify.sh
- Application.kt
- ItemUi.kt
- SharedCore.kt
- KdfParams
- PasswordNormalizer
- Крипто-ядро клієнта (Crypto Core) — Implementation Plan
- withCBytes
- CryptoVectors.kt
- CryptoLayerTest
- caveman/SKILL.md
- KeyHierarchyTest
- NormalizationVectorsTest
- HkdfLabels.kt
- UserRecord
- ExposedRefreshTokenRepository
- JwtTokenService
- ServerSettings.kt
- Result
- FakeCryptoService
- InMemoryVaultItemRepository
- VaultItemTest
- PostgresTest
- VaultItem
- create
- SqlDelightVaultItemRepository
- Клієнтське ядро vault (Client Vault Core) — Implementation Plan
- Розглянуті підходи
- StoredResponse
- RedisDeviceDenylist
- GcmCiphertext
- FakeCryptoService
- RateLimitDecision
- ServerSettingsTest
- VaultLayerTest
- AuthKeys
- Tables.kt
- PlatformCrypto
- FakeCryptoService
- ServerLayerTest
- .invoke
- PlatformCrypto
- VaultItemContent
- ItemId.kt
- installSafeCallLogging
- Cursor
- SafeCallLoggingTest
- MigrationTest
- .invoke
- DatabaseFactoryTest
- LockVault
- .invoke
- VaultKey
- .unlock
- .respond
- .register
- SyncNowTest
- AuthenticatedUser
- toDomain
- AuthTokens
- PendingChange
- .invoke
- .weakPasswordReturnsWeakMasterPassword
- KdfParams
- Review Focus
- FakeBiometricPolicyStore
- SqlDelightSyncStoreTest
- serverModule
- SyncRoutes.kt
- PushChanges
- SqlDelightSyncStore
- Review Focus
- identityRoutes
- PasswordNormalizer
- .login
- SyncPortsTest
- .spendAndRotate
- RemoteItem
- AuthDtos.kt
- SyncDtos.kt
- .invoke
- installStatusPages
- .logoutClearsSessionAndLocks
- AuthLayerTest
- SyncLayerTest
- .get
- .resolveDevice
- deviceRoutes
- PreloginUser
- DevicePlatform
- createAuthApi
- DeviceDto.kt
- InMemoryRateLimiter
- RateLimitDecision
- ItemDetailStoreTest
- InMemoryVaultItemRepository
- AuthTypes.kt
- PresentationLayerTest
- requireSession
- FakeCryptoService
- .invoke
- ItemUiMappingTest
- AuthDtos.kt
- AuthTokens
- FakeBiometricGate
- ItemId.kt
- .resolveDevice
- PreloginUser
- .invoke
- DevicePlatform
- SessionStoreTest
- .push
- Argon2
- WeakPasswordReason
- HasStoredSession
- AccessTokenClaims
- StoredResponse
- JwtTokenService
- .graph
- SessionStore
- InMemoryVaultItemRepository
- authModule
- Argon2Libsodium
- RegisterStoreTest
- FakeBiometricPolicyStore
- .resolveDevice
- PlatformNormalizer
- PreloginUser
- PlatformNormalizer
- ItemDetailStore
- ByteArray
- LockedAwareInMemoryRepository
- SettingsStore
- AndroidBiometricVault
- Застосунки: акаунт, налаштування та тригери синку (App Account UI) — Implementation Plan
- Клієнт: життєвий цикл акаунта (Client Account Lifecycle) — Implementation Plan
- Сервер: відновлення доступу та видалення акаунта (Server Recovery + Account) — Implementation Plan
- TokenFlowTest
- DevicesStore
- IosBiometricVault
- from
- InMemoryVaultItemRepository
- VaultUseCasesTest
- androidPlatformModule
- iosPlatformModule
- ItemEditTypeTest
- CurrentActivityProvider
- .decode
- RegisterIntent
- LoginStoreTest
- SessionAccessTokenProvider
- PresentationModuleTest
- FixedBiometricPolicyStore
- .invoke
- DevicesStoreTest
- androidMain/kotlin/com/kwazzar/lockruna/data/platform/PlatformFactories.kt
- iosMain/kotlin/com/kwazzar/lockruna/data/platform/PlatformFactories.kt
- SyncDtos.kt
- ItemUiMappingTest
- AuthTypes.kt

## God Nodes (most connected - your core abstractions)
1. `AppError` - 138 edges
2. `Result` - 127 edges
3. `module()` - 73 edges
4. `AuthenticatedUser` - 55 edges
5. `InMemoryRateLimiter` - 52 edges
6. `VaultItem` - 44 edges
7. `AppModel` - 37 edges
8. `InMemoryDeviceDenylist` - 37 edges
9. `ItemId` - 33 edges
10. `AuthTokens` - 31 edges

## Surprising Connections (you probably didn't know these)
- `identityRoutes()` --calls--> `PreloginResponse`  [INFERRED]
  server/src/main/kotlin/com/kwazzar/lockruna/server/identity/api/IdentityRoutes.kt → api-contract/src/commonMain/kotlin/com/kwazzar/lockruna/contract/AuthDtos.kt
- `identityRoutes()` --calls--> `TokenPairDto`  [INFERRED]
  server/src/main/kotlin/com/kwazzar/lockruna/server/identity/api/IdentityRoutes.kt → api-contract/src/commonMain/kotlin/com/kwazzar/lockruna/contract/AuthDtos.kt
- `identityRoutes()` --calls--> `RecoveryStartResponseDto`  [INFERRED]
  server/src/main/kotlin/com/kwazzar/lockruna/server/identity/api/IdentityRoutes.kt → api-contract/src/commonMain/kotlin/com/kwazzar/lockruna/contract/AuthDtos.kt
- `AppRoot()` --calls--> `startScreen()`  [INFERRED]
  androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/AppRoot.kt → shared/presentation/src/commonMain/kotlin/com/kwazzar/lockruna/presentation/app/AppScreen.kt
- `toResponse()` --calls--> `AuthResponseDto`  [INFERRED]
  server/src/main/kotlin/com/kwazzar/lockruna/server/identity/api/IdentityRoutes.kt → api-contract/src/commonMain/kotlin/com/kwazzar/lockruna/contract/AuthDtos.kt

## Import Cycles
- None detected.

## Communities (206 total, 56 thin omitted)

### Community 0 - "AppError"
Cohesion: 0.22
Nodes (5): DeleteAccount, ByteArray, ValidateMasterPassword, MasterPassword, of()

### Community 1 - "Рішення"
Cohesion: 0.09
Nodes (21): 10. Тестування, 1. Структура, 2. Стек, 3. Sync, 4. Токени й пристрої, 5. Автентифікація й анти-enumeration, 6. Rate limiting, 7. Аудит (+13 more)

### Community 2 - "PassKeeper: модель загроз (STRIDE)"
Cohesion: 0.12
Nodes (16): 1. Обсяг, 2. Припущення (поза межами моделі), 3. Активи, 4. Межі довіри, 5. Зловмисники, 6.1 `/auth`: реєстрація, вхід, токени, 6.2 `/auth/recovery/*`, 6.3 `/sync` і `/devices` (+8 more)

### Community 4 - "Рішення"
Cohesion: 0.12
Nodes (15): 10. Тестування, 1. Модулі Gradle, 2. Domain, 3. Application: use cases, 4. Помилки, 5. Локальне сховище без SQLCipher, 6. Папки, 7. Sync-рушій на клієнті (+7 more)

### Community 5 - "PassKeeper: вимоги до MVP"
Cohesion: 0.12
Nodes (15): PassKeeper: вимоги до MVP, US-01 Реєстрація і майстер-пароль, US-02 Вхід, US-03 Recovery key і відновлення доступу, US-04 Створення запису, US-05 Редагування і видалення, US-06 Перегляд і копіювання, US-07 Біометричне розблокування (+7 more)

### Community 6 - "command"
Cohesion: 0.08
Nodes (25): agents, build, plan, permissions, XCODEBUILDMCP_ENABLED_WORKFLOWS, XCODEBUILDMCP_SENTRY_DISABLED, mcp, servers (+17 more)

### Community 7 - "Рішення"
Cohesion: 0.13
Nodes (14): 1. Примітиви, 2. Ієрархія ключів, 3. Шифрування записів і AAD, 4. Відновлення, 5. Argon2id: гібрид за портом `Argon2`, 6. Біометрія, 7. Пам'ять, ADR-003: Криптографічна модель (+6 more)

### Community 8 - "DDD audit — Lockruna"
Cohesion: 0.15
Nodes (12): 10. Done criteria, 1. Layer boundaries, 2. Aggregate / anemic model, 3. Value Objects, 4. Consistency boundaries, 5. Ports, repositories, use cases, 6. Presentation, 7. Errors and logging (+4 more)

### Community 9 - "AGENTS.md"
Cohesion: 0.20
Nodes (8): Commands, Done means, graphify, Invariants live in the aggregate, Naming, Rules that are easy to break, Skills, Workflow

### Community 10 - "Adding an aggregate — Lockruna"
Cohesion: 0.20
Nodes (9): Adding an aggregate — Lockruna, Common mistakes, Step 0 — Plan before code, Step 1 — Domain (`shared/domain`), Step 2 — Port (`shared/domain`), Step 3 — Data (`shared/data`), Step 4 — Application (`shared/application`), Step 5 — Presentation and DI (+1 more)

### Community 11 - "ContentView.swift"
Cohesion: 0.10
Nodes (19): Date, AppModel, Bool, Error, PresentationDevicesStore, PresentationRecoveryStore, PresentationRegisterStore, PresentationSettingsStore (+11 more)

### Community 12 - "ItemTypeDto"
Cohesion: 0.33
Nodes (5): ItemTypeDto, CARD, FOLDER, LOGIN, NOTE

### Community 13 - "KdfParamsDtoTest"
Cohesion: 0.07
Nodes (20): AccountDeleted, BiometricsChanged, BiometricsToggled, DeleteAccount, DeletionPasswordChanged, ErrorDismissed, Flow, Load (+12 more)

### Community 15 - "DatabaseDriverFactory"
Cohesion: 0.18
Nodes (4): SecureAuthStateStore, FakeClock, ByteArray, SecureAuthStateStoreTest

### Community 16 - "ADR-001: KMP-ядро + нативний UI"
Cohesion: 0.33
Nodes (5): ADR-001: KMP-ядро + нативний UI, Альтернативи, Контекст, Наслідки, Рішення

### Community 17 - "ADR-002: Clean Architecture + DDD-lite, MVI у presentation"
Cohesion: 0.33
Nodes (5): ADR-002: Clean Architecture + DDD-lite, MVI у presentation, Альтернативи, Контекст, Наслідки, Рішення

### Community 18 - "VaultItemType"
Cohesion: 0.37
Nodes (4): FakeCryptoService, PassKeeperDatabase, SqlDriver, SqlDelightVaultItemRepositoryTest

### Community 19 - "MainActivity"
Cohesion: 0.14
Nodes (7): Flow, Locked, State, Unlocked, VaultSession, VaultSessionTest, StateFlow

### Community 20 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 21 - "ServerConfigTest"
Cohesion: 0.17
Nodes (18): requireAllowed(), RevokeDevice, RuntimeException, Problem, ProblemException, unauthorized(), receiveTextLimited(), asUuid() (+10 more)

### Community 27 - "ServerConfig.kt"
Cohesion: 0.06
Nodes (32): VaultRoutes(), ItemDetailScreen(), ItemRow(), VaultListScreen(), Closed, Copy, DeleteConfirmed, Edit (+24 more)

### Community 29 - "Application.kt"
Cohesion: 0.19
Nodes (10): InMemoryDeviceDenylist, SyncClient, change(), ByteArray, JsonObject, record(), register(), registerUser() (+2 more)

### Community 30 - "ItemUi.kt"
Cohesion: 0.05
Nodes (31): DisableBiometrics, EnableBiometrics, Logout, UnlockWithPassword, CreateItem, T, FakeSyncApi, PushCall (+23 more)

### Community 44 - "PasswordNormalizer"
Cohesion: 0.17
Nodes (8): Applied, ApplyResult, ChangeRecord, Conflict, SealedItemRecord, VaultItemRepository, ExposedVaultItemRepository, VaultItemRepository

### Community 45 - "Крипто-ядро клієнта (Crypto Core) — Implementation Plan"
Cohesion: 0.15
Nodes (12): Global Constraints, Review Focus, Task 1: Доменні порти, KDF-параметри та HKDF-рядки, Task 2: Реалізація для JVM і Android (BouncyCastle), Task 3: Реалізація для iOS (libsodium через ionspin), Task 4: Спільні KAT-вектори й збіжність платформ, Task 5: Ієрархія ключів, архітектурний тест і правка ADR, Власність файлів (паралельна робота) (+4 more)

### Community 46 - "withCBytes"
Cohesion: 0.30
Nodes (5): ByteArray, T, withCBytes(), ByteArray, PlatformCrypto

### Community 47 - "CryptoVectors.kt"
Cohesion: 0.43
Nodes (5): Argon2Case, CryptoVectors, hexToByteArray(), ByteArray, NormalizationCase

### Community 49 - "caveman/SKILL.md"
Cohesion: 0.33
Nodes (5): Auto-Clarity, Boundaries, Intensity, Persistence, Rules

### Community 57 - "UserRecord"
Cohesion: 0.06
Nodes (12): Algorithm, DecodedJWT, JWTVerifier, ByteArray, RefreshTokenRecord, RefreshTokenRepository, AccessTokenClaims, TokenService (+4 more)

### Community 58 - "ExposedRefreshTokenRepository"
Cohesion: 0.13
Nodes (12): SyncWorker, Conflict, CoroutineWorker, KoinComponent, Batch, ConflictOutcome, Continue, Failed (+4 more)

### Community 59 - "JwtTokenService"
Cohesion: 0.09
Nodes (13): InMemoryVaultItemRepository, VaultItemRepository, ByteArray, Flow, T, VaultItemRepository, SqlDelightVaultItemRepository, VaultItemContent (+5 more)

### Community 60 - "ServerSettings.kt"
Cohesion: 0.12
Nodes (14): HikariConfig, fromEnv(), ByteArray, required(), ServerSettings, toFakeSecret(), toJwtKey(), toPepper() (+6 more)

### Community 61 - "Result"
Cohesion: 0.20
Nodes (3): TestIdGenerator, IdGenerator, FixedIdGenerator

### Community 65 - "PostgresTest"
Cohesion: 0.15
Nodes (7): PostgreSQLContainer, ExposedTransactionRunner, T, TransactionRunner, Database, HikariDataSource, PostgresTest

### Community 66 - "VaultItem"
Cohesion: 0.16
Nodes (6): IdentityApiTest, ByteArray, HttpResponse, JsonObject, Counter, InMemoryRateLimiter

### Community 67 - "create"
Cohesion: 0.17
Nodes (12): ChangeDto, daysFromCivil(), daysInMonth(), fromBase64(), invalidInstant(), isLeapYear(), ByteArray, parseInstantMillis() (+4 more)

### Community 68 - "SqlDelightVaultItemRepository"
Cohesion: 0.14
Nodes (14): Any, DomainAutoLockPolicy, describe(), RecoveryView, RegisterView, SettingsView, Error, PresentationRecoveryStore (+6 more)

### Community 69 - "Клієнтське ядро vault (Client Vault Core) — Implementation Plan"
Cohesion: 0.15
Nodes (12): Global Constraints, Task 1: Value objects і агрегат VaultItem, Task 2: Порти та тестові фейки, Task 3: SQLDelight-реалізація репозиторію, Task 4: Use cases для роботи з записами, Task 5: Сесія, автоблокування та інтеграційний цикл, Власність файлів (паралельна робота), Залежність від інших планів (і правило злиття) (+4 more)

### Community 70 - "Розглянуті підходи"
Cohesion: 0.17
Nodes (11): Global Constraints, Task 1: Каркас сервера: шари, Ktor, Koin, конфіг, помилки, Task 2: Схема БД, Exposed-таблиці та репозиторії, Task 3: Identity API — prelogin, register, login, Task 4: Токени, refresh-ротація та керування пристроями, Task 5: Sync API — курсор, push, ідемпотентність, конфлікти, Власність файлів (паралельна робота), Не в обсязі цього плану (+3 more)

### Community 71 - "StoredResponse"
Cohesion: 0.17
Nodes (3): DeviceRecord, DeviceRepository, ExposedDeviceRepository

### Community 72 - "RedisDeviceDenylist"
Cohesion: 0.22
Nodes (3): DeviceDenylist, AutoCloseable, RedisDeviceDenylist

### Community 73 - "GcmCiphertext"
Cohesion: 0.22
Nodes (5): KeyDerivation, ByteArray, com, CryptoService, ByteArray

### Community 75 - "RateLimitDecision"
Cohesion: 0.07
Nodes (5): FakeSyncStore, LocalRecord, Outbox, SyncStore, PendingChange

### Community 79 - "Tables.kt"
Cohesion: 0.48
Nodes (6): Devices, IdempotencyKeys, RefreshTokens, Users, VaultItems, Table

### Community 83 - ".invoke"
Cohesion: 0.12
Nodes (18): AuthResponseDto, DeleteAccountRequest, LoginRequest, PreloginRequest, PreloginResponse, RecoveryCompleteRequest, RecoveryStartResponseDto, RefreshRequest (+10 more)

### Community 84 - "PlatformCrypto"
Cohesion: 0.20
Nodes (3): ByteArray, PlatformCrypto, GcmCiphertext

### Community 85 - "VaultItemContent"
Cohesion: 0.18
Nodes (5): DeleteAccount, ListDevices, RevokeDevice, DeleteAccountTest, DeviceUseCasesTest

### Community 86 - "ItemId.kt"
Cohesion: 0.13
Nodes (13): BiometricsEnabled, EmailChanged, EnableBiometrics, ErrorDismissed, Flow, PasswordChanged, SubmitBiometrics, SubmitPassword (+5 more)

### Community 87 - "installSafeCallLogging"
Cohesion: 0.17
Nodes (4): SecureBiometricPolicyStore, InMemorySecureStorage, ByteArray, SecureBiometricPolicyStoreTest

### Community 91 - ".invoke"
Cohesion: 0.11
Nodes (11): LockVault, ObserveVaultState, Flow, ObserveItems, coreModule(), HttpClient, platformClock(), platformHttpClient() (+3 more)

### Community 94 - "LockVault"
Cohesion: 0.14
Nodes (11): DeviceNameChanged, EmailChanged, ErrorDismissed, Flow, LoggedIn, LoginEffect, LoginIntent, LoginState (+3 more)

### Community 95 - ".invoke"
Cohesion: 0.12
Nodes (9): DomainKdfParams, createAuthApi(), HttpClient, createAuthApi(), HttpClient, createAuthApi(), HttpClient, AuthApi (+1 more)

### Community 96 - "VaultKey"
Cohesion: 0.09
Nodes (10): KeystoreSecureStorage, ByteArray, SecretKey, ByteArray, SecureStorage, KeychainSecureStorage, ByteArray, CFDataRef (+2 more)

### Community 97 - ".unlock"
Cohesion: 0.18
Nodes (3): UnlockWithBiometrics, EnableBiometricsTest, UnlockWithBiometricsTest

### Community 98 - ".respond"
Cohesion: 0.10
Nodes (11): MockEngine, HttpResponse, T, KtorSyncApi, KtorSyncApiTest, KtorAuthApiHttpTest, KtorRecoveryApiHttpTest, DeviceIdentity (+3 more)

### Community 99 - ".register"
Cohesion: 0.20
Nodes (4): ByteArray, HttpResponse, JsonObject, RecoveryAccountApiTest

### Community 100 - "SyncNowTest"
Cohesion: 0.27
Nodes (6): SyncNowTest, AppliedChange, Conflict, PushResult, SyncPage, com

### Community 101 - "AuthenticatedUser"
Cohesion: 0.15
Nodes (5): FakeAuthStateStore, InMemoryAuthStateStore, AuthStateStore, AuthenticatedUser, FakeAuthStateStore

### Community 102 - "toDomain"
Cohesion: 0.10
Nodes (10): CopySecret, CopySecretTest, FailingClipboard, RecordingClipboard, AndroidClipboard, Job, IosClipboard, FakeClipboard (+2 more)

### Community 103 - "AuthTokens"
Cohesion: 0.40
Nodes (3): HttpClient, platformHttpClient(), platformIdGenerator()

### Community 104 - "PendingChange"
Cohesion: 0.10
Nodes (14): DeviceInfoDto, KdfParamsDto, PushRequestDto, KdfParamsDtoTest, DeviceApiTest, bearer(), IdentityClient, json() (+6 more)

### Community 105 - ".invoke"
Cohesion: 0.19
Nodes (9): of(), Secret, of(), Title, of(), Url, create(), createWithGeneratedId() (+1 more)

### Community 106 - ".weakPasswordReturnsWeakMasterPassword"
Cohesion: 0.19
Nodes (6): ByteArray, Register, RegisterCommand, RegisterOutcome, RegisterTest, canonicalEmail()

### Community 107 - "KdfParams"
Cohesion: 0.09
Nodes (15): Argon2VectorsTest, Argon2Factory, Argon2Libsodium, ByteArray, Argon2BouncyCastle, Argon2Factory, ByteArray, Argon2DispatchTest (+7 more)

### Community 108 - "Review Focus"
Cohesion: 0.15
Nodes (12): Global Constraints, Review Focus, Task 0: Оновлення docs (ADR-003, погроза), Task 1: Domain-типи auth і порти, Task 2: Login, Logout, Unlock (use cases), Task 3: Register, Task 4: Біометрия: EnableBiometrics, UnlockWithBiometrics, Task 5: KtorAuthApi і SessionAccessTokenProvider (+4 more)

### Community 111 - "serverModule"
Cohesion: 0.11
Nodes (11): ListDevices, LoginCommand, LoginUser, LogoutSession, PreloginResult, PreloginUser, RecoveryStartResult, StartRecovery (+3 more)

### Community 112 - "SyncRoutes.kt"
Cohesion: 0.18
Nodes (11): Content, DevicesView, PresentationDevicesStore, PrivacyOverlay, PrivacyShield, PrivacyShieldModifier, Bool, View (+3 more)

### Community 113 - "PushChanges"
Cohesion: 0.18
Nodes (7): FakeSecureKeyStore, from(), ByteArray, random(), SecureKeyStore, VaultKey, FakeSecureKeyStore

### Community 115 - "Review Focus"
Cohesion: 0.18
Nodes (10): Global Constraints, Review Focus, Task 1: Domain-типи sync і порти, Task 2: SQLDelight-запити + SqlDelightSyncStore, Task 3: KtorSyncApi, Task 4: SyncNow — use case sync-алгоритму, Task 5: Koin-модуль + архітектурний тест + повна верифікація, Не в обсязі цієї хвилі (+2 more)

### Community 116 - "identityRoutes"
Cohesion: 0.32
Nodes (11): accountRoutes(), clientIp(), fromBase64(), identityRoutes(), ByteArray, T, receiveJson(), requirePlatform() (+3 more)

### Community 117 - "PasswordNormalizer"
Cohesion: 0.10
Nodes (8): EmailAlreadyRegisteredException, RuntimeException, ByteArray, UserRecord, UserRepository, ExposedUserRepository, ByteArray, UserRepositoryTest

### Community 118 - ".login"
Cohesion: 0.27
Nodes (3): CredentialsCommand, Login, LoginTest

### Community 120 - ".spendAndRotate"
Cohesion: 0.39
Nodes (6): Issued, RefreshSession, Rejected, Reused, Rotation, TokenPair

### Community 121 - "RemoteItem"
Cohesion: 0.18
Nodes (3): contentEqualsOrNull(), ByteArray, RemoteItem

### Community 122 - "AuthDtos.kt"
Cohesion: 0.20
Nodes (3): BlockingVaultItemRepository, ItemEditStoreTest, VaultItemRepository

### Community 123 - "SyncDtos.kt"
Cohesion: 0.32
Nodes (5): AppliedChange, Conflict, ByteArray, PushChanges, PushOutcome

### Community 124 - ".invoke"
Cohesion: 0.21
Nodes (6): RegisterCommand, AuthSession, CompleteRecovery, CompleteRecoveryCommand, RegisterCommand, RegisterUser

### Community 125 - "installStatusPages"
Cohesion: 0.29
Nodes (3): installStatusPages(), respondProblem(), StatusPagesTest

### Community 126 - ".logoutClearsSessionAndLocks"
Cohesion: 0.29
Nodes (6): App, BackgroundTasks, LockrunaApp, Network, Scene, Shared

### Community 129 - ".get"
Cohesion: 0.20
Nodes (6): CompleteRecovery, CompleteRecovery, ByteArray, RecoverCommand, RotatedKeys, CompleteRecoveryTest

### Community 130 - ".resolveDevice"
Cohesion: 0.17
Nodes (4): FixedAutoLockSettingsStore, AutoLockPolicy, AutoLockSettingsStore, InMemoryAutoLockSettingsStore

### Community 132 - "PreloginUser"
Cohesion: 0.18
Nodes (10): Global Constraints, Review Focus, Task 0: Платформа пристрою в `CredentialsCommand`, Task 1: LoginStore, Task 2: UnlockStore, Task 3: SessionStore, Task 4: AppGate — стартовий екран, Task 5: Koin + Konsist + повна верифікація (+2 more)

### Community 133 - "DevicePlatform"
Cohesion: 0.15
Nodes (7): AuthStateStoreTest, BiometricPolicyStoreTest, FakeArgon2Test, FakeArgon2, FakePasswordNormalizer, ByteArray, toHexString()

### Community 134 - "createAuthApi"
Cohesion: 0.17
Nodes (11): Global Constraints, Review Focus, Review волни (після Task 6), Task 1: ItemUi v2 + мапінг, Task 2: Порт Clipboard + Reveal/Copy use cases, Task 3: VaultListStore, Task 4: ItemDetailStore, Task 5: ItemEditStore (+3 more)

### Community 135 - "DeviceDto.kt"
Cohesion: 0.50
Nodes (3): DeviceDto, deviceRoutes(), toDto()

### Community 136 - "InMemoryRateLimiter"
Cohesion: 0.12
Nodes (12): CardCvvChanged, CardExpiryChanged, CardNumberChanged, FolderSelected, ItemEditIntent, NoteBodyChanged, Save, SecretChanged (+4 more)

### Community 137 - "RateLimitDecision"
Cohesion: 0.13
Nodes (12): DeviceNameChanged, EmailChanged, ErrorDismissed, Flow, NewPasswordChanged, Recovered, RecoveryEffect, RecoveryIntent (+4 more)

### Community 139 - "InMemoryVaultItemRepository"
Cohesion: 0.18
Nodes (10): Global Constraints, Review Focus, Task 1: Android composition root + app shell, Task 2: Android екрани розблокування, входу і vault, Task 3: Android lifecycle, приватність, accessibility, Task 4: iOS Kotlin-фасад + Swift app shell, Task 5: iOS екрани, Task 6: iOS lifecycle, приватність, accessibility + фінальна верифікація (+2 more)

### Community 140 - "AuthTypes.kt"
Cohesion: 0.24
Nodes (13): DomainVaultItemType, Int, ContentView, ItemDetailView, ItemEditView, LoginView, Error, String (+5 more)

### Community 142 - "requireSession"
Cohesion: 0.12
Nodes (3): HasStoredSession, HasStoredSessionTest, LockrunaIos

### Community 144 - ".invoke"
Cohesion: 0.16
Nodes (8): FragmentActivity, MainActivity, AppLifecycleEffects(), AppNetworkSync(), AppRoot(), Theme(), androidx, Bundle

### Community 145 - "ItemUiMappingTest"
Cohesion: 0.21
Nodes (8): AppScreens(), autoLockLabel(), DevicesScreen(), SettingsScreen(), LoginScreen(), RecoveryScreen(), RegisterScreen(), UnlockScreen()

### Community 147 - "AuthDtos.kt"
Cohesion: 0.18
Nodes (3): FakeClock, Clock, FakeClock

### Community 150 - "ItemId.kt"
Cohesion: 0.16
Nodes (10): AppScreen, DEVICES, LOGIN, RECOVERY, REGISTER, SETTINGS, UNLOCK, VAULT (+2 more)

### Community 151 - ".resolveDevice"
Cohesion: 0.22
Nodes (6): Allowed, Blocked, RateLimitDecision, RateLimiter, AutoCloseable, RedisRateLimiter

### Community 152 - "PreloginUser"
Cohesion: 0.22
Nodes (8): Global Constraints, Review Focus, Task 1: Спільне сховище сесії, Task 2: Android Keystore + біометрія + буфер, Task 3: iOS Keychain + біометрія + буфер, Task 4: Фабрики платформи та coreModule, Нативний platform-шар клієнта (Native Platform Layer) — Implementation Plan, Порядок злиття

### Community 153 - ".invoke"
Cohesion: 0.08
Nodes (23): R, flatMap(), E, map(), FakeRecoveryApi, ByteArray, HttpClient, KtorRecoveryApi (+15 more)

### Community 154 - "DevicePlatform"
Cohesion: 0.11
Nodes (11): LockrunaApplication, Application, io, createAuthApi(), authModule(), HttpClient, Module, HttpClient (+3 more)

### Community 157 - "Argon2"
Cohesion: 0.23
Nodes (3): Cipher, AndroidBiometricVault, SecretKey

### Community 158 - "WeakPasswordReason"
Cohesion: 0.17
Nodes (6): FakeClock, ApplyAutoLockPolicy, AutoLockPolicyTest, FakeAutoLockSettingsStore, FakeCryptoService, ByteArray

### Community 159 - "HasStoredSession"
Cohesion: 0.27
Nodes (3): AuthModuleTest, HttpClient, KoinApplication

### Community 160 - "AccessTokenClaims"
Cohesion: 0.18
Nodes (4): BiometricUnlockState, ObserveBiometricUnlock, CompletableDeferred, RecoveryStoreTest

### Community 161 - "StoredResponse"
Cohesion: 0.27
Nodes (5): IdempotencyRepository, ByteArray, StoredResponse, ExposedIdempotencyRepository, ByteArray

### Community 163 - ".graph"
Cohesion: 0.26
Nodes (4): LAContext, IosBiometricVault, CFDataRef, CFStringRef

### Community 164 - "SessionStore"
Cohesion: 0.22
Nodes (8): Flow, Lock, Locked, Resumed, SessionEffect, SessionIntent, SessionState, SessionStore

### Community 165 - "InMemoryVaultItemRepository"
Cohesion: 0.13
Nodes (10): DeleteItem, GetItem, RevealSecret, hasValidDashes(), hasValidHexChars(), hasValidVariant(), hasValidVersion(), isValidUuidV4() (+2 more)

### Community 166 - "authModule"
Cohesion: 0.47
Nodes (4): isCommonPattern(), isSequential(), strength(), PasswordStrength

### Community 167 - "Argon2Libsodium"
Cohesion: 0.14
Nodes (5): Argon2Factory, PlatformNormalizer, PlatformNormalizer, PlatformNormalizer, PasswordNormalizer

### Community 168 - "RegisterStoreTest"
Cohesion: 0.17
Nodes (3): FakeBiometricPolicyStore, FakePasswordNormalizer, FixedIdGenerator

### Community 169 - "FakeBiometricPolicyStore"
Cohesion: 0.31
Nodes (5): ItemEditEffect, ItemEditState, ItemEditStore, Saved, ValidationError

### Community 175 - "ByteArray"
Cohesion: 0.22
Nodes (3): Bip39Test, ByteArray, Sha256Crypto

### Community 176 - "LockedAwareInMemoryRepository"
Cohesion: 0.20
Nodes (4): FakeBiometricGate, BiometricGate, FakeBiometricGate, CompletableDeferred

### Community 179 - "Застосунки: акаунт, налаштування та тригери синку (App Account UI) — Implementation Plan"
Cohesion: 0.15
Nodes (12): File Structure, Global Constraints, Review Focus, Spec coverage, Task 1: iOS фасад — нові стори, Task 2: Android — реєстрація та recovery, Task 3: Android — пристрої та налаштування, Task 4: Android — CARD/NOTE і папки (+4 more)

### Community 180 - "Клієнт: життєвий цикл акаунта (Client Account Lifecycle) — Implementation Plan"
Cohesion: 0.15
Nodes (12): File Structure, Global Constraints, Review Focus, Spec coverage, Task 1: `CryptoService.sha256()` + KAT, Task 2: BIP39 кодек, Task 3: Порти `RecoveryApi`, `DeviceApi`, `AutoLockSettingsStore`, Task 4: Use cases (+4 more)

### Community 181 - "Сервер: відновлення доступу та видалення акаунта (Server Recovery + Account) — Implementation Plan"
Cohesion: 0.15
Nodes (12): File Structure, Global Constraints, Review Focus, Spec coverage, Task 1: Contract DTOs, Task 2: Репозиторії — delete / revoke all, Task 3: StartRecovery, Task 4: CompleteRecovery (+4 more)

### Community 182 - "TokenFlowTest"
Cohesion: 0.18
Nodes (6): DataSource, main(), module(), ByteArray, TokenFlowTest, Timestamp

### Community 183 - "DevicesStore"
Cohesion: 0.22
Nodes (9): DeviceRevoked, DevicesEffect, DevicesIntent, DevicesState, DevicesStore, ErrorDismissed, Flow, Load (+1 more)

### Community 184 - "IosBiometricVault"
Cohesion: 0.39
Nodes (3): Fields, T, UpdateItem

### Community 185 - "from"
Cohesion: 0.36
Nodes (5): AuthSessionCodec, from(), KdfParamsRecord, ByteArray, SessionRecord

### Community 186 - "InMemoryVaultItemRepository"
Cohesion: 0.22
Nodes (3): InMemoryVaultItemRepository, Flow, VaultItemRepository

### Community 187 - "VaultUseCasesTest"
Cohesion: 0.07
Nodes (18): ItemEditScreen(), secretLabel(), titleLabel(), typeLabel(), usernameLabel(), CreateItemCommand, RevealSecretTest, Flow (+10 more)

### Community 188 - "androidPlatformModule"
Cohesion: 0.24
Nodes (6): DatabaseDriverFactory, Context, SqlDriver, androidPlatformModule(), Context, Module

### Community 189 - "iosPlatformModule"
Cohesion: 0.20
Nodes (6): DatabaseDriverFactory, SqlDriver, iosPlatformModule(), Module, Module, platformModule()

### Community 191 - "CurrentActivityProvider"
Cohesion: 0.28
Nodes (4): CurrentActivityProvider, FragmentActivity, ActivityProvider, FragmentActivity

### Community 192 - ".decode"
Cohesion: 0.42
Nodes (3): IntArray, Bip39, ByteArray

### Community 193 - "RegisterIntent"
Cohesion: 0.09
Nodes (15): ConfirmationChanged, ConfirmRecoveryWords, DeviceNameChanged, EmailChanged, ErrorDismissed, Flow, PasswordChanged, RecoveryKeyReady (+7 more)

### Community 196 - "SessionAccessTokenProvider"
Cohesion: 0.23
Nodes (4): SessionAccessTokenProvider, InMemoryAuthStateStoreTest, SessionAccessTokenProviderTest, AuthTokens

### Community 209 - "androidMain/kotlin/com/kwazzar/lockruna/data/platform/PlatformFactories.kt"
Cohesion: 0.40
Nodes (3): HttpClient, platformHttpClient(), platformIdGenerator()

### Community 210 - "iosMain/kotlin/com/kwazzar/lockruna/data/platform/PlatformFactories.kt"
Cohesion: 0.40
Nodes (3): HttpClient, platformHttpClient(), platformIdGenerator()

### Community 212 - "SyncDtos.kt"
Cohesion: 0.48
Nodes (6): AppliedChangeDto, ConflictDto, EncryptedItemDto, PushResultDto, SyncPageDto, toDto()

### Community 213 - "ItemUiMappingTest"
Cohesion: 0.29
Nodes (3): requireSession(), AuthenticateSession, bearerToken()

### Community 217 - "AuthTypes.kt"
Cohesion: 0.60
Nodes (4): ByteArray, pack(), SealedBlob, unpack()

## Knowledge Gaps
- **346 isolated node(s):** `LOGIN`, `CARD`, `NOTE`, `FOLDER`, `Network` (+341 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **56 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `module()` connect `TokenFlowTest` to `deviceRoutes`, `DeviceDto.kt`, `ServerConfigTest`, `.resolveDevice`, `DevicePlatform`, `Application.kt`, `HasStoredSession`, `JwtTokenService`, `androidPlatformModule`, `iosPlatformModule`, `ServerSettings.kt`, `VaultItem`, `RedisDeviceDenylist`, `.invoke`, `.register`, `PendingChange`, `serverModule`, `identityRoutes`, `installStatusPages`?**
  _High betweenness centrality (0.215) - this node is a cross-community bridge._
- **Why does `serverModule()` connect `serverModule` to `PostgresTest`, `StoredResponse`, `StoredResponse`, `RedisDeviceDenylist`, `PasswordNormalizer`, `ServerSettings.kt`, `ItemDetailStore`, `ItemUiMappingTest`, `TokenFlowTest`, `ServerConfigTest`, `.spendAndRotate`, `.resolveDevice`, `PasswordNormalizer`, `SyncDtos.kt`, `.invoke`, `FakeCryptoService`, `UserRecord`?**
  _High betweenness centrality (0.133) - this node is a cross-community bridge._
- **Why does `Result` connect `.invoke` to `AppError`, `.get`, `KdfParamsDtoTest`, `ItemUi.kt`, `InMemoryVaultItemRepository`, `IosBiometricVault`, `ExposedRefreshTokenRepository`, `VaultUseCasesTest`, `JwtTokenService`, `InMemoryVaultItemRepository`, `.decode`, `SessionAccessTokenProvider`, `.invoke`, `VaultItemContent`, `LockVault`, `.invoke`, `.unlock`, `.respond`, `toDomain`, `.invoke`, `.weakPasswordReturnsWeakMasterPassword`, `KdfParams`, `.login`, `AuthDtos.kt`?**
  _High betweenness centrality (0.115) - this node is a cross-community bridge._
- **Are the 67 inferred relationships involving `module()` (e.g. with `deviceRoutes()` and `accountRoutes()`) actually correct?**
  _`module()` has 67 INFERRED edges - model-reasoned connections that need verification._
- **Are the 15 inferred relationships involving `AuthenticatedUser` (e.g. with `.wireUp()` and `.aSavedSessionMeansAStoredSession()`) actually correct?**
  _`AuthenticatedUser` has 15 INFERRED edges - model-reasoned connections that need verification._
- **Are the 47 inferred relationships involving `InMemoryRateLimiter` (e.g. with `.`a device of another user is not revoked`()` and `.`deleting a device blocks its access token immediately`()`) actually correct?**
  _`InMemoryRateLimiter` has 47 INFERRED edges - model-reasoned connections that need verification._
- **What connects `LOGIN`, `CARD`, `NOTE` to the rest of the system?**
  _346 weakly-connected nodes found - possible documentation gaps or missing edges._