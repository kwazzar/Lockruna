# Graph Report - Lockruna  (2026-11-10)

## Corpus Check
- 173 files · ~43,605 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 1120 nodes · 1780 edges · 96 communities (63 shown, 33 thin omitted)
- Extraction: 84% EXTRACTED · 16% INFERRED · 0% AMBIGUOUS · INFERRED: 282 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `df40e3b5`
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

## God Nodes (most connected - your core abstractions)
1. `module()` - 54 edges
2. `InMemoryRateLimiter` - 43 edges
3. `AppError` - 38 edges
4. `Result` - 30 edges
5. `VaultItem` - 30 edges
6. `InMemoryDeviceDenylist` - 28 edges
7. `IdentityApiTest` - 26 edges
8. `serverModule()` - 24 edges
9. `Problem` - 21 edges
10. `GcmCiphertext` - 21 edges

## Surprising Connections (you probably didn't know these)
- `identityRoutes()` --calls--> `PreloginResponse`  [INFERRED]
  server/src/main/kotlin/com/kwazzar/lockruna/server/identity/api/IdentityRoutes.kt → api-contract/src/commonMain/kotlin/com/kwazzar/lockruna/contract/AuthDtos.kt
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

## Communities (96 total, 33 thin omitted)

### Community 0 - "AppError"
Cohesion: 0.13
Nodes (14): AppError, InvalidCredentials, InvalidItem, ItemCorrupted, ItemNotFound, NetworkUnavailable, RateLimited, Unauthorized (+6 more)

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
Cohesion: 0.05
Nodes (22): FakeCryptoService, CreateItem, CreateItemCommand, VaultCycleTest, VaultUseCasesTest, FakeClock, SqlDriver, SqlDelightVaultItemRepositoryTest (+14 more)

### Community 19 - "MainActivity"
Cohesion: 0.40
Nodes (3): MainActivity, Bundle, ComponentActivity

### Community 20 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 21 - "ServerConfigTest"
Cohesion: 0.05
Nodes (50): AuthResponseDto, PreloginRequest, PreloginResponse, TokenPairDto, DeviceDto, AppliedChangeDto, ConflictDto, EncryptedItemDto (+42 more)

### Community 27 - "ServerConfig.kt"
Cohesion: 0.07
Nodes (17): Module, ListDevices, LogoutSession, PreloginResult, PreloginUser, Issued, RefreshSession, Rejected (+9 more)

### Community 29 - "Application.kt"
Cohesion: 0.05
Nodes (38): DeviceInfoDto, LoginRequest, RefreshRequest, RegisterRequest, ChangeDto, PushRequestDto, DataSource, main() (+30 more)

### Community 43 - "KdfParams"
Cohesion: 0.06
Nodes (18): Argon2Factory, PlatformNormalizer, Argon2VectorsTest, Argon2Factory, Argon2Libsodium, ByteArray, PlatformNormalizer, Argon2BouncyCastle (+10 more)

### Community 44 - "PasswordNormalizer"
Cohesion: 0.11
Nodes (13): AppliedChange, Conflict, ByteArray, PushChanges, PushOutcome, Applied, ApplyResult, ChangeRecord (+5 more)

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
Cohesion: 0.11
Nodes (8): EmailAlreadyRegisteredException, RuntimeException, ByteArray, UserRecord, UserRepository, ExposedUserRepository, ByteArray, UserRepositoryTest

### Community 58 - "ExposedRefreshTokenRepository"
Cohesion: 0.12
Nodes (6): ByteArray, RefreshTokenRecord, RefreshTokenRepository, ExposedRefreshTokenRepository, ByteArray, RefreshTokenRepositoryTest

### Community 59 - "JwtTokenService"
Cohesion: 0.09
Nodes (10): Algorithm, DecodedJWT, JWTVerifier, requireSession(), AuthenticateSession, AccessTokenClaims, TokenService, JwtTokenService (+2 more)

### Community 60 - "ServerSettings.kt"
Cohesion: 0.12
Nodes (14): HikariConfig, fromEnv(), ByteArray, required(), ServerSettings, toFakeSecret(), toJwtKey(), toPepper() (+6 more)

### Community 61 - "Result"
Cohesion: 0.14
Nodes (10): E, DeleteItem, Fields, UpdateItem, Failure, T, Result, Success (+2 more)

### Community 62 - "FakeCryptoService"
Cohesion: 0.14
Nodes (6): FakeClock, ApplyAutoLockPolicy, AutoLockPolicyTest, FakeCryptoService, ByteArray, AutoLockPolicy

### Community 63 - "InMemoryVaultItemRepository"
Cohesion: 0.12
Nodes (6): SupportFakesTest, FakeClock, FakeIdGenerator, InMemoryVaultItemRepository, VaultItemRepository, IdGenerator

### Community 65 - "PostgresTest"
Cohesion: 0.15
Nodes (7): PostgreSQLContainer, ExposedTransactionRunner, T, TransactionRunner, Database, HikariDataSource, PostgresTest

### Community 66 - "VaultItem"
Cohesion: 0.16
Nodes (6): Flow, VaultItemRepository, LockedAwareInMemoryRepository, VaultItem, Flow, VaultItemRepository

### Community 67 - "create"
Cohesion: 0.20
Nodes (8): of(), Secret, of(), Title, of(), Url, create(), createWithGeneratedId()

### Community 68 - "SqlDelightVaultItemRepository"
Cohesion: 0.25
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
Cohesion: 0.27
Nodes (3): CryptoService, GcmCiphertext, ByteArray

### Community 75 - "RateLimitDecision"
Cohesion: 0.22
Nodes (5): Allowed, Blocked, RateLimitDecision, AutoCloseable, RedisRateLimiter

### Community 79 - "Tables.kt"
Cohesion: 0.48
Nodes (6): Devices, IdempotencyKeys, RefreshTokens, Users, VaultItems, Table

### Community 83 - ".invoke"
Cohesion: 0.40
Nodes (3): ValidateMasterPassword, MasterPassword, of()

### Community 86 - "ItemId.kt"
Cohesion: 0.60
Nodes (5): hasValidDashes(), hasValidHexChars(), hasValidVariant(), hasValidVersion(), isValidUuidV4()

## Knowledge Gaps
- **183 isolated node(s):** `LOGIN`, `CARD`, `NOTE`, `FOLDER`, `Shared` (+178 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **33 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `module()` connect `Application.kt` to `RedisDeviceDenylist`, `RateLimitDecision`, `ServerConfigTest`, `installSafeCallLogging`, `ServerConfig.kt`, `ServerSettings.kt`?**
  _High betweenness centrality (0.222) - this node is a cross-community bridge._
- **Why does `deviceRoutes()` connect `ServerConfigTest` to `VaultItem`, `Application.kt`?**
  _High betweenness centrality (0.213) - this node is a cross-community bridge._
- **Why does `serverModule()` connect `ServerConfig.kt` to `PostgresTest`, `StoredResponse`, `RedisDeviceDenylist`, `PasswordNormalizer`, `ServerConfigTest`, `UserRecord`, `ExposedRefreshTokenRepository`, `JwtTokenService`, `ServerSettings.kt`, `Application.kt`?**
  _High betweenness centrality (0.146) - this node is a cross-community bridge._
- **Are the 48 inferred relationships involving `module()` (e.g. with `deviceRoutes()` and `identityRoutes()`) actually correct?**
  _`module()` has 48 INFERRED edges - model-reasoned connections that need verification._
- **Are the 38 inferred relationships involving `InMemoryRateLimiter` (e.g. with `.`a device of another user is not revoked`()` and `.`deleting a device blocks its access token immediately`()`) actually correct?**
  _`InMemoryRateLimiter` has 38 INFERRED edges - model-reasoned connections that need verification._
- **Are the 2 inferred relationships involving `VaultItem` (e.g. with `.`InMemoryVaultItemRepository observeAll emits list`()` and `.`InMemoryVaultItemRepository save and get`()`) actually correct?**
  _`VaultItem` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `LOGIN`, `CARD`, `NOTE` to the rest of the system?**
  _183 weakly-connected nodes found - possible documentation gaps or missing edges._