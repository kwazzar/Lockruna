# Сервер: відновлення доступу та видалення акаунта (Server Recovery + Account) — Implementation Plan

> **Для агентних виконавців:** ОБОВ'ЯЗКОВО: використати `superpowers:subagent-driven-development` (рекомендовано) або `superpowers:executing-plans` для виконання цього плану по завданнях. Кроки використовують checkbox (`- [ ]`) для відстеження.

**Воркспейс:** гілка `feature/server-recovery-account` від `main`. Володіє: `api-contract/**`, `server/**`, `docs/openapi.yaml` (тільки перевірка відповідності). `shared/**`, `androidApp/**`, `iosApp/**` — не редагуються.

**Goal:** Реалізувати серверну частину US-03 і US-11: `/auth/recovery/start`, `/auth/recovery/complete`, `DELETE /account` за `docs/openapi.yaml` і ADR-004, з anti-enumeration, rate limiting, ротацією/відкликанням пристроїв і каскадним видаленням.

**Architecture:** У bounded context `identity` додаються use cases `StartRecovery`, `CompleteRecovery`, `DeleteAccount`, що використовують наявні порти `UserRepository`, `DeviceRepository`, `RefreshTokenRepository`. DTO — у спільному `api-contract`. Маршрути додаються в `IdentityRoutes.kt`. Фіктивні дані для неіснуючого email виводяться через `authFakeSecret`, як у `PreloginUser`. Аудит-події (ADR-004, Mongo) **поза обсягом цієї хвилі** — інфраструктури Mongo в репо немає; операції логуються структуровано без ключового матеріалу.

**Tech Stack:** Kotlin 2.4.20, Ktor 3.6.1, Exposed 1.6.1, Flyway 13.12.1, HikariCP, Redis (Lettuce), Testcontainers (Postgres), kotlinx.serialization.

**Spec:** `docs/requirements.md` (US-03, US-11), `docs/openapi.yaml` (`/auth/recovery/*`, `/account`), `docs/adr/004-backend.md` (§2, §3, §4, §6), `docs/threat-model.md`.

## Global Constraints

- Сервер зберігає лише `HMAC(pepper, authKey)` і `HMAC(pepper, recoveryAuthKey)`; `authKey`/`recoveryAuthKey`/`vaultKey`/`kek` на сервері не потрапляють ніколи.
- Порівняння хешів — константного часу (`AuthKeys.constantTimeEquals`).
- `recovery/start` для неіснуючого email повертає фіктивні `userId` і `recoveryWrappedVaultKey` тієї ж форми; в обох гілках — однаковий обсяг роботи.
- `recovery/complete` і `delete` повертають ідентичну 401 для невірних ключів; не розрізняють «немає акаунта» й «невірний ключ» за часом.
- `Idempotency-Key` не потрібен для цих ендпоінтів.
- Відповіді помилок — `application/problem+json` зі стабільним `code`.
- Кожен запит до БД scoping за `user_id`; жодного запиту без фільтра за власником.
- Тіла запитів/відповідей не логуються; секрети (`pepper`, JWT-ключ, паролі БД) лише з оточення.
- Код/коментарі/коміти англійською; docs/ українською.
- JDK 25: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.
- Локально потрібно `DOCKER_HOST=unix:///Users/nazar/.colima/default/docker.sock` і `TESTCONTAINERS_RYUK_DISABLED=true` (Colima).
- Точка входу: `./scripts/verify.sh` (серверні тести потребують Docker/Testcontainers).

## Review Focus

- `recovery/start` для існуючого й неіснуючого email дає однакову форму відповіді й однаковий час; тест на anti-enumeration.
- Повторне використання невірного `recoveryAuthKey` не відкриває, чи існує акаунт.
- `recovery/complete` видає токени лише після перевірки `recoveryAuthKey`; старі пристрої й токени відкликаються.
- `DELETE /account` перевіряє `authKey` у сталому часі; каскад видаляє всі дані користувача; повторний запит дає 401.
- Rate limiting рахується за парою email+IP, як для входу.
- Фіктивний `userId` для неіснуючого email детермінований (HMAC), щоб клієнт міг використати його як AAD.

## File Structure

- Modify: `api-contract/src/commonMain/kotlin/com/kwazzar/lockruna/contract/AuthDtos.kt` — додати `RecoveryStartResponseDto`, `RecoveryCompleteRequest`, `DeleteAccountRequest`.
- Modify: `server/src/main/kotlin/com/kwazzar/lockruna/server/identity/domain/UserRepository.kt` — `deleteById(id)`.
- Modify: `server/src/main/kotlin/com/kwazzar/lockruna/server/identity/domain/DeviceRepository.kt` — `revokeAllForUser(userId, exceptDeviceId, revokedAt)`.
- Modify: `server/src/main/kotlin/com/kwazzar/lockruna/server/identity/domain/RefreshTokenRepository.kt` — `revokeAllForUser(userId, revokedAt)`.
- Create: `server/src/main/kotlin/com/kwazzar/lockruna/server/identity/application/StartRecovery.kt`
- Create: `server/src/main/kotlin/com/kwazzar/lockruna/server/identity/application/CompleteRecovery.kt`
- Create: `server/src/main/kotlin/com/kwazzar/lockruna/server/identity/application/DeleteAccount.kt`
- Modify: `server/src/main/kotlin/com/kwazzar/lockruna/server/identity/api/IdentityRoutes.kt` — маршрути + DTO-мапінг.
- Modify: `server/src/main/kotlin/com/kwazzar/lockruna/server/platform/di/ServerModule.kt` — реєстрація use cases.
- Modify: Exposed-реалізації `ExposedUserRepository`, `ExposedDeviceRepository`, `ExposedRefreshTokenRepository`.
- Test: `server/src/test/kotlin/com/kwazzar/lockruna/server/identity/RecoveryApiTest.kt`, `AccountApiTest.kt`.

## Task 1: Contract DTOs

**Files:**
- Modify: `api-contract/src/commonMain/kotlin/com/kwazzar/lockruna/contract/AuthDtos.kt`
- Test: `api-contract/src/commonTest/.../AuthDtosTest.kt` (якщо є; інакше додати серіалізаційний тест)

**Interfaces:**
- Produces: `RecoveryStartResponseDto(userId: String, recoveryWrappedVaultKey: ByteArray)` (base64 у JSON як String), `RecoveryCompleteRequest(email, recoveryAuthKey, newAuthKey, newKdfSalt, newKdfParams, newWrappedVaultKey, device)`, `DeleteAccountRequest(authKey)`.

- [ ] **Step 1:** Додати DTO за схемами `openapi.yaml` (`RecoveryStartResponse`, `RecoveryCompleteRequest`), серіалізацію base64 для байтів.
- [ ] **Step 2:** Тест round-trip серіалізації.
- [ ] **Step 3:** Commit: `feat(contract): add recovery and account deletion DTOs`.

## Task 2: Репозиторії — delete / revoke all

**Files:**
- Modify: `server/.../identity/domain/UserRepository.kt`, `DeviceRepository.kt`, `RefreshTokenRepository.kt`
- Modify: `server/.../identity/infrastructure/ExposedUserRepository.kt`, `ExposedDeviceRepository.kt`, `ExposedRefreshTokenRepository.kt`

**Interfaces:**
- Consumes: наявні `UserRecord`, `DeviceRecord`, `RefreshTokenRecord`.
- Produces: `UserRepository.deleteById(id): Boolean`; `DeviceRepository.revokeAllForUser(userId, exceptDeviceId: String?, revokedAt): Int`; `RefreshTokenRepository.revokeAllForUser(userId, revokedAt): Int`.

- [ ] **Step 1:** Додати сигнатури в інтерфейси.
- [ ] **Step 2:** Реалізація в Exposed (DELETE users WHERE id = ?; UPDATE devices SET revoked_at = ? WHERE user_id = ? AND (except IS NULL OR id <> ?); UPDATE refresh_tokens SET revoked_at = ? WHERE user_id = ? AND revoked_at IS NULL).
- [ ] **Step 3:** Юніт/інтеграційний тест (Testcontainers) на каскад.
- [ ] **Step 4:** Commit: `feat(server): add delete and revoke-all repository methods`.

## Task 3: StartRecovery

**Files:**
- Create: `server/.../identity/application/StartRecovery.kt`

**Interfaces:**
- Consumes: `UserRepository`, `RateLimiter`, `authFakeSecret: ByteArray`.
- Produces: `StartRecovery(email, clientIp): RecoveryStartResult(userId: String, recoveryWrappedVaultKey: ByteArray)`.

- [ ] **Step 1:** Написати тест: існуючий email → реальні `userId`/`recoveryWrappedVaultKey`; неіснуючий → фіктивні, але тієї ж форми; rate limit 5/15 хв.
- [ ] **Step 2:** Реалізувати: rate limit email+IP; якщо user == null → `userId = UUID.nameUUIDFromBytes(AuthKeys.hash(authFakeSecret, email.toByteArray()))` (або HMAC-derived), `recoveryWrappedVaultKey = AuthKeys.hash(authFakeSecret, email.toByteArray()).copyOf(64)`; інакше реальні поля.
- [ ] **Step 3:** Перевірити anti-enumeration (час/форма) тестом.
- [ ] **Step 4:** Commit: `feat(server): implement recovery start with anti-enumeration`.

## Task 4: CompleteRecovery

**Files:**
- Create: `server/.../identity/application/CompleteRecovery.kt`

**Interfaces:**
- Consumes: `UserRepository`, `DeviceRepository`, `RefreshTokenRepository`, `TokenService`, `RateLimiter`, `TransactionRunner`, `ServerSettings`, `Clock`.
- Produces: `CompleteRecovery(command): AuthSession` (новий пристрій + токени).

- [ ] **Step 1:** Тест: невірний `recoveryAuthKey` → 401; вірний → оновлені key material, нові токени, старі пристрої відкликано.
- [ ] **Step 2:** Реалізувати: rate limit; знайти user за email; якщо немає або `AuthKeys.constantTimeEquals(AuthKeys.hash(pepper, recoveryAuthKey), user.recoveryAuthHash)` false → 401; у транзакції: `updateKeyMaterial` (authHash, recoveryAuthHash без змін, kdfSalt, wrappedVaultKey), створити пристрій, випустити токени, `revokeAllForUser` (крім нового пристрою) + `revokeAllForUser` токени.
- [ ] **Step 3:** Тест на відкликання старих токенів.
- [ ] **Step 4:** Commit: `feat(server): implement recovery complete`.

## Task 5: DeleteAccount

**Files:**
- Create: `server/.../identity/application/DeleteAccount.kt`

**Interfaces:**
- Consumes: `UserRepository`, `DeviceRepository`, `RefreshTokenRepository`, `RateLimiter`, `TransactionRunner`, `ServerSettings`.
- Produces: `DeleteAccount(userId, authKey): Unit` (кидає 401 при невірному ключі).

- [ ] **Step 1:** Тест: невірний `authKey` → 401; вірний → 204 і каскадне видалення.
- [ ] **Step 2:** Реалізувати: знайти user за userId; перевірити `AuthKeys.constantTimeEquals(AuthKeys.hash(pepper, authKey), user.authHash)`; при невідповідності 401; `users.deleteById(userId)` (FK cascade).
- [ ] **Step 3:** Очистка Redis-ключів rate limit і denylist для цього user (якщо є).
- [ ] **Step 4:** Commit: `feat(server): implement account deletion`.

## Task 6: Маршрути та DI

**Files:**
- Modify: `server/.../identity/api/IdentityRoutes.kt`
- Modify: `server/.../platform/di/ServerModule.kt`

- [ ] **Step 1:** Додати `POST /auth/recovery/start`, `POST /auth/recovery/complete`, `DELETE /account` з мапінгом DTO ↔ команди, rate limit, status pages.
- [ ] **Step 2:** Зареєструвати use cases в Koin.
- [ ] **Step 3:** Інтеграційні тести `RecoveryApiTest`, `AccountApiTest` (anti-enumeration, cascade, 401).
- [ ] **Step 4:** Commit: `feat(server): wire recovery and account deletion routes`.

## Task 7: Перевірка

- [ ] **Step 1:** `DOCKER_HOST=... TESTCONTAINERS_RYUK_DISABLED=true ./gradlew :server:test`.
- [ ] **Step 2:** Перевірити, що `openapi.yaml` і DTO збігаються (контрактний тест, якщо є).
- [ ] **Step 3:** `gitleaks detect --source . --no-banner`.
- [ ] **Step 4:** Commit: `docs(server): update graphify` (якщо змінювались).

## Spec coverage

US-03 (серверна частина), US-11 (серверна частина), ADR-004 §2/§3/§4/§6. Аудит-події (ADR-004, Mongo) — свідомо відкладено: інфраструктури Mongo в репо немає. Клієнтська частина — WS2/WS3.
