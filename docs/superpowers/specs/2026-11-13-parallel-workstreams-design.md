# Паралельні worktree для history, audit і завершення планів — дизайн

**Статус:** погоджено  
**Дата:** 2026-11-13  
**База:** `main` (`d324e2b`)

## Мета

Розділити роботу так, щоб незалежні реалізації виконувалися в ізольованих worktree, а bookkeeping не виглядав як виконана робота без доказів.

## Рішення щодо публічного контракту

Серверна `item_history` у цій хвилі є **внутрішнім архівом PostgreSQL**. Вона не має endpoint, OpenAPI-схем, DTO або клієнтського UI. API та UI залишаються окремою наступною хвилею.

## Підходи

1. **Дві паралельні implementation-гілки та фінальна bookkeeping-гілка — обрано.**
   - `feature/us-05-item-history-core` і `feature/us-11-audit-mongo` стартують паралельно від однакового `main`.
   - `chore/plan-completion-bookkeeping` створюється після їх merge, щоб виправити реальні прогалини та зібрати ручні докази на фінальному кандидаті.
2. **Три однаково паралельні гілки — відхилено.**
   - Bookkeeping залежить від коду та ручних доказів.
   - Mongo audit і history конкурують за `PushChanges.kt`, `ServerModule.kt` та тести.
3. **Повністю послідовне виконання — відхилено.**
   - Воно простіше, але не використовує незалежність history та audit.

## Worktree A — Item history core

**Гілка:** `feature/us-05-item-history-core`

### Мета

- Додати серверну внутрішню таблицю `item_history` та архівувати попередню запечатану версію під час успішного оновлення або видалення.
- Зберігати максимум 10 історичних версій на запис.
- Виправити локальну історію так, щоб для кожного рядка зберігалися `type` та фактичний `aad_version`; `aad_version` дорівнює `version` для чистого запису та `version + 1` для dirty-запису.
- Не змінювати публічний API та UI.

### Власність

- `server/src/main/resources/db/migration/V2__item_history.sql`
- `server/src/main/kotlin/com/kwazzar/lockruna/server/platform/db/Tables.kt`
- `server/src/main/kotlin/com/kwazzar/lockruna/server/sync/domain/VaultItemRepository.kt`
- `server/src/main/kotlin/com/kwazzar/lockruna/server/sync/infrastructure/ExposedVaultItemRepository.kt`
- `shared/data/src/commonMain/sqldelight/com/kwazzar/lockruna/data/db/PassKeeperDatabase.sq` та SQLDelight migration
- `shared/data/src/commonMain/kotlin/com/kwazzar/lockruna/data/db/SqlDelightVaultItemRepository.kt`
- `shared/data/src/commonMain/kotlin/com/kwazzar/lockruna/data/sync/SqlDelightSyncStore.kt`
- відповідні тести та `docs/adr/004-backend.md`

`PushChanges.kt`, `ServerModule.kt`, `api-contract/**`, `docs/openapi.yaml`, presentation та native UI не входять до ownership Worktree A.

### Ключові інваріанти

- Архівується лише версія, яку сервер реально замінив; conflict не створює history row.
- History/archive/current-state зміни комітяться атомарно.
- Історія не перевищує 10 версій на запис.
- Видалення акаунта каскадно видаляє серверну історію.
- Історичний `type` та `aad_version` достатні для правильного AAD; `dirty` не відновлюється як «відсутній» ефективний номер версії.

### Відомий ризик

Conflict retry у `SyncNow` зараз змінює `baseVersion`, але не перешифровує ciphertext під новий server version. До завершення history foundation цей шлях не можна вважати коректним: або ciphertext перешифровується під target version, або повторний push відхиляється. Цей root-cause fix входить у Worktree A, бо інакше збережена history може бути недешифрованою.

## Worktree B — Mongo audit

**Гілка:** `feature/us-11-audit-mongo`

### Мета

- Додати MongoDB runtime wiring, pinned driver, `MONGODB_URI` та Compose-сервіс.
- Реалізувати audit vertical slice за ADR-004: domain event, repository port, Mongo adapter та DI.
- Записувати визначені ADR-004 дії без ключових матеріалів, plaintext, ciphertext чи nonce.
- Зберігати `account.delete` після каскадного видалення акаунта з PostgreSQL.

### Власність

- `gradle/libs.versions.toml`, `server/build.gradle.kts`, `docker-compose.yml`
- `server/src/main/kotlin/com/kwazzar/lockruna/server/audit/**`
- `server/src/main/kotlin/com/kwazzar/lockruna/server/platform/config/ServerSettings.kt`
- `server/src/main/kotlin/com/kwazzar/lockruna/server/platform/di/ServerModule.kt`
- `server/src/main/kotlin/com/kwazzar/lockruna/server/Application.kt`
- server identity/sync use cases, яким потрібні actor device, IP та audit action
- server audit tests та fake

`api-contract/**`, `docs/openapi.yaml`, `shared/**`, native UI та PostgreSQL migration не входять до ownership Worktree B.

### Ключові інваріанти

- Audit failure не змінює результат auth/sync request.
- Події не містять auth/recovery/vault keys, key hashes, plaintext, item ciphertext або nonce.
- Runtime collection має індекс `{userId: 1, ts: -1}` і TTL 365 днів.
- Idempotent sync replay не створює повторний `sync.push`.
- Тестовий MongoDB використовує наявний `testcontainers-mongodb`; runtime driver є окремою pinned dependency.

## Worktree C — Plan completion та bookkeeping

**Гілка:** `chore/plan-completion-bookkeeping`

### Мета

Створити після merge Worktree A/B, щоб усе bookkeeping відповідало фактичному стану.

### Робота

- Закрити 9 реальних прогалин, а не лише checkbox:
  1. contract DTO round-trip test;
  2. `recovery/start` rate-limit test;
  3. anti-enumeration known/unknown shape test;
  4. атомарний `CompleteRecovery`;
  5. Redis cleanup під час видалення акаунта;
  6. Android password confirmation;
  7. iOS CARD/NOTE forms;
  8. iOS sync debounce;
  9. iOS search/reveal/navigation gap.
- Зібрати 5 ручних доказів: Android biometric, Android emulator flow, Android privacy/auto-lock/TalkBack, iOS screenshots, iOS full verification.
- Після фактичного виконання оновити 14 checkbox у `docs/superpowers/plans/**`.

### Базa та merge-порядок

- Worktree C створюється від оновленого `main` після merge A/B.
- iOS-клієнт та ручні iOS-скриншоти виконуються після merge `feature/meditate-note-dark-design`, бо ця гілка змінює `iosApp/Lockruna/ContentView.swift`.
- Checkbox оновлюється лише після наявності коду, автоматичного тесту або ручного доказу.

## Послідовність виконання

1. Створити Worktree A та B від однакового `main`, кожен у власній OpenCode-сесії.
2. На початку кожної сесії виконати baseline verify, потім реалізувати лише власний scope.
3. Провести окремий review A/B; для A виконати crypto/security review, для B — persistence/dependency security review.
4. Rebase/merge A і B у `main`, дозволивши лише одному власнику виконувати `graphify update .` після merge.
5. Після merge створити Worktree C, закрити реальні прогалини, виконати manual evidence та оновити bookkeeping.
6. Наприкінці виконати повну перевірку на `main`.

## Інтеграція та конфлікти

- Worktree A не редагує `PushChanges.kt`; Worktree B володіє ним для `sync.push` audit.
- Worktree A не редагує `ServerModule.kt`, build files чи Compose; Worktree B володіє ними.
- Worktree C не починається до merge A/B і не працює поверх незавершених гілок.
- `graphify-out/**` не комітиться з паралельних гілок; оновлення виконується на `main` після merge.
- Gradle/CI та Docker-тести можна виконувати паралельно, але не на однакових fixed host ports; Mongo/Postgres integration suites запускаються через окремі Testcontainers instances або послідовно.

## Верифікація

### Worktree A

- `:shared:data:jvmTest`
- `:server:test` з Docker
- `:architecture-tests:test`
- `ktlintCheck`, `detekt`, `gitleaks detect --source . --no-banner`
- known-answer vectors на JVM та iOS simulator
- security review diff та `ddd-audit`

### Worktree B

- `:server:test` з Docker/Testcontainers
- `:architecture-tests:test`
- `ktlintCheck`, `detekt`, `gitleaks detect --source . --no-banner`
- security review diff; перевірка Mongo TTL/index та відсутності секретів у документах

### Worktree C та main

- touched-module tests
- `./scripts/verify.sh --ios`
- XcodeBuildMCP для iOS build/launch/screenshots
- Android emulator/manual evidence
- `graphify update .`
- `gitleaks detect --source . --no-banner`

## Не входить у дизайн

- Публічний history endpoint, OpenAPI/DTO та history UI.
- Відновлення історії записів.
- Незначні audit fields, новий analytics pipeline або Mongo aggregation.
- Масове позначення checkbox без реального коду/тестів/доказів.
