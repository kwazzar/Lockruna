# Graph Report - client-sync  (2026-11-10)

## Corpus Check
- 219 files · ~59,222 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1535 nodes · 2594 edges · 136 communities (93 shown, 43 thin omitted)
- Extraction: 84% EXTRACTED · 16% INFERRED · 0% AMBIGUOUS · INFERRED: 415 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `b82e5209`
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

## God Nodes (most connected - your core abstractions)
1. `AppError` - 79 edges
2. `Result` - 68 edges
3. `module()` - 57 edges
4. `InMemoryRateLimiter` - 43 edges
5. `VaultItem` - 30 edges
6. `InMemoryDeviceDenylist` - 28 edges
7. `AuthenticatedUser` - 27 edges
8. `IdentityApiTest` - 26 edges
9. `serverModule()` - 24 edges
10. `GcmCiphertext` - 24 edges

## Surprising Connections (you probably didn't know these)
- `identityRoutes()` --calls--> `TokenPairDto`  [INFERRED]
  server/src/main/kotlin/com/kwazzar/lockruna/server/identity/api/IdentityRoutes.kt → api-contract/src/commonMain/kotlin/com/kwazzar/lockruna/contract/AuthDtos.kt
- `toResponse()` --calls--> `AuthResponseDto`  [INFERRED]
  server/src/main/kotlin/com/kwazzar/lockruna/server/identity/api/IdentityRoutes.kt → api-contract/src/commonMain/kotlin/com/kwazzar/lockruna/contract/AuthDtos.kt
- `toDto()` --calls--> `EncryptedItemDto`  [INFERRED]
  server/src/main/kotlin/com/kwazzar/lockruna/server/sync/api/SyncRoutes.kt → api-contract/src/commonMain/kotlin/com/kwazzar/lockruna/contract/SyncDtos.kt
- `toDto()` --calls--> `ChangeDto`  [INFERRED]
  shared/data/src/commonMain/kotlin/com/kwazzar/lockruna/data/sync/SyncMapper.kt → api-contract/src/commonMain/kotlin/com/kwazzar/lockruna/contract/SyncDtos.kt
- `toDto()` --calls--> `AppliedChangeDto`  [INFERRED]
  server/src/main/kotlin/com/kwazzar/lockruna/server/sync/api/SyncRoutes.kt → api-contract/src/commonMain/kotlin/com/kwazzar/lockruna/contract/SyncDtos.kt

## Import Cycles
- None detected.

## Communities (136 total, 43 thin omitted)

### Community 0 - "AppError"
Cohesion: 0.10
Nodes (18): DeleteItem, AppError, BiometricsUnavailable, EmailAlreadyRegistered, IdempotencyConflict, InvalidCredentials, InvalidItem, ItemCorrupted (+10 more)

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
Cohesion: 0.14
Nodes (13): 1. Примітиви, 2. Ієрархія ключів, 3. Шифрування записів і AAD, 4. Відновлення, 5. Argon2id: гібрид за портом `Argon2`, 6. Біометрія, 7. Пам'ять, ADR-003: Криптографічна модель (+5 more)

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
Cohesion: 0.22
Nodes (7): App, ContentView, LockrunaApp, Scene, Shared, SwiftUI, View

### Community 12 - "ItemTypeDto"
Cohesion: 0.33
Nodes (5): ItemTypeDto, CARD, FOLDER, LOGIN, NOTE

### Community 15 - "DatabaseDriverFactory"
Cohesion: 0.40
Nodes (3): Context, DatabaseDriverFactory, SqlDriver

### Community 16 - "ADR-001: KMP-ядро + нативний UI"
Cohesion: 0.33
Nodes (5): ADR-001: KMP-ядро + нативний UI, Альтернативи, Контекст, Наслідки, Рішення

### Community 17 - "ADR-002: Clean Architecture + DDD-lite, MVI у presentation"
Cohesion: 0.33
Nodes (5): ADR-002: Clean Architecture + DDD-lite, MVI у presentation, Альтернативи, Контекст, Наслідки, Рішення

