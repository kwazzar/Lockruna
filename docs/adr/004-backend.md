# ADR-004: Архітектура бекенду

- **Статус:** прийнято
- **Дата:** 2026-11-07
- **Пов'язані:** ADR-002, ADR-003, openapi.yaml, threat-model.md

## Контекст

Ktor-сервер зберігає лише шифротекст (ADR-003) і має надійно синхронізувати кілька пристроїв одного користувача, не маючи змоги прочитати дані.

## Рішення

### 1. Структура

Один Gradle-модуль `server`, пакети за bounded contexts (`identity`, `sync`, `audit`, `platform`). У кожному контексті шари `api → application → domain ← infrastructure`. Правило залежностей перевіряє Konsist у CI. Маршрути не звертаються до репозиторіїв напряму.

Спільний KMP-модуль `api-contract` містить DTO для сервера й клієнтів (kotlinx.serialization). Контрактні тести звіряють його з `openapi.yaml`.

### 2. Стек

| Задача | Вибір |
|---|---|
| HTTP | Ktor, Auth (JWT) |
| DI | Koin |
| Postgres | Exposed, Flyway, HikariCP |
| Mongo | офіційний Kotlin coroutine driver |
| Redis | Lettuce |
| Тести | Testcontainers (Postgres, Mongo, Redis) |
| Метрики й логи | Micrometer, структуровані логи |

Версії фіксуються в `libs.versions.toml`.

### 3. Sync

- **Єдиний шлях запису:** `POST /sync/push`. Окремих CRUD-ендпоінтів для записів немає.
- **Курсор:** лічильник `users.sync_seq`. Кожна застосована зміна в одній транзакції збільшує його (`UPDATE users SET sync_seq = sync_seq + 1 ... RETURNING`). Рядок користувача блокується до коміту, тому паралельні push одного користувача виконуються послідовно, а номери фіксуються в порядку коміту. Курсор у `GET /sync` непрозорий і кодує останній `sync_seq`. Курсор по `updated_at` або глобальному serial відкинуто: транзакції можуть завершуватись у зворотному порядку, і клієнт назавжди пропустив би запис.
- **Push:** для кожної зміни умовний `UPDATE ... WHERE user_id = ? AND id = ? AND version = baseVersion`; для нового запису (`baseVersion = 0`) `INSERT ... ON CONFLICT DO NOTHING`. Нуль змінених рядків означає конфлікт, у відповіді `conflicts` повертається серверна версія. Редагування видаленого запису теж конфлікт (серверна версія це tombstone). Нова `version = baseVersion + 1`.
- **Tombstone:** при видаленні сервер ставить `deleted_at` і очищає `ciphertext` та `nonce`.
- **Збір сміття:** tombstone живе на сервері 90 днів, потім рядок видаляється назавжди. Пристрій, який не синхронізувався понад 90 днів, не приймає часткову відповідь, а очищає локальні записи й проходить повну синхронізацію — інакше видалені записи воскресили б.
- **Ідемпотентність:** заголовок `Idempotency-Key` обов'язковий. Ключ і хеш тіла зберігаються в Postgres (`idempotency_keys`) в тій самій транзакції, що й запис, тому повтор того самого запиту повертає збережену відповідь, а той самий ключ з іншим тілом дає HTTP 409. Записи прибираються через 24 години.
- **Розв'язання конфліктів** виконує клієнт (ADR-005): last-write-wins, програшна версія зберігається в історії запису.

### 4. Токени й пристрої

- Access JWT живе 15 хвилин і містить `deviceId`.
- Refresh-токен випадковий, 256 біт, одноразовий, у базі лише його SHA-256. Повторне використання вже використаного токена відкликає всю сім'ю токенів (`family_id`).
- Refresh живе 30 днів; кожна ротація видає новий на 30 днів. Абсолютний ліміт сім'ї — 90 днів від її створення (`family_created_at`), після нього refresh більше не видається і потрібен новий вхід.
- Відкликання пристрою ставить `devices.revoked_at`, `deviceId` потрапляє в Redis denylist на 15 хвилин (час життя access-токена), тож блокування негайне без запиту в базу на кожен виклик.
- Ключ JWT: сервер приймає `active` і `previous` ключі (перекриття 48 годин), щоб ротація не відкидала живі сесії.
- `pepper` у MVP не ротується: ротація змусила б усіх користувачів перелогінитись. Тригер — компрометація; тоді додається flow повторного логіну з прийомом старого й нового `pepper`.

### 5. Автентифікація й анти-enumeration

- Сервер зберігає `HMAC(pepper, authKey)` і `HMAC(pepper, recoveryAuthKey)`, порівняння константного часу.
- `prelogin` і `recovery/start` для неіснуючого email повертають фіктивні дані тієї ж форми: salt, `userId` і обгортка виводяться як `HMAC(serverSecret, email)`, параметри це поточні дефолтні. В обох гілках виконується однаковий обсяг роботи.
- `login` дає однакову 401 для невірного пароля й неіснуючого email.
- `POST /auth/register` повертає 409 для існуючого email (прийнятий ризик, стримується rate limiting).

### 6. Rate limiting

Лічильники в Redis окремо по `email` і по `IP` (ключі з хешем email). Для входу: 5 невдалих спроб за 15 хвилин на пару email+IP, далі блокування на 15 хвилин (429 із `Retry-After`). `X-Forwarded-For` довіряється лише від власного проксі.

Реєстрація: 3 спроби на годину на `email` і 10 спроб на годину на `IP`. Відновлення (`/auth/recovery/start` і `/auth/recovery/complete`): 5 спроб за 15 хвилин на пару email+IP, як для входу.

### 7. Аудит

Mongo зберігає лише `audit_events` (append-only, TTL 365 днів). Запис асинхронний: збій аудиту не валить запит, але рахується в метриці.

### 8. Помилки, логи, конфігурація

- Помилки у форматі `application/problem+json` зі стабільним полем `code`.
- Тіла запитів і відповідей `/auth` та `/sync` не логуються взагалі; тест на витік перевіряє відсутність секретів у логах.
- Секрети (`pepper`, ключ JWT, паролі БД) лише з оточення, ніколи з репозиторію.
- Обмеження розміру: шифротекст до 64 KiB на запис, до 100 змін на push (413).

### 9. Видалення акаунта

- `DELETE /account` вимагає повторного підтвердження майстер-паролем: клієнт надсилає `authKey`, сервер порівнює `HMAC(pepper, authKey)` у сталому часі.
- Видалення каскадне: `users`, `devices`, `refresh_tokens`, `vault_items`, `idempotency_keys` зникають разом (FK `ON DELETE CASCADE`), ключі Redis і denylist пристроїв очищаються, audit-подія `account.delete` лишається в Mongo до TTL.
- Операція необоротна: у UI підтвердження вимагає повторного введення майстер-пароля.

### 10. Тестування

Unit для domain/application; інтеграційні з Testcontainers для `/auth`, `/sync`, `/devices`; контрактні проти `openapi.yaml`; IDOR-тести; concurrency-тест (два пристрої шлють push з однаковим `baseVersion`, виграє рівно один); тест на витік секретів у логи.

## Наслідки

- (+) Немає пропущених змін у sync через порядок комітів.
- (+) Одне джерело правди для sync і одна гілка коду для конфліктів.
- (+) Типи запитів і відповідей спільні для сервера й клієнтів.
- (−) Push одного користувача серіалізований (прийнятно для vault); користувач може уповільнити лише власний sync.
- (−) Аудит асинхронний і може втратити подію при збої.

## Прийняті ризики

- Last-write-wins порівнює час локальної зміни (годинник пристрою) із серверним `updated_at`; перекіс годинників може дати неочікуваного переможця. Втрати даних немає, бо програшна версія завжди зберігається в історії.
- Скомпрометований сервер може віддати застарілу, але валідну версію запису (rollback); клієнт ловить це лише для вже відомих записів.

## Відкриті питання

- Certificate pinning та вирівнювання розміру шифротексту (обидва опційні).
- Ротація `pepper`: відкладена до компрометації, тоді додається flow повторного логіну.

## Альтернативи

- Курсор по `updated_at` або глобальному serial: пропускає зміни при зворотному порядку комітів.
- Окремі `/items` CRUD разом із `/sync`: дві реалізації перевірки версій і конфліктів.
- Idempotency у Redis: не атомарна з транзакцією запису.
- Mongo `sync_events`: дублює Postgres, дельту дає `sync_seq`.

## Додаток: дані

### PostgreSQL

```sql
CREATE TABLE users (
  id                         UUID PRIMARY KEY,
  email                      TEXT NOT NULL,
  auth_hash                  BYTEA NOT NULL,   -- HMAC(pepper, authKey)
  recovery_auth_hash         BYTEA NOT NULL,   -- HMAC(pepper, recoveryAuthKey)
  kdf_salt                   BYTEA NOT NULL,   -- 16 байт
  kdf_params                 JSONB NOT NULL,   -- {"alg":"argon2id","m":65536,"t":3,"p":1}
  wrapped_vault_key          BYTEA NOT NULL,
  recovery_wrapped_vault_key BYTEA NOT NULL,
  sync_seq                   BIGINT NOT NULL DEFAULT 0,
  created_at                 TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_users_email ON users (lower(email));

CREATE TABLE devices (
  id          UUID PRIMARY KEY,
  user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name        TEXT NOT NULL,
  platform    TEXT NOT NULL CHECK (platform IN ('ios','android')),
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_seen   TIMESTAMPTZ,
  revoked_at  TIMESTAMPTZ
);

CREATE TABLE refresh_tokens (
  id          UUID PRIMARY KEY,
  device_id   UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
  family_id   UUID NOT NULL,
  token_hash  BYTEA NOT NULL UNIQUE,           -- SHA-256 випадкового токена
  expires_at  TIMESTAMPTZ NOT NULL,
  family_created_at TIMESTAMPTZ NOT NULL,      -- абсолютний ліміт сім'ї (90 днів)
  used_at     TIMESTAMPTZ,
  revoked_at  TIMESTAMPTZ
);
CREATE INDEX idx_refresh_family ON refresh_tokens (family_id);

CREATE TABLE vault_items (
  user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  id          UUID NOT NULL,                   -- генерується клієнтом
  type        TEXT NOT NULL CHECK (type IN ('login','card','note','folder')),
  ciphertext  BYTEA,                           -- NULL для tombstone
  nonce       BYTEA,
  version     BIGINT NOT NULL,
  sync_seq    BIGINT NOT NULL,
  deleted_at  TIMESTAMPTZ,
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, id),
  CHECK (deleted_at IS NOT NULL OR (ciphertext IS NOT NULL AND nonce IS NOT NULL))
);
CREATE INDEX idx_items_sync ON vault_items (user_id, sync_seq);

CREATE TABLE idempotency_keys (
  user_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  idempotency_key  UUID NOT NULL,
  body_hash        BYTEA NOT NULL,
  response         JSONB NOT NULL,
  created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, idempotency_key)
);
```

Первинний ключ `vault_items` складений `(user_id, id)`: `id` генерується клієнтом, тож глобальний первинний ключ дозволив би дізнатися, чи використовує чужий акаунт певний `id`.

### MongoDB

```
audit_events { userId, deviceId, action, ip, ts, meta{} }
  action: register | login | login.failed | recovery | device.revoke
          | token.reuse | sync.push | account.delete
  індекси: {userId:1, ts:-1}, TTL на ts (365 днів)
```

Чому так (для README): SQL там, де потрібні зв'язки й цілісність (користувачі, пристрої, записи, sync). NoSQL там, де append-only потік подій із TTL (аудит).

### Redis

- Лічильники rate limiting (`rl:login:email:{hash}`, `rl:login:ip:{ip}`, TTL 15 хвилин; окремо `rl:register:*` і `rl:recovery:*` з TTL 1 година для реєстрації та 15 хвилин для відновлення).
- Denylist відкликаних пристроїв (`denylist:device:{id}`, TTL 15 хвилин).
- Кеш відповідей перевірки паролів HIBP (опційно, поза MVP).
