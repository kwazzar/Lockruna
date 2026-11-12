# Graph Report - Lockruna  (2026-11-12)

## Corpus Check
- 323 files · ~99,248 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 2584 nodes · 4579 edges · 197 communities (138 shown, 59 thin omitted)
- Extraction: 86% EXTRACTED · 14% INFERRED · 0% AMBIGUOUS · INFERRED: 654 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `9a9058ad`
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
- FakeBiometricGate
- ItemId.kt
- .resolveDevice
- PreloginUser
- .invoke
- DevicePlatform
- SessionStoreTest
- WeakPasswordReason
- HasStoredSession
- AccessTokenClaims
- StoredResponse
- JwtTokenService
- .graph
- InMemoryVaultItemRepository
- authModule
- Argon2Libsodium
- FakeBiometricPolicyStore
- .resolveDevice
- PlatformNormalizer
- PreloginUser
- PlatformNormalizer
- ByteArray
- SettingsStore
- AndroidBiometricVault
- Застосунки: акаунт, налаштування та тригери синку (App Account UI) — Implementation Plan
- Клієнт: життєвий цикл акаунта (Client Account Lifecycle) — Implementation Plan
- Сервер: відновлення доступу та видалення акаунта (Server Recovery + Account) — Implementation Plan
- TokenFlowTest
- DevicesStore
- IosBiometricVault
- BiometricGate
- VaultUseCasesTest
- androidPlatformModule
- iosPlatformModule
- ItemEditTypeTest
- CurrentActivityProvider
- .decode
- LoginStoreTest
- KeystoreSecureStorage
- PresentationModuleTest
- .createUser
- .invoke
- BiometricSettings.kt
- SecureStorage
- DevicesStoreTest
- androidMain/kotlin/com/kwazzar/lockruna/data/platform/PlatformFactories.kt
- iosMain/kotlin/com/kwazzar/lockruna/data/platform/PlatformFactories.kt
- ItemUiMappingTest

## God Nodes (most connected - your core abstractions)
1. `AppError` - 130 edges
2. `Result` - 117 edges
3. `module()` - 71 edges
4. `InMemoryRateLimiter` - 52 edges
5. `AuthenticatedUser` - 51 edges
6. `VaultItem` - 42 edges
7. `InMemoryDeviceDenylist` - 37 edges
8. `AppModel` - 34 edges
9. `ItemId` - 33 edges
10. `GcmCiphertext` - 28 edges

## Surprising Connections (you probably didn't know these)
- `identityRoutes()` --calls--> `PreloginResponse`  [INFERRED]
  server/src/main/kotlin/com/kwazzar/lockruna/server/identity/api/IdentityRoutes.kt → api-contract/src/commonMain/kotlin/com/kwazzar/lockruna/contract/AuthDtos.kt
- `identityRoutes()` --calls--> `TokenPairDto`  [INFERRED]
  server/src/main/kotlin/com/kwazzar/lockruna/server/identity/api/IdentityRoutes.kt → api-contract/src/commonMain/kotlin/com/kwazzar/lockruna/contract/AuthDtos.kt
- `identityRoutes()` --calls--> `RecoveryStartResponseDto`  [INFERRED]
  server/src/main/kotlin/com/kwazzar/lockruna/server/identity/api/IdentityRoutes.kt → api-contract/src/commonMain/kotlin/com/kwazzar/lockruna/contract/AuthDtos.kt
- `toDto()` --calls--> `DeviceInfoDto`  [INFERRED]
  shared/data/src/commonMain/kotlin/com/kwazzar/lockruna/data/auth/KtorAuthApi.kt → api-contract/src/commonMain/kotlin/com/kwazzar/lockruna/contract/AuthDtos.kt
- `toResponse()` --calls--> `AuthResponseDto`  [INFERRED]
  server/src/main/kotlin/com/kwazzar/lockruna/server/identity/api/IdentityRoutes.kt → api-contract/src/commonMain/kotlin/com/kwazzar/lockruna/contract/AuthDtos.kt

## Import Cycles
- None detected.

## Communities (197 total, 59 thin omitted)

### Community 0 - "AppError"
Cohesion: 0.05
Nodes (33): E, CompleteRecovery, DeleteAccount, ByteArray, ValidateMasterPassword, PreLoginData, canonicalEmail(), DeviceApi (+25 more)

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
Cohesion: 0.06
Nodes (48): App, Bool, Date, Int, DevicesView, RecoveryView, RegisterView, SettingsView (+40 more)

### Community 12 - "ItemTypeDto"
Cohesion: 0.33
Nodes (5): ItemTypeDto, CARD, FOLDER, LOGIN, NOTE

### Community 16 - "ADR-001: KMP-ядро + нативний UI"
Cohesion: 0.33
Nodes (5): ADR-001: KMP-ядро + нативний UI, Альтернативи, Контекст, Наслідки, Рішення

### Community 17 - "ADR-002: Clean Architecture + DDD-lite, MVI у presentation"
Cohesion: 0.33
Nodes (5): ADR-002: Clean Architecture + DDD-lite, MVI у presentation, Альтернативи, Контекст, Наслідки, Рішення

### Community 18 - "VaultItemType"
Cohesion: 0.41
Nodes (4): FakeCryptoService, PassKeeperDatabase, SqlDriver, SqlDelightVaultItemRepositoryTest

### Community 19 - "MainActivity"
Cohesion: 0.15
Nodes (6): Locked, State, Unlocked, VaultSession, VaultSessionTest, StateFlow

### Community 20 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 21 - "ServerConfigTest"
Cohesion: 0.15
Nodes (11): LoginCommand, LoginUser, requireAllowed(), RevokeDevice, RuntimeException, Problem, ProblemException, unauthorized() (+3 more)

### Community 27 - "ServerConfig.kt"
Cohesion: 0.11
Nodes (17): VaultRoutes(), ItemDetailScreen(), ItemRow(), VaultListScreen(), CopySecret, ItemClicked, Job, Lock (+9 more)

### Community 29 - "Application.kt"
Cohesion: 0.16
Nodes (12): ChangeDto, InMemoryDeviceDenylist, HttpResponse, SyncClient, change(), ByteArray, JsonObject, record() (+4 more)

### Community 43 - "KdfParams"
Cohesion: 0.25
Nodes (4): BlockingVaultItemRepository, FixedIdGenerator, Flow, VaultItemRepository

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
Cohesion: 0.05
Nodes (14): EmailAlreadyRegisteredException, RuntimeException, ByteArray, RefreshTokenRecord, RefreshTokenRepository, ByteArray, UserRecord, UserRepository (+6 more)

### Community 58 - "ExposedRefreshTokenRepository"
Cohesion: 0.20
Nodes (9): Conflict, Batch, ConflictOutcome, Continue, Failed, Finished, com, PushStep (+1 more)

### Community 59 - "JwtTokenService"
Cohesion: 0.11
Nodes (9): Algorithm, DecodedJWT, JWTVerifier, requireSession(), AuthenticateSession, AccessTokenClaims, TokenService, JwtTokenService (+1 more)

### Community 60 - "ServerSettings.kt"
Cohesion: 0.12
Nodes (14): HikariConfig, fromEnv(), ByteArray, required(), ServerSettings, toFakeSecret(), toJwtKey(), toPepper() (+6 more)

### Community 61 - "Result"
Cohesion: 0.18
Nodes (3): TestIdGenerator, IdGenerator, FixedItemIdGenerator

### Community 63 - "InMemoryVaultItemRepository"
Cohesion: 0.20
Nodes (4): SupportFakesTest, FakeIdGenerator, InMemoryVaultItemRepository, VaultItemRepository

### Community 65 - "PostgresTest"
Cohesion: 0.15
Nodes (7): PostgreSQLContainer, ExposedTransactionRunner, T, TransactionRunner, Database, HikariDataSource, PostgresTest

### Community 66 - "VaultItem"
Cohesion: 0.16
Nodes (6): IdentityApiTest, ByteArray, HttpResponse, JsonObject, Counter, InMemoryRateLimiter

### Community 67 - "create"
Cohesion: 0.18
Nodes (12): daysFromCivil(), daysInMonth(), fromBase64(), invalidInstant(), isLeapYear(), ByteArray, parseInstantMillis(), toBase64() (+4 more)

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
Cohesion: 0.19
Nodes (11): handleException(), ByteArray, HttpClient, T, KtorAuthApi, toDomain(), toDto(), toFailure() (+3 more)

### Community 84 - "PlatformCrypto"
Cohesion: 0.24
Nodes (3): ByteArray, PlatformCrypto, GcmCiphertext

### Community 86 - "ItemId.kt"
Cohesion: 0.13
Nodes (13): BiometricsEnabled, EmailChanged, EnableBiometrics, ErrorDismissed, Flow, PasswordChanged, SubmitBiometrics, SubmitPassword (+5 more)

### Community 87 - "installSafeCallLogging"
Cohesion: 0.53
Nodes (3): FakeRecoveryApiForStore, ByteArray, com

### Community 91 - ".invoke"
Cohesion: 0.12
Nodes (10): LockVault, DeleteItem, GetItem, Flow, ObserveItems, coreModule(), HttpClient, platformClock() (+2 more)

### Community 94 - "LockVault"
Cohesion: 0.13
Nodes (12): LoginScreen(), DeviceNameChanged, EmailChanged, ErrorDismissed, Flow, LoggedIn, LoginEffect, LoginIntent (+4 more)

### Community 95 - ".invoke"
Cohesion: 0.09
Nodes (14): DomainKdfParams, FakeAuthApi, createAuthApi(), HttpClient, createAuthApi(), HttpClient, createAuthApi(), HttpClient (+6 more)

### Community 96 - "VaultKey"
Cohesion: 0.18
Nodes (5): KeychainSecureStorage, ByteArray, CFDataRef, CFStringRef, KeychainSecureStorageTest

### Community 97 - ".unlock"
Cohesion: 0.17
Nodes (4): EnableBiometrics, UnlockWithBiometrics, EnableBiometricsTest, UnlockWithBiometricsTest

### Community 98 - ".respond"
Cohesion: 0.12
Nodes (7): MockEngine, HttpResponse, T, KtorSyncApi, KtorSyncApiTest, KtorAuthApiHttpTest, KtorRecoveryApiHttpTest

### Community 99 - ".register"
Cohesion: 0.16
Nodes (7): DeviceInfoDto, PreloginRequest, KdfParamsDto, ByteArray, HttpResponse, JsonObject, RecoveryAccountApiTest

### Community 100 - "SyncNowTest"
Cohesion: 0.27
Nodes (6): SyncNowTest, AppliedChange, Conflict, PushResult, SyncPage, com

### Community 101 - "AuthenticatedUser"
Cohesion: 0.07
Nodes (14): Logout, UnlockWithPassword, HasStoredSessionTest, LogoutTest, UnlockWithPasswordTest, InMemoryAuthStateStore, AuthStateStore, AuthenticatedUser (+6 more)

### Community 102 - "toDomain"
Cohesion: 0.17
Nodes (7): CopySecret, CopySecretTest, FailingClipboard, RecordingClipboard, Clipboard, FakeClock, RecordingClipboard

### Community 103 - "AuthTokens"
Cohesion: 0.40
Nodes (3): HttpClient, platformHttpClient(), platformIdGenerator()

### Community 104 - "PendingChange"
Cohesion: 0.11
Nodes (15): AuthResponseDto, DeleteAccountRequest, LoginRequest, PreloginResponse, RecoveryCompleteRequest, RecoveryStartResponseDto, RefreshRequest, RegisterRequest (+7 more)

### Community 105 - ".invoke"
Cohesion: 0.20
Nodes (8): of(), Secret, of(), Title, of(), Url, create(), createWithGeneratedId()

### Community 106 - ".weakPasswordReturnsWeakMasterPassword"
Cohesion: 0.18
Nodes (5): ByteArray, Register, RegisterCommand, RegisterOutcome, RegisterTest

### Community 107 - "KdfParams"
Cohesion: 0.12
Nodes (10): AuthSessionCodec, from(), KdfParamsRecord, ByteArray, SessionRecord, Argon2VectorsTest, Argon2DispatchTest, isAcceptable() (+2 more)

### Community 108 - "Review Focus"
Cohesion: 0.15
Nodes (12): Global Constraints, Review Focus, Task 0: Оновлення docs (ADR-003, погроза), Task 1: Domain-типи auth і порти, Task 2: Login, Logout, Unlock (use cases), Task 3: Register, Task 4: Біометрия: EnableBiometrics, UnlockWithBiometrics, Task 5: KtorAuthApi і SessionAccessTokenProvider (+4 more)

### Community 111 - "serverModule"
Cohesion: 0.12
Nodes (9): ListDevices, LogoutSession, PreloginResult, PreloginUser, RecoveryStartResult, StartRecovery, DataSource, Module (+1 more)

### Community 112 - "SyncRoutes.kt"
Cohesion: 0.21
Nodes (18): AppliedChangeDto, ConflictDto, EncryptedItemDto, PushRequestDto, PushResultDto, SyncPageDto, asUuid(), bodyHash() (+10 more)

### Community 113 - "PushChanges"
Cohesion: 0.18
Nodes (7): FakeSecureKeyStore, from(), ByteArray, random(), SecureKeyStore, VaultKey, FakeSecureKeyStore

### Community 115 - "Review Focus"
Cohesion: 0.18
Nodes (10): Global Constraints, Review Focus, Task 1: Domain-типи sync і порти, Task 2: SQLDelight-запити + SqlDelightSyncStore, Task 3: KtorSyncApi, Task 4: SyncNow — use case sync-алгоритму, Task 5: Koin-модуль + архітектурний тест + повна верифікація, Не в обсязі цієї хвилі (+2 more)

### Community 116 - "identityRoutes"
Cohesion: 0.32
Nodes (11): accountRoutes(), clientIp(), fromBase64(), identityRoutes(), ByteArray, T, receiveJson(), requirePlatform() (+3 more)

### Community 118 - ".login"
Cohesion: 0.32
Nodes (3): CredentialsCommand, Login, LoginTest

### Community 119 - "SyncPortsTest"
Cohesion: 0.23
Nodes (4): FakeSyncApi, PushCall, ByteArray, SyncPortsTest

### Community 120 - ".spendAndRotate"
Cohesion: 0.39
Nodes (6): Issued, RefreshSession, Rejected, Reused, Rotation, TokenPair

### Community 121 - "RemoteItem"
Cohesion: 0.17
Nodes (3): contentEqualsOrNull(), ByteArray, RemoteItem

### Community 123 - "SyncDtos.kt"
Cohesion: 0.32
Nodes (5): AppliedChange, Conflict, ByteArray, PushChanges, PushOutcome

### Community 124 - ".invoke"
Cohesion: 0.29
Nodes (4): RegisterCommand, AuthSession, RegisterCommand, RegisterUser

### Community 125 - "installStatusPages"
Cohesion: 0.29
Nodes (3): installStatusPages(), respondProblem(), StatusPagesTest

### Community 129 - ".get"
Cohesion: 0.14
Nodes (13): FolderSelected, ItemEditEffect, ItemEditIntent, ItemEditState, ItemEditStore, Save, Saved, SecretChanged (+5 more)

### Community 130 - ".resolveDevice"
Cohesion: 0.09
Nodes (15): ConfirmationChanged, ConfirmRecoveryWords, DeviceNameChanged, EmailChanged, ErrorDismissed, Flow, PasswordChanged, RecoveryKeyReady (+7 more)

### Community 132 - "PreloginUser"
Cohesion: 0.18
Nodes (10): Global Constraints, Review Focus, Task 0: Платформа пристрою в `CredentialsCommand`, Task 1: LoginStore, Task 2: UnlockStore, Task 3: SessionStore, Task 4: AppGate — стартовий екран, Task 5: Koin + Konsist + повна верифікація (+2 more)

### Community 133 - "DevicePlatform"
Cohesion: 0.27
Nodes (5): FakeArgon2Test, FakeArgon2, FakePasswordNormalizer, ByteArray, toHexString()

### Community 134 - "createAuthApi"
Cohesion: 0.17
Nodes (11): Global Constraints, Review Focus, Review волни (після Task 6), Task 1: ItemUi v2 + мапінг, Task 2: Порт Clipboard + Reveal/Copy use cases, Task 3: VaultListStore, Task 4: ItemDetailStore, Task 5: ItemEditStore (+3 more)

### Community 135 - "DeviceDto.kt"
Cohesion: 0.50
Nodes (3): DeviceDto, deviceRoutes(), toDto()

### Community 136 - "InMemoryRateLimiter"
Cohesion: 0.50
Nodes (3): DevicePlatform, ANDROID, IOS

### Community 137 - "RateLimitDecision"
Cohesion: 0.08
Nodes (17): CompleteRecovery, ByteArray, RecoverCommand, RotatedKeys, CompleteRecoveryTest, DeviceNameChanged, EmailChanged, ErrorDismissed (+9 more)

### Community 139 - "InMemoryVaultItemRepository"
Cohesion: 0.18
Nodes (10): Global Constraints, Review Focus, Task 1: Android composition root + app shell, Task 2: Android екрани розблокування, входу і vault, Task 3: Android lifecycle, приватність, accessibility, Task 4: iOS Kotlin-фасад + Swift app shell, Task 5: iOS екрани, Task 6: iOS lifecycle, приватність, accessibility + фінальна верифікація (+2 more)

### Community 140 - "AuthTypes.kt"
Cohesion: 0.50
Nodes (3): WeakPasswordReason, TOO_SHORT, TOO_WEAK

### Community 144 - ".invoke"
Cohesion: 0.06
Nodes (26): FragmentActivity, MainActivity, AppLifecycleEffects(), AppNetworkSync(), AppRoot(), Theme(), androidx, Bundle (+18 more)

### Community 145 - "ItemUiMappingTest"
Cohesion: 0.27
Nodes (7): AppScreens(), autoLockLabel(), DevicesScreen(), SettingsScreen(), RecoveryScreen(), RegisterScreen(), UnlockScreen()

### Community 150 - "ItemId.kt"
Cohesion: 0.60
Nodes (5): hasValidDashes(), hasValidHexChars(), hasValidVariant(), hasValidVersion(), isValidUuidV4()

### Community 151 - ".resolveDevice"
Cohesion: 0.22
Nodes (6): Allowed, Blocked, RateLimitDecision, RateLimiter, AutoCloseable, RedisRateLimiter

### Community 152 - "PreloginUser"
Cohesion: 0.22
Nodes (8): Global Constraints, Review Focus, Task 1: Спільне сховище сесії, Task 2: Android Keystore + біометрія + буфер, Task 3: iOS Keychain + біометрія + буфер, Task 4: Фабрики платформи та coreModule, Нативний platform-шар клієнта (Native Platform Layer) — Implementation Plan, Порядок злиття

### Community 153 - ".invoke"
Cohesion: 0.08
Nodes (12): DeleteAccount, ListDevices, RevokeDevice, DeleteAccountTest, DeviceUseCasesTest, FakeRecoveryApi, ByteArray, AccountApi (+4 more)

### Community 154 - "DevicePlatform"
Cohesion: 0.10
Nodes (10): io, FakeAccessTokenProvider, createAuthApi(), SessionAccessTokenProvider, authModule(), HttpClient, Module, InMemoryAuthStateStoreTest (+2 more)

### Community 158 - "WeakPasswordReason"
Cohesion: 0.06
Nodes (15): FakeClock, ApplyAutoLockPolicy, AutoLockPolicyTest, FakeAutoLockSettingsStore, FakeCryptoService, ByteArray, FixedAutoLockSettingsStore, AutoLockPolicy (+7 more)

### Community 161 - "StoredResponse"
Cohesion: 0.27
Nodes (5): IdempotencyRepository, ByteArray, StoredResponse, ExposedIdempotencyRepository, ByteArray

### Community 162 - "JwtTokenService"
Cohesion: 0.15
Nodes (6): LockrunaApplication, Application, HttpClient, syncModule(), FakeClock, SyncModuleTest

### Community 163 - ".graph"
Cohesion: 0.43
Nodes (3): KoinApplication, CoreModuleTest, FakeClipboard

### Community 165 - "InMemoryVaultItemRepository"
Cohesion: 0.06
Nodes (14): RevealSecret, Fields, UpdateItem, Flow, VaultItemRepository, LockedAwareInMemoryRepository, ItemId, of() (+6 more)

### Community 166 - "authModule"
Cohesion: 0.15
Nodes (13): Closed, Copy, DeleteConfirmed, Edit, Hide, ItemDetailEffect, ItemDetailIntent, ItemDetailState (+5 more)

### Community 167 - "Argon2Libsodium"
Cohesion: 0.08
Nodes (13): Argon2Factory, PlatformNormalizer, Argon2Factory, Argon2Libsodium, ByteArray, PlatformNormalizer, Argon2BouncyCastle, Argon2Factory (+5 more)

### Community 169 - "FakeBiometricPolicyStore"
Cohesion: 0.17
Nodes (3): FakeBiometricPolicyStore, FakePasswordNormalizer, FixedIdGenerator

### Community 173 - "PlatformNormalizer"
Cohesion: 0.24
Nodes (4): SecureAutoLockSettingsStore, InMemorySecureStorage, ByteArray, SecureAutoLockSettingsStoreTest

### Community 175 - "ByteArray"
Cohesion: 0.22
Nodes (3): Bip39Test, ByteArray, Sha256Crypto

### Community 177 - "SettingsStore"
Cohesion: 0.12
Nodes (14): AccountDeleted, BiometricsChanged, BiometricsToggled, DeleteAccount, DeletionPasswordChanged, ErrorDismissed, Flow, Load (+6 more)

### Community 178 - "AndroidBiometricVault"
Cohesion: 0.23
Nodes (3): Cipher, AndroidBiometricVault, SecretKey

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
Cohesion: 0.15
Nodes (8): DataSource, main(), module(), DeviceApiTest, ByteArray, TokenFlowTest, IdentityClient, Timestamp

### Community 183 - "DevicesStore"
Cohesion: 0.22
Nodes (9): DeviceRevoked, DevicesEffect, DevicesIntent, DevicesState, DevicesStore, ErrorDismissed, Flow, Load (+1 more)

### Community 184 - "IosBiometricVault"
Cohesion: 0.26
Nodes (4): LAContext, IosBiometricVault, CFDataRef, CFStringRef

### Community 186 - "BiometricGate"
Cohesion: 0.20
Nodes (4): FakeBiometricGate, BiometricGate, FakeBiometricGate, CompletableDeferred

### Community 187 - "VaultUseCasesTest"
Cohesion: 0.08
Nodes (15): ItemEditScreen(), secretLabel(), titleLabel(), typeLabel(), usernameLabel(), CreateItem, CreateItemCommand, RevealSecretTest (+7 more)

### Community 188 - "androidPlatformModule"
Cohesion: 0.18
Nodes (8): DatabaseDriverFactory, Context, SqlDriver, androidPlatformModule(), Context, Module, AndroidClipboard, Job

### Community 189 - "iosPlatformModule"
Cohesion: 0.17
Nodes (7): DatabaseDriverFactory, SqlDriver, iosPlatformModule(), Module, Module, platformModule(), IosClipboard

### Community 191 - "CurrentActivityProvider"
Cohesion: 0.28
Nodes (4): CurrentActivityProvider, FragmentActivity, ActivityProvider, FragmentActivity

### Community 192 - ".decode"
Cohesion: 0.42
Nodes (3): IntArray, Bip39, ByteArray

### Community 198 - "KeystoreSecureStorage"
Cohesion: 0.39
Nodes (3): KeystoreSecureStorage, ByteArray, SecretKey

### Community 206 - "BiometricSettings.kt"
Cohesion: 0.50
Nodes (3): BiometricUnlockState, DisableBiometrics, ObserveBiometricUnlock

### Community 209 - "androidMain/kotlin/com/kwazzar/lockruna/data/platform/PlatformFactories.kt"
Cohesion: 0.40
Nodes (3): HttpClient, platformHttpClient(), platformIdGenerator()

### Community 210 - "iosMain/kotlin/com/kwazzar/lockruna/data/platform/PlatformFactories.kt"
Cohesion: 0.40
Nodes (3): HttpClient, platformHttpClient(), platformIdGenerator()

## Knowledge Gaps
- **343 isolated node(s):** `LOGIN`, `CARD`, `NOTE`, `FOLDER`, `Network` (+338 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **59 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `module()` connect `TokenFlowTest` to `deviceRoutes`, `DeviceDto.kt`, `.resolveDevice`, `DevicePlatform`, `Application.kt`, `JwtTokenService`, `.graph`, `androidPlatformModule`, `iosPlatformModule`, `ServerSettings.kt`, `VaultItem`, `RedisDeviceDenylist`, `.invoke`, `.register`, `PendingChange`, `serverModule`, `SyncRoutes.kt`, `identityRoutes`, `installStatusPages`?**
  _High betweenness centrality (0.228) - this node is a cross-community bridge._
- **Why does `serverModule()` connect `serverModule` to `PostgresTest`, `StoredResponse`, `StoredResponse`, `RedisDeviceDenylist`, `SyncDtos.kt`, `PasswordNormalizer`, `.invoke`, `ServerSettings.kt`, `ServerConfigTest`, `TokenFlowTest`, `.resolveDevice`, `.spendAndRotate`, `UserRecord`, `JwtTokenService`, `.invoke`, `FakeCryptoService`?**
  _High betweenness centrality (0.119) - this node is a cross-community bridge._
- **Why does `AppError` connect `AppError` to `.decode`, `.respond`, `SqlDelightVaultItemRepository`, `AuthenticatedUser`, `InMemoryVaultItemRepository`, `toDomain`, `.invoke`, `AuthTypes.kt`, `.invoke`, `VaultItemContent`, `.login`, `SyncPortsTest`, `installSafeCallLogging`, `.invoke`, `AuthDtos.kt`, `.invoke`?**
  _High betweenness centrality (0.106) - this node is a cross-community bridge._
- **Are the 65 inferred relationships involving `module()` (e.g. with `deviceRoutes()` and `accountRoutes()`) actually correct?**
  _`module()` has 65 INFERRED edges - model-reasoned connections that need verification._
- **Are the 47 inferred relationships involving `InMemoryRateLimiter` (e.g. with `.`a device of another user is not revoked`()` and `.`deleting a device blocks its access token immediately`()`) actually correct?**
  _`InMemoryRateLimiter` has 47 INFERRED edges - model-reasoned connections that need verification._
- **Are the 12 inferred relationships involving `AuthenticatedUser` (e.g. with `.wireUp()` and `.aSavedSessionMeansAStoredSession()`) actually correct?**
  _`AuthenticatedUser` has 12 INFERRED edges - model-reasoned connections that need verification._
- **What connects `LOGIN`, `CARD`, `NOTE` to the rest of the system?**
  _343 weakly-connected nodes found - possible documentation gaps or missing edges._