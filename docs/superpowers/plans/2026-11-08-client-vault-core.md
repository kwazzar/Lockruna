# Клієнтське ядро vault (Client Vault Core) — Implementation Plan

> **Для агентних виконавців:** ОБОВ'ЯЗКОВО: використати `superpowers:subagent-driven-development` (рекомендовано) або `superpowers:executing-plans` для виконання цього плану по завданнях. Кроки використовують checkbox (`- [ ]`) для відстеження.

**Мета:** Реалізувати доменну модель vault-записів і сесії, порти репозиторію, use cases та SQLDelight-реалізацію локального сховища — щоб запис можна було створити, змінити, видалити, побачити й запечатати в локальній БД.

**Архітектура:** `shared/domain/vault` — агрегат `VaultItem` і value objects (`ItemId`, `Title`, `Secret`, `Url`); `shared/domain/session` — `VaultSession` (Locked/Unlocked, тримає `vaultKey` у пам'яті) та порти `Clock`, `IdGenerator`, `SecureKeyStore`, `BiometricGate`. `shared/application/vault` — use cases `CreateItem` / `UpdateItem` / `DeleteItem` / `GetItem` / `ObserveItems`; `shared/application/session` — `LockVault`, `ApplyAutoLockPolicy`. `shared/data/db` — `SqlDelightVaultItemRepository`: шифрує/дешифрує записи через порт `CryptoService`, мапить рядки ↔ домен, тримає історію 10 версій і `max_seen_version`.

**Технології:** Kotlin 2.4.20, KMP, SQLDelight 2.4.1 (наявна схема `PassKeeperDatabase.sq`), kotlinx-coroutines (`Flow`), kotlin.test.

**Специфікація:** `docs/requirements.md` (US-04, US-05, US-08), `docs/adr/002-clean-architecture-mvi.md`, `docs/adr/005-client-shared.md` (§2, §5, §8), `docs/threat-model.md`.

## Global Constraints

- Інваріанти живуть **в агрегаті**: версія, `dirty`, м'яке видалення, обов'язковість `secret` для `login`, незмінність value objects. Ніде в use case, store чи репозиторії не писати `copy(version = item.version + 1, dirty = true)`.
- Порт `CryptoService` та типи `KdfParams` / `GcmCiphertext` виробляє план `crypto-core` (див. «Залежність від інших планів»). Сигнатури — нижче, дослівно.
- На диску лежать **лише запечатані** записи: `ciphertext`, `nonce` + відкриті метадані (`id`, `type`, `version`, `sync_seq`, `deleted`, `dirty`, `max_seen_version`, `updated_at`). Назва, логін, секрет, url, папка — **всередині шифротексту**.
- AAD записи = `itemId | type | version`; `version` — значення, яке запис матиме після push (`baseVersion + 1`).
- Секрет, назва, логін, url **не потрапляють** у логи, у `toString()` доменних типів, в імена тестів і в фікстури. Тести використовують нечутливі рядки на кшталт `"secret-value"`.
- `vaultKey` живе в пам'яті лише в розблокованому стані; при блокуванні масив обнуляється (best effort).
- Автоблокування рахується від **монотонного** часу (`Clock.monotonicMillis()`), а не від дати пристрою.
- Історія зберігає останні **10** версій кожного запису; `max_seen_version` не обмежується і є основою захисту від rollback.
- Код, коментарі, повідомлення комітів — англійською. `docs/` — українською.
- JDK 25: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.
- Точка входу для перевірки: `./scripts/verify.sh`.
- `gradle/libs.versions.toml` **не** редагується: нових залежностей у цьому плані немає.

## Залежність від інших планів (і правило злиття)

Цей план споживає `CryptoService` з плану `crypto-core`. Очікувані сигнатури (якщо `feature/crypto-core` ще не злито):

```kotlin
package com.kwazzar.lockruna.domain.crypto

data class GcmCiphertext(val ciphertext: ByteArray, val nonce: ByteArray, val tag: ByteArray)

interface CryptoService {
    fun hkdfSha256(inputKey: ByteArray, salt: ByteArray?, info: ByteArray, length: Int = 32): ByteArray
    fun encryptGcm(key: ByteArray, plaintext: ByteArray, aad: ByteArray): GcmCiphertext
    fun decryptGcm(key: ByteArray, ciphertext: GcmCiphertext, aad: ByteArray): ByteArray
    fun randomBytes(size: Int): ByteArray
}
```

Якщо файлу `shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/crypto/CryptoService.kt` ще немає в гілці — створити його з наведеним кодом (без `KdfParams`, він не потрібен). Під час злиття з `feature/crypto-core` залишити **версію з `crypto-core`**; злиття add/add вирішується вручну.

Решта планів (`server-auth-sync`) не перетинається з цим планом жодним файлом.

## Власність файлів (паралельна робота)

Цей план володіє:

- `shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/vault/**`
- `shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/session/**`
- `shared/domain/src/commonTest/kotlin/com/kwazzar/lockruna/domain/vault/**`
- `shared/domain/src/commonTest/kotlin/com/kwazzar/lockruna/domain/session/**`
- `shared/application/src/commonMain/kotlin/com/kwazzar/lockruna/application/vault/**`
- `shared/application/src/commonMain/kotlin/com/kwazzar/lockruna/application/session/**`
- `shared/application/src/commonTest/kotlin/com/kwazzar/lockruna/application/**`
- `shared/data/src/commonMain/kotlin/com/kwazzar/lockruna/data/db/**` (крім `DatabaseDriverFactory.kt`)
- `shared/data/src/commonTest/kotlin/com/kwazzar/lockruna/data/db/**`
- `architecture-tests/src/test/kotlin/com/kwazzar/lockruna/architecture/VaultLayerTest.kt` (новий файл)

`shared/domain/src/commonMain/.../domain/crypto/CryptoService.kt` — спільна з `crypto-core` (див. правило злиття вище).

## Не в обсязі цього плану

- `shared/presentation/**` (MVI-стори, `ItemUi`-мапінг) — наступна хвиля; `presentation` залишається без змін.
- Sync-рушій, outbox, курсор, `SyncStateStore` — наступна хвиля.
- Use cases реєстрації/входу/розблокування — наступна хвиля.
- Нативні реалізації `SecureKeyStore` / `BiometricGate` (Keychain / Keystore) — наступна хвиля; тут лише порти.
- BIP39 recovery key, зміна майстер-пароля, відновлення доступу — наступна хвиля.

## Розглянуті підходи

1. **Шифрування в use case (application)** — відхилено: use case мусив би знати про `CryptoService` і формат AAD; крипто — відповідальність `data`.
2. **Мутабельний агрегат з публічними полями** — відхилено: правило репозиторію забороняє публічні змінні поля в агрегатах.
3. **Незмінний агрегат + `copy` лише всередині агрегата** — вибрано: інваріанти неможливо обійти ззовні, а тести й use cases працюють з'immutable' значеннями.
4. **Час у домені як `Long` epoch millis** — вибрано замість `kotlin.time.Instant`: збігається з колонкою `updated_at INTEGER` і не тягне експериментальний API.

---

### Task 1: Value objects і агрегат VaultItem

**Files:**

- Create: `shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/vault/ItemId.kt`
- Create: `shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/vault/Title.kt`
- Create: `shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/vault/Secret.kt`
- Create: `shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/vault/Url.kt`
- Create: `shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/vault/VaultItem.kt`
- Test: `shared/domain/src/commonTest/kotlin/com/kwazzar/lockruna/domain/vault/VaultItemTest.kt`

**Interfaces:**

- Produces:

```kotlin
package com.kwazzar.lockruna.domain.vault

@JvmInline
value class ItemId private constructor(val value: String) {
    companion object {
        fun of(raw: String): Result<ItemId, AppError>
    }
}

@JvmInline
value class Title private constructor(val value: String) {
    companion object {
        fun of(raw: String): Result<Title, AppError>
    }
}

@JvmInline
value class Secret private constructor(val value: String) {
    companion object {
        fun of(raw: String): Result<Secret, AppError>
    }
}

@JvmInline
value class Url private constructor(val value: String) {
    companion object {
        fun of(raw: String): Result<Url, AppError>
    }
}

data class VaultItem(
    val id: ItemId,
    val type: VaultItemType,
    val title: Title,
    val username: String?,
    val secret: Secret?,
    val url: Url?,
    val folderId: ItemId?,
    val version: Long,
    val dirty: Boolean,
    val deleted: Boolean,
    val updatedAtMillis: Long,
) {
    companion object {
        fun create(
            id: ItemId,
            type: VaultItemType,
            title: Title,
            username: String?,
            secret: Secret?,
            url: Url?,
            folderId: ItemId?,
            nowMillis: Long,
        ): Result<VaultItem, AppError>
    }

    fun update(
        title: Title,
        username: String?,
        secret: Secret?,
        url: Url?,
        folderId: ItemId?,
        nowMillis: Long,
    ): Result<VaultItem, AppError>

    fun delete(nowMillis: Long): VaultItem
}
```

**Step 1: Write the failing test**

`VaultItemTest.kt`:

- `Title.of("")` і `Title.of("   ")` → `Failure`; `Title.of("GitHub")` → `Success`.
- `Secret.of("")` → `Failure`.
- `ItemId.of("not-a-uuid")` → `Failure` (має бути UUID v4).
- `Url.of("ftp://host")` → `Failure` (дозволено лише `http`/`https`).
- `create` для `VaultItemType.LOGIN` без `secret` → `Failure(AppError.InvalidItem)`; для `NOTE` без `secret` → `Success`.
- `create` → `version == 0`, `dirty == true`, `deleted == false`.
- `update` → `version == 1`, `dirty == true`, `updatedAtMillis` оновлено; `update` на видаленому записі → `Failure`.
- `delete` → `deleted == true`, `version` збільшено на 1, `dirty == true`; повторний `delete` → `Failure`.
- `toString()` жодного VO та агрегата **не містить** значення `secret`/`title`.

**Step 2: Run test to verify it fails**

Run: `./gradlew :shared:domain:jvmTest --tests '*VaultItemTest*'`
Expected: FAIL — типів немає.

**Step 3: Implement**

- `ItemId.of` — валідація UUID через `java.util.UUID` **недоступна** у commonMain; валідувати вручну: 36 символів, 4 позиції дефісів, лише hex-символи ✅ (без залежностей).
- `Title.of` — `raw.isNotBlank()`.
- `Secret.of` — `raw.isNotEmpty()`.
- `Url.of` — префікс `http://` або `https://`.
- `AppError` — додати гілку `data class InvalidItem(val reason: String) : AppError` у `shared/domain/.../domain/error/AppError.kt` (файл належить цьому плану; `crypto-core` його не чіпає).
- `VaultItem.create` — перевіряє обов'язковість `secret` для `LOGIN`; `version = 0`, `dirty = true`.
- `VaultItem.update` — якщо `deleted` → `Failure`; перевіряє обов'язковість `secret`; повертає копію з `version + 1`, `dirty = true`, новим `updatedAtMillis`.
- `VaultItem.delete` — якщо `deleted` → `Failure`; копія з `deleted = true`, `version + 1`, `dirty = true`, новим `updatedAtMillis`.
- `toString()` — перевизначити у VO, щоб друкувати лише довжину (`Secret` — довжину, `Title` — довжину); агрегат — default `toString` містить поля, тому перевизначити й його так, щоб не показувати `secret`/`title`.

**Step 4: Run test to verify it passes**

Run: `./gradlew :shared:domain:jvmTest --tests '*VaultItemTest*'`
Expected: PASS.

**Step 5: Commit**

```bash
git add shared/domain
git commit -m "feat(domain): add the vault item aggregate and its value objects"
```

---

### Task 2: Порти та тестові фейки

**Files:**

- Create: `shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/vault/VaultItemRepository.kt`
- Create: `shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/session/Clock.kt`
- Create: `shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/session/IdGenerator.kt`
- Create: `shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/session/SecureKeyStore.kt`
- Create: `shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/session/BiometricGate.kt`
- Create: `shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/session/VaultKey.kt`
- Create: `shared/application/src/commonTest/kotlin/com/kwazzar/lockruna/application/support/InMemoryVaultItemRepository.kt`
- Create: `shared/application/src/commonTest/kotlin/com/kwazzar/lockruna/application/support/FakeClock.kt`
- Create: `shared/application/src/commonTest/kotlin/com/kwazzar/lockruna/application/support/FakeIdGenerator.kt`

**Interfaces:**

- Produces:

```kotlin
package com.kwazzar.lockruna.domain.vault

interface VaultItemRepository {
    suspend fun save(item: VaultItem): VaultItem
    suspend fun get(id: ItemId): VaultItem?
    suspend fun observeAll(): Flow<List<VaultItem>>
}

package com.kwazzar.lockruna.domain.session

interface Clock {
    /** Wall clock, epoch millis — для updatedAt. */
    fun nowMillis(): Long
    /** Монотонний час — для автоблокування; не залежить від налаштування системного годинника. */
    fun monotonicMillis(): Long
}

interface IdGenerator {
    fun newItemId(): ItemId
    fun newUuid(): String
}

interface SecureKeyStore {
    suspend fun saveVaultKey(key: VaultKey)
    suspend fun loadVaultKey(): VaultKey?
    suspend fun clear()
}

interface BiometricGate {
    suspend fun isAvailable(): Boolean
    suspend fun authenticate(reason: String): Boolean
}

@JvmInline
value class VaultKey private constructor(val bytes: ByteArray) {
    fun zeroize() { bytes.fill(0) }
    companion object {
        fun random(cryptoService: CryptoService): VaultKey
    }
}
```

- Фейки (у `shared/application/src/commonTest/.../support/`):
  - `InMemoryVaultItemRepository : VaultItemRepository` — сховище в `MutableMap`, `observeAll()` через `MutableStateFlow`.
  - `FakeClock(var wallMillis: Long, var monotonicMillis: Long) : Clock`.
  - `FakeIdGenerator : IdGenerator` — послідовні UUID-рядки (`"00000000-0000-4000-8000-000000000001"`, …).

**Step 1: Write the failing test**

`shared/application/src/commonTest/kotlin/com/kwazzar/lockruna/application/support/SupportFakesTest.kt`: `InMemoryVaultItemRepository.save` → `get` повертає той самий запис; `observeAll()` емітить список; `FakeClock` повертає задані значення; `FakeIdGenerator` видає різні UUID.

**Step 2: Run test to verify it fails**

Run: `./gradlew :shared:application:jvmTest --tests '*SupportFakesTest*'`
Expected: FAIL — типів немає.

**Step 3: Implement the ports and fakes**

Точно за наведеними сигнатурами. `VaultKey.random(cryptoService)` — `cryptoService.randomBytes(32)`. `VaultKey` — `@JvmInline value class` над `ByteArray`: Kotlin вимагає `@JvmInline` value class з массивом — допустимо ✅; `zeroize()` обнуляє масив ✅.

**Step 4: Run test to verify it passes**

Run: `./gradlew :shared:application:jvmTest --tests '*SupportFakesTest*'`
Expected: PASS.

**Step 5: Commit**

```bash
git add shared/domain shared/application/src/commonTest
git commit -m "feat(domain): add vault, session and clock ports with test fakes"
```

---

### Task 3: SQLDelight-реалізація репозиторію

**Files:**

- Create: `shared/data/src/commonMain/kotlin/com/kwazzar/lockruna/data/db/VaultItemRowMapper.kt`
- Create: `shared/data/src/commonMain/kotlin/com/kwazzar/lockruna/data/db/SqlDelightVaultItemRepository.kt`
- Test: `shared/data/src/commonTest/kotlin/com/kwazzar/lockruna/data/db/SqlDelightVaultItemRepositoryTest.kt`
- Test: `shared/data/src/commonTest/kotlin/com/kwazzar/lockruna/data/db/FakeCryptoService.kt`

**Interfaces:**

- Consumes: Task 1-2 (`VaultItem`, `VaultItemRepository`, `VaultSession`), `CryptoService` (див. «Залежність від інших планів»).
- Produces:

```kotlin
class SqlDelightVaultItemRepository(
    private val database: PassKeeperDatabase,
    private val cryptoService: CryptoService,
    private val session: VaultSession,
) : VaultItemRepository {
    override suspend fun save(item: VaultItem): VaultItem
    override suspend fun get(id: ItemId): VaultItem?
    override suspend fun observeAll(): Flow<List<VaultItem>>
}
```

- `VaultItemRowMapper` — мапить рядок `vault_item` ↔ `VaultItem` і рядок `item_history` ↔ запечатана версія; серіалізація внутрішнього вмісту — JSON (kotlinx.serialization) полів `username`, `secret`, `url`, `folderId`, `title`; шифрується **все** це поле `ciphertext` разом із `title` ✅.

**Step 1: Write the failing test**

`SqlDelightVaultItemRepositoryTest.kt` (jvmTest, in-memory SQLite: `JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)` + `PassKeeperDatabase.Schema.create(driver)`):

- `save` запису → `get` повертає **те саме** значення (title, username, secret, url, folderId, version, dirty).
- У таблиці `vault_item` `ciphertext` і `nonce` **не порожні**, а `title`/`username`/`secret` **відсутні** як окремі колонки ✅ (перевірити, що в таблиці немає таких колонок — тобто прочитати лише `ciphertext`).
- Дешифрування з іншим AAD/ключем кидає помилку → репозиторій перехоплює і повертає `AppError.ItemCorrupted` (замість винятка назовні).
- Заблокована сесія (`VaultSession` у стані Locked) → `get` повертає `Failure`-сумісний результат? — порт повертає `VaultItem?`, тому замість цього: `save` на заблокованій сесії кидає `IllegalStateException` **внутрішньо**, а публічний API репозиторію повертає `null`/`Result` — узгодження нижче в Step 3.
- Історія: після 12 послідовних `update` у `item_history` залишається **10** записів (11-12 витіснені).
- `max_seen_version` оновлюється і не зменшується.

`FakeCryptoService.kt` — тестова реалізація `CryptoService`: детермінований «шифр» (XOR із ключем), `hkdfSha256` — простий тестовий HMAC-заглушка; **не** використовується в production-коді.

**Step 2: Run test to verify it fails**

Run: `./gradlew :shared:data:jvmTest --tests '*SqlDelightVaultItemRepositoryTest*'`
Expected: FAIL — репозиторію немає.

**Step 3: Implement the repository**

- Порт `VaultItemRepository` розширити до:

```kotlin
interface VaultItemRepository {
    suspend fun save(item: VaultItem): Result<VaultItem, AppError>
    suspend fun get(id: ItemId): Result<VaultItem?, AppError>
    suspend fun observeAll(): Flow<List<VaultItem>>
}
```

(оновлення Task 2; `Result` — з `domain/core`).

- `save`: якщо сесія заблокована → `Failure(AppError.VaultLocked)`; інакше: серіалізувати внутрішній вміст → JSON → `cryptoService.encryptGcm(vaultKey, json, aad)`, де `aad = "$id|$type|${version + 1}"`; upsert у `vault_item` (`sync_seq` = 0 для нових, `deleted`, `dirty`, `updated_at`); попередню версію — у `item_history`; обрізати історію до 10; оновити `max_seen_version = max(max_seen_version, version)`.
- `get`: прочитати рядок → розшифрувати (`decryptGcm`) з AAD `id|type|version` → десериалізувати JSON → зібрати `VaultItem`; помилка шифрування/дешифрування → `Failure(AppError.ItemCorrupted)`.
- `observeAll()` — `Flow` на основі `Query` SQLDelight (`asFlow().mapToList()`), мапінг через `VaultItemRowMapper`; заблокована сесія → порожній список.

**Step 4: Run test to verify it passes**

Run: `./gradlew :shared:data:jvmTest --tests '*SqlDelightVaultItemRepositoryTest*'`
Expected: PASS.

**Step 5: Commit**

```bash
git add shared/domain shared/application shared/data
git commit -m "feat(data): seal vault items in SQLDelight and keep ten losing versions"
```

---

### Task 4: Use cases для роботи з записами

**Files:**

- Create: `shared/application/src/commonMain/kotlin/com/kwazzar/lockruna/application/vault/CreateItem.kt`
- Create: `shared/application/src/commonMain/kotlin/com/kwazzar/lockruna/application/vault/UpdateItem.kt`
- Create: `shared/application/src/commonMain/kotlin/com/kwazzar/lockruna/application/vault/DeleteItem.kt`
- Create: `shared/application/src/commonMain/kotlin/com/kwazzar/lockruna/application/vault/GetItem.kt`
- Create: `shared/application/src/commonMain/kotlin/com/kwazzar/lockruna/application/vault/ObserveItems.kt`
- Test: `shared/application/src/commonTest/kotlin/com/kwazzar/lockruna/application/vault/VaultUseCasesTest.kt`

**Interfaces:**

- Consumes: Task 1-3.
- Produces:

```kotlin
data class CreateItemCommand(
    val type: VaultItemType,
    val title: String,
    val username: String?,
    val secret: String?,
    val url: String?,
    val folderId: ItemId?,
)

class CreateItem(
    private val repository: VaultItemRepository,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
) {
    suspend operator fun invoke(command: CreateItemCommand): Result<VaultItem, AppError>
}

class UpdateItem(private val repository: VaultItemRepository, private val clock: Clock) {
    suspend operator fun invoke(id: ItemId, title: String, username: String?, secret: String?, url: String?, folderId: ItemId?): Result<VaultItem, AppError>
}

class DeleteItem(private val repository: VaultItemRepository, private val clock: Clock) {
    suspend operator fun invoke(id: ItemId): Result<VaultItem, AppError>
}

class GetItem(private val repository: VaultItemRepository) {
    suspend operator fun invoke(id: ItemId): Result<VaultItem?, AppError>
}

class ObserveItems(private val repository: VaultItemRepository) {
    operator fun invoke(): Flow<List<VaultItem>>
}
```

**Step 1: Write the failing test**

`VaultUseCasesTest.kt` (з `InMemoryVaultItemRepository`, `FakeClock`, `FakeIdGenerator`):

- `CreateItem` з порожнім title → `Failure`; з валідними даними → `Success`, `version == 0`, `dirty == true`, `id` з `FakeIdGenerator`.
- `CreateItem` для `LOGIN` без `secret` → `Failure`.
- `UpdateItem` для неіснуючого id → `Failure(AppError.ItemNotFound)`; для наявного → `version == 1`, `dirty == true`.
- `DeleteItem` → `deleted == true`, `version == 1`.
- `GetItem` для відсутнього id → `Success(null)`.
- `ObserveItems()` емітить список записів і реагує на наступний `save`.

**Step 2: Run test to verify it fails**

Run: `./gradlew :shared:application:jvmTest --tests '*VaultUseCasesTest*'`
Expected: FAIL — use case-класів немає.

**Step 3: Implement the use cases**

Кожен — одна `operator fun invoke`, повертає `Result<T, AppError>`; валідацію полів (title, secret, url) виконує **агрегат/VO**, use case лише перетворює сирі рядки у VO й прокидає помилку; жодних `copy(version = ...)` у use case ✅. `AppError` — додати гілку `data object ItemNotFound : AppError`.

**Step 4: Run test to verify it passes**

Run: `./gradlew :shared:application:jvmTest --tests '*VaultUseCasesTest*'`
Expected: PASS.

**Step 5: Commit**

```bash
git add shared/domain shared/application
git commit -m "feat(application): add vault item use cases"
```

---

### Task 5: Сесія, автоблокування та інтеграційний цикл

**Files:**

- Create: `shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/session/VaultSession.kt`
- Create: `shared/application/src/commonMain/kotlin/com/kwazzar/lockruna/application/session/LockVault.kt`
- Create: `shared/application/src/commonMain/kotlin/com/kwazzar/lockruna/application/session/ApplyAutoLockPolicy.kt`
- Test: `shared/domain/src/commonTest/kotlin/com/kwazzar/lockruna/domain/session/VaultSessionTest.kt`
- Test: `shared/application/src/commonTest/kotlin/com/kwazzar/lockruna/application/session/AutoLockPolicyTest.kt`
- Test: `shared/application/src/commonTest/kotlin/com/kwazzar/lockruna/application/vault/VaultCycleTest.kt`
- Create: `architecture-tests/src/test/kotlin/com/kwazzar/lockruna/architecture/VaultLayerTest.kt`

**Interfaces:**

- Consumes: Task 1-4.
- Produces:

```kotlin
class VaultSession {
    val state: State
    fun unlock(vaultKey: VaultKey)
    fun lock()
    fun currentVaultKey(): VaultKey?
    sealed interface State {
        data object Locked : State
        data object Unlocked : State
    }
}

class LockVault(private val session: VaultSession) {
    operator fun invoke()
}

class ApplyAutoLockPolicy(
    private val session: VaultSession,
    private val clock: Clock,
    private val policy: AutoLockPolicy,
) {
    operator fun invoke(backgroundedAtMonotonicMillis: Long): Boolean
}

data class AutoLockPolicy(val timeoutMillis: Long) {
    companion object {
        val IMMEDIATE = AutoLockPolicy(0)
        val ONE_MINUTE = AutoLockPolicy(60_000)
        val FIVE_MINUTES = AutoLockPolicy(5 * 60_000)
        val FIFTEEN_MINUTES = AutoLockPolicy(15 * 60_000)
    }
}
```

**Step 1: Write the failing test**

`VaultSessionTest.kt`: після `unlock` → `state == Unlocked` і `currentVaultKey() != null`; після `lock` → `state == Locked`, `currentVaultKey() == null`, а масив ключа обнулений (перевірити, що байти стали нульовими).

`AutoLockPolicyTest.kt`:

- `ApplyAutoLockPolicy` з `ONE_MINUTE`: якщо мон��тонний час після згортання ще менше за таймаут → `false` (сесія не заблокована).
- Якщо мон��тонний час перевищив таймаут → `true` і сесія заблокована.
- **Зміна системного годинника не впливає**: `FakeClock.wallMillis` змінити на ±1 день, `monotonicMillis` не змінюємо → блокування **не** спрацьовує.

`VaultCycleTest.kt` (інтеграція з `InMemoryVaultItemRepository`):

- create → update → delete: `version` 0 → 1 → 2, `dirty == true`, `deleted == true`.
- Після `lock` репозиторій більше не віддає записи (`Failure(AppError.VaultLocked)` на `save`).

**Step 2: Run test to verify it fails**

Run: `./gradlew :shared:domain:jvmTest --tests '*VaultSessionTest*'` та `./gradlew :shared:application:jvmTest --tests '*AutoLockPolicyTest*' --tests '*VaultCycleTest*'`
Expected: FAIL — класів немає.

**Step 3: Implement**

- `VaultSession` — змінний стан сесії (це не агрегат запиту, а стан розблокування): `state`, `unlock`, `lock`, `currentVaultKey()`. `lock()` викликає `vaultKey.zeroize()` ✅ (best effort) і скидає посилання.
- `LockVault` — делегує `session.lock()`.
- `ApplyAutoLockPolicy` — рахує `clock.monotonicMillis() - backgroundedAtMonotonicMillis >= policy.timeoutMillis`; якщо так → `session.lock()` і повертає `true`, інакше `false`.

**Step 4: Add the architecture test**

`architecture-tests/src/test/kotlin/com/kwazzar/lockruna/architecture/VaultLayerTest.kt` — Konsist:

- `shared/domain/.../domain/vault/**` та `.../domain/session/**` не імпортують `app.cash.sqldelight.`, `io.ktor.`, `kotlinx.serialization.`, `org.jetbrains.exposed.`;
- `shared/application/.../application/vault/**` не імпортують `app.cash.sqldelight.`, `io.ktor.`;
- `shared/data/.../data/db/**` не імпортують `com.kwazzar.lockruna.presentation.`;
- у `shared/domain/vault/**` немає класів `Base*`.

Run: `./gradlew :architecture-tests:test`
Expected: PASS.

**Step 5: Run the full verification**

Run: `./gradlew ktlintCheck detekt` і `./scripts/verify.sh`
Expected: BUILD SUCCESSFUL / «All checks passed».

**Step 6: Commit**

```bash
git add shared/domain shared/application shared/data architecture-tests
git commit -m "feat(domain): add the vault session and the auto-lock policy"
```

---

## Порядок злиття

1. `feature/crypto-core` — першим (дає `CryptoService`).
2. `feature/client-vault-core` — перебазувати на `feature/crypto-core` перед злиттям; під час злиття тримати версію `CryptoService.kt` з `crypto-core`.
3. `feature/server-auth-sync` — незалежний, зливається в будь-якому порядку.

Після злиття всіх трьох — виконати `graphify update .` один раз у `main` і закомітити оновлення `graphify-out/`.
