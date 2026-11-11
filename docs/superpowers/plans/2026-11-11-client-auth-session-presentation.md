# Презентація: вхід, розблокування, сесія (Client Auth & Session Presentation) — Implementation Plan

> **Для агентних виконавців:** ОБОВ'ЯЗКОВО: використати `superpowers:subagent-driven-development` (рекомендовано) або `superpowers:executing-plans` для виконання цього плану по завданнях. Кроки використовують checkbox (`- [x]`) для відстеження.

**Воркспейс:** гілка `feature/client-auth-session-presentation` від `main`. Файли, які ця хвиля володіє (інші хвилі їх не редагують): `shared/presentation/src/commonMain/**/presentation/auth/**`, `.../presentation/session/**`, `shared/presentation/src/commonMain/**/presentation/di/PresentationModule.kt`, `shared/presentation/src/commonTest/**/presentation/support/**`, `shared/application/src/commonMain/**/application/auth/Login.kt`, `shared/application/src/commonTest/**/application/auth/LoginTest.kt`.

**Мета:** MVI-стори `LoginStore`, `UnlockStore`, `SessionStore` поверх наявних use cases, без змін їхніх сигнатур (крім `Login` — див. Task 0), плюс Koin-реєстрація і Konsist-гард.

**Архітектура:** `shared/presentation` — Store-класи з чистою логікою в `commonTest`; State містить лише прості типи й `AppError`; майстер-пароль ніколи не потрапляє в State/logs/`toString` — лише приватне поле і прапор `hasPassword`. Store викликає тільки use cases.

**Технології:** Kotlin 2.4.20, kotlinx-coroutines 1.11.0 (StateFlow/SharedFlow), koin-core 4.2.2, kotlin.test. Нових залежностей немає.

**Специфікація:** `docs/requirements.md` (US-02, US-07, US-08), `docs/adr/002-clean-architecture-mvi.md`, `docs/adr/005-client-shared.md` §9, `docs/threat-model.md`.

## Global Constraints

- Інваріанти — в агрегатах; Store не робить `copy(version = ...)` і не торкається репозиторіїв, Ktor чи `CryptoService`.
- Пароль: тільки приватне поле Store і `Intent.PasswordChanged` (його `toString()` редагує значення, як `ItemEditIntent.SecretChanged`); у `State` — `hasPassword: Boolean`.
- Помилки: `AppError` у State, не винятки. Невірний пароль і неіснуючий email дають однаковий `AppError.InvalidCredentials` — UI не розрізняє.
- Назви: `<Screen>Store`, `<Screen>State`, `<Screen>Intent`, `<Screen>Effect`; `factory` у Koin; жодних `Base*`.
- Код/коментарі/коміти англійською; docs/ українською.
- JDK 25: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.
- Не в обсязі: `RegisterStore` (потребує BIP39 — окрема хвиля), `SettingsStore`, `DevicesStore`, `RecoveryStore`, статус sync, нативний UI, реальні Keychain/Keystore.

## Review Focus

- Подвійний Submit (подвійний тап) не робить двох `prelogin/login` — один виклик, другий ігнорується; тест у Task 1.
- Пароль не з'являється в `state.toString()`, логах і `Intent.toString()`; тест у Task 1/2.
- Біометрія недоступна/відмовили — пароль лишається робочим шляхом, без блокування екрана; тест у Task 2.
- Повернення з фону після спливу таймауту блокує сесію і стирає ключ із пам'яті; тест у Task 3.
- `Resumed` без попереднього `Backgrounded` (перший старт) не блокує щойно розблоковану сесію; тест у Task 3.

---

### Task 0: Платформа пристрою в `CredentialsCommand`

**Files:**

- Modify: `shared/application/src/commonMain/kotlin/com/kwazzar/lockruna/application/auth/Login.kt`
- Test: `shared/application/src/commonTest/kotlin/com/kwazzar/lockruna/application/auth/LoginTest.kt`

**Interfaces:**

- Produces: `data class CredentialsCommand(val email: String, val password: String, val deviceName: String, val deviceId: String?, val platform: DevicePlatform)`; `Login` передає `platform` у `DeviceIdentity` замість константи `DevicePlatform.ANDROID`.

- [x] **Step 1: Write the failing test.** У `LoginTest` додати тест: `CredentialsCommand(..., platform = DevicePlatform.IOS)` → `FakeAuthApi.lastLoginDevice?.platform == DevicePlatform.IOS`. У `FakeAuthApi` (application `commonTest/support/TestFakes.kt`) додати `var lastLoginDevice: DeviceIdentity? = null` і зберігати його в `login(...)`.
- [x] **Step 2: Run to verify it fails.** `./gradlew :shared:application:jvmTest --tests '*Login*'` — FAIL (немає поля `platform`).
- [x] **Step 3: Implement.** Додати `platform` у `CredentialsCommand`, прибрати `DevicePlatform.ANDROID` з `Login`.
- [x] **Step 4: Run to verify it passes.** Та сама команда — PASS.
- [x] **Step 5: Commit.** `feat(auth): take the device platform from the login command`

---

### Task 1: LoginStore

**Files:**

- Create: `shared/presentation/src/commonMain/kotlin/com/kwazzar/lockruna/presentation/auth/LoginStore.kt`
- Test: `shared/presentation/src/commonTest/kotlin/com/kwazzar/lockruna/presentation/auth/LoginStoreTest.kt`
- Test support: `shared/presentation/src/commonTest/kotlin/com/kwazzar/lockruna/presentation/support/AuthTestSupport.kt`

**Interfaces:**

- Consumes: `Login`, `CredentialsCommand`, `DevicePlatform`, `AppError`.
- Produces:

```kotlin
class LoginStore(
    private val login: Login,
    private val platform: DevicePlatform,
) {
    val state: StateFlow<LoginState>
    val effects: SharedFlow<LoginEffect>
    suspend fun dispatch(intent: LoginIntent)
    fun close()
}

data class LoginState(
    val email: String = "",
    val deviceName: String = "",
    val hasPassword: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: AppError? = null,
)

sealed interface LoginIntent {
    data class EmailChanged(val value: String) : LoginIntent
    data class PasswordChanged(val value: String) : LoginIntent   // toString() → redacted
    data class DeviceNameChanged(val value: String) : LoginIntent
    data object Submit : LoginIntent
    data object ErrorDismissed : LoginIntent
}

sealed interface LoginEffect { data object LoggedIn : LoginEffect }
```

- [x] **Step 1: Write the failing tests.** У `AuthTestSupport` — фейки `FakeAuthApi` (з `loginCalls: Int`), `FakeArgon2`, `FakePasswordNormalizer`, `FakeAuthStateStore`, `FakeCryptoService` (уже є в `support/TestSupport.kt`), щоб підняти справжній `Login`. Тести: успіх → `effects` дає `LoggedIn`, `state.isSubmitting == false`, `VaultSession.state == Unlocked`; невірний пароль → `state.error == AppError.InvalidCredentials`, effect не емітиться; два `Submit` поспіль → `loginCalls == 1`; `state.toString()` не містить введеного пароля.
- [x] **Step 2: Run to verify it fails.** `./gradlew :shared:presentation:jvmTest --tests '*LoginStoreTest*'` — FAIL.
- [x] **Step 3: Implement.** Приватні `draftPassword: String`; `Submit` під `Mutex.tryLock` (як `ItemEditStore`), виклик `login(CredentialsCommand(email, draftPassword, deviceName, deviceId = null, platform))`, успіх → `LoggedIn`, невдача → `state.error`, `isSubmitting = false`; очищати `draftPassword` і `hasPassword` після успіху; `close()` скасовує scope (той самий патерн, що `VaultListStore`).
- [x] **Step 4: Run to verify it passes.** Та сама команда — PASS.
- [x] **Step 5: Commit.** `feat(presentation): login store with redacted password and single-flight submit`

---

### Task 2: UnlockStore

**Files:**

- Create: `shared/presentation/src/commonMain/kotlin/com/kwazzar/lockruna/presentation/auth/UnlockStore.kt`
- Test: `shared/presentation/src/commonTest/kotlin/com/kwazzar/lockruna/presentation/auth/UnlockStoreTest.kt`

**Interfaces:**

- Consumes: `UnlockWithPassword`, `UnlockWithBiometrics`, `EnableBiometrics`, `BiometricGate`.
- Produces:

```kotlin
class UnlockStore(
    private val unlockWithPassword: UnlockWithPassword,
    private val unlockWithBiometrics: UnlockWithBiometrics,
    private val enableBiometrics: EnableBiometrics,
    private val biometricGate: BiometricGate,
) {
    val state: StateFlow<UnlockState>
    val effects: SharedFlow<UnlockEffect>
    suspend fun dispatch(intent: UnlockIntent)
    fun close()
}

data class UnlockState(
    val email: String = "",
    val hasPassword: Boolean = false,
    val isUnlocking: Boolean = false,
    val biometricAvailable: Boolean = false,
    val error: AppError? = null,
)

sealed interface UnlockIntent {
    data class EmailChanged(val value: String) : UnlockIntent
    data class PasswordChanged(val value: String) : UnlockIntent   // toString() → redacted
    data object SubmitPassword : UnlockIntent
    data object SubmitBiometrics : UnlockIntent
    data object EnableBiometrics : UnlockIntent
    data object ErrorDismissed : UnlockIntent
}

sealed interface UnlockEffect {
    data object Unlocked : UnlockEffect
    data object BiometricsEnabled : UnlockEffect
}
```

- [x] **Step 1: Write the failing tests.** `init` питає `biometricGate.isAvailable()` → `biometricAvailable`; пароль-успіх → `Unlocked` + `VaultSession` розблокована; пароль-невдача → `state.error == InvalidCredentials`, без effect; біометрія-успіх → `Unlocked`; відмова біометрії → `state.error`, парольний шлях далі працює; `EnableBiometrics` → `BiometricsEnabled`; `state.toString()` без пароля.
- [x] **Step 2: Run to verify it fails.** `./gradlew :shared:presentation:jvmTest --tests '*UnlockStoreTest*'` — FAIL.
- [x] **Step 3: Implement.** Той самий патерн, що Task 1 (приватний `draftPassword`, `Mutex.tryLock`, `isUnlocking`); `init` — `scope.launch { state = state.copy(biometricAvailable = biometricGate.isAvailable()) }`.
- [x] **Step 4: Run to verify it passes.** Та сама команда — PASS.
- [x] **Step 5: Commit.** `feat(presentation): unlock store for password and biometrics`

---

### Task 3: SessionStore

**Files:**

- Create: `shared/presentation/src/commonMain/kotlin/com/kwazzar/lockruna/presentation/session/SessionStore.kt`
- Test: `shared/presentation/src/commonTest/kotlin/com/kwazzar/lockruna/presentation/session/SessionStoreTest.kt`

**Interfaces:**

- Consumes: `ObserveVaultState`, `LockVault`, `ApplyAutoLockPolicy`.
- Produces:

```kotlin
class SessionStore(
    private val observeVaultState: ObserveVaultState,
    private val lockVault: LockVault,
    private val applyAutoLockPolicy: ApplyAutoLockPolicy,
) {
    val state: StateFlow<SessionState>
    val effects: SharedFlow<SessionEffect>
    suspend fun dispatch(intent: SessionIntent)
    fun close()
}

data class SessionState(val isLocked: Boolean = true)

sealed interface SessionIntent {
    data object Lock : SessionIntent
    data class Resumed(val backgroundedAtMonotonicMillis: Long) : SessionIntent
}

sealed interface SessionEffect { data object Locked : SessionEffect }
```

- [x] **Step 1: Write the failing tests.** (справжні use cases + `VaultSession` + `FakeClock`): старт → `isLocked == true`; `session.unlock(...)` → `isLocked == false`; `Resumed(now - 61_000)` при політиці 1 хвилина → `Locked`, `session.currentVaultKey() == null`; `Resumed(now - 10)` → лишається розблокованою; `Lock` → `Locked`.
- [x] **Step 2: Run to verify it fails.** `./gradlew :shared:presentation:jvmTest --tests '*SessionStoreTest*'` — FAIL.
- [x] **Step 3: Implement.** `init` збирає `observeVaultState()` у `state.isLocked`; `Resumed` викликає `applyAutoLockPolicy(backgroundedAt)` і на `true` емітить `Locked`; `Lock` → `lockVault()` + `Locked`; `close()`.
- [x] **Step 4: Run to verify it passes.** Та сама команда — PASS.
- [x] **Step 5: Commit.** `feat(presentation): session store with auto-lock on resume`

---

### Task 4: AppGate — стартовий екран

**Files:**

- Create: `shared/presentation/src/commonMain/kotlin/com/kwazzar/lockruna/presentation/app/AppGate.kt`
- Test: `shared/presentation/src/commonTest/kotlin/com/kwazzar/lockruna/presentation/app/AppGateTest.kt`

**Interfaces:**

- Produces:

```kotlin
enum class AppScreen { UNLOCK, LOGIN, VAULT }

/** Locked with a stored session → unlock; locked without one → sign in. */
fun startScreen(isLocked: Boolean, hasSession: Boolean): AppScreen
```

- [x] **Step 1: Write the failing test.** Три комбінації: `(true, true) → UNLOCK`, `(true, false) → LOGIN`, `(false, false) → VAULT`.
- [x] **Step 2: Run to verify it fails.** `./gradlew :shared:presentation:jvmTest --tests '*AppGateTest*'` — FAIL.
- [x] **Step 3: Implement** `when` на три гілки.
- [x] **Step 4: Run to verify it passes.** Та сама команда — PASS.
- [x] **Step 5: Commit.** `feat(presentation): start screen gate for the app shell`

---

### Task 5: Koin + Konsist + повна верифікація

**Files:**

- Modify: `shared/presentation/src/commonMain/kotlin/com/kwazzar/lockruna/presentation/di/PresentationModule.kt`
- Modify: `shared/presentation/src/commonTest/kotlin/com/kwazzar/lockruna/presentation/di/PresentationModuleTest.kt`
- Modify: `architecture-tests/src/test/kotlin/com/kwazzar/lockruna/architecture/PresentationLayerTest.kt`

**Interfaces:**

- Produces: `presentationModule` реєструє `LoginStore`, `UnlockStore`, `SessionStore` як `factory`.

- [x] **Step 1: Extend the module test (RED).** Додати `single { AutoLockPolicy.ONE_MINUTE }`, `single<BiometricGate>`, `single<AuthStateStore>`; перевірити `koin.get<LoginStore>()`, `get<UnlockStore>()`, `get<SessionStore>()`, і що кожен `factory` дає новий інстанс.
- [x] **Step 2: Run to verify it fails.** `./gradlew :shared:presentation:jvmTest --tests '*PresentationModuleTest*'` — FAIL.
- [x] **Step 3: Register stores.** `factory { LoginStore(get(), get()) }`, `factory { UnlockStore(get(), get(), get(), get()) }`, `factory { SessionStore(get(), get(), get()) }`. `DevicePlatform` — біндинг платформи (`single { DevicePlatform.ANDROID }` у тесті; нативний — у хвилі native-platform).
- [x] **Step 4: Konsist.** Додати правило: файли `presentation/auth/*Store.kt` і `presentation/session/*Store.kt` імпортують `com.kwazzar.lockruna.application.` і не імпортують `com.kwazzar.lockruna.data.`, `io.ktor.`, `app.cash.sqldelight.`; `*State` не має властивостей з іменами `password`, `secret`, `vaultKey`.
- [x] **Step 5: Full verification.** `./gradlew :shared:presentation:jvmTest :shared:application:jvmTest :shared:domain:jvmTest :architecture-tests:test ktlintCheck detekt` — BUILD SUCCESSFUL.
- [x] **Step 6: Commit.** `feat(presentation): register auth and session stores`

## Порядок злиття

- Незалежна від `native-platform`; для `native-ui` — мерджити **до** неї (UI бере ці стори).