### Community 18 - "VaultItemType"
Cohesion: 0.08
Nodes (17): FakeCryptoService, PassKeeperDatabase, VaultUseCasesTest, FakeClock, SqlDriver, SqlDelightVaultItemRepositoryTest, SyncModuleTest, Locked (+9 more)

### Community 19 - "MainActivity"
Cohesion: 0.40
Nodes (3): MainActivity, Bundle, ComponentActivity

### Community 20 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 21 - "ServerConfigTest"
Cohesion: 0.20
Nodes (8): requireAllowed(), RuntimeException, Problem, ProblemException, unauthorized(), receiveTextLimited(), PullChanges, PulledPage

### Community 27 - "ServerConfig.kt"
Cohesion: 0.20
Nodes (3): DeviceRecord, DeviceRepository, ExposedDeviceRepository

### Community 29 - "Application.kt"
Cohesion: 0.05
Nodes (39): DeviceInfoDto, ChangeDto, DataSource, main(), module(), Allowed, Blocked, RateLimitDecision (+31 more)

### Community 43 - "KdfParams"
Cohesion: 0.17
Nodes (8): Argon2Factory, Argon2Libsodium, ByteArray, Argon2BouncyCastle, Argon2Factory, ByteArray, Argon2, ByteArray

### Community 44 - "PasswordNormalizer"
Cohesion: 0.17
Nodes (8): Applied, ApplyResult, ChangeRecord, Conflict, SealedItemRecord, VaultItemRepository, ExposedVaultItemRepository, VaultItemRepository

### Community 45 - "Крипто-ядро клієнта (Crypto Core) — Implementation Plan"
Cohesion: 0.15
Nodes (12): Global Constraints, Review Focus, Task 1: Доменні порти, KDF-параметри та HKDF-рядки, Task 2: Реалізація для JVM і Android (BouncyCastle), Task 3: Реалізація для iOS (libsodium через ionspin), Task 4: Спільні KAT-вектори й збіжність платформ, Task 5: Ієрархія ключів, архітектурний тест і правка ADR, Власність файлів (паралельна робота) (+4 more)

### Community 46 - "withCBytes"
Cohesion: 0.31
Nodes (5): ByteArray, T, withCBytes(), ByteArray, PlatformCrypto

### Community 47 - "CryptoVectors.kt"
Cohesion: 0.43
Nodes (5): Argon2Case, CryptoVectors, hexToByteArray(), ByteArray, NormalizationCase

### Community 49 - "caveman/SKILL.md"
Cohesion: 0.33
Nodes (5): Auto-Clarity, Boundaries, Intensity, Persistence, Rules

### Community 57 - "UserRecord"
Cohesion: 0.09
Nodes (9): EmailAlreadyRegisteredException, RuntimeException, ByteArray, UserRecord, UserRepository, ExposedUserRepository, ByteArray, RefreshTokenRepositoryTest (+1 more)

### Community 58 - "ExposedRefreshTokenRepository"
Cohesion: 0.07
Nodes (16): Conflict, io, Batch, ConflictOutcome, SyncNow, FakeAccessTokenProvider, createAuthApi(), SessionAccessTokenProvider (+8 more)

### Community 59 - "JwtTokenService"
Cohesion: 0.06
Nodes (14): Algorithm, DecodedJWT, JWTVerifier, requireSession(), AuthenticateSession, ByteArray, RefreshTokenRecord, RefreshTokenRepository (+6 more)

### Community 60 - "ServerSettings.kt"
Cohesion: 0.12
Nodes (14): HikariConfig, fromEnv(), ByteArray, required(), ServerSettings, toFakeSecret(), toJwtKey(), toPepper() (+6 more)

### Community 61 - "Result"
Cohesion: 0.13
Nodes (7): GetItem, Fields, UpdateItem, TestIdGenerator, IdGenerator, ItemId, of()

### Community 62 - "FakeCryptoService"
Cohesion: 0.15
Nodes (6): FakeClock, ApplyAutoLockPolicy, AutoLockPolicyTest, FakeCryptoService, ByteArray, AutoLockPolicy

