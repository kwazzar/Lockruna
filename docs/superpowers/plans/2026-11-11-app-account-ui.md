# Застосунки: акаунт, налаштування та тригери синку (App Account UI) — Implementation Plan

> **Для агентних виконавців:** ОБОВ'ЯЗКОВО: використати `superpowers:subagent-driven-development` (рекомендовано) або `superpowers:executing-plans` для виконання цього плану по завданнях. Кроки використовують checkbox (`- [ ]`) для відстеження.

**Воркспейс:** гілка `feature/app-account-ui` від `main`, **після** мерджу WS1 і WS2. Володіє: `androidApp/**`, `iosApp/**`, `shared/umbrella/**` (і `shared/umbrella/build.gradle.kts` за потреби). `shared/data/**`, `shared/presentation/**`, `shared/application/**`, `shared/domain/**`, `server/**` — не редагуються.

**Goal:** Зробити робочими екрани реєстрації/відновлення/пристроїв/налаштувань і форми CARD/NOTE/папок на SwiftUI та Compose, підключити налаштування автоблокування, вмикання біометрії, видалення акаунта і тригери синхронізації.

**Architecture:** UI лишається тонким: читає `state`, шле `intent`, реагує на `effects`. Композиційний корінь — у застосунку. Синк викликається через `SyncNow` (Koin) на тригерах: unlock, resume, після локального запису (debounce 2 с), ручна кнопка, повернення мережі (foreground-only у цій хвилі; WorkManager/BGTaskScheduler — окремо). iOS — через фасад `LockrunaIos` (нові getter-и).

**Tech Stack:** Compose BOM/Material3 (є в `androidApp`), SwiftUI + SKIE 0.10.15, Kotlin 2.4.20, Koin 4.2.2. Нових залежностей немає.

**Spec:** `docs/requirements.md` (US-01, US-03, US-04, US-05, US-06, US-07, US-08, US-09, US-10, US-11), `docs/adr/002-clean-architecture-mvi.md`, `docs/adr/005-client-shared.md` §9, `docs/threat-model.md`.

**Залежність:** WS1 + WS2. До їхнього мерджу можна почати лише з тригерів синку (використовує наявний `SyncNow`) — інше заблоковано.

## Global Constraints

- Стори й сигнатури — з WS2 (`RegisterStore`, `RecoveryStore`, `DevicesStore`, `SettingsStore`) і наявних `VaultListStore`, `ItemDetailStore`, `ItemEditStore`. Змінювати стори заборонено.
- Секрет ніколи не потрапляє в UI-стан: тільки локальна змінна на час показу, очищається при Hide/закритті/фоні.
- Пароль — `SecureField`/`PasswordVisualTransformation`, не зберігається в `remember`/`@State` після Submit.
- Автоблокування: у фоні фіксуємо monotonic-час, на resume шлемо `SessionIntent.Resumed(ts)`; таймаут береться з `SettingsStore`.
- Приватність: Android `FLAG_SECURE`; iOS — блюр-оверлей при `scenePhase != .active`, банер після скриншота.
- Accessibility: таргети ≥ 44 pt, `contentDescription`, Dynamic Type, контраст.
- Код/коментарі/коміти англійською; docs/ українською.
- JDK 25: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.
- Поза обсягом: Register/Recovery/Devices/Settings на рівні OS-автозаповнення, WorkManager/BGTaskScheduler, зміна майстер-пароля поза recovery, локалізація.

## Review Focus

- Старт застосунку при замкненій сесії показує unlock/login, не флеш списку (WS2 додає `REGISTER` — навігація має це врахувати).
- Подвійний Submit не дублює запит (WS2 вже захищає; UI не має обходити).
- Recovery key показується один раз, підтверджується повторним введенням; після підтвердження очищається з пам'яті UI.
- Видалення акаунта вимагає повторного пароля і очищає локальну БД/сесію.
- Синк-тригери не блокують UI-потік (корутини/Task).
- Поворот екрана не втрачає введений email і не дублює Submit.

## File Structure

- Modify: `androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/AppRoot.kt` — навігація з урахуванням `AppScreen.REGISTER/RECOVERY/DEVICES/SETTINGS`.
- Create: `androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/RegisterScreen.kt`, `RecoveryScreen.kt`, `DevicesScreen.kt`, `SettingsScreen.kt`.
- Modify: `androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/ItemEditScreen.kt` — тип + CARD/NOTE-поля + вибір папки.
- Modify: `androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/VaultListScreen.kt` — фільтр/групування папок, кнопка Sync.
- Modify: `shared/umbrella/src/commonMain/kotlin/com/kwazzar/lockruna/umbrella/LockrunaIos.kt` — getter-и нових сторів.
- Modify: `iosApp/Lockruna/LockrunaApp.swift`, `iosApp/Lockruna/ContentView.swift` — екрани, навігація, тригери.

## Task 1: iOS фасад — нові стори

**Files:**
- Modify: `shared/umbrella/src/commonMain/kotlin/com/kwazzar/lockruna/umbrella/LockrunaIos.kt`
- Modify: `shared/umbrella/src/commonMain/kotlin/com/kwazzar/lockruna/umbrella/SharedCore.kt` (якщо потрібно)

**Interfaces:**
- Consumes: стори з WS2.
- Produces: `LockrunaIos.registerStore()`, `recoveryStore()`, `devicesStore()`, `settingsStore()`.

- [x] **Step 1:** Додати поля-рефи та getter-и, ініціалізацію в `start()`.
- [x] **Step 2:** Скомпілювати `:shared:umbrella:assemble`.
- [x] **Step 3:** Commit: `feat(umbrella): expose account lifecycle stores to iOS`.

## Task 2: Android — реєстрація та recovery

**Files:**
- Create: `androidApp/.../ui/RegisterScreen.kt`, `RecoveryScreen.kt`
- Modify: `androidApp/.../ui/AppRoot.kt`

- [ ] **Step 1:** Екран реєстрації: email/password/confirm, валідація (12+ символів), після успіху — показ 12 слів з `Effect.RecoveryKeyReady`, підтвердження повторним введенням, потім перехід у vault.
- [x] **Step 2:** Екран відновлення: email + введення 12 слів + новий пароль → `RecoveryStore` → перехід у vault.
- [x] **Step 3:** Навігація `AppScreen` з урахуванням нових станів.
- [x] **Step 4:** Commit: `feat(android): add registration and recovery screens`.

## Task 3: Android — пристрої та налаштування

**Files:**
- Create: `androidApp/.../ui/DevicesScreen.kt`, `SettingsScreen.kt`
- Modify: `androidApp/.../ui/VaultListScreen.kt` (меню/кнопки)

- [x] **Step 1:** Екран пристроїв: список, позначка поточного, відкликання з підтвердженням.
- [x] **Step 2:** Екран налаштувань: 4 варіанти автоблокування, перемикач біометрії (виклик `EnableBiometrics`), видалення акаунта з повторним паролем.
- [x] **Step 3:** Кнопка ручного Sync + індикатор стану.
- [x] **Step 4:** Commit: `feat(android): add devices, settings and manual sync`.

## Task 4: Android — CARD/NOTE і папки

**Files:**
- Modify: `androidApp/.../ui/ItemEditScreen.kt`, `VaultListScreen.kt`

- [x] **Step 1:** Селектор типу при створенні; додаткові поля для CARD/NOTE.
- [x] **Step 2:** Вибір папки при редагуванні; створення папки.
- [x] **Step 3:** Commit: `feat(android): add card/note forms and folders`.

## Task 5: iOS — екрани (SwiftUI)

**Files:**
- Modify: `iosApp/Lockruna/LockrunaApp.swift`, `iosApp/Lockruna/ContentView.swift`
- Create: окремі view-файли за потреби

- [x] **Step 1:** Register/Recovery/Devices/Settings views через фасад.
- [ ] **Step 2:** CARD/NOTE-форми, папки.
- [x] **Step 3:** Автоблокування з налаштувань, біометрія, ручний Sync.
- [x] **Step 4:** Commit: `feat(ios): add account lifecycle screens`.

## Task 6: Тригери синхронізації (Android + iOS)

**Files:**
- Modify: `androidApp/.../ui/AppRoot.kt` (lifecycle observer)
- Modify: `iosApp/Lockruna/LockrunaApp.swift` (scenePhase + NWPathMonitor)

- [x] **Step 1:** Android: виклик `SyncNow` на unlock, resume, після збереження запису (debounce 2 с), ручна кнопка, повернення мережі (ConnectivityManager callback).
- [ ] **Step 2:** iOS: те саме через фасад, `NWPathMonitor` для мережі.
- [x] **Step 3:** Переконатися, що виклики не блокують UI (корутини/Task).
- [x] **Step 4:** Commit: `feat(apps): wire sync triggers`.

## Task 7: Перевірка

- [x] **Step 1:** `./gradlew :androidApp:assembleDebug`.
- [x] **Step 2:** `./gradlew :shared:umbrella:assemble`.
- [x] **Step 3:** XcodeBuildMCP: збірка iOS і запуск на симуляторі (AGENTS: не raw xcodebuild).
- [x] **Step 4:** `./scripts/verify.sh --ios` (з env-змінними для Docker, якщо потрібні).
- [x] **Step 5:** `gitleaks detect --source . --no-banner`.
- [x] **Step 6:** Commit: `docs(apps): update graphify`.

## Spec coverage

US-01, US-03, US-04, US-05, US-06, US-07, US-08, US-09, US-10, US-11 (клієнтська/UI частина). WorkManager/BGTaskScheduler і Mongo-audit — окремі хвилі.
