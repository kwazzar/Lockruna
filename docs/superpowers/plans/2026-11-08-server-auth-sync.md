# Сервер ідентичності та синхронізації (Server Auth + Sync) — Implementation Plan

> **Для агентних виконавців:** ОБОВ'ЯЗКОВО: використати `superpowers:subagent-driven-development` (рекомендовано) або `superpowers:executing-plans` для виконання цього плану по завданнях. Кроки використовують checkbox (`- [ ]`) для відстеження.

**Мета:** Реалізувати серверну частину MVP за ADR-004: Ktor-структура за bounded contexts, Postgres-схема з Flyway, ендпоінти `/auth/prelogin`, `/auth/register`, `/auth/login`, `/auth/refresh`, `/auth/logout`, `/devices`, `/devices/{deviceId}`, `/sync`, `/sync/push` — з anti-enumeration, rate limiting, ротацією refresh-токенів, ідемпотентністю та конфліктами версій.

**Архітектура:** один модуль `server`, пакети за bounded contexts (`identity`, `sync`, `platform`, `audit`), у кожному — шари `api → application → domain ← infrastructure`. Маршрути не звертаються до репозиторіїв напряму. DTO — у спільному `api-contract`. Контрактні тести звіряють DTO з `docs/openapi.yaml`.

**Технології:** Ktor 3.6.1 (server core/netty/auth-jwt/content-negotiation), Koin 4.2.2, Exposed 1.6.1, Flyway 13.12.1, HikariCP 7.1.0, Lettuce 7.9.0 (Redis), kotlinx.serialization, Testcontainers 2.0.5 (Postgres), Micrometer.

**Специфікація:** `docs/openapi.yaml`, `docs/adr/004-backend.md`, `docs/adr/003-cryptography.md` (HMAC/pepper), `docs/requirements.md` (US-01, US-02, US-09, US-10), `docs/threat-model.md`.

## Global Constraints

- Сервер зберігає **лише** `HMAC(pepper, authKey)` і `HMAC(pepper, recoveryAuthKey)`; сам `authKey`/`recoveryAuthKey`/`vaultKey`/`kek` на сервері не потрапляють ніколи.
- Порівняння хешів — **константного часу**.
- `prelogin` для неіснуючого email повертає **детерміновані фіктивні** `kdfSalt` і `kdfParams` тієї ж форми; `login` для неіснуючого email і невірного пароля дає **ідентичну** 401.
- `POST /auth/register` → 409 для існуючого email (прийнятий ризик, стримується rate limiting).
- Access JWT — 15 хвилин, містить `deviceId`. Refresh — випадковий 256 біт, одноразовий, у базі лише SHA-256; 30 днів; абсолютний ліміт сім'ї — 90 днів; повторне використання відкликає всю сім'ю.
- `Idempotency-Key` обов'язковий для `POST /sync/push`; той самий ключ з іншим тілом → **409**; той самий ключ з тим самим тілом → збережена відповідь.
- `baseVersion` перевіряється умовним `UPDATE`; розбіжність → `conflicts`, HTTP-помилки немає.
- Відповіді помилок — `application/problem+json` зі стабільним полем `code`.
- Тіла запитів/відповіді `/auth` і `/sync` **не логуються**; секрети (`pepper`, JWT-ключ, паролі БД) лише з оточення.
- Кожен запит до БД scoping за `user_id`; жодного запиту без фільтра за власником.
- Ліміт: до 100 змін на push (413), шифротекст до 64 KiB на запис (413).
- Код, коментарі, повідомлення комітів — англійською. `docs/` — українською.
- JDK 25: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.
- Точка входу для перевірки: `./scripts/verify.sh`.

> **Передумова:** інтеграційні тести потребують Docker (Testcontainers піднімає Postgres). Без Docker `:server:test` не пройде — тоді тести запускаються в CI, а локально доступні лише юніт-тести.

## Власність файлів (паралельна робота)

Цей план володіє:

- `server/**`
- `api-contract/src/commonMain/kotlin/com/kwazzar/lockruna/contract/**`
- `api-contract/src/commonTest/kotlin/com/kwazzar/lockruna/contract/**`
- `architecture-tests/src/test/kotlin/com/kwazzar/lockruna/architecture/ServerLayerTest.kt` (новий файл)

`gradle/libs.versions.toml` **не** редагується: усі потрібні залежності (ktor, koin, exposed, flyway, hikari, lettuce, micrometer, postgresql, testcontainers) уже задекларовані. Mongo-драйвер для аудиту — у наступній хвилі; у цьому плані аудит реалізовано портом із тестовою реалізацією.

## Не в обсязі цього плану

- `/auth/recovery/start`, `/auth/recovery/complete`, `DELETE /account` — наступна хвиля (потрібні клієнтські recovery-обгортки).
- Mongo-аудит (зараз — порт `AuditLog` + тестова реалізація).
- Redis як обов'язковий runtime-компонент: rate limiting і denylist мають порт із Redis-реалізацією та in-memory фейком для тестів.
- Клієнтський sync-рушій (належить клієнтському ядру).

## Розглянуті підходи

1. **Один контекст `server` без шарів** — відхилено (ADR-004): маршрути починають звертатися до репозиторіїв, тести стають неізольованими.
2. **Postgres-only rate limiting (лічильники в таблиці)** — відхилено: ADR-004 фіксує Redis-лічильники з TTL.
3. **In-memory rate limiter у production** — відхилено: не витримує кілька інстансів.
4. **Порт `RateLimiter` + Redis-реалізація + in-memory фейк для тестів** — вибрано: тести не потребують Redis-контейнера, а production-семантика лишається як в ADR.

---

### Task 1: Каркас сервера: шари, Ktor, Koin, конфіг, помилки

**Files:**

- Create: `server/src/main/kotlin/com/kwazzar/lockruna/server/platform/config/ServerSettings.kt`
- Create: `server/src/main/kotlin/com/kwazzar/lockruna/server/platform/error/Problem.kt`
- Create: `server/src/main/kotlin/com/kwazzar/lockruna/server/platform/error/StatusPages.kt`
- Create: `server/src/main/kotlin/com/kwazzar/lockruna/server/platform/logging/SafeCallLogging.kt`
- Create: `server/src/main/kotlin/com/kwazzar/lockruna/server/platform/di/ServerModule.kt`
- Modify: `server/src/main/kotlin/com/kwazzar/lockruna/server/Application.kt`
- Test: `server/src/test/kotlin/com/kwazzar/lockruna/server/platform/config/ServerSettingsTest.kt`
- Test: `server/src/test/kotlin/com/kwazzar/lockruna/server/HealthRouteTest.kt`

**Interfaces:**

- Produces:

```kotlin
data class ServerSettings(
    val host: String,
    val port: Int,
    val databaseUrl: String,
    val redisUrl: String,
    val jwtIssuer: String,
    val jwtAudience: String,
    val jwtActiveKey: String,
    val jwtPreviousKey: String?,
    val pepper: ByteArray,
) {
    companion object {
        fun fromEnv(env: Map<String, String> = System.getenv()): ServerSettings
    }
}
```

- `Problem(status: Int, code: String, title: String)` + `StatusPages` — усі помилки як `application/problem+json` з полями `type`, `title`, `status`, `code`.
- `SafeCallLogging` — CallLogging, який пише лише метод, шлях, статус і тривалість; **без** тіл запиту/відповіді.

**Step 1: Write the failing test**

`ServerSettingsTest.kt`: `fromEnv` читає `HOST`, `PORT`, `DATABASE_URL`, `REDIS_URL`, `JWT_ISSUER`, `JWT_AUDIENCE`, `JWT_ACTIVE_KEY`, `JWT_PREVIOUS_KEY`, `PEPPER` (hex → 32 байти); відсутність `PEPPER` → виняток `IllegalStateException` (сервер не стартує без нього).

`HealthRouteTest.kt`: `testApplication { application { module() } }` → `GET /health` повертає 200 і `{"status":"ok"}`.

**Step 2: Run test to verify it fails**

Run: `./gradlew :server:test --tests '*ServerSettingsTest*' --tests '*HealthRouteTest*'`
Expected: FAIL — класів немає.

**Step 3: Implement**

- `ServerSettings.fromEnv` — усі секрети обов'язкові, крім `JWT_PREVIOUS_KEY`; `PEPPER` — hex-рядок, довжина 64 символи.
- `Application.kt`: `fun main()` з `embeddedServer(Netty, port, host) { module() }`; окрема функція `fun Application.module()` — точка входу для тестів.
- Ktor-плагини: `ContentNegotiation` (json), `StatusPages`, `CallLogging` (через `SafeCallLogging`), `install(Koin)` з модулем `ServerModule`.
- `ServerModule` — Koin-модуль, поки що лише `single { ServerSettings.fromEnv() }`; розширюється в наступних завданнях.

**Step 4: Run test to verify it passes**

Run: `./gradlew :server:test --tests '*ServerSettingsTest*' --tests '*HealthRouteTest*'`
Expected: PASS.

**Step 5: Commit**

```bash
git add server
git commit -m "feat(server): add layered skeleton with settings, problem errors and DI"
```

---

### Task 2: Схема БД, Exposed-таблиці та репозиторії

**Files:**

- Create: `server/src/main/resources/db/migration/V1__baseline.sql`
- Create: `server/src/main/kotlin/com/kwazzar/lockruna/server/platform/db/DatabaseFactory.kt`
- Create: `server/src/main/kotlin/com/kwazzar/lockruna/server/identity/domain/UserRepository.kt`
- Create: `server/src/main/kotlin/com/kwazzar/lockruna/server/identity/infrastructure/ExposedUserRepository.kt`
- Create: `server/src/main/kotlin/com/kwazzar/lockruna/server/identity/domain/DeviceRepository.kt`
- Create: `server/src/main/kotlin/com/kwazzar/lockruna/server/identity/infrastructure/ExposedDeviceRepository.kt`
- Create: `server/src/main/kotlin/com/kwazzar/lockruna/server/identity/domain/RefreshTokenRepository.kt`
- Create: `server/src/main/kotlin/com/kwazzar/lockruna/server/identity/infrastructure/ExposedRefreshTokenRepository.kt`
- Create: `server/src/main/kotlin/com/kwazzar/lockruna/server/sync/domain/VaultItemRepository.kt`
- Create: `server/src/main/kotlin/com/kwazzar/lockruna/server/sync/infrastructure/ExposedVaultItemRepository.kt`
- Create: `server/src/main/kotlin/com/kwazzar/lockruna/server/sync/domain/IdempotencyRepository.kt`
- Create: `server/src/main/kotlin/com/kwazzar/lockruna/server/sync/infrastructure/ExposedIdempotencyRepository.kt`
- Test: `server/src/test/kotlin/com/kwazzar/lockruna/server/support/PostgresTest.kt`
- Test: `server/src/test/kotlin/com/kwazzar/lockruna/server/support/MigrationTest.kt`
- Test: `server/src/test/kotlin/com/kwazzar/lockruna/server/identity/infrastructure/UserRepositoryTest.kt`

**Interfaces:**

- Produces (порти в `domain`, реалізації в `infrastructure`):

```kotlin
interface UserRepository {
    suspend fun findByEmail(email: String): UserRecord?
    suspend fun create(user: UserRecord): UserRecord
    suspend fun findById(id: String): UserRecord?
    suspend fun updateKeyMaterial(id: String, authHash: ByteArray, recoveryAuthHash: ByteArray, kdfSalt: ByteArray, wrappedVaultKey: ByteArray): Boolean
}
data class UserRecord(
    val id: String,
    val email: String,
    val authHash: ByteArray,
    val recoveryAuthHash: ByteArray,
    val kdfSalt: ByteArray,
    val kdfParams: String,
    val wrappedVaultKey: ByteArray,
    val recoveryWrappedVaultKey: ByteArray,
    val syncSeq: Long,
)

interface DeviceRepository {
    suspend fun create(device: DeviceRecord): DeviceRecord
    suspend fun listByUser(userId: String): List<DeviceRecord>
    suspend fun revoke(userId: String, deviceId: String): Boolean
    suspend fun findById(deviceId: String): DeviceRecord?
}
data class DeviceRecord(val id: String, val userId: String, val name: String, val platform: String, val createdAt: Instant, val lastSeen: Instant?, val revokedAt: Instant?)

interface RefreshTokenRepository {
    suspend fun insert(token: RefreshTokenRecord): RefreshTokenRecord
    suspend fun findByHash(hash: ByteArray): RefreshTokenRecord?
    suspend fun markUsed(id: String, usedAt: Instant): Boolean
    suspend fun revokeFamily(familyId: String, revokedAt: Instant): Int
}

interface VaultItemRepository {
    suspend fun listSince(userId: String, afterSyncSeq: Long, limit: Int): List<SealedItemRecord>
    suspend fun apply(userId: String, change: ChangeRecord, newSyncSeq: Long, updatedAt: Instant): ApplyResult
    suspend fun currentSyncSeq(userId: String): Long
    suspend fun nextSyncSeq(userId: String): Long
}
data class SealedItemRecord(val userId: String, val id: String, val type: String, val ciphertext: ByteArray?, val nonce: ByteArray?, val version: Long, val syncSeq: Long, val deletedAt: Instant?, val updatedAt: Instant)

interface IdempotencyRepository {
    suspend fun find(userId: String, key: String): StoredResponse?
    suspend fun store(userId: String, key: String, bodyHash: ByteArray, response: String, createdAt: Instant): StoredResponse
}
data class StoredResponse(val bodyHash: ByteArray, val response: String, val createdAt: Instant)
```

**Step 1: Write the failing test**

`MigrationTest.kt` — підняти Postgres через Testcontainers, накатити Flyway-міграції, перевірити наявність таблиць `users`, `devices`, `refresh_tokens`, `vault_items`, `idempotency_keys` та їхніх індексів.

`UserRepositoryTest.kt` — `create` + `findByEmail` (case-insensitive за `lower(email)`) + `findByEmail` для неіснуючого email → `null`.

**Step 2: Run test to verify it fails**

Run: `./gradlew :server:test --tests '*MigrationTest*' --tests '*UserRepositoryTest*'`
Expected: FAIL — міграції та репозиторіїв немає. Якщо Docker відсутній — тест падає з помилкою підключення; це очікувано, зафіксувати в звіті і перейти до наступного кроку.

**Step 3: Implement the migration**

`V1__baseline.sql` — дослівно схему з додатка ADR-004: `users` (з `uq_users_email` на `lower(email)`), `devices`, `refresh_tokens` (+ `idx_refresh_family`), `vault_items` (складений PK `(user_id, id)`, `CHECK` на tombstone), `idempotency_keys`; `ON DELETE CASCADE` на FK.

**Step 4: Implement the repositories**

Exposed-таблиці + реалізації портів. Ключові деталі:

- `findByEmail` — `lower(email) = lower(:email)`.
- `apply` — умовний `UPDATE ... WHERE user_id = :userId AND id = :id AND version = :baseVersion`; для `baseVersion = 0` — `INSERT ... ON CONFLICT DO NOTHING`; 0 змінених рядків → `ApplyResult.Conflict(serverRecord)`.
- `nextSyncSeq` — `UPDATE users SET sync_seq = sync_seq + 1 WHERE id = :userId RETURNING sync_seq` (блокує рядок до коміту → послідовні нумерації).
- `store` — `INSERT ... ON CONFLICT DO NOTHING`, потім `find` (захист від дублікату).

**Step 5: Run test to verify it passes**

Run: `./gradlew :server:test --tests '*MigrationTest*' --tests '*UserRepositoryTest*'`
Expected: PASS (за наявності Docker).

**Step 6: Commit**

```bash
git add server
git commit -m "feat(server): add Postgres schema and Exposed repositories"
```

---

### Task 3: Identity API — prelogin, register, login

**Files:**

- Create: `api-contract/src/commonMain/kotlin/com/kwazzar/lockruna/contract/AuthDtos.kt`
- Create: `server/src/main/kotlin/com/kwazzar/lockruna/server/identity/domain/RateLimiter.kt`
- Create: `server/src/main/kotlin/com/kwazzar/lockruna/server/identity/domain/AuthKeys.kt`
- Create: `server/src/main/kotlin/com/kwazzar/lockruna/server/identity/application/RegisterUser.kt`
- Create: `server/src/main/kotlin/com/kwazzar/lockruna/server/identity/application/LoginUser.kt`
- Create: `server/src/main/kotlin/com/kwazzar/lockruna/server/identity/api/IdentityRoutes.kt`
- Create: `server/src/main/kotlin/com/kwazzar/lockruna/server/platform/redis/RedisRateLimiter.kt`
- Test: `server/src/test/kotlin/com/kwazzar/lockruna/server/identity/IdentityApiTest.kt`
- Test: `server/src/test/kotlin/com/kwazzar/lockruna/server/support/InMemoryRateLimiter.kt`

**Interfaces:**

- Produces (DTO в `api-contract`, 1:1 до `openapi.yaml`):

```kotlin
@Serializable data class PreloginRequest(val email: String)
@Serializable data class PreloginResponse(@SerialName("kdfSalt") val kdfSalt: String, val kdfParams: KdfParamsDto)
@Serializable data class RegisterRequest(
    val email: String,
    val authKey: String,
    val recoveryAuthKey: String,
    val kdfSalt: String,
    val kdfParams: KdfParamsDto,
    val wrappedVaultKey: String,
    val recoveryWrappedVaultKey: String,
    val device: DeviceInfoDto,
)
@Serializable data class LoginRequest(val email: String, val authKey: String, val device: DeviceInfoDto)
@Serializable data class DeviceInfoDto(val deviceId: String? = null, val name: String, val platform: String)
@Serializable data class TokenPairDto(val accessToken: String, val refreshToken: String, val expiresIn: Int)
@Serializable data class AuthResponseDto(
    val accessToken: String, val refreshToken: String, val expiresIn: Int,
    val userId: String, val deviceId: String, val kdfSalt: String,
    val kdfParams: KdfParamsDto, val wrappedVaultKey: String,
)
```

- `interface RateLimiter { suspend fun check(scope: String, key: String, limit: Int, window: Duration): RateLimitDecision }` — `RateLimitDecision.Allowed` / `.Blocked(retryAfterSeconds: Int)`.
- `object AuthKeys { fun hash(pepper: ByteArray, key: ByteArray): ByteArray; fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean }` — HMAC-SHA256 через JDK `Mac`, порівняння — `MessageDigest.isEqual`.

**Step 1: Write the failing test**

`IdentityApiTest.kt` (Postgres у Testcontainers):

- `prelogin` для невідомого email → 200, `kdfSalt` — 16 байт base64, `kdfParams` = `{alg: argon2id, m: 65536, t: 3, p: 1}`; **другий виклик дає той самий salt** (детерміновано).
- `prelogin` для відомого email → 200 із реальним salt користувача.
- `register` для нового email → 201, `userId` і `deviceId` — UUID; у БД лежить `HMAC(pepper, authKey)`, **не** сам `authKey`.
- `register` для існуючого email → 409 з `code = "email_already_registered"`.
- `login` з правильним `authKey` → 200, `accessToken` розбирається, `deviceId` всередині.
- `login` з неправильним `authKey` → 401 `code = "invalid_credentials"`.
- `login` для неіснуючого email → 401 з **тим самим** кодом і **тим самим** текстом тіла.
- 6 невдалих `login` з однієї пари email+IP → 429 з `Retry-After`.

**Step 2: Run test to verify it fails**

Run: `./gradlew :server:test --tests '*IdentityApiTest*'`
Expected: FAIL — маршрутів немає.

**Step 3: Implement the DTOs and the domain helpers**

DTO — точно за схемами `openapi.yaml`; байтові поля — base64 (рядки `format: byte`).

`AuthKeys.hash` — `Mac.getInstance("HmacSHA256")` з ключем `pepper`, `doFinal(key)`. `constantTimeEquals` — `MessageDigest.isEqual`.

`RateLimiter` — порт у `identity/domain`; `RedisRateLimiter` — інкрементація ключа в Redis з TTL (ключ `rl:<scope>:email:<hash(email)>` / `rl:<scope>:ip:<ip>`; hash email — SHA-256, сирий email у ключі не пишемо). Для тестів — `InMemoryRateLimiter` у `server/src/test/.../support/`.

**Step 4: Implement the routes**

- `POST /auth/prelogin`: знайти користувача за email; якщо немає — вивести фіктивні `salt = HMAC(serverSecret, email)[0..16]` і дефолтні params. `serverSecret` — окремий env `AUTH_FAKE_SECRET` (у складі `ServerSettings`, додати в Task 3).
- `POST /auth/register`: перевірити rate limit (3/год на email, 10/год на IP) → якщо email існує → 409; інакше створити `users` + `devices`, випустити токени (див. Task 4; поки повертати `AuthResponseDto` із заглушковими токенами й замінити в Task 4).
- `POST /auth/login`: rate limit (5/15 хв на пару email+IP) → знайти користувача; якщо немає або `HMAC` не збігається → **однакова** 401; інакше створити/прив'язати пристрій і випустити токени.

**Step 5: Run test to verify it passes**

Run: `./gradlew :server:test --tests '*IdentityApiTest*'`
Expected: PASS.

**Step 6: Commit**

```bash
git add api-contract server
git commit -m "feat(server): add prelogin, register and login with anti-enumeration"
```

---

### Task 4: Токени, refresh-ротація та керування пристроями

**Files:**

- Create: `api-contract/src/commonMain/kotlin/com/kwazzar/lockruna/contract/DeviceDtos.kt`
- Create: `server/src/main/kotlin/com/kwazzar/lockruna/server/identity/domain/TokenService.kt`
- Create: `server/src/main/kotlin/com/kwazzar/lockruna/server/identity/application/RefreshSession.kt`
- Create: `server/src/main/kotlin/com/kwazzar/lockruna/server/identity/application/RevokeDevice.kt`
- Create: `server/src/main/kotlin/com/kwazzar/lockruna/server/identity/api/DeviceRoutes.kt`
- Modify: `server/src/main/kotlin/com/kwazzar/lockruna/server/identity/api/IdentityRoutes.kt`
- Modify: `server/src/main/kotlin/com/kwazzar/lockruna/server/platform/di/ServerModule.kt`
- Test: `server/src/test/kotlin/com/kwazzar/lockruna/server/identity/TokenFlowTest.kt`
- Test: `server/src/test/kotlin/com/kwazzar/lockruna/server/identity/DeviceApiTest.kt`

**Interfaces:**

- Produces:

```kotlin
@Serializable data class DeviceDto(val id: String, val name: String, val platform: String, val lastSeen: String? = null, val current: Boolean)

interface TokenService {
    fun issueAccessToken(userId: String, deviceId: String): String
    suspend fun issueRefreshToken(userId: String, deviceId: String, familyId: String, familyCreatedAt: Instant): String
    fun verifyAccessToken(token: String): AccessTokenClaims
}
data class AccessTokenClaims(val userId: String, val deviceId: String)
```

- `POST /auth/refresh` (body: `{"refreshToken": "..."}`) → `TokenPairDto`.
- `POST /auth/logout` → 204, відкликає токени поточного пристрою.
- `GET /devices` → `List<DeviceDto>`.
- `DELETE /devices/{deviceId}` → 204.

**Step 1: Write the failing test**

`TokenFlowTest.kt`:

- `register` → `login` → `refresh` повертає **новий** `refreshToken`, старий стає `used_at`; повторний `refresh` зі старим токеном → 401 і **вся сім'я** відкликана (наступний refresh з нового токена теж 401).
- Refresh старший за 90 днів → 401 (`family_created_at`).
- Access JWT містить `deviceId` і протухає через 15 хв (`expiresIn == 900`).

`DeviceApiTest.kt`:

- `GET /devices` повертає список пристроїв користувача, `current = true` для того, хто виконав запит.
- `DELETE /devices/{deviceId}` → 204, після чого access-токен цього пристрою дає 401 **негайно** (denylist у Redis, TTL 15 хв).
- **IDOR:** `DELETE /devices/{ чужий deviceId }` → 404, чужий пристрій **не** відкликається; `GET /devices` не показує чужих пристроїв.

**Step 2: Run test to verify it fails**

Run: `./gradlew :server:test --tests '*TokenFlowTest*' --tests '*DeviceApiTest*'`
Expected: FAIL — сервісів токенів немає.

**Step 3: Implement**

- `TokenService.issueAccessToken` — Ktor JWT (`auth-jwt` вже в залежностях), issuer/audience з `ServerSettings`, `expiresIn = 900`, claim `deviceId`.
- `issueRefreshToken` — 32 випадкових байти (base64url), у БД — **лише** SHA-256, `family_id` (UUID), `family_created_at`, `expires_at = +30 днів`.
- `RefreshSession` — знайти токен за hash; якщо `used_at != null` або `revoked_at != null` → відкликати всю сім'ю (`revokeFamily`) і повернути 401; якщо `family_created_at + 90 днів < now` → 401; інакше пометити використаним і випустити нову пару з тією ж `family_id`.
- Denylist відкликаних пристроїв — `denylist:device:<id>` у Redis, TTL 15 хв; JWT-плагін перевіряє denylist перед прийняттям запиту.
- `DELETE /devices/{deviceId}` — `WHERE user_id = :currentUserId AND id = :deviceId` (scoping за власником → для чужого ID 0 рядків → 404).

**Step 4: Run test to verify it passes**

Run: `./gradlew :server:test --tests '*TokenFlowTest*' --tests '*DeviceApiTest*'`
Expected: PASS.

**Step 5: Commit**

```bash
git add api-contract server
git commit -m "feat(server): add token rotation, device management and revocation"
```

---

### Task 5: Sync API — курсор, push, ідемпотентність, конфлікти

**Files:**

- Create: `api-contract/src/commonMain/kotlin/com/kwazzar/lockruna/contract/SyncDtos.kt`
- Create: `server/src/main/kotlin/com/kwazzar/lockruna/server/sync/domain/Cursor.kt`
- Create: `server/src/main/kotlin/com/kwazzar/lockruna/server/sync/application/PushChanges.kt`
- Create: `server/src/main/kotlin/com/kwazzar/lockruna/server/sync/application/PullChanges.kt`
- Create: `server/src/main/kotlin/com/kwazzar/lockruna/server/sync/api/SyncRoutes.kt`
- Test: `server/src/test/kotlin/com/kwazzar/lockruna/server/sync/SyncApiTest.kt`

**Interfaces:**

- Produces:

```kotlin
@Serializable data class EncryptedItemDto(
    val id: String, val type: String, val ciphertext: String? = null, val nonce: String? = null,
    val version: Long, val deletedAt: String? = null, val updatedAt: String,
)
@Serializable data class ChangeDto(
    val id: String, val type: String, val ciphertext: String? = null, val nonce: String? = null,
    val baseVersion: Long, val deleted: Boolean = false,
)
@Serializable data class PushRequestDto(val changes: List<ChangeDto>)
@Serializable data class AppliedChangeDto(val id: String, val version: Long, val updatedAt: String)
@Serializable data class ConflictDto(val id: String, val server: EncryptedItemDto)
@Serializable data class PushResultDto(val applied: List<AppliedChangeDto>, val conflicts: List<ConflictDto>)
@Serializable data class SyncPageDto(val items: List<EncryptedItemDto>, val nextCursor: String, val hasMore: Boolean)
```

- `object Cursor { fun encode(syncSeq: Long): String; fun decode(cursor: String?): Long }` — непрозорий cursor (base64url від `sync_seq`), `decode(null) = 0`.
- `POST /sync/push` — обов'язковий заголовок `Idempotency-Key` (UUID).

**Step 1: Write the failing test**

`SyncApiTest.kt`:

- `push` без `Idempotency-Key` → 400 (`code = "idempotency_key_required"`).
- `push` 101 зміни → 413.
- `push` запису з `baseVersion = 0` → `applied` із `version = 1`; повторний `push` з тим самим ключем і тілом → **те саме** тіло відповіді.
- Той самий `Idempotency-Key` з **іншим** тілом → 409 (`code = "idempotency_key_reuse"`).
- `push` з `baseVersion = 0`, коли запис уже існує → `conflicts` із серверною версією, HTTP 200.
- `push` двох пристроїв з однаковим `baseVersion` → рівно один `applied`, другий у `conflicts` (concurrency-тест: дві coroutine-паралельно).
- `GET /sync` без cursor → усі записи, включно з tombstone; `nextCursor` непрозорий; наступний запит із cursor → порожній список, `hasMore = false`.
- **IDOR:** користувач B не бачить записи користувача A в `GET /sync` і не може змінити запис A через `push` (конфлікт, не зміна).

**Step 2: Run test to verify it fails**

Run: `./gradlew :server:test --tests '*SyncApiTest*'`
Expected: FAIL — маршрутів sync немає.

**Step 3: Implement the routes**

- `POST /sync/push`: перевірити `Idempotency-Key` і ліміт 100 змін; обчислити `bodyHash = SHA-256(body)`; якщо ключ існує з тим самим hash → повернути збережену відповідь; з іншим hash → 409; інакше — в **одній транзакції**: для кожної зміни `apply(...)`, зібрати `applied`/`conflicts`, зберегти відповідь в `idempotency_keys`, закомітити.
- Версія нової зміни = `baseVersion + 1`; `sync_seq` з `users` зростає **в тій самій транзакції**; tombstone → `deleted_at` заповнюється, `ciphertext`/`nonce` очищаються.
- `GET /sync`: `cursor` → `sync_seq`; віддати записи `WHERE user_id = :currentUserId AND sync_seq > :cursor ORDER BY sync_seq LIMIT :limit`; `nextCursor` = останній `sync_seq` (або вхідний, якщо порожньо); `hasMore` = існують записи з більшим `sync_seq`.

**Step 4: Run test to verify it passes**

Run: `./gradlew :server:test --tests '*SyncApiTest*'`
Expected: PASS.

**Step 5: Add the architecture test**

`architecture-tests/src/test/kotlin/com/kwazzar/lockruna/architecture/ServerLayerTest.kt` — Konsist: у `server/.../identity/api/` та `server/.../sync/api/` немає імпортів `Exposed`, `Hikari`, `Lettuce`; у `server/.../domain/` немає імпортів `io.ktor.*`, `org.jetbrains.exposed.*`, `io.lettuce.*`; у `server/.../infrastructure/` немає імпортів `io.ktor.server.routing.*`.

Run: `./gradlew :architecture-tests:test`
Expected: PASS.

**Step 6: Run the full verification**

Run: `./gradlew ktlintCheck detekt` і `./scripts/verify.sh`
Expected: BUILD SUCCESSFUL / «All checks passed».

**Step 7: Commit**

```bash
git add api-contract server architecture-tests
git commit -m "feat(server): add sync pull and push with idempotency and conflicts"
```

---

## Порядок злиття

`feature/server-auth-sync` **не залежить** від інших двох планів і зливається першим або останнім — конфліктів файлів немає (`server/**`, `api-contract/**` не перетинаються з `shared/domain/crypto/**` і `shared/data/crypto/**`).

Після злиття всіх трьох — виконати `graphify update .` один раз у `main` і закомітити оновлення `graphify-out/`.
