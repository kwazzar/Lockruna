# Синхронізація клієнта (Client Sync) — Implementation Plan

> **Для агентних виконавців:** ОБОВ'ЯЗКОВО: використати `superpowers:subagent-driven-development` (рекомендовано) або `superpowers:executing-plans` для виконання цього плану по завданнях. Кроки використовують checkbox (`- [ ]`) для відстеження.

**Мета:** Sync-рушій клієнта за ADR-005 §7: pull за курсором із захистом від rollback, push батчами ≤100 через outbox з Idempotency-Key, конфлікти last-write-wins по `updatedAt`, скидання локальних даних, якщо останній sync старше 90 днів.

**Архітектура:** порт `SyncApi`, типи обміну і порт `SyncStore` у `shared/domain/sync`; реалізації `SqlDelightSyncStore` і `KtorSyncApi` у `shared/data/sync`; use case `SyncNow` у `shared/application/sync` інкапсулює весь алгоритм pull → push. Токен доступу береться через порт `AccessTokenProvider`; його продукційна реалізація приходить у хвилі `client-auth` (у тестах — fake).

**Технології:** Kotlin 2.4.20, Ktor client 3.6.1, kotlinx-serialization 1.12.0, SQLDelight 2.4.1 (наявна схема `PassKeeperDatabase.sq`), kotlin.test. Нових рядків у `gradle/libs.versions.toml` — 2 (`ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json`); версії ktor/serialization вже в каталозі.

**Специфікація:** `docs/requirements.md` (US-09), `docs/adr/005-client-shared.md` §7, `docs/openapi.yaml` (`GET /sync`, `POST /sync/push`), `docs/threat-model.md` (rollback, idempotency), серверний код `server/.../sync/infrastructure/ExposedVaultItemRepository.kt` (семантика `baseVersion` — не змінюється).

## Global Constraints

- **Семантика push, зчитана з серверного коду (ExposedVaultItemRepository.apply):** `baseVersion == 0` — insert новий запис (конфлікт, якщо такий id уже існує); `baseVersion == V` — `UPDATE ... WHERE version == V AND deleted_at IS NULL` → applied version = `V + 1`, інакше `conflicts` з поточним серверним записом (відсутній запис — серверний «нічий» рядок з version 0). Тобто `baseVersion` = локальна `version` рядка, а vault-core записує контент із AAD `rowVersion + 1` — після applied-відповіді рядок отримує `version = baseVersion + 1`, і AAD знову збігається. **Повторного шифрування при conflict-win не потрібно** — пушиться той самий ciphertext зі зміненим `baseVersion`.
- Конфлікти — не HTTP-помилки. HTTP 409 повертається ТІЛЬКИ за повторний Idempotency-Key з іншим тілом → нова гілка `AppError.IdempotencyConflict`.
- Retry йде з тим самим ключем і тим самим батчем: outbox зберігає батч + ключ до відповіді.
- Відповідь сервера з `version` < локального `max_seen_version` → рядок НЕ перезаписується (skip); `max_seen_version` не зменшується.
- domain-sync типи спілкуються wire-значеннями: id — `String` (uuid), не `ItemId` VO; DTO-мапінг (серіалізація) — тільки всередині `data`.
- Секретний матеріал не логується і не в `toString()`: `PendingChange`/`RemoteItem`/`Outbox` перевизначають `toString` без `ciphertext`.
- LWW по `updatedAt`: нічиє (рівні) → серверний запис перемагає.
- Кожна відповідь мережі мапиться в `AppError`: 401 → `Unauthorized`, 429 (`Retry-After`) → `RateLimited`, транспорт → `NetworkUnavailable`. Мапінг — тільки в `data`.
- `gradle/libs.versions.toml` — тільки 2 нові строки (нижче). Якщо після merge рядок уже існує (з client-auth) — тримати один примірник.
- Тригери sync (unlock, debounce, network return, WorkManager/BGTask) — воротний завдання: вони відкривають інтерфейс для native waves.
- Код/коментарі/коміти англійською; docs/ українською. JDK 25: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.

## Review Focus

- Повторний Idempotency-Key з іншим тілом (409) — очікуємо `Failure(IdempotencyConflict)`; outbox лишається, щоб оригінальний батч не було втрачено.
- Сервер вертає `version < max_seen_version` (rollback-підміна) — рядок НЕ перезаписується; тест у Task 4 фіксує.
- Sync із неводящим токеном (`token() == null`) — `Failure(Unauthorized)` БЕЗ мережевого виклику (перевірка перед будь-яким мережевим викликом).
- Після remote-wins конфлікту рядок dirty = false (інакше вечній sync loop).
- Повторний виклик `SyncNow` одразу після успішного — нуль мережевих викликів (ідеемпотентність).

---

### Task 1: Domain-типи sync і порти

**Files:**

- Create: `shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/sync/SyncTypes.kt`
- Create: `shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/sync/SyncApi.kt`
- Create: `shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/sync/SyncStore.kt`
- Test: `shared/application/src/commonTest/kotlin/com/kwazzar/lockruna/application/support/FakeSyncApi.kt`
- Test: `shared/application/src/commonTest/kotlin/com/kwazzar/lockruna/application/support/FakeSyncStore.kt`
- Test: `shared/application/src/commonTest/kotlin/com/kwazzar/lockruna/application/sync/SyncPortsTest.kt`

**Interfaces:**

```kotlin
// SyncTypes.kt
package com.kwazzar.lockruna.domain.sync

data class PendingChange(
    val id: String,          // uuid як рядок
    val type: VaultItemType, // com.kwazzar.lockruna.domain.vault.VaultItemType
    val ciphertext: ByteArray?,
    val nonce: ByteArray?,
    val baseVersion: Long,
) {
    override fun toString() = "PendingChange(id=$id, baseVersion=$baseVersion)"
}

data class RemoteItem(
    val id: String,
    val type: VaultItemType,
    val ciphertext: ByteArray?, // null для tombstone
    val nonce: ByteArray?,
    val version: Long,
    val deleted: Boolean,
    val updatedAtMillis: Long,
) {
    override fun toString() = "RemoteItem(id=$id, version=$version, deleted=$deleted)"
}

data class SyncPage(items: List<RemoteItem>, nextCursor: String, hasMore: Boolean)
data class AppliedChange(id: String, newVersion: Long)
data class Conflict(id: String, server: RemoteItem)
data class PushResult(applied: List<AppliedChange>, conflicts: List<Conflict>)

// SyncApi.kt
interface SyncApi {
    suspend fun pull(cursor: String?): Result<SyncPage, AppError>
    suspend fun push(changes: List<PendingChange>): Result<PushResult, AppError>
}

interface AccessTokenProvider {
    suspend fun token(): String?   // null → сесія не автентифікована
}

// SyncStore.kt
data class LocalRecord(
    val id: String,
    val version: Long,
    val updatedAtMillis: Long,
    val ciphertext: ByteArray?,
    val nonce: ByteArray?,
    val dirty: Boolean,
    val maxSeenVersion: Long,
)

interface SyncStore {
    suspend fun cursor(): String?
    suspend fun setCursor(cursor: String?)
    suspend fun lastSyncAtMillis(): Long?
    suspend fun markSynced(nowMillis: Long)
    suspend fun peek(id: String): LocalRecord?
    suspend fun dirtyChanges(limit: Int): List<PendingChange>   // baseVersion = локальна version рядка
    suspend fun applyRemote(item: RemoteItem)                   // upsert; попередній вміст → item_history; dirty = false
    suspend fun stageOutbox(idempotencyKey: String, changes: List<PendingChange>)
    suspend fun pendingOutbox(): Outbox?
    suspend fun clearOutbox()
    suspend fun resetLocalData()                                // wipe pull-стану для 90d-гейта
}

data class Outbox(val idempotencyKey: String, val changes: List<PendingChange>)
```

`AppError.kt` — ця хвиля є **єдиним власником** файлу `shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/error/AppError.kt`: додати дві гілки — `data object IdempotencyConflict : AppError` (409 на повторний Idempotency-Key) і `data object EmailAlreadyRegistered : AppError` (409 на register). Другу гілку додаємо тут, щоб auth-хвиля не редагувала `AppError.kt` і merge був безконфліктним.

- [ ] **Step 1: Write the failing test** — `SyncPortsTest`: `FakeSyncApi` віддає задані страницы/результати; `FakeSyncStore` roundtrip stage→pending→clearOutbox; `dirtyChanges` повертає staged value.
- [ ] **Step 2: verify fails** `./gradlew :shared:application:jvmTest --tests '*SyncPortsTest*'` — FAIL.
- [ ] **Step 3: Implement** — ти сами типи/порти, скелет фейків.
- [ ] **Step 4: verify passes.**
- [ ] **Step 5: Commit** `feat(domain): sync exchange types and ports`

---

### Task 2: SQLDelight-запити + SqlDelightSyncStore

**Files:**

- Modify: `shared/data/src/commonMain/sqldelight/com/kwazzar/lockruna/data/db/PassKeeperDatabase.sq` (додати запити; схему таблиць НЕ змінюємо — `outbox`/`sync_state`/`item_history` існують)
- Create: `shared/data/src/commonMain/kotlin/com/kwazzar/lockruna/data/sync/SqlDelightSyncStore.kt`
- Test: `shared/data/src/commonTest/kotlin/com/kwazzar/lockruna/data/sync/SqlDelightSyncStoreTest.kt`

**Interfaces:**

- Consumes: `SyncStore`, Task 1-типи; конструктор `(private val database: PassKeeperDatabase)`.
- Produces: нові SQL-запити (назви фіксуються нижче — class репозиторію відкривається 1 раз):

```sql
upsertRemoteItem(id, type, ciphertext, nonce, version, deleted, maxSeenVersion, updatedAt);
markItemApplied(id, version, maxSeenVersion, updatedAt);
selectDirtyItems(limit);
insertLosingVersion(itemId, version, ciphertext, nonce, updatedAt);
selectSyncState();              -- id = 'single'
upsertSyncState(cursor, lastSyncAt);
selectOutbox(); insertOutbox(idempotencyKey, body); deleteOutbox();
resetLocalData();               -- DELETE з vault_item, item_history, outbox; sync_state.cursor = NULL
```

Реалізація:
- `dirtyChanges(limit)` — `selectDirtyItems` → мапа рядків → `PendingChange(baseVersion = version)`.
- `applyRemote` — існуючий рядок із збереженим контентом → спочатку `insertLosingVersion` (попередня версія → `item_history`), потім upsert; `max_seen_version = max(старе, remote.version)`; `dirty = 0`.
- `stageOutbox` — серіалізація `List<PendingChange>` → JSON `List<ChangeDto>` (DTO з `api-contract`) → `body`; `pendingOutbox` — зворотний парс.
- `peek` — прямий select по id.

- [ ] **Step 1: failing test** — in-memory SQLite (`JdbcSqliteDriver.IN_MEMORY` + `PassKeeperDatabase.Schema.create(driver)`): roundtrip outbox; applyRemote перезаписує, пише історію, підвищує max_seen_version; dirtyChanges; resetLocalData очищає.
- [ ] **Step 2: verify fails** `./gradlew :shared:data:jvmTest --tests '*SqlDelightSyncStoreTest*'` — FAIL.
- [ ] **Step 3: Implement** — SQL-dsl лише тут (тільки ця @Task2 не торкається `SqlDelightVaultItemRepository`).
- [ ] **Step 4: verify passes.**
- [ ] **Step 5: Commit** `feat(data): sync store over sqldelight with outbox`

---

### Task 3: KtorSyncApi

**Files:**

- Modify: `gradle/libs.versions.toml` — 2 рядки:
  ```toml
  ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
  ktor-serialization-kotlinx-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
  ```
- Modify: `shared/data/build.gradle.kts` — `implementation(libs.ktor.client.core)`, `implementation(libs.ktor.client.content.negotiation)`, `implementation(libs.ktor.serialization.kotlinx.json)`
- Create: `shared/data/src/commonMain/kotlin/com/kwazzar/lockruna/data/sync/KtorSyncApi.kt`
- Test: `shared/data/src/commonTest/kotlin/com/kwazzar/lockruna/data/sync/KtorSyncApiTest.kt`

**Interfaces:**

```kotlin
class KtorSyncApi(
    private val httpClient: HttpClient,   // engine — в native/DI хвилі (OkHttp/Darwin)
    private val baseUrl: String,
    private val accessTokenProvider: AccessTokenProvider,
) : SyncApi
```

- push: `Authorization: Bearer <token>` (token() == null → `Failure(Unauthorized)` без виклику); заголовок `Idempotency-Key` — РІВНО ОДИН на batch, згенерований тут з `IdGenerator.newUuid` (інжектする → конструктор приймає `IdGenerator` четвертим параметром); тіло — `PushRequestDto(changes = мапінг → ChangeDto)`
- DTO-конвенції: `PushRequestDto`, `AppliedChangeDto`, `ConflictDto`, `PushResultDto`, `SyncPageDto`, `EncryptedItemDto`, `ChangeDto`, `ItemTypeDto` — всі існують у `api-contract`.
- Мапінг результатів (кожен випадок): `200` → success; `409` → `IdempotencyConflict`; `429` → `RateLimited(header Retry-After)`; `401` → `Unauthorized`; інші/транспорт → `NetworkUnavailable`.
- `RemoteItem.deleted = ciphertext == null || deletedAt != null`; `updatedAt` — ISO-дата `String` → `Long` epoch millis.

- [ ] **Step 1: failing test** — `ktor-client-mock`: один roundtrip push (перевірка боді + header Idempotency-Key + Bearer) і pull (tombstone → `deleted = true`).
- [ ] **Step 2: verify fails.**
- [ ] **Step 3: Implement** + toml-рядки.
- [ ] **Step 4: verify passes** + `./gradlew ktlintCheck detekt` — BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `feat(data): ktor sync api with idempotency header`

---

### Task 4: SyncNow — use case sync-алгоритму

**Files:**

- Create: `shared/application/src/commonMain/kotlin/com/kwazzar/lockruna/application/sync/SyncNow.kt`
- Test: `shared/application/src/commonTest/kotlin/com/kwazzar/lockruna/application/sync/SyncNowTest.kt`

**Interfaces:**

```kotlin
class SyncNow(
    private val syncApi: SyncApi,
    private val store: SyncStore,
    private val accessTokenProvider: AccessTokenProvider,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
) {
    suspend operator fun invoke(): Result<Unit, AppError>
}
```

Алгоритм (фіксхаунок, кожен крок має сценарій-тест):
1. `token() == null` → `Failure(Unauthorized)`, без мережевого виклику.
2. **Stale reset:** `lastSyncAtMillis() != null && clock.nowMillis() - lastSyncAtMillis > 90 * 24 * 60 * 60 * 1000L` → `store.resetLocalData()` і далі повний pull (cursor null). Консервативно: константа `STALE_AFTER_MILLIS = 90L * 24 * 60 * 60 * 1000`.
3. **Pull-loop:** `pull(cursor)` → до `hasMore == false`; кожен item:
   - `item.version < store.peek(item.id)?.maxSeenVersion` → skip (нічого);
   - інакше → `store.applyRemote(item)`;
   - після кожної затвердженої страницы оновити курсор.
4. `store.markSynced(clock.nowMillis())`.
5. **Push-loop:** доки є outbox або dirty:
   - batch = наявний `pendingOutbox()` АБО (`clearOutbox()` попередньо?) новий ≤ 100 з `dirtyChanges(100)` + `stageOutbox(newKey, batch)`, ключ з `idGenerator.newUuid()`;
   - `syncApi.push(batch.changes)`:
     - успіх: кожен applied → `markItemApplied(id, newVersion, max_seen_version = max(...))`;потім `clearOutbox()`;
     - conflicts → **LWW в resolved:**
       - `server.updatedAtMillis >= local.updatedAtMillis` (peek) → `applyRemote(server)` (dirty=false);
       - інакше (local wins) → на наступній ітерації re-push того ж `PendingChange` з `baseVersion = conflict.server.version` (без re-шифровки — AAD уже `rowVersion+1`); на id максимум 3 re-push, далі строк лишається dirty і цикл завершується;
   - мережева невдача → `Failure(NetworkUnavailable)`, outbox і ключ НЕ чистяться (наступний виклик використає той самий batch).

- [ ] **Step 1: failing tests** — сценарії:
  1. success pull → push (markSynced виклик, поля оновлені);
  2. rollback-skip (serve повертає запис зі старішою версією → applyRemote НЕ викликаний);
  3. LWW remote-wins → applyRemote, dirty=false;
  4. LWW local-wins → re-push з `baseVersion = server.version`, потім applied;
  5. мережа fail → outbox лишається; повторний виклик шле той самий Idempotency-Key і той самий batch (фиксація в `FakeSyncApi`);
  6. >100 dirty → дві пачки по ≤100;
  7. stale reset → перед pull викликано `resetLocalData()`;
  8. token null → `Unauthorized`, api.pull не викликаний.
- [ ] **Step 2: verify fails** `./gradlew :shared:application:jvmTest --tests '*SyncNowTest*'`.
- [ ] **Step 3: Implement** — один use case; LWW/re-push логіка в приватних приватних функціях.
- [ ] **Step 4: verify passes.**
- [ ] **Step 5: Commit** `feat(application): sync engine with outbox, lww and rollback guard`

---

### Task 5: Koin-модуль + архітектурний тест + повна верифікація

**Files:**

- Create: `shared/data/src/commonMain/kotlin/com/kwazzar/lockruna/data/di/SyncModule.kt`
- Create: `architecture-tests/src/test/kotlin/com/kwazzar/lockruna/architecture/SyncLayerTest.kt`

**Interfaces:**

```kotlin
package com.kwazzar.lockruna.data.di

fun syncModule(
    httpClient: HttpClient,
    baseUrl: String,
    accessTokenProvider: AccessTokenProvider,   // реальну реалізацію дає client-auth
) = module {
    single { KtorSyncApi(httpClient, baseUrl, accessTokenProvider, get()) }  // IdGenerator last
    single<SyncApi> { get<KtorSyncApi>() }
    single<SyncStore> { SqlDelightSyncStore(get()) }
    single { SyncNow(get(), get(), get(), get(), get()) }
}
```

Konsist-тест `SyncLayerTest`:
- `shared/domain/sync/**` не імпортує `io.ktor.`, `app.cash.sqldelight.`, `kotlinx.serialization.`;
- `shared/data/sync/**` не імпортує `com.kwazzar.lockruna.presentation.`;
- `shared/application/sync/**` не імпортує `io.ktor.`, `app.cash.sqldelight.`.

- [ ] **Step 1: konsist RED** (пустий файл тесту) → run `./gradlew :architecture-tests:test` — зафіксувати FAIL.
- [ ] **Step 2: модуль + build** → GREEN.
- [ ] **Step 3: повна верифікація:**
  - `./gradlew ktlintCheck detekt`
  - `./gradlew :shared:domain:jvmTest :shared:application:jvmTest :shared:data:jvmTest :architecture-tests:test`
  - `gitleaks detect --source . --no-banner`
  - Expected: BUILD SUCCESSFUL.
- [ ] **Step 4: Commit** `feat(sync): di module and architecture guard`

## Не в обсязі цієї хвилі

- Продукційна реалізація `AccessTokenProvider` і refresh-механіка — у `client-auth`.
- Тригери (unlock, debounce change, network return, periodic WorkManager/BGTaskScheduler) — native-хвиля; `SyncNow` тут викликається вручну/тестами.
- UI-індикатор стану sync — презентаційна хвиля.
- BIP39 recovery — auth-хвиля.

## Порядок злиття

Порядок: **client-presentation → client-sync → client-auth**. Всі три незалежні за логікою.

Правило одного власника на спільні файли (щоб merge був безконфліктним):
- `shared/domain/error/AppError.kt` — власник **client-sync** (додає `IdempotencyConflict` + `EmailAlreadyRegistered`; auth їх лише використовує).
- `gradle/libs.versions.toml` — власник **client-sync** (додає 2 ktor-клієнт рядки; auth їх лише використовує).
- `shared/data/build.gradle.kts` — власник **client-sync** (додає ktor-клієнт залежності; auth їх лише використовує).
- `shared/domain/session/SessionPorts.kt` — власник **client-auth** (`VaultKey.from`), presentation/sync його не чіпають.
- DI-композиція всіх модулів — окрема нативна хвиля.