### Community 63 - "InMemoryVaultItemRepository"
Cohesion: 0.16
Nodes (4): SupportFakesTest, FakeIdGenerator, InMemoryVaultItemRepository, VaultItemRepository

### Community 65 - "PostgresTest"
Cohesion: 0.15
Nodes (7): PostgreSQLContainer, ExposedTransactionRunner, T, TransactionRunner, Database, HikariDataSource, PostgresTest

### Community 66 - "VaultItem"
Cohesion: 0.18
Nodes (6): CreateItem, CreateItemCommand, Flow, VaultItemRepository, LockedAwareInMemoryRepository, VaultCycleTest

### Community 67 - "create"
Cohesion: 0.19
Nodes (9): of(), Secret, of(), Title, of(), Url, create(), createWithGeneratedId() (+1 more)

### Community 68 - "SqlDelightVaultItemRepository"
Cohesion: 0.24
Nodes (6): ByteArray, Flow, T, VaultItemRepository, SqlDelightVaultItemRepository, Vault_item

### Community 69 - "Клієнтське ядро vault (Client Vault Core) — Implementation Plan"
Cohesion: 0.15
Nodes (12): Global Constraints, Task 1: Value objects і агрегат VaultItem, Task 2: Порти та тестові фейки, Task 3: SQLDelight-реалізація репозиторію, Task 4: Use cases для роботи з записами, Task 5: Сесія, автоблокування та інтеграційний цикл, Власність файлів (паралельна робота), Залежність від інших планів (і правило злиття) (+4 more)

### Community 70 - "Розглянуті підходи"
Cohesion: 0.17
Nodes (11): Global Constraints, Task 1: Каркас сервера: шари, Ktor, Koin, конфіг, помилки, Task 2: Схема БД, Exposed-таблиці та репозиторії, Task 3: Identity API — prelogin, register, login, Task 4: Токени, refresh-ротація та керування пристроями, Task 5: Sync API — курсор, push, ідемпотентність, конфлікти, Власність файлів (паралельна робота), Не в обсязі цього плану (+3 more)

### Community 71 - "StoredResponse"
Cohesion: 0.27
Nodes (5): IdempotencyRepository, ByteArray, StoredResponse, ExposedIdempotencyRepository, ByteArray

### Community 72 - "RedisDeviceDenylist"
Cohesion: 0.22
Nodes (3): DeviceDenylist, AutoCloseable, RedisDeviceDenylist

### Community 73 - "GcmCiphertext"
Cohesion: 0.23
Nodes (5): com, KeyDerivation, ByteArray, CryptoService, ByteArray

### Community 75 - "RateLimitDecision"
Cohesion: 0.08
Nodes (4): FakeSyncStore, LocalRecord, Outbox, SyncStore

### Community 79 - "Tables.kt"
Cohesion: 0.48
Nodes (6): Devices, IdempotencyKeys, RefreshTokens, Users, VaultItems, Table

### Community 81 - "FakeCryptoService"
Cohesion: 0.23
Nodes (3): GcmCiphertext, FakeCryptoService, ByteArray

### Community 83 - ".invoke"
Cohesion: 0.40
Nodes (3): ValidateMasterPassword, MasterPassword, of()

### Community 84 - "PlatformCrypto"
Cohesion: 0.31
Nodes (3): Argon2Factory, ByteArray, PlatformCrypto

### Community 86 - "ItemId.kt"
Cohesion: 0.60
Nodes (5): hasValidDashes(), hasValidHexChars(), hasValidVariant(), hasValidVersion(), isValidUuidV4()

### Community 95 - ".invoke"
Cohesion: 0.11
Nodes (14): DomainKdfParams, E, createAuthApi(), HttpClient, ByteArray, KtorAuthApi, AuthApi, ByteArray (+6 more)

### Community 96 - "VaultKey"
Cohesion: 0.09
Nodes (10): FakeBiometricGate, FakeClock, FakeSecureKeyStore, BiometricGate, Clock, from(), ByteArray, random() (+2 more)

### Community 97 - ".unlock"
Cohesion: 0.12
Nodes (6): EnableBiometrics, UnlockWithBiometrics, UnlockWithPassword, EnableBiometricsTest, UnlockWithBiometricsTest, UnlockWithPasswordTest

### Community 98 - ".respond"
Cohesion: 0.24
Nodes (5): MockEngine, HttpResponse, T, KtorSyncApi, KtorSyncApiTest

### Community 99 - ".register"
Cohesion: 0.17
Nodes (15): AuthResponse, DeviceDto, hex(), hexToByteArray(), KdfParamsDto, ByteArray, HttpClient, T (+7 more)

### Community 100 - "SyncNowTest"
Cohesion: 0.27
Nodes (5): SyncNowTest, AppliedChange, Conflict, PushResult, SyncPage

### Community 101 - "AuthenticatedUser"
Cohesion: 0.18
Nodes (4): FakeAuthStateStore, InMemoryAuthStateStore, AuthStateStore, AuthenticatedUser

### Community 102 - "toDomain"
Cohesion: 0.19
Nodes (10): daysFromCivil(), fromBase64(), invalidInstant(), ByteArray, parseInstantMillis(), toBase64(), toDomain(), toDto() (+2 more)

### Community 103 - "AuthTokens"
Cohesion: 0.16
Nodes (7): AuthStateStoreTest, BiometricPolicyStoreTest, AuthTokens, ByteArray, pack(), SealedBlob, unpack()

### Community 104 - "PendingChange"
Cohesion: 0.15
Nodes (4): FakeSyncApi, PushCall, SyncApi, PendingChange

### Community 105 - ".invoke"
Cohesion: 0.22
Nodes (5): ByteArray, Register, RegisterCommand, RegisterOutcome, RegisterTest

### Community 106 - ".weakPasswordReturnsWeakMasterPassword"
Cohesion: 0.22
Nodes (6): FakeArgon2Test, FakeArgon2, FakeAuthApi, FakePasswordNormalizer, ByteArray, toHexString()

### Community 107 - "KdfParams"
Cohesion: 0.18
Nodes (5): Argon2VectorsTest, Argon2DispatchTest, isAcceptable(), KdfParams, KdfParamsTest

### Community 108 - "Review Focus"
Cohesion: 0.15
Nodes (12): Global Constraints, Review Focus, Task 0: Оновлення docs (ADR-003, погроза), Task 1: Domain-типи auth і порти, Task 2: Login, Logout, Unlock (use cases), Task 3: Register, Task 4: Біометрия: EnableBiometrics, UnlockWithBiometrics, Task 5: KtorAuthApi і SessionAccessTokenProvider (+4 more)

### Community 111 - "serverModule"
Cohesion: 0.17
Nodes (6): ListDevices, LogoutSession, RevokeDevice, DataSource, Module, serverModule()

### Community 112 - "SyncRoutes.kt"
Cohesion: 0.38
Nodes (11): asUuid(), bodyHash(), fromBase64(), idempotencyKey(), invalidRequest(), ByteArray, limit(), parse() (+3 more)

### Community 113 - "PushChanges"
Cohesion: 0.32
Nodes (5): AppliedChange, Conflict, ByteArray, PushChanges, PushOutcome

### Community 115 - "Review Focus"
Cohesion: 0.18
Nodes (10): Global Constraints, Review Focus, Task 1: Domain-типи sync і порти, Task 2: SQLDelight-запити + SqlDelightSyncStore, Task 3: KtorSyncApi, Task 4: SyncNow — use case sync-алгоритму, Task 5: Koin-модуль + архітектурний тест + повна верифікація, Не в обсязі цієї хвилі (+2 more)

### Community 116 - "identityRoutes"
Cohesion: 0.33
Nodes (10): clientIp(), fromBase64(), identityRoutes(), ByteArray, T, receiveJson(), requirePlatform(), toBase64() (+2 more)

### Community 117 - "PasswordNormalizer"
Cohesion: 0.18
Nodes (4): PlatformNormalizer, PlatformNormalizer, PlatformNormalizer, PasswordNormalizer

### Community 118 - ".login"
Cohesion: 0.44
Nodes (3): CredentialsCommand, Login, LoginTest

### Community 120 - ".spendAndRotate"
Cohesion: 0.39
Nodes (6): Issued, RefreshSession, Rejected, Reused, Rotation, TokenPair

### Community 121 - "RemoteItem"
Cohesion: 0.22
Nodes (3): contentEqualsOrNull(), ByteArray, RemoteItem

### Community 122 - "AuthDtos.kt"
Cohesion: 0.25
Nodes (7): AuthResponseDto, LoginRequest, PreloginRequest, PreloginResponse, RefreshRequest, RegisterRequest, TokenPairDto

### Community 123 - "SyncDtos.kt"
Cohesion: 0.39
Nodes (7): AppliedChangeDto, ConflictDto, EncryptedItemDto, PushRequestDto, PushResultDto, SyncPageDto, toDto()

### Community 124 - ".invoke"
Cohesion: 0.29
Nodes (4): RegisterCommand, AuthSession, RegisterCommand, RegisterUser

### Community 125 - "installStatusPages"
Cohesion: 0.29
Nodes (3): installStatusPages(), respondProblem(), StatusPagesTest

### Community 133 - "DevicePlatform"
Cohesion: 0.50
Nodes (3): DevicePlatform, ANDROID, IOS

## Knowledge Gaps
- **214 isolated node(s):** `PreloginRequest`, `PreloginResponse`, `RegisterRequest`, `LoginRequest`, `RefreshRequest` (+209 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **43 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `module()` connect `Application.kt` to `deviceRoutes`, `RedisDeviceDenylist`, `serverModule`, `SyncRoutes.kt`, `VaultItemType`, `identityRoutes`, `installSafeCallLogging`, `ExposedRefreshTokenRepository`, `ServerSettings.kt`, `installStatusPages`?**
  _High betweenness centrality (0.209) - this node is a cross-community bridge._
- **Why does `serverModule()` connect `serverModule` to `PostgresTest`, `.resolveDevice`, `PreloginUser`, `StoredResponse`, `RedisDeviceDenylist`, `ServerConfig.kt`, `PasswordNormalizer`, `ServerSettings.kt`, `PushChanges`, `ServerConfigTest`, `.spendAndRotate`, `UserRecord`, `JwtTokenService`, `.invoke`, `Application.kt`?**
  _High betweenness centrality (0.088) - this node is a cross-community bridge._
- **Why does `Result` connect `.invoke` to `AppError`, `.get`, `deviceRoutes`, `ExposedRefreshTokenRepository`, `Result`, `InMemoryVaultItemRepository`, `VaultItem`, `create`, `SqlDelightVaultItemRepository`, `.invoke`, `VaultItemContent`, `.unlock`, `.respond`, `.register`, `AuthTokens`, `PendingChange`, `.invoke`, `.weakPasswordReturnsWeakMasterPassword`, `.login`, `.logoutClearsSessionAndLocks`?**
  _High betweenness centrality (0.088) - this node is a cross-community bridge._
- **Are the 51 inferred relationships involving `module()` (e.g. with `deviceRoutes()` and `identityRoutes()`) actually correct?**
  _`module()` has 51 INFERRED edges - model-reasoned connections that need verification._
- **Are the 38 inferred relationships involving `InMemoryRateLimiter` (e.g. with `.`a device of another user is not revoked`()` and `.`deleting a device blocks its access token immediately`()`) actually correct?**
  _`InMemoryRateLimiter` has 38 INFERRED edges - model-reasoned connections that need verification._
- **Are the 2 inferred relationships involving `VaultItem` (e.g. with `.`InMemoryVaultItemRepository observeAll emits list`()` and `.`InMemoryVaultItemRepository save and get`()`) actually correct?**
  _`VaultItem` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `PreloginRequest`, `PreloginResponse`, `RegisterRequest` to the rest of the system?**
  _214 weakly-connected nodes found - possible documentation gaps or missing edges._