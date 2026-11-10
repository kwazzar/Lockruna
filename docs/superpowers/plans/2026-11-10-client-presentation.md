# Клієнтська презентація (Client Presentation) — Implementation Plan

> **Для агентних виконавців:** ОБОВ'ЯЗКОВО: використати `superpowers:subagent-driven-development` (рекомендовано) або `superpowers:executing-plans` для виконання цього плану по завданнях. Кроки використовують checkbox (`- [ ]`) для відстеження.

**Мета:** MVI-стори для екранів vault (список, деталі, редагування), мапінг `VaultItem → ItemUi`, expose/reveal та copy секрету через use cases і порт `Clipboard`, Koin-модуль презентації.

**Архітектура:** `shared/presentation` — Store-класи з чистою логікою в `commonTest`; State тримає лише `ItemUi` (без секрету); секрет у State не потрапляє ніколи — він проходить через `Effect` одноразово. Створення/редагування йде через наявні use cases, інваріанти lives в агрегаті.

**Технології:** Kotlin 2.4.20, kotlinx-coroutines (StateFlow/SharedFlow), koin-core 4.2.2, kotlin.test. Нових залежностей у каталозі немає; до `shared/presentation/build.gradle.kts` додається лише `libs.koin.core`.

**Специфікація:** `docs/requirements.md` (US-04, US-05, US-06, US-08), `docs/adr/002-clean-architecture-mvi.md`, `docs/adr/005-client-shared.md` §9, `docs/threat-model.md`.

## Global Constraints

- Пріоритет того, що вже є: використовувати `ObserveItems`, `CreateItem`, `UpdateItem`, `DeleteItem`, `GetItem`, `LockVault` як є — без змін сигнатур.
- Секрет (ідентичний полю `secret`) не потрапляє у `State`, логи, `toString`, імена тестів. У тестах — `"secret-value"`.
- Store викликає тільки use cases, ніколи — репозиторії, Ktor чи CryptoService.
- Назвуємо: `<Screen>Store`, `<Screen>State`, `<Screen>Intent`, `<Screen>Effect`. Одне<<<Store>> = один екран.
- UI-тексти в цьому плані не пишуться (remarks нативної UI-хвилі); лише логіка станів.
- Немає `Base*` абстракцій; ні одного `copy(version = ...)` поза агрегатом.
- Код/комменти/коміти англійською; docs/ українською.
- JDK 25: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.
- Його хвилі (out of scope): нативні екрани (SwiftUI/Compose), Register/Login/Unlock-стори (auth-хвиля), DI-композиція в нативних app (own wave), native `Clipboard`-impl (тут лише порт домену + тест — fake).

## Review Focus

- Reveal секрету для запису без секрету (`NOTE` без secret або `Secret?` == null) — очікуємо `Failure(InvalidItem)`, а не виняток або null у `Effect`; тест у Task 3/4 fix.
- Пошук у `VaultListStore` на пустий/пробілний query — очікуємо повний список, регістр-незалежність (трімін-cased fold).
- Копіювання неіснуючого `id` — `Failure(ItemNotFound)`, без краш-у.
- `Effect.SecretRevealed` перевиданий повторно (re-collect SharedFlow при re-subscribe) — не повинен зберігатись у StateStore-у (checks: `replay == 0`).
- Видалення активного (відкритого у detail) запису — State має вернутися у порожній, без NPE.

---

### Task 1: ItemUi v2 + мапінг

**Files:**

- Modify: `shared/presentation/src/commonMain/kotlin/com/kwazzar/lockruna/presentation/vault/ItemUi.kt`
- Create: `shared/presentation/src/commonMain/kotlin/com/kwazzar/lockruna/presentation/vault/ItemUiMapping.kt`
- Test: `shared/presentation/src/commonTest/kotlin/com/kwazzar/lockruna/presentation/vault/ItemUiMappingTest.kt`

**Interfaces:**

- Consumes: `VaultItem` (`com.kwazzar.lockruna.domain.vault`), `VaultItemType`.
- Produces:

```kotlin
data class ItemUi(
    val id: String,
    val type: VaultItemType,
    val title: String,
    val username: String?,
    val folderId: String?,
    val updatedAtMillis: Long,
    val hasSecret: Boolean,
)

fun VaultItem.toItemUi(): ItemUi
```

- [x] **Step 1: Write the failing test** — `toItemUi()` переносить id/type/title/username/folderId/updatedAt, `hasSecret = secret != null`; тест-агрегат будується через `VaultItem.create` (версія 0); **жодна інстанція ItemUi не містить значення секрету** (перевірити `hasSecret == true`, absence самого рядка).

- [x] **Step 2: Run to verify fails:** `./gradlew :shared:presentation:jvmTest --tests '*ItemUiMappingTest*'` — FAIL.

- [x] **Step 3: Implement.** Розширити `ItemUi` полями `username`, `updatedAtMillis`; мапінг — одна функція extension, без логіки.

- [x] **Step 4: Run to verify passes:** same command — PASS.

- [x] **Step 5: Commit** `feat(presentation): map vault items to ItemUi without secrets`

---

### Task 2: Порт Clipboard + Reveal/Copy use cases

**Files:**

- Create: `shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/vault/Clipboard.kt`
- Create: `shared/application/src/commonMain/kotlin/com/kwazzar/lockruna/application/vault/RevealSecret.kt`
- Create: `shared/application/src/commonMain/kotlin/com/kwazzar/lockruna/application/vault/CopySecret.kt`
- Test: `shared/application/src/commonTest/kotlin/com/kwazzar/lockruna/application/vault/RevealSecretTest.kt`
- Test: `shared/application/src/commonTest/kotlin/com/kwazzar/lockruna/application/vault/CopySecretTest.kt`

**Interfaces:**

```kotlin
// domain/vault/Clipboard.kt
interface Clipboard {
    /** Копіює з прапорцем sensitive; нативна реалізація очищає через 30 с. */
    suspend fun copySecret(value: String)
}

// application/vault
class RevealSecret(private val repository: VaultItemRepository) {
    suspend operator fun invoke(id: ItemId): Result<String?, AppError>
}

class CopySecret(
    private val repository: VaultItemRepository,
    private val clipboard: Clipboard,
) {
    suspend operator fun invoke(id: ItemId): Result<Unit, AppError>
}
```

- `RevealSecret` — запису немає → `Failure(ItemNotFound)`; є, але `secret == null` → `Failure(InvalidItem)`; інакше `Success(secret.value)`.
- `CopySecret` — та сама валідація; далі `clipboard.copySecret(secret.value)` → `Success(Unit)`.
- Обидва беруть запис через `repository.get(id)` напряму — окремого GetItem-делегування всередині application не потрібне.

- [x] **Step 1: Write the failing tests** (увага: три гілки кожного use case; фейковий `RecordingClipboard` у тесті).
- [x] **Step 2: Run to verify fails** —`./gradlew :shared:application:jvmTest --tests '*RevealSecret*' --tests '*CopySecret*'` — FAIL.
- [x] **Step 3: Implement** — по одному `operator fun invoke`, тіла за гілками вище (запис — `repository.get(id)`).
- [x] **Step 4: Run to verify passes.**
- [x] **Step 5: Commit** `feat(application): reveal and copy secrets via clipboard port`

---

### Task 3: VaultListStore

**Files:**

- Create: `shared/presentation/src/commonMain/kotlin/com/kwazzar/lockruna/presentation/vault/VaultListStore.kt`
- Test: `shared/presentation/src/commonTest/kotlin/com/kwazzar/lockruna/presentation/vault/VaultListStoreTest.kt`

Фейкові залежності store-тестів: локальний `FakeVaultItemRepository` (own файл у `commonTest`, ~30 рядків) + `RecordingClipboard` і `FakeClock` — support-файл `shared/presentation/src/commonTest/kotlin/com/kwazzar/lockruna/presentation/support/TestSupport.kt`.

**Interfaces:**

- Consumes: `ObserveItems`, `LockVault`, `RevealSecret`, `CopySecret`, `ItemUi`/`toItemUi()`.
- Produces:

```kotlin
class VaultListStore(
    observeItems: ObserveItems,
    lockVault: LockVault,
    revealSecret: RevealSecret,
    copySecret: CopySecret,
) {
    val state: StateFlow<VaultListState>
    val effects: SharedFlow<VaultListEffect>
    suspend fun dispatch(intent: VaultListIntent)   // Search теж через dispatch
}

data class VaultListState(
    val items: List<ItemUi>,
    val searchQuery: String?,
    val isLoading: Boolean,
)

sealed interface VaultListIntent {
    data class Search(val query: String?) : VaultListIntent
    data class ItemClicked(val id: String) : VaultListIntent
    data class RevealSecret(val id: String) : VaultListIntent
    data class CopySecret(val id: String) : VaultListIntent
    data object Lock : VaultListIntent
}

sealed interface VaultListEffect {
    data class OpenItem(val id: String) : VaultListEffect
    data class SecretRevealed(val value: String) : VaultListEffect
    data object SecretCopied : VaultListEffect
}
```

- Пошук: title + username, `lowercase()` contains, trim; `null`/порожній → усе.
- `RevealSecret`-Intent — виклик use case; успіх → effect `SecretRevealed`. Помилка (нешіснуючий id, запис без секрету) → effect НЕ emit (UI загального банера в цій хвилі немає).
- Фікс-тест: секрет у `state` не з'являється поле-за-полем, і `SecretRevealed` з `replay == 0` не перевидається повторно при re-collect.
- [x] **Step 1: Write failing tests:** emit списку (`isLoading` true→false після перших даних), фільтр регістр-незалежний, `Lock` викликає `LockVault`, Reveal → effect з секретом, Copy → `RecordingClipboard` + effect. Reveal для відсутнього id → без effect.
- [x] **Step 2: verify fails** `./gradlew :shared:presentation:jvmTest --tests '*VaultListStoreTest*'` — FAIL.
- [x] **Step 3: Implement.** У `init` — `observeItems.invoke()` collect у state (`isLoading = items.isEmpty()` не потрібен: порожній список валідний стан). Ефекти — приватний `MutableSharedFlow<VaultListEffect>(extraBufferCapacity = 0, replay = 0)`, відкритий як `SharedFlow`; send — `trySend`.
- [x] **Step 4: verify passes.**
- [x] **Step 5: Commit** `feat(presentation): vault list store with search and lock`

---

### Task 4: ItemDetailStore

**Files:**

- Create: `shared/presentation/src/commonMain/kotlin/com/kwazzar/lockruna/presentation/vault/ItemDetailStore.kt`
- Test: `shared/presentation/src/commonTest/kotlin/com/kwazzar/lockruna/presentation/vault/ItemDetailStoreTest.kt`

**Interfaces:**

```kotlin
class ItemDetailStore(
    getItem: GetItem,
    revealSecret: RevealSecret,
    copySecret: CopySecret,
    deleteItem: DeleteItem,
) {
    val state: StateFlow<ItemDetailState>
    val effects: SharedFlow<ItemDetailEffect>
    suspend fun open(id: String)
    suspend fun dispatch(intent: ItemDetailIntent)
}

data class ItemDetailState(val item: ItemUi?, val secretVisible: Boolean)

sealed interface ItemDetailIntent {
    data object Reveal : ItemDetailIntent
    data object Hide : ItemDetailIntent
    data object Copy : ItemDetailIntent
    data object Edit : ItemDetailIntent
    data object DeleteConfirmed : ItemDetailIntent
}

sealed interface ItemDetailEffect {
    data class NavigateToEdit(val id: String) : ItemDetailEffect
    data object Closed : ItemDetailEffect
    data class SecretRevealed(val value: String) : ItemDetailEffect
    data object SecretCopied : ItemDetailEffect
}
```

- Внутрішній transient var `revealedSecret: String?` — store тримає секрет лише поки `secretVisible == true`; `Hide` обнуляє. State НЕ містить його.
- `open(id)` — `GetItem` → state.item = `toItemUi()` або `null`.
- `DeleteConfirmed` — `DeleteItem(id)` успіх → effect `Closed` + `state.item = null` (тест фіксує, що після видалення повторний Reveal не працює і без винятків).

- [x] **Step 1: failing tests** (5 інтентів; Reveal → `SecretRevealed` при `secretValue != null`; Hide → помилка не викидається; Delete → `Closed`).
- [x] **Step 2: verify fails.**
- [x] **Step 3: implement.**
- [x] **Step 4: verify passes.**
- [x] **Step 5: Commit** `feat(presentation): item detail store with reveal and delete`

---

### Task 5: ItemEditStore

**Files:**

- Create: `shared/presentation/src/commonMain/kotlin/com/kwazzar/lockruna/presentation/vault/ItemEditStore.kt`
- Test: `shared/presentation/src/commonTest/kotlin/com/kwazzar/lockruna/presentation/vault/ItemEditStoreTest.kt`

**Interfaces:**

```kotlin
class ItemEditStore(
    createItem: CreateItem,
    updateItem: UpdateItem,
    getItem: GetItem,
) {
    val state: StateFlow<ItemEditState>
    val effects: SharedFlow<ItemEditEffect>
    suspend fun begin(existingId: String?)            // null → режим створення
    suspend fun dispatch(intent: ItemEditIntent)
}

data class ItemEditState(
    val existingId: String?,
    val title: String,
    val username: String,
    val url: String,
    val folderId: String?,
    val hasSecretDraft: Boolean,
    val isSaving: Boolean,
)

sealed interface ItemEditIntent {
    data class TitleChanged(val value: String) : ItemEditIntent
    data class UsernameChanged(val value: String) : ItemEditIntent
    data class SecretChanged(val value: String) : ItemEditIntent
    data class UrlChanged(val value: String) : ItemEditIntent
    data class FolderSelected(val folderId: String?) : ItemEditIntent
    data object Save : ItemEditIntent
}

sealed interface ItemEditEffect {
    data class Saved(val id: String) : ItemEditEffect
    data class ValidationError(val error: AppError) : ItemEditEffect
}
```

- Рішення про секрет у чернетці: **секрет не зберігається у State**. Приватний `var draftSecret: String` тримає введення (`SecretChanged`); у State замість нього — прапор `hasSecretDraft: Boolean`. Фінальні поля `ItemEditState`:
```kotlin
data class ItemEditState(
    val existingId: String?,
    val title: String,
    val username: String,
    val url: String,
    val folderId: String?,
    val hasSecretDraft: Boolean,
    val isSaving: Boolean,
)
```
- `begin(existingId != null)` — `GetItem` + наповнення полів; існуючий секрет у чернетку не копіюється (порожній, користувач друкує лише якщо змінює).
- `Save` — існуючий → `UpdateItem`, новий → `CreateItem(type = LOGIN, ...)`; невдача → `ValidationError` + `isSaving = false`. Успіх → effect `Saved(id)`; secrets не в Effect (оновлення екрану через `begin`).

- [ ] **Step 1: failing tests** (save успіх create/update; порожній title → ValidationError(InvalidItem); пустий secret для LOGIN → ValidationError; isSaving гейт).
- [ ] **Step 2: verify fails.**
- [ ] **Step 3: implement.**
- [ ] **Step 4: verify passes.**
- [ ] **Step 5: Commit** `feat(presentation): item edit store with draft state and validation`

---

### Task 6: Koin-модуль + архітектурний тест + повна верифікація

**Files:**

- Modify: `shared/presentation/build.gradle.kts` — `commonMain.dependencies { implementation(libs.koin.core) }` (поруч зі стандартним стилем файлу)
- Create: `shared/presentation/src/commonMain/kotlin/com/kwazzar/lockruna/presentation/di/PresentationModule.kt`
- Create: `architecture-tests/src/test/kotlin/com/kwazzar/lockruna/architecture/PresentationLayerTest.kt`

**Interfaces:**

```kotlin
package com.kwazzar.lockruna.presentation.di

val presentationModule = module {
    single { VaultListStore(ObserveItems(get()), LockVault(get()), RevealSecret(get()), CopySecret(get())) }
    single { ItemDetailStore(GetItem(get()), RevealSecret(get()), CopySecret(get()), DeleteItem(get())) }
    single { ItemEditStore(CreateItem(get(), get(), get()), UpdateItem(get(), get()), GetItem(get())) }
}
```

(`ObserveItems(get())` тощо — сигнатури див. в `shared/application/.../application/vault/*`; порядок параметрів — за їхніми конструкторами.)

Konsist-тест `PresentationLayerTest`:
- `shared/presentation/**` не імпортує `app.cash.sqldelight.`, `io.ktor.`, `androidx.`, `com.kwazzar.lockruna.domain.vault.VaultItemRepository` (прямі виклики репозиторію заборонені);
- класи `*Store` не звертаються до `com.kwazzar.lockruna.data.`.

- [ ] **Step 1: konsist тест RED** (не збирається — зʼявився файл) → run `./gradlew :architecture-tests:test` і зафіксувати FAIL на відсутність файлу.
- [ ] **Step 2: додати module + build dep** → GREEN.
- [ ] **Step 3: повна верифікація:**
  - `./gradlew ktlintCheck detekt`
  - `./gradlew :shared:presentation:jvmTest :shared:application:jvmTest :shared:domain:jvmTest :architecture-tests:test`
  - Expected: BUILD SUCCESSFUL. Збій (не середовище) = фікс тут.
- [ ] **Step 4: Commit** `feat(presentation): koin module and architecture guard`

## Порядок злиття

- Хвиля незалежна від `client-sync` і `client-auth`, злиття в будь-якому порядку. Koin-композиція всіх модулів — окрема нативна хвиля.
