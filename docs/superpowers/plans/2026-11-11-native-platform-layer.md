# Нативний platform-шар клієнта (Native Platform Layer) — Implementation Plan

> **Для агентних виконавців:** ОБОВ'ЯЗКОВО: використати `superpowers:subagent-driven-development` (рекомендовано) або `superpowers:executing-plans` для виконання цього плану по завданнях. Кроки використовують checkbox (`- [ ]`) для відстеження.

**Воркспейс:** гілка `feature/native-platform` від `main`. Файли, які ця хвиля володіє: `shared/data/src/*/kotlin/**/data/platform/**`, `shared/data/src/commonMain/**/data/auth/SecureAuthStateStore.kt`, `AuthSessionCodec.kt`, `shared/data/src/commonMain/**/data/di/CoreModule.kt`, `shared/data/src/commonMain/**/data/di/SyncModule.kt`, `shared/data/src/commonMain/**/data/di/AuthModule.kt`, `gradle/libs.versions.toml` (один рядок), `shared/data/build.gradle.kts`. Нативні застосунки (`iosApp/`, `androidApp/`), `shared/presentation/**` і `shared/umbrella/**` — **не** редагуються (це хвиля native-ui).

**Мета:** реальні платформні реалізації портів `AuthStateStore`, `SecureKeyStore`, `BiometricGate`, `Clipboard`, `Clock`, `IdGenerator`; фабрика `HttpClient`; Koin-модуль ядра (`coreModule`), щоб застосунок міг підняти весь граф без InMemory-заглушок.

**Архітектура:** `shared/data` — реалізації портів, домен не змінюється. Спільна логіка (серіалізація сесії, адаптер `SecureKeyStore` над захищеним сховищем) — у `commonMain` і тестується на JVM з in-memory сховищем. Платформне — мінімальні actual-класи: Android Keystore + androidx.biometric, iOS Keychain + LocalAuthentication + UIPasteboard. Один об'єкт на платформі реалізує і `BiometricGate`, і `SecureKeyStore`, щоб біометричний контекст використовувався один раз (без подвійного промпта) і ключ інвалідувався при зміні набору біометрії (US-07).

**Технології:** Kotlin 2.4.20, Koin 4.2.2, Ktor client 3.6.1 (OkHttp / Darwin), kotlinx-serialization, SQLDelight 2.4.1, Android Keystore + `androidx.biometric:biometric:1.1.0` (єдина нова залежність; зафіксувати точну версію в `libs.versions.toml` і згадати в коміті), iOS Security/LocalAuthentication/UIKit (Kotlin/Native cinterop, без Swift).

**Специфікація:** `docs/requirements.md` (US-07, US-08, US-06 clipboard, US-10 device id), `docs/adr/003-cryptography.md` (жодного власного крипто), `docs/adr/005-client-shared.md` §2/§8/§10, `docs/threat-model.md`.

## Global Constraints

- Нових портів у `domain` немає; `SecureStorage` — деталь `data`, не домен.
- Ключовий матеріал (`vaultKey`, `wrappedVaultKey`, `kdfSalt`) не логується, не потрапляє в `toString`, назви тестів і винятки. `AuthenticatedUser.toString()` уже редагований — зберегти.
- Android: `AuthStateStore` і `SecureKeyStore` — різні захищені сховища/ключі; після зміни біометрії недійсний лише ключ vault (сесія лишається, потрібен пароль).
- iOS: Keychain items `kSecAttrAccessibleWhenUnlockedThisDeviceOnly`, `kSecAttrSynchronizable = false`.
- 30-секундне очищення буфера — у реалізації `Clipboard`, не в UI.
- Код/коментарі/коміти англійською; docs/ українською.
- JDK 25: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.
- Не в обсязі: нативні екрани, композиція Koin у застосунках, тригери sync/автоблокування, `SettingsStore`, Register/Recovery/Devices, `AutoLockPolicy` з налаштувань, інтерцептор авто-refresh токена.

## Review Focus

- Перший запуск без збереженої сесії: `loadSession()` → `null`, без винятку; тест у Task 1.
- Пошкоджений/чужий запис у сховищі: `loadSession()` → `null` і запис очищено, а не краш; тест у Task 1.
- Після зміни набору біометрії `loadVaultKey()` → `null` (ключ інвалідовано), пароль лишається шляхом входу; тест у Task 3 (iOS) і ручна перевірка Android (Task 2).
- Буфер очищається через 30 с навіть якщо застосунок у фоні; тест у Task 5 (iOS-симулятор) / ручна перевірка Android.
- Повторний `saveVaultKey` перезаписує ключ, а не створює другий запис; ручна перевірка в Task 2.

---

### Task 1: Спільне сховище сесії

**Files:**

- Create: `shared/data/src/commonMain/kotlin/com/kwazzar/lockruna/data/platform/SecureStorage.kt`
- Create: `shared/data/src/commonMain/kotlin/com/kwazzar/lockruna/data/auth/AuthSessionCodec.kt`
- Create: `shared/data/src/commonMain/kotlin/com/kwazzar/lockruna/data/auth/SecureAuthStateStore.kt`
- Test: `shared/data/src/commonTest/kotlin/com/kwazzar/lockruna/data/auth/SecureAuthStateStoreTest.kt`
- Test support: `shared/data/src/commonTest/kotlin/com/kwazzar/lockruna/data/platform/InMemorySecureStorage.kt`

**Interfaces:**

- Produces:

```kotlin
interface SecureStorage {
    suspend fun read(key: String): ByteArray?
    suspend fun write(key: String, value: ByteArray)
    suspend fun delete(key: String)
}

class SecureAuthStateStore(private val storage: SecureStorage) : AuthStateStore
```

- `SecureAuthStateStore` ключ `"auth_session"`: JSON (`@Serializable` DTO, `ByteArray` → Base64) ↔ `AuthenticatedUser`; `loadSession()` на биті байти/невідомий формат → `storage.delete(...)` + `null`.
- `SecureKeyStore` реалізують платформні vault-об'єкти (Task 2/3), бо ключ vault прив'язаний до біометрії; спільного адаптера для нього немає.

- [x] **Step 1: Write the failing test.** Round-trip `saveSession` → `loadSession()` (усі поля, `tokens`), `clear()` → `null`, повторний save перезаписує; битий JSON → `null` + сховище порожнє; `AuthenticatedUser.toString()` не містить жодного байта ключа.
- [x] **Step 2: Run to verify it fails.** `./gradlew :shared:data:jvmTest --tests '*SecureAuthStateStoreTest*'` — FAIL.
- [x] **Step 3: Implement** кодек + сховище над `SecureStorage` (без нових залежностей: `kotlin.io.encoding.Base64`).
- [x] **Step 4: Run to verify it passes.** Та сама команда — PASS.
- [x] **Step 5: Commit.** `feat(data): secure session and vault key stores over a byte storage`

---

### Task 2: Android Keystore + біометрія + буфер

**Files:**

- Create: `shared/data/src/androidMain/kotlin/com/kwazzar/lockruna/data/platform/KeystoreSecureStorage.kt`
- Create: `shared/data/src/androidMain/kotlin/com/kwazzar/lockruna/data/platform/AndroidBiometricVault.kt`
- Create: `shared/data/src/androidMain/kotlin/com/kwazzar/lockruna/data/platform/AndroidClipboard.kt`
- Create: `shared/data/src/androidMain/kotlin/com/kwazzar/lockruna/data/platform/ActivityProvider.kt`
- Create: `shared/data/src/androidMain/kotlin/com/kwazzar/lockruna/data/di/AndroidPlatformModule.kt`
- Modify: `gradle/libs.versions.toml`, `shared/data/build.gradle.kts`

**Interfaces:**

- Produces:

```kotlin
interface ActivityProvider { fun current(): FragmentActivity? }

class KeystoreSecureStorage(context: Context) : SecureStorage
class AndroidBiometricVault(
    private val activityProvider: ActivityProvider,
    private val storage: SecureStorage,          // для ciphertext ключа vault
) : BiometricGate, SecureKeyStore
class AndroidClipboard(private val context: Context) : Clipboard

fun androidPlatformModule(context: Context, activityProvider: ActivityProvider): Module
```

- `AndroidBiometricVault`: AES-256-GCM ключ в AndroidKeyStore з `setUserAuthenticationRequired(true)` + `setInvalidatedByBiometricEnrollment(true)`; `authenticate()` запускає `BiometricPrompt` з `CryptoObject` від того ж cipher, тож `loadVaultKey()` після успіху не показує другий промпт. `KeyPermanentlyInvalidatedException` / `UnrecoverableKeyException` у `loadVaultKey()` → `storage.delete("vault_key")` + `null`.
- `AndroidClipboard`: `ClipData` + `ClipDescription.EXTRA_IS_SENSITIVE` (API 33+, `Build.VERSION` guard), очищення `clearPrimaryClip()` через 30 с у власному `CoroutineScope`.
- `androidPlatformModule` реєструє: `DatabaseDriverFactory` (з `initialize(context)`), `Clipboard`, `SecureStorage`, `AuthStateStore`, `BiometricGate` і `SecureKeyStore` (один інстанс `AndroidBiometricVault`), `DevicePlatform.ANDROID`.

- [x] **Step 1: Dependency.** Додати `androidx-biometric = { module = "androidx.biometric:biometric", version = "1.1.0" }` і `implementation(libs.androidx.biometric)` у `androidMain` (згадати в коміті).
- [x] **Step 2: Implement** три класи + модуль. `BiometricPrompt` — `authenticate(reason)` у `suspendCancellableCoroutine`; `isAvailable()` → `BiometricManager.from(context).canAuthenticate(BIOMETRIC_WEAK) == BIOMETRIC_SUCCESS`.
- [x] **Step 3: Compile.** `./gradlew :androidApp:assembleDebug` — BUILD SUCCESSFUL (застосунок ще не підключає модуль, це лише компіляція `shared:data`).
- [ ] **Step 4: Manual check (записати в PR).** На пристрої/емуляторі: save → load ключа; після додавання нового відбитка `loadVaultKey() == null`. Якщо пристрою немає — зафіксувати як environmental, не як pass.
- [x] **Step 5: Commit.** `feat(data): android keystore, biometrics and sensitive clipboard`

---

### Task 3: iOS Keychain + біометрія + буфер

**Files:**

- Create: `shared/data/src/iosMain/kotlin/com/kwazzar/lockruna/data/platform/KeychainSecureStorage.kt`
- Create: `shared/data/src/iosMain/kotlin/com/kwazzar/lockruna/data/platform/IosBiometricVault.kt`
- Create: `shared/data/src/iosMain/kotlin/com/kwazzar/lockruna/data/platform/IosClipboard.kt`
- Create: `shared/data/src/iosMain/kotlin/com/kwazzar/lockruna/data/di/IosPlatformModule.kt`
- Test: `shared/data/src/iosTest/kotlin/com/kwazzar/lockruna/data/platform/KeychainSecureStorageTest.kt`

**Interfaces:**

- Produces:

```kotlin
class KeychainSecureStorage : SecureStorage                       // generic password, ключ = service
class IosBiometricVault : BiometricGate, SecureKeyStore           // item з SecAccessControl .biometryCurrentSet
class IosClipboard : Clipboard                                    // UIPasteboard, expirationDate +30s, localOnly = true

fun iosPlatformModule(): Module
```

- `IosBiometricVault` тримає `LAContext` успішної автентифікації й передає його в `SecItemCopyMatching` через `kSecUseAuthenticationContext`, тому промпт один; зміна набору біометрії інвалідує item (`.biometryCurrentSet`).
- `iosPlatformModule` реєструє `DatabaseDriverFactory`, `Clipboard`, `SecureStorage`, `AuthStateStore`, `BiometricGate`+`SecureKeyStore` (один інстанс), `DevicePlatform.IOS`.

- [x] **Step 1: Write the failing test.** `KeychainSecureStorageTest`: write/read/delete round-trip для двох різних ключів; повторний write перезаписує; `delete` неіснуючого — без винятку.
- [x] **Step 2: Run to verify it fails.** `./gradlew :shared:data:iosSimulatorArm64Test --tests '*KeychainSecureStorageTest*'` — FAIL.
- [x] **Step 3: Implement** Keychain-сховище (Kotlin/Native `platform.Security`), потім біометричний vault і буфер (без тестів: Face ID у симуляторі не автоматизується).
- [x] **Step 4: Run to verify it passes.** Та сама команда — PASS.
- [x] **Step 5: Compile the framework.** `./gradlew :shared:umbrella:assemble` — BUILD SUCCESSFUL.
- [x] **Step 6: Commit.** `feat(data): ios keychain, biometric vault and sensitive clipboard`

---

### Task 4: Фабрики платформи та coreModule

**Files:**

- Create: `shared/data/src/commonMain/kotlin/com/kwazzar/lockruna/data/platform/PlatformFactories.kt`
- Create: `shared/data/src/androidMain/kotlin/com/kwazzar/lockruna/data/platform/PlatformFactories.android.kt`
- Create: `shared/data/src/iosMain/kotlin/com/kwazzar/lockruna/data/platform/PlatformFactories.ios.kt`
- Create: `shared/data/src/jvmMain/kotlin/com/kwazzar/lockruna/data/platform/PlatformFactories.jvm.kt`
- Create: `shared/data/src/commonMain/kotlin/com/kwazzar/lockruna/data/di/CoreModule.kt`
- Modify: `shared/data/src/commonMain/kotlin/com/kwazzar/lockruna/data/di/SyncModule.kt`
- Modify: `shared/data/src/commonMain/kotlin/com/kwazzar/lockruna/data/di/AuthModule.kt`

**Interfaces:**

- Produces:

```kotlin
expect fun platformClock(): Clock
expect fun platformIdGenerator(): IdGenerator
expect fun platformHttpClient(): HttpClient

fun coreModule(autoLockPolicy: AutoLockPolicy = AutoLockPolicy.ONE_MINUTE): Module
fun syncModule(httpClient: HttpClient, baseUrl: String): Module   // AccessTokenProvider береться з Koin
```

- `platformClock`: `System.currentTimeMillis()` / `SystemClock.elapsedRealtime()` (Android), `NSDate` / `System.nanoTime()`-еквівалент (iOS — `NSDate.date.timeIntervalSince1970`, монотонний — `mach_absolute_time` через `platform.posix.clock_gettime`).
- `platformIdGenerator`: `UUID.randomUUID()` / `NSUUID.UUID().UUIDString`.
- `platformHttpClient`: OkHttp (android/jvm), Darwin (ios) з `ContentNegotiation(Json)` і `HttpTimeout` 30 с.
- `coreModule` реєструє: `Clock`, `IdGenerator`, `VaultSession`, `PlatformCrypto`, `PlatformNormalizer`, `Argon2Factory.create()`, `PassKeeperDatabase` (з `get<DatabaseDriverFactory>().create()`, фабрику реєструє platform-модуль), `VaultItemRepository`, vault use cases (`CreateItem`/`UpdateItem`/`DeleteItem`/`GetItem`/`ObserveItems`/`RevealSecret`/`CopySecret`), `ObserveVaultState`/`LockVault`, `ApplyAutoLockPolicy` (з `autoLockPolicy`).
- `syncModule` більше не приймає `accessTokenProvider` — усередині `get()` (провайдер реєструє `authModule`).

- [x] **Step 1: Write the failing test.** У `shared/data/src/jvmTest` — тест графа: `startKoin { modules(coreModule(), jvmPlatformTestModule()) }` (fake `DatabaseDriverFactory` через `PassKeeperDatabase(JdbcSqliteDriver(IN_MEMORY))`, fake `Clipboard`, `AuthStateStore`, `SecureKeyStore`, `BiometricGate`) → `koin.get<VaultItemRepository>()`, `get<CreateItem>()`, `get<ObserveVaultState>()`, `get<ApplyAutoLockPolicy>()` не null.
- [x] **Step 2: Run to verify it fails.** `./gradlew :shared:data:jvmTest --tests '*CoreModuleTest*'` — FAIL.
- [x] **Step 3: Implement** фабрики + `coreModule`; у `SyncModule` прибрати параметр `accessTokenProvider` (брати `get()`); в `AuthModule` прибрати біндинг `InMemoryAuthStateStore` — `AuthStateStore` реєструє platform-модуль.
- [x] **Step 4: Run to verify it passes.** `./gradlew :shared:data:jvmTest :shared:umbrella:assemble :androidApp:assembleDebug` — BUILD SUCCESSFUL.
- [x] **Step 5: Full verification.** `./gradlew ktlintCheck detekt` + `./gradlew :shared:data:iosSimulatorArm64Test` — BUILD SUCCESSFUL (iOS-тести: macOS).
- [x] **Step 6: Commit.** `feat(data): platform factories and the core koin module`

## Порядок злиття

- Незалежна від `client-auth-session-presentation` (різні файли). `native-ui` мерджити **після** цієї хвилі: UI-композиція бере `androidPlatformModule`/`iosPlatformModule` і `coreModule`.
