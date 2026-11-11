# Graph Report - Lockruna  (2026-11-11)

## Corpus Check
- 289 files · ~79,661 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 2124 nodes · 3567 edges · 175 communities (123 shown, 52 thin omitted)
- Extraction: 86% EXTRACTED · 14% INFERRED · 0% AMBIGUOUS · INFERRED: 507 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `974747a9`
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
- authModule
- FakeBiometricGate
- ItemId.kt
- .resolveDevice
- PreloginUser
- .invoke
- DevicePlatform
- SessionStoreTest
- .invoke
- FakeBiometricPolicyStore
- WeakPasswordReason
- HasStoredSession
- AccessTokenClaims
- StoredResponse
- JwtTokenService
- .graph
- from
- InMemoryVaultItemRepository
- authModule
- Argon2Libsodium
- AuthTestSupport.kt
- FakeBiometricPolicyStore
- .resolveDevice
- PlatformNormalizer
- PreloginUser
- PlatformNormalizer
- HasStoredSessionTest

## God Nodes (most connected - your core abstractions)
1. `AppError` - 96 edges
2. `Result` - 84 edges
3. `module()` - 61 edges
4. `InMemoryRateLimiter` - 43 edges
5. `AuthenticatedUser` - 42 edges
6. `VaultItem` - 42 edges
7. `ItemId` - 30 edges
8. `InMemoryDeviceDenylist` - 28 edges
9. `IdentityApiTest` - 26 edges
10. `GcmCiphertext` - 26 edges

## Surprising Connections (you probably didn't know these)
- `AppRoot()` --calls--> `startScreen()`  [INFERRED]
  androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/AppRoot.kt → shared/presentation/src/commonMain/kotlin/com/kwazzar/lockruna/presentation/app/AppScreen.kt
- `identityRoutes()` --calls--> `TokenPairDto`  [INFERRED]
  server/src/main/kotlin/com/kwazzar/lockruna/server/identity/api/IdentityRoutes.kt → api-contract/src/commonMain/kotlin/com/kwazzar/lockruna/contract/AuthDtos.kt
- `toDto()` --calls--> `EncryptedItemDto`  [INFERRED]
  server/src/main/kotlin/com/kwazzar/lockruna/server/sync/api/SyncRoutes.kt → api-contract/src/commonMain/kotlin/com/kwazzar/lockruna/contract/SyncDtos.kt
- `toDto()` --calls--> `AppliedChangeDto`  [INFERRED]
  server/src/main/kotlin/com/kwazzar/lockruna/server/sync/api/SyncRoutes.kt → api-contract/src/commonMain/kotlin/com/kwazzar/lockruna/contract/SyncDtos.kt
- `toDto()` --calls--> `ConflictDto`  [INFERRED]
  server/src/main/kotlin/com/kwazzar/lockruna/server/sync/api/SyncRoutes.kt → api-contract/src/commonMain/kotlin/com/kwazzar/lockruna/contract/SyncDtos.kt

## Import Cycles
- None detected.

## Communities (175 total, 52 thin omitted)

### Community 0 - "AppError"
Cohesion: 0.06
Nodes (30): DomainKdfParams, E, FakeSyncApi, PushCall, FakeAuthApi, AuthApi, ByteArray, PreLoginData (+22 more)

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
Cohesion: 0.11
Nodes (28): App, Bool, Date, ContentView, ItemDetailView, ItemEditView, LoginView, String (+20 more)

### Community 12 - "ItemTypeDto"
Cohesion: 0.33
Nodes (5): ItemTypeDto, CARD, FOLDER, LOGIN, NOTE

### Community 15 - "DatabaseDriverFactory"
Cohesion: 0.17
Nodes (4): SecureAuthStateStore, SecureAuthStateStoreTest, InMemorySecureStorage, ByteArray

### Community 16 - "ADR-001: KMP-ядро + нативний UI"
Cohesion: 0.33
Nodes (5): ADR-001: KMP-ядро + нативний UI, Альтернативи, Контекст, Наслідки, Рішення

### Community 17 - "ADR-002: Clean Architecture + DDD-lite, MVI у presentation"
Cohesion: 0.33
Nodes (5): ADR-002: Clean Architecture + DDD-lite, MVI у presentation, Альтернативи, Контекст, Наслідки, Рішення

### Community 18 - "VaultItemType"
Cohesion: 0.38
Nodes (4): FakeCryptoService, PassKeeperDatabase, SqlDriver, SqlDelightVaultItemRepositoryTest

### Community 19 - "MainActivity"
Cohesion: 0.20
Nodes (4): FragmentActivity, MainActivity, Theme(), Bundle

### Community 20 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 21 - "ServerConfigTest"
Cohesion: 0.16
Nodes (12): deviceRoutes(), toDto(), LoginCommand, LoginUser, requireAllowed(), RuntimeException, Problem, ProblemException (+4 more)

### Community 23 - "DatabaseDriverFactory"
Cohesion: 0.12
Nodes (6): GetItem, RevealSecret, Fields, UpdateItem, ItemId, of()

### Community 27 - "ServerConfig.kt"
Cohesion: 0.10
Nodes (14): CopySecret, ItemClicked, Job, Lock, OpenItem, RevealSecret, Search, SecretCopied (+6 more)

### Community 29 - "Application.kt"
Cohesion: 0.19
Nodes (10): InMemoryDeviceDenylist, SyncClient, change(), ByteArray, JsonObject, record(), register(), registerUser() (+2 more)

### Community 43 - "KdfParams"
Cohesion: 0.22
Nodes (3): PlatformNormalizer, PlatformNormalizer, PasswordNormalizer

### Community 44 - "PasswordNormalizer"
Cohesion: 0.17
Nodes (8): Applied, ApplyResult, ChangeRecord, Conflict, SealedItemRecord, VaultItemRepository, ExposedVaultItemRepository, VaultItemRepository

### Community 45 - "Крипто-ядро клієнта (Crypto Core) — Implementation Plan"
Cohesion: 0.15
Nodes (12): Global Constraints, Review Focus, Task 1: Доменні порти, KDF-параметри та HKDF-рядки, Task 2: Реалізація для JVM і Android (BouncyCastle), Task 3: Реалізація для iOS (libsodium через ionspin), Task 4: Спільні KAT-вектори й збіжність платформ, Task 5: Ієрархія ключів, архітектурний тест і правка ADR, Власність файлів (паралельна робота) (+4 more)

### Community 46 - "withCBytes"
Cohesion: 0.24
Nodes (6): ByteArray, ByteArray, T, withCBytes(), ByteArray, PlatformCrypto

### Community 47 - "CryptoVectors.kt"
Cohesion: 0.43
Nodes (5): Argon2Case, CryptoVectors, hexToByteArray(), ByteArray, NormalizationCase

### Community 49 - "caveman/SKILL.md"
Cohesion: 0.33
Nodes (5): Auto-Clarity, Boundaries, Intensity, Persistence, Rules

### Community 57 - "UserRecord"
Cohesion: 0.11
Nodes (8): EmailAlreadyRegisteredException, RuntimeException, ByteArray, UserRecord, UserRepository, ExposedUserRepository, ByteArray, UserRepositoryTest

### Community 58 - "ExposedRefreshTokenRepository"
Cohesion: 0.09
Nodes (18): LockrunaApplication, Application, Conflict, io, Batch, ConflictOutcome, Continue, Failed (+10 more)

### Community 59 - "JwtTokenService"
Cohesion: 0.08
Nodes (11): Algorithm, DecodedJWT, JWTVerifier, ByteArray, RefreshTokenRecord, RefreshTokenRepository, AccessTokenClaims, TokenService (+3 more)

### Community 60 - "ServerSettings.kt"
Cohesion: 0.12
Nodes (14): HikariConfig, fromEnv(), ByteArray, required(), ServerSettings, toFakeSecret(), toJwtKey(), toPepper() (+6 more)

### Community 61 - "Result"
Cohesion: 0.07
Nodes (13): HttpClient, platformHttpClient(), platformIdGenerator(), HttpClient, platformHttpClient(), platformIdGenerator(), HttpClient, platformHttpClient() (+5 more)

### Community 62 - "FakeCryptoService"
Cohesion: 0.24
Nodes (4): FakeClock, AutoLockPolicyTest, FakeCryptoService, ByteArray

### Community 63 - "InMemoryVaultItemRepository"
Cohesion: 0.20
Nodes (4): SupportFakesTest, FakeIdGenerator, InMemoryVaultItemRepository, VaultItemRepository

### Community 65 - "PostgresTest"
Cohesion: 0.15
Nodes (7): PostgreSQLContainer, ExposedTransactionRunner, T, TransactionRunner, Database, HikariDataSource, PostgresTest

### Community 66 - "VaultItem"
Cohesion: 0.19
Nodes (4): IdentityApiTest, ByteArray, HttpResponse, JsonObject

### Community 67 - "create"
Cohesion: 0.06
Nodes (25): AppliedChangeDto, ChangeDto, ConflictDto, EncryptedItemDto, PushResultDto, SyncPageDto, toDto(), RevealSecretTest (+17 more)

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
Cohesion: 0.12
Nodes (5): ListDevices, DeviceRecord, DeviceRepository, ExposedDeviceRepository, RefreshTokenRepositoryTest

### Community 72 - "RedisDeviceDenylist"
Cohesion: 0.22
Nodes (3): DeviceDenylist, AutoCloseable, RedisDeviceDenylist

### Community 73 - "GcmCiphertext"
Cohesion: 0.23
Nodes (5): KeyDerivation, ByteArray, com, CryptoService, ByteArray

### Community 75 - "RateLimitDecision"
Cohesion: 0.11
Nodes (3): FakeSyncStore, LocalRecord, Outbox

### Community 79 - "Tables.kt"
Cohesion: 0.48
Nodes (6): Devices, IdempotencyKeys, RefreshTokens, Users, VaultItems, Table

### Community 83 - ".invoke"
Cohesion: 0.10
Nodes (21): createAuthApi(), HttpClient, AuthResponse, DeviceDto, hex(), hexToByteArray(), KdfParamsDto, ByteArray (+13 more)

### Community 84 - "PlatformCrypto"
Cohesion: 0.25
Nodes (3): ByteArray, PlatformCrypto, GcmCiphertext

### Community 86 - "ItemId.kt"
Cohesion: 0.13
Nodes (13): BiometricsEnabled, EmailChanged, EnableBiometrics, ErrorDismissed, Flow, PasswordChanged, SubmitBiometrics, SubmitPassword (+5 more)

### Community 87 - "installSafeCallLogging"
Cohesion: 0.25
Nodes (3): DeviceApiTest, Counter, InMemoryRateLimiter

### Community 91 - ".invoke"
Cohesion: 0.11
Nodes (10): ApplyAutoLockPolicy, LockVault, DeleteItem, Flow, ObserveItems, coreModule(), HttpClient, platformClock() (+2 more)

### Community 94 - "LockVault"
Cohesion: 0.13
Nodes (12): LoginScreen(), DeviceNameChanged, EmailChanged, ErrorDismissed, Flow, LoggedIn, LoginEffect, LoginIntent (+4 more)

### Community 95 - ".invoke"
Cohesion: 0.50
Nodes (3): DevicePlatform, ANDROID, IOS

### Community 96 - "VaultKey"
Cohesion: 0.06
Nodes (17): KeystoreSecureStorage, ByteArray, SecretKey, ByteArray, SecureStorage, DatabaseDriverFactory, SqlDriver, iosPlatformModule() (+9 more)

### Community 97 - ".unlock"
Cohesion: 0.18
Nodes (4): EnableBiometrics, UnlockWithBiometrics, EnableBiometricsTest, UnlockWithBiometricsTest

### Community 98 - ".respond"
Cohesion: 0.24
Nodes (5): MockEngine, HttpResponse, T, KtorSyncApi, KtorSyncApiTest

### Community 99 - ".register"
Cohesion: 0.16
Nodes (6): Locked, State, Unlocked, VaultSession, VaultSessionTest, StateFlow

### Community 100 - "SyncNowTest"
Cohesion: 0.26
Nodes (6): SyncNowTest, toDomain(), AppliedChange, Conflict, PushResult, SyncPage

### Community 101 - "AuthenticatedUser"
Cohesion: 0.15
Nodes (5): FakeAuthStateStore, InMemoryAuthStateStore, AuthStateStore, AuthenticatedUser, FakeAuthStateStore

### Community 102 - "toDomain"
Cohesion: 0.08
Nodes (17): CurrentActivityProvider, FragmentActivity, CopySecret, CopySecretTest, FailingClipboard, RecordingClipboard, DatabaseDriverFactory, Context (+9 more)

### Community 103 - "AuthTokens"
Cohesion: 0.16
Nodes (6): CreateItem, CreateItemCommand, Flow, VaultItemRepository, LockedAwareInMemoryRepository, VaultCycleTest

### Community 104 - "PendingChange"
Cohesion: 0.10
Nodes (13): DeviceInfoDto, PushRequestDto, ByteArray, TokenFlowTest, bearer(), IdentityClient, json(), ByteArray (+5 more)

### Community 105 - ".invoke"
Cohesion: 0.10
Nodes (12): of(), Secret, of(), Title, of(), Url, create(), createWithGeneratedId() (+4 more)

### Community 106 - ".weakPasswordReturnsWeakMasterPassword"
Cohesion: 0.21
Nodes (5): ByteArray, Register, RegisterCommand, RegisterOutcome, RegisterTest

### Community 107 - "KdfParams"
Cohesion: 0.18
Nodes (5): Argon2VectorsTest, Argon2DispatchTest, isAcceptable(), KdfParams, KdfParamsTest

### Community 108 - "Review Focus"
Cohesion: 0.15
Nodes (12): Global Constraints, Review Focus, Task 0: Оновлення docs (ADR-003, погроза), Task 1: Domain-типи auth і порти, Task 2: Login, Logout, Unlock (use cases), Task 3: Register, Task 4: Біометрия: EnableBiometrics, UnlockWithBiometrics, Task 5: KtorAuthApi і SessionAccessTokenProvider (+4 more)

### Community 111 - "serverModule"
Cohesion: 0.12
Nodes (9): LogoutSession, PreloginResult, PreloginUser, RevokeDevice, DataSource, Module, serverModule(), PullChanges (+1 more)

### Community 112 - "SyncRoutes.kt"
Cohesion: 0.38
Nodes (11): asUuid(), bodyHash(), fromBase64(), idempotencyKey(), invalidRequest(), ByteArray, limit(), parse() (+3 more)

### Community 113 - "PushChanges"
Cohesion: 0.06
Nodes (17): Cipher, LAContext, FakeBiometricGate, FakeSecureKeyStore, AndroidBiometricVault, SecretKey, IosBiometricVault, CFDataRef (+9 more)

### Community 115 - "Review Focus"
Cohesion: 0.18
Nodes (10): Global Constraints, Review Focus, Task 1: Domain-типи sync і порти, Task 2: SQLDelight-запити + SqlDelightSyncStore, Task 3: KtorSyncApi, Task 4: SyncNow — use case sync-алгоритму, Task 5: Koin-модуль + архітектурний тест + повна верифікація, Не в обсязі цієї хвилі (+2 more)

### Community 116 - "identityRoutes"
Cohesion: 0.14
Nodes (18): AuthResponseDto, LoginRequest, PreloginRequest, PreloginResponse, RefreshRequest, RegisterRequest, TokenPairDto, clientIp() (+10 more)

### Community 117 - "PasswordNormalizer"
Cohesion: 0.22
Nodes (7): Closed, ItemDetailEffect, ItemDetailState, ItemDetailStore, NavigateToEdit, SecretCopied, SecretRevealed

### Community 118 - ".login"
Cohesion: 0.32
Nodes (3): CredentialsCommand, Login, LoginTest

### Community 120 - ".spendAndRotate"
Cohesion: 0.39
Nodes (6): Issued, RefreshSession, Rejected, Reused, Rotation, TokenPair

### Community 121 - "RemoteItem"
Cohesion: 0.18
Nodes (3): contentEqualsOrNull(), ByteArray, RemoteItem

### Community 122 - "AuthDtos.kt"
Cohesion: 0.15
Nodes (5): BlockingVaultItemRepository, FixedIdGenerator, ItemEditStoreTest, Flow, VaultItemRepository

### Community 123 - "SyncDtos.kt"
Cohesion: 0.32
Nodes (5): AppliedChange, Conflict, ByteArray, PushChanges, PushOutcome

### Community 124 - ".invoke"
Cohesion: 0.40
Nodes (3): RegisterCommand, AuthSession, RegisterUser

### Community 126 - ".logoutClearsSessionAndLocks"
Cohesion: 0.16
Nodes (6): Logout, UnlockWithPassword, AuthStateStoreTest, LogoutTest, UnlockWithPasswordTest, AuthTokens

### Community 129 - ".get"
Cohesion: 0.15
Nodes (12): FolderSelected, ItemEditEffect, ItemEditIntent, ItemEditState, ItemEditStore, Save, Saved, SecretChanged (+4 more)

### Community 130 - ".resolveDevice"
Cohesion: 0.40
Nodes (3): Argon2BouncyCastle, Argon2Factory, ByteArray

### Community 131 - "deviceRoutes"
Cohesion: 0.28
Nodes (5): DataSource, main(), module(), installSafeCallLogging(), SafeCallLogging

### Community 132 - "PreloginUser"
Cohesion: 0.18
Nodes (10): Global Constraints, Review Focus, Task 0: Платформа пристрою в `CredentialsCommand`, Task 1: LoginStore, Task 2: UnlockStore, Task 3: SessionStore, Task 4: AppGate — стартовий екран, Task 5: Koin + Konsist + повна верифікація (+2 more)

### Community 133 - "DevicePlatform"
Cohesion: 0.22
Nodes (5): BiometricPolicyStoreTest, FakeArgon2Test, FakeArgon2, ByteArray, toHexString()

### Community 134 - "createAuthApi"
Cohesion: 0.17
Nodes (11): Global Constraints, Review Focus, Review волни (після Task 6), Task 1: ItemUi v2 + мапінг, Task 2: Порт Clipboard + Reveal/Copy use cases, Task 3: VaultListStore, Task 4: ItemDetailStore, Task 5: ItemEditStore (+3 more)

### Community 136 - "InMemoryRateLimiter"
Cohesion: 0.25
Nodes (3): Clock, FakeClock, RecordingClipboard

### Community 139 - "InMemoryVaultItemRepository"
Cohesion: 0.18
Nodes (10): Global Constraints, Review Focus, Task 1: Android composition root + app shell, Task 2: Android екрани розблокування, входу і vault, Task 3: Android lifecycle, приватність, accessibility, Task 4: iOS Kotlin-фасад + Swift app shell, Task 5: iOS екрани, Task 6: iOS lifecycle, приватність, accessibility + фінальна верифікація (+2 more)

### Community 140 - "AuthTypes.kt"
Cohesion: 0.60
Nodes (4): ByteArray, pack(), SealedBlob, unpack()

### Community 144 - ".invoke"
Cohesion: 0.24
Nodes (6): AppScreen, LOGIN, UNLOCK, VAULT, startScreen(), AppScreenTest

### Community 145 - "ItemUiMappingTest"
Cohesion: 0.20
Nodes (5): AppRoot(), ItemDetailScreen(), ItemEditScreen(), UnlockScreen(), VaultListScreen()

### Community 148 - "authModule"
Cohesion: 0.22
Nodes (8): Flow, Lock, Locked, Resumed, SessionEffect, SessionIntent, SessionState, SessionStore

### Community 149 - "FakeBiometricGate"
Cohesion: 0.12
Nodes (3): CompletableDeferred, LoginStoreTest, UnlockStoreTest

### Community 150 - "ItemId.kt"
Cohesion: 0.60
Nodes (5): hasValidDashes(), hasValidHexChars(), hasValidVariant(), hasValidVersion(), isValidUuidV4()

### Community 151 - ".resolveDevice"
Cohesion: 0.22
Nodes (6): Allowed, Blocked, RateLimitDecision, RateLimiter, AutoCloseable, RedisRateLimiter

### Community 152 - "PreloginUser"
Cohesion: 0.22
Nodes (8): Global Constraints, Review Focus, Task 1: Спільне сховище сесії, Task 2: Android Keystore + біометрія + буфер, Task 3: iOS Keychain + біометрія + буфер, Task 4: Фабрики платформи та coreModule, Нативний platform-шар клієнта (Native Platform Layer) — Implementation Plan, Порядок злиття

### Community 154 - "DevicePlatform"
Cohesion: 0.14
Nodes (5): FakeAccessTokenProvider, SessionAccessTokenProvider, InMemoryAuthStateStoreTest, SessionAccessTokenProviderTest, AccessTokenProvider

### Community 156 - ".invoke"
Cohesion: 0.22
Nodes (4): ValidateMasterPassword, canonicalEmail(), MasterPassword, of()

### Community 158 - "WeakPasswordReason"
Cohesion: 0.50
Nodes (3): WeakPasswordReason, TOO_SHORT, TOO_WEAK

### Community 160 - "AccessTokenClaims"
Cohesion: 0.29
Nodes (3): requireSession(), AuthenticateSession, bearerToken()

### Community 161 - "StoredResponse"
Cohesion: 0.27
Nodes (5): IdempotencyRepository, ByteArray, StoredResponse, ExposedIdempotencyRepository, ByteArray

### Community 163 - ".graph"
Cohesion: 0.27
Nodes (4): KoinApplication, CoreModuleTest, FakeClipboard, AutoLockPolicy

### Community 164 - "from"
Cohesion: 0.36
Nodes (5): AuthSessionCodec, from(), KdfParamsRecord, ByteArray, SessionRecord

### Community 165 - "InMemoryVaultItemRepository"
Cohesion: 0.22
Nodes (3): InMemoryVaultItemRepository, Flow, VaultItemRepository

### Community 166 - "authModule"
Cohesion: 0.33
Nodes (6): Copy, DeleteConfirmed, Edit, Hide, ItemDetailIntent, Reveal

### Community 167 - "Argon2Libsodium"
Cohesion: 0.38
Nodes (4): Argon2Factory, Argon2Libsodium, Argon2, ByteArray

### Community 168 - "AuthTestSupport.kt"
Cohesion: 0.40
Nodes (3): FakeArgon2, FakePasswordNormalizer, ByteArray

## Knowledge Gaps
- **285 isolated node(s):** `PreloginRequest`, `PreloginResponse`, `RegisterRequest`, `LoginRequest`, `RefreshRequest` (+280 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **52 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `module()` connect `deviceRoutes` to `VaultKey`, `VaultItem`, `.graph`, `toDomain`, `RedisDeviceDenylist`, `PendingChange`, `RateLimitDecision`, `serverModule`, `SyncRoutes.kt`, `identityRoutes`, `ServerConfigTest`, `installSafeCallLogging`, `.resolveDevice`, `ExposedRefreshTokenRepository`, `.invoke`, `ServerSettings.kt`, `Application.kt`?**
  _High betweenness centrality (0.249) - this node is a cross-community bridge._
- **Why does `serverModule()` connect `serverModule` to `AccessTokenClaims`, `PostgresTest`, `StoredResponse`, `deviceRoutes`, `StoredResponse`, `RedisDeviceDenylist`, `SyncDtos.kt`, `PasswordNormalizer`, `ServerSettings.kt`, `ServerConfigTest`, `.resolveDevice`, `.spendAndRotate`, `UserRecord`, `JwtTokenService`, `.invoke`?**
  _High betweenness centrality (0.129) - this node is a cross-community bridge._
- **Why does `coreModule()` connect `.invoke` to `deviceRoutes`, `SqlDelightVaultItemRepository`, `.graph`, `.register`, `AuthTokens`, `PlatformNormalizer`, `requireSession`, `VaultItemType`, `PlatformCrypto`, `DatabaseDriverFactory`, `.invoke`, `ExposedRefreshTokenRepository`?**
  _High betweenness centrality (0.107) - this node is a cross-community bridge._
- **Are the 55 inferred relationships involving `module()` (e.g. with `deviceRoutes()` and `identityRoutes()`) actually correct?**
  _`module()` has 55 INFERRED edges - model-reasoned connections that need verification._
- **Are the 38 inferred relationships involving `InMemoryRateLimiter` (e.g. with `.`a device of another user is not revoked`()` and `.`deleting a device blocks its access token immediately`()`) actually correct?**
  _`InMemoryRateLimiter` has 38 INFERRED edges - model-reasoned connections that need verification._
- **Are the 11 inferred relationships involving `AuthenticatedUser` (e.g. with `.aSavedSessionMeansAStoredSession()` and `.authenticatedUser()`) actually correct?**
  _`AuthenticatedUser` has 11 INFERRED edges - model-reasoned connections that need verification._
- **What connects `PreloginRequest`, `PreloginResponse`, `RegisterRequest` to the rest of the system?**
  _285 weakly-connected nodes found - possible documentation gaps or missing edges._