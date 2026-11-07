# ADR-005: Архітектура клієнтського ядра shared/

- **Статус:** прийнято
- **Дата:** 2026-11-07
- **Пов'язані:** ADR-001, ADR-002, ADR-003, ADR-004

## Контекст

Клієнтська логіка (ключі, сховище, sync, екрани) пишається один раз у KMP (ADR-001) за Clean Architecture і MVI (ADR-002). Потрібне локальне сховище, що не залежить від незрілих нативних інтеграцій.

## Рішення

### 1. Модулі Gradle

```
api-contract/        KMP, DTO (спільний із сервером)
shared/
  domain/            чистий Kotlin
  application/       use cases
  data/              SQLDelight, Ktor client, реалізації портів
  presentation/      MVI-стори
  umbrella/          експорт у XCFramework для iOS
androidApp/  iosApp/
```

Правило залежностей забезпечує компілятор (окремі модулі), Konsist перевіряє додатково.

### 2. Domain

- **Агрегати:** `VaultItem` (value objects `ItemId`, `Title`, `Secret`, `Url`), `Folder`, `VaultSession` (стан Locked/Unlocked, тримає `VaultKey` у пам'яті), `SyncState` (курсор і максимальна бачена `version` кожного запису).
- **Порти:** `CryptoService`, `Argon2`, `PasswordNormalizer`, `VaultRepository`, `SyncStateStore`, `SecureKeyStore` (Keychain/Keystore), `BiometricGate`, `Clipboard`, `Clock`, `IdGenerator`.
- `BiometricGate` реалізується в `androidApp`/`iosApp` (потрібні Activity і ViewController) та інжектиться.
- **Нормалізація NFKC:** у common Kotlin її немає. Правило живе в `domain` (порт `PasswordNormalizer`), примітив — у платформній реалізації: Android `java.text.Normalizer`, iOS — системний ICU4C (`unorm2`) через тонкий C-шім у `iosApp`. Обов'язкові тест-вектори з кирилицею та комбінованими символами, однакові на обох платформах.

### 3. Application: use cases

| Контекст | Use cases |
|---|---|
| Identity | Register, Login, Logout, UnlockWithPassword, UnlockWithBiometrics, EnableBiometrics, CompleteRecovery, ListDevices, RevokeDevice |
| Vault | Create/Update/DeleteItem, GetItem, ObserveItems (пошук, фільтр за папкою), RevealSecret, CopySecret, операції з папками |
| Session | LockVault, ApplyAutoLockPolicy |
| Sync | SyncNow, ObserveSyncState |

### 4. Помилки

Власний `Result<T, AppError>` замість винятків: виняток Kotlin не переходить у Swift без `@Throws`, тож межа з iOS має бути без винятків. `AppError` це sealed-ієрархія з однією гілкою на контекст, наприклад `VaultLocked`, `InvalidCredentials`, `WeakKdfParams`, `RateLimited(retryAfter)`, `NetworkUnavailable`, `Unauthorized`, `ItemCorrupted`. Коди problem+json мапляться на `AppError` лише в `data`, тексти для користувача лишаються в нативному UI.

### 5. Локальне сховище без SQLCipher

SQLDelight (звичайний SQLite) зберігає ті самі запечатані записи, що й сервер.

| Колонка | Вміст |
|---|---|
| `id`, `type` | відкриті |
| `ciphertext`, `nonce` | шифротекст запису (AES-GCM, ключ `vaultKey`) |
| `version`, `sync_seq` | серверні значення |
| `deleted`, `dirty` | локальні прапорці |
| `max_seen_version` | захист від rollback |
| `updated_at` | час останньої зміни |

Назва, логін, секрет, url і папка лежать у шифротексті. Додаткові таблиці: історія програшних версій (теж запечатана), `sync_state` (курсор), outbox (пакет змін разом з `Idempotency-Key`).
Історія тримає останні 10 версій кожного запису, старіші видаляються. `max_seen_version` зберігається окремо й не обмежується — це основа захисту від rollback.

Пошук і сортування працюють через індекс у пам'яті, який будується при розблокуванні й скидається при блокуванні. Ключа БД немає: на диску лише біометрично захищений `vaultKey` у Keychain/Keystore (ADR-003).

SQLCipher відкинуто з двох причин:

- на iOS потрібне ручне налаштування лінкування замість системного sqlite3;
- готовий KMP-драйвер для SQLDelight зараз незрілий (версія 0.1.0).

### 6. Папки

Папки синхронізуються як записи типу `folder` (шифротекст містить назву). Тип додано до enum у `openapi.yaml` і до `CHECK` у `vault_items`.
У MVP папки плоскі: запис належить рівно одній папці (`folderId`), вкладеності немає. Поле `parentId` не вводиться, щоб не ускладнювати модель до появи потреби.

### 7. Sync-рушій на клієнті

1. **Pull:** запит за курсором. Для кожного запису перевіряється, що `version` не нижча за `max_seen_version` (rollback), потім розшифрування з AAD (ADR-003).
2. **Push:** пакет до 100 змін. Пакет разом з `Idempotency-Key` зберігається в outbox до відповіді; повтор шле той самий пакет з тим самим ключем. AAD нового шифротексту використовує `baseVersion + 1`.
3. **Конфлікти** з `conflicts`: last-write-wins по `updatedAt`, програшна версія йде в локальну історію запису.
4. **Тригери:** розблокування, зміна (з debounce), повернення мережі, періодично через `WorkManager`/`BGTaskScheduler`. Планувальники нативні, логіка в `shared`.
5. **Скидання сховища:** якщо остання синхронізація старша за 90 днів (server-side TTL tombstone), клієнт очищає локальні записи й проходить повну синхронізацію — часткова відповідь у такому разі не приймається.

### 8. Сесія, блокування, біометрія

- Автоблокування рахується від моменту згортання за монотонним годинником, а не за датою пристрою. Нативний шар повідомляє `shared` про події background/foreground. Поведінку монотонного часу на призупиненому пристрої перевіряє тест.
- `lock()` обнуляє `vaultKey` (best effort) і скидає індекс у пам'яті.
- Біометричний ключ: Keychain (поточний набір біометрії, `ThisDeviceOnly`) і Keystore (інвалідація при зміні біометрії).

### 9. Presentation і DI

Стори: Register, Login/Unlock, Recovery, VaultList, ItemDetail, ItemEdit, Devices, Settings. `State` містить `ItemUi` без `secret`. Для SwiftUI береться SKIE; версії Kotlin (2.4.21) і SKIE (0.10.16) звіряються за таблицею сумісності під час створення скелета. DI на Koin: модулі на шар, платформні реалізації портів підключаються з нативних застосунків.

Мова інтерфейсу в MVP — лише англійська; базова accessibility обов'язкова (Dynamic Type, VoiceOver/TalkBack, контраст, таргети 44 pt).

Оцінка стійкості майстер-пароля — zxcvbn через `expect/actual`: Android використовує JVM-бібліотеку, iOS — порт алгоритму зі словниками; результат однаковий на обох платформах.

Захист екранів: Android — `FLAG_SECURE`; iOS — blur у app switcher, приховування вмісту під час `UIScreen.isCaptured` і попередження після скриншота (заборонити скриншот iOS не вміє). Clipboard: Android — `EXTRA_IS_SENSITIVE` і очистка через 30 секунд, iOS — `expirationDate` і `localOnly`.

Crash-звіти: Sentry з opt-in (користувач вмикає явно), без PII, без секретів і шифротексту; прив'язка до випадкового install id, не до акаунта.

### 10. Тестування

Domain і application у `commonTest` з фейками (`InMemory<X>Repository`). Еталонні вектори Argon2id, AES-GCM, HKDF, нормалізації пароля, кодування BIP39 для recovery key і zxcvbn для оцінки стійкості. Тест sync-рушія проти фейкового сервера (конфлікти, rollback, повтор з тим самим `Idempotency-Key`), пізніше проти реального. Konsist для перевірки залежностей.

## Наслідки

- (+) Жодної нативної залежності для БД, один формат для диска й sync.
- (+) Ключа БД немає, на диску лише `vaultKey` під біометричним захистом.
- (+) Правило залежностей гарантоване на рівні збірки.
- (−) Індекс у пам'яті збільшує обсяг розшифрованих даних, поки сховище розблоковане.
- (−) Метадані (тип, версії, час, кількість записів) читаються з диска.
- (−) Шість модулів ускладнюють збірку Gradle + Xcode.
- (−) zxcvbn на iOS вимагає порту алгоритму зі словниками: підтримуваний Swift-порт перевіряється під час створення скелета, інакше потрібна згода на відкат до спільної евристики.

## Альтернативи

- **SQLCipher** (готовий драйвер v0.1.0 або власна cinterop-інтеграція): незріла залежність або високий ризик збірки на iOS.
- **Один модуль із пакетами:** правило залежностей лише за домовленістю.
- **Виняткове API з `@Throws` у Swift:** ламає однорідність обробки помилок між платформами.
