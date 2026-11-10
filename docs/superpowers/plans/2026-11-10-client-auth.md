# Автентифікація клієнта (Client Auth) — Implementation Plan

> **Для агентних виконавців:** ОБОВ'ЯЗКОВО: використати `superpowers:subagent-driven-development` (рекомендовано) або `superpowers:executing-plans` для виконання цього плану по завданнях. Кроки використовують checkbox (`- [ ]`) для відстеження.

**Мета:** Серверна автентифікація клієнта: Register, Login, offline Unlock (пароль), Unlock біометрією, EnableBiometrics, Logout — через порт `AuthApi`, Ktor-реалізацію, сховище сесії та токенів у `shared/data`; плюс реалізація `AccessTokenProvider` для sync-хвилі.

**Архітектура:** `shared/domain/auth` — порт `AuthApi` + типи ({link} AuthenticatedUser, AuthTokens) + порти `AuthStateStore`, `BiometricPolicyStore`; `shared/application/auth` — use cases; `shared/data/auth` — `KtorAuthApi` (DTO-мапінг тільки тут), `SessionAccessTokenProvider`, Koin-модуль. Реальний Keychain/Keystore для `AuthStateStore` і `SecureKeyStore` — поточная нативна хвиля: у цій хвилі порт + InMemory impl.

**Технології:** Kotlin 2.4.20, Ktor client 3.6.1 (але ВИКОРИСТОВУЄ наявні toml-рядки ктор-клієнта, які додає client-sync — незалежна точка), kotlinx-serialization, kotlin.test. Koin core — лише для ди-модуля без нових версій.

**Специфікація:** `docs/requirements.md` (US-01, US-02, US-07, US-08), `docs/adr/003-cryptography.md` (ієрархія ключів — §2 і §4), `docs/adr/005-client-shared.md` §3 і §8, `docs/openapi.yaml` (`/auth/*`), `docs/threat-model.md`.

## Global Constraints

- **Ієрерія ключів (ADR-003 §2), не змінюється:** `masterKey = Argon2id( password, kdfSalt )` → `authKey = HKDF(masterKey, "passkeeper/auth/v1")` → `kek = HKDF(masterKey, "passkeeper/kek/v1")` → `wrapped_vault_key = AES-GCM(kek, vaultKey)`.
- **ЗАТВЕРДЖЕНО РІШЕННЯ (2026-11-10): AAD для wrapped_vault_key та recovery_wrapped_vault_key = email (UTF-8 байти).** Заміна `userId` з ADR-003 §2.5: `userId` клієнт отримує тільки з відповідей register/login, а email відомий уже під час register (wrap відбувається в request) і під час login/recovery (unwrap). Task 0 оновлює ADR-003 і один рядок threat-model. Сервер жодних змін не потребує: байти wrapped-ключа для нього непрозорі; email так само, як userId, унеможливлює підміну wrapped-ключа між акаунтами.
- Пароль перед KDF нормалізується NFKC через порт `PasswordNormalizer` (домен), не natively.
- Client перевіряє `kdfSr` через `KdfParams.isAcceptable` на prelogin — інакше `Failure(AppError.WeakKdfParams)`.
- 401 при login — одна гілка (`InvalidCredentials`), зі `same` для неправильного пароля й неіснуючого email; безРозбіжна інша кодировка.
- Секрети: `authKey`, `refreshToken`, `trait material`, пароль — не в лог що, `toString()`, фікстури. Test strings — `"secret"` / dump.
- Наявна use case-вока `ValidateMasterPassword` (12+ символів) використовується що є, без редагувань.
- Перезапуск app — сесія заблокована, бо `VaultSession` живе в пам'яті; тому «після рестарку» RequireMasterPassword вже задоволено природно.
- 5 невдалих спроб біометрії в процесі → password-required (в пам'яті processes counter). 7-днівний гейт — з `BiometricPolicyStore` (зберігається).
- Код/коментарі/коміти англійською; docs/ українською. JDK 25: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.

## Review Focus

- Гідрогія `wrapped_vault_key`: при register відсутність `userId` — через рішення AAD=email. Забезпечити, що decrypt під час login використовує ТУ САМУ АA D, інакше будується помилка различення результату.
- NFKC: той самий пароль у складеній (combining) і розкладеній (decomposed) формі має давати ідентичний `authKey` — тест-вектор із кирилицею та combining-символом у Task 2.
- `Project Failure на 409 register` (email вже зареєстрований) — ОБОВ'ЯЗКОВО гкі `AppError.EmailAlreadyRegistered`, не `InvalidCredentials`.
- озer `WrappedVaultKey` НЕ розшифровується при login, якщо до цього сесію захищено: буде розшифрування cекундарного, обсяг — ТИСЯЧІ `GcmCiphertext` з відповідем … до тесту Task 3/4.
- Rate limiting (429) — `RateLimited(retryAfterSeconds)` з головами відповідей, НЕ грубить.

---

### Task 0: Оновлення docs (ADR-003, погроза)

**Files:**

- Modify: `docs/adr/003-cryptography.md` §2 пункт 5 — «AAD = userId» → «AAD = email (UTF-8)»: клієнт отримує `userId` лише у відповідях API, а email відомий і під час реєстрації (wrap іде в запиті), і під час login/recovery (unwrap). Рішення затверджено 2026-11-10.
- Modify: `docs/threat-model.md` — рядок про swap-захист wrapped_vault_key (AAD): заменить `userId` на `email` для консистентності.

- [ ] **Step 1: Додати правки** (точний рядок з причиною; доки фіксують рішення від 2026-11-10).
- [ ] **Step 2: Commit** `docs: use email as wrap-key AAD instead of userId`
> \(This task email documented `docs` — тримаємі тобто в укрідській мовю jazyke цілих речень.\)

---

### Task 1: Domain-типи auth і порти

**Files:**

- Create: `shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/auth/DeviceIdentity.kt`
- Create: `shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/auth/AuthTypes.kt`
- Create: `shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/auth/AuthApi.kt`
- Create: `shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/auth/AuthStateStore.kt`
- Create: `shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/auth/BiometricPolicyStore.kt`
- Test-фейки: `shared/application/src/commonTest/kotlin/com/kwazzar/lockruna/application/support/FakeAuthApi.kt`, `FakeAuthStateStore.kt`, `FakeBiometricPolicyStore.kt`, `FakeArgon2.kt` (короткий кругу, `hash = HKDF(password-salt)` — 创建 для use-case-тестів, НЕ для security)

**Interfaces (Producing):**

```kotlin
package com.kwazzar.lockruna.domain.auth

enum class DevicePlatform { IOS, ANDROID }
data class DeviceIdentity(deviceId: String?, name: String, platform: DevicePlatform) {
    override fun toString() = "DeviceIdentity(name=$name)"
}

data class AuthTokens(accessToken: String, refreshToken: String, expiresInSeconds: Int) {
    override fun toString() = "AuthTokens(redacted)"
}
data class AuthenticatedUser(
    userId: String,
    deviceId: String,
    wrappedVaultKey: ByteArray,   // ciphertext||tag + nonce; формат фіксується нижче
    keyFNonce: ByteArray,         // nonce для wrappedVaultKey
    kdfSalt: ByteArray,
    kdfParams: KdfParams,         // com.kwazzar.lockruna.domain.crypto
    tokens: AuthTokens,
) {
    override fun toString() = "AuthenticatedUser(userId=$userId)"
}
data class PreLoginData(kdfSalt: ByteArray, kdfParams: KdfParams)

interface AuthApi {
    suspend fun prelogin(email: String): Result<PreLoginData, AppError>
    suspend fun register(
        email: String, authKey: ByteArray, recoveryAuthKey: ByteArray,
        kdfSalt: ByteArray, kdfParams: KdfParams,
        wrappedVaultKey: ByteArray, recoveryWrappedVaultKey: ByteArray,
        device: DeviceIdentity,
    ): Result<AuthenticatedUser, AppError>
    suspend fun login(email: String, authKey: ByteArray, device: DeviceIdentity): Result<AuthenticatedUser, AppError>
    suspend fun refresh(refreshToken: String): Result<AuthTokens, AppError>
    suspend fun logout(): Result<Unit, AppError>
}

interface AuthStateStore {
    suspend fun saveSession(user: AuthenticatedUser)
    suspend fun loadSession(): AuthenticatedUser?
    suspend fun clear()
}
interface BiometricPolicyStore {
    suspend fun failures(): Int
    suspend fun recordFailure()
    suspend fun resetFailures()
    suspend fun setLastSuccessfulUnlock(millis: Long)
    suspend fun lastSuccessfulUnlock(): Long?
}
```

Формат `wrappedVaultKey` на wire: `nonce(12) || ciphertext||tag` (AES-GCM 128-біт tag). Допоміжний внутрішний тип:

```kotlin
// domain/auth/SealedBlob.kt — helper упаковки wire-формату wrapped-ключа
data class SealedBlob(val nonce: ByteArray, val payload: ByteArray) {
    companion object {
        fun pack(nonce: ByteArray, ciphertext: GcmCiphertext): ByteArray   // nonce||ct||tag
        fun unpack(bytes: ByteArray): SealedBlob
    }
}
```

`AppError.kt` — додати `data object EmailAlreadyRegistered : AppError` (sync-хвиля має `IdempotencyConflict` — merge ротація: тримати обидві гілки).

- `VaultSession`-порт малий поштовх: до `domain/session/SessionPorts.kt` додати `VaultKey.from(bytes: ByteArray): VaultKey` = compackage of `VaultKey` (конструктор private — це роздолиний дало volition для unlock flow). У `VaultKey` інший оре що `zeroize` існує.
- [ ] **Step 1: failing test** — `FakeAuthStateStore` save/load/clear roundtrip; `FakeBiometricPolicyStore` counters; `FakeArgon2` детермінований;
- [ ] **Step 2: verify fails** `./gradlew :shared:application:jvmTest --tests '*AuthStateStore*'`.
- [ ] **Step 3: Implement** всі domain-типи відповідно вище; нічого мережевого.
- [ ] **Step 4: verify passes.**
- [ ] **Step 5: Commit** `feat(domain): auth exchange types and session ports`

---

### Task 2: Login, Logout, Unlock (use cases)

**Files:**

- Create: `shared/application/src/commonMain/kotlin/com/kwazzar/lockruna/application/auth/Login.kt`
- Create: `shared/application/src/commonMain/kotlin/com/kwazzar/lockruna/application/auth/Logout.kt`
- Create: `shared/application/src/commonMain/kotlin/com/kwazzar/lockruna/application/auth/UnlockWithPassword.kt`
- Create: `shared/application/src/commonMain/kotlin/com/kwazzar/lockruna/application/auth/KeyDerivation.kt` (internal helper; держить всі кроки виведення ключів у одному місці)
- Test: `shared/application/src/commonTest/kotlin/com/kwazzar/lockruna/application/auth/LoginTest.kt`, `LogoutTest.kt`, `UnlockWithPasswordTest.kt`

**Interfaces:**

```kotlin
// shared/application/.../application/auth/KeyDerivation.kt  (internal - над'être only для тестів)
internal object KeyDerivation {
    fun normalizeAndDerive(password: String, salt: ByteArray, argon2: Argon2, normalizer: PasswordNormalizer): ByteArray   // masterKey
    fun authKey(masterKey: ByteArray, crypto: CryptoService): ByteArray   // HKDF, info = HkdfLabels.AUTH
    fun kek(masterKey: ByteArray, crypto: CryptoService): ByteArray       // HKDF, info = HkdfLabels.KEK
    fun unwrapVaultKey(kek: ByteArray, blob: ByteArray, aadEmail: String, crypto: CryptoService): ByteArray  // SealedBlob.unpack → decryptGcm(kek, GcmCiphertext(payload), aad = email.bytes)
}

data class CredentialsCommand(email: String, password: String, deviceName: String, deviceId: String?)

class Login(
    private val authApi: AuthApi,
    private val argon2: Argon2,
    private val normalizer: PasswordNormalizer,
    private val crypto: CryptoService,
    private val authStateStore: AuthStateStore,
    private val session: VaultSession,
) {
    suspend operator fun invoke(command: CredentialsCommand): Result<Unit, AppError>
}

class Logout(
    private val authApi: AuthApi,
    private val authStateStore: AuthStateStore,
    private val session: VaultSession,
) {
    suspend operator fun invoke(): Result<Unit, AppError>
}

class UnlockWithPassword(
    private val argon2: Argon2,
    private val normalizer: PasswordNormalizer,
    private val crypto: CryptoService,
    private val authStateStore: AuthStateStore,
    private val session: VaultSession,
) {
    suspend operator fun invoke(email: String, password: String): Result<Unit, AppError>
}
```

Алгоритм `Login` (пінома стій-byte test):
1. `prelogin(email)` → `KdfParams.isAcceptable(params)` невірно → `Failure(WeakKdfParams)`.
2. `masterKey = argon2.hash(normalizer.normalize(password), prelogin.kdfSalt, params)`.
3. `authKey = HKDF(masterKey, HkdfLabels.AUTH)`.
4. `authApi.login(email, authKey, device)` → невдача — прокорається `AppError` (існуючі гілки: `InvalidCredentials` для 401, `RateLimited` для 429).
5. `kek = HKDF(masterKey, HkdfLabels.KEK)`; `vaultKey = unwrapVaultKey(kek, user.wrappedVaultKey, aad = email)`.
6. `authStateStore.saveSession(user)`; `session.unlock(VaultKey.from(vaultKey))`; Success.

`UnlockWithPassword` — БЕЗ мережі: той самий шлях 1→5, але розшифровує `wrappedVaultKey` з `authStateStore.loadSession()`; від компоненти треш `kdfSalt`, `kdfParams` зі збереженої сесії. Сесія відстутня → `Failure(Unauthorized)`. Останній → Success (даже when session already unlocked).
`Logout` → `authApi.logout()` → `authStateStore.clear()` → `session.lock()`.

Неможливо логин для незвісного email — **идентична обробка** в `FakeAuthApi`/`KtorAuthApi`: `InvalidCredentials`.

- [ ] **Step 1: failing tests:**
  - Login успішний: сесія unlocked, `currentVaultKey() != null` і її bytes рівні fake-crypto roundtrips;
  - `WeakKdfParams` при `isAcceptable == false`;
  - 401 обох сценаріїв → `InvalidCredentials`;
  - 429 → `RateLimited`;
  - Unlock offline з залогиненим сесією, невёл → Unauthorized;
  - Logout → сесія locked, store порожній.
- [ ] **Step 2: verify fails** `./gradlew :shared:application:jvmTest --tests '*Login*' --tests '*UnlockWithPassword*' --tests '*Logout*'`.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: verify passes.**
- [ ] **Step 5: Commit** `feat(application): login, logout and offline unlock`

---

### Task 3: Register

**Files:**

- Create: `shared/application/src/commonMain/kotlin/com/kwazzar/lockruna/application/auth/Register.kt`
- Test: `shared/application/src/commonTest/kotlin/com/kwazzar/lockruna/application/auth/RegisterTest.kt`

**Interfaces:**

```kotlin
data class RegisterCommand(email: String, password: String, deviceName: String, deviceId: String?)
data class RegisterOutcome(recoveryKey: ByteArray)   // 16 bytes; BIP39-перегли — окрема хвиля

class Register(
    private val authApi: AuthApi,
    private val argon2: Argon2,
    private val normalizer: PasswordNormalizer,
    private val crypto: CryptoService,
    private val authStateStore: AuthStateStore,
    private val session: VaultSession,
    private val idGenerator: IdGenerator,   // для deviceId, якщо клиент генерує
) {
    suspend operator fun invoke(command: RegisterCommand): Result<RegisterOutcome, AppError>
}
```

Алгоритм (централизований doc треба віджает в ADR-003):
1. `ValidateMasterPassword(password)` — weak → прокидається `WeakMasterPassword`.
2. `kdfSalt = crypto.randomBytes(16)`; `vaultKey = VaultKey.random(crypto)` (32); `recoveryKey = crypto.randomBytes(16)`.
3. `masterKey = argon2.hash(normalizer.normalize(password), kdfSalt, KdfParams.SUPPORTED)`.
4. `authKey = HKDF(masterKey, AUTH)`; `kek = HKDF(masterKey, KEK)`.
5. `recoveryWrapKey = HKDF(recoveryKey, HkdfLabels.RECOVERY_WRAP)`; `recoveryAuthKey = HKDF(recoveryKey, HkdfLabels.RECOVERY_AUTH)`.
   - *HkdfLabels.RECOVERY_WRAP і RECOVERY_AUTH вже існують (crypto-core).*
6. Wraps, ОБВАЯ — **AAD = trailing bytes(email, UTF-8)**:
   - `wrappedVaultKey = SealedBlob.pack(encryptGcm(kek, vaultKey.bytes, emailBytes))`;
   - `recoveryWrappedVaultKey = SealedBlob.pack(encryptGcm(recoveryWrapKey, vaultKey.bytes, emailBytes))`.
7. `authApi.register(email, authKey, recoveryAuthKey, kdfSalt, params, wrappedVaultKey, recoveryWrappedVaultKey, device)`.
8. Як у Login, кроки 5-6: unwrap через KEK з тим самим AAD (email); `saveSession`; `session.unlock(...)`; `Success(RegisterOutcome(recoveryKey))`.
   - 409 → `Failure(AppError.EmailAlreadyRegistered)`.

- [ ] **Step 1: failing tests:**
  - рiste success: `VaultSession` unlocked; `authStateStore` заповнений; `RegisterOutcome.recoveryKey` розміром 16 байт і НЕ dump у помилку.
  - слабкий password → WeakMasterPassword;
  - vault key, який клієнт зареєстрував, розгортається тим самим KEK (roundtrip через fake crypto);
  - email-register wrap інваріант: спроба запакувати/розпакувати з "EmailDifferent@example.com" return `Failure(...ItemCorrupted)` — фіксисував АДЕ-піняцію.
- [ ] **Step 2: verify fails / verify passes / Commit** `feat(application): registration with recovery key and aad-email wrap`.

---

### Task 4: Біометрия: EnableBiometrics, UnlockWithBiometrics

**Files:**

- Create: `shared/application/src/commonMain/kotlin/com/kwazzar/lockruna/application/auth/EnableBiometrics.kt`
- Create: `shared/application/src/commonMain/kotlin/com/kwazzar/lockruna/application/auth/UnlockWithBiometrics.kt`
- Test: `shared/application/src/commonTest/kotlin/com/kwazzar/lockruna/application/auth/BiometricUnlockTest.kt`

**Interfaces:**

```kotlin
class EnableBiometrics(
    private val biometricGate: BiometricGate,       // domain/session (існуюча)
    private val secureKeyStore: SecureKeyStore,     // domain/session
    private val session: VaultSession,
) {
    suspend operator fun invoke(reason: String): Result<Unit, AppError>
    // 1) biometricGate.isAvailable() == false → Failure(AppError.BiometricsUnavailable) — нова гілка в AppError (хвиля auth);
    // Порядок кроків:
    // 2) biometricGate.authenticate(reason) == false → Failure(AppError.InvalidCredentials);
    // 3) session.currentVaultKey() == null → Failure(AppError.VaultLocked);
    // 4) secureKeyStore.saveVaultKey(vaultKey) → Success(Unit).
}

class UnlockWithBiometrics(
    private val biometricGate: BiometricGate,
    private val secureKeyStore: SecureKeyStore,
    private val biometricPolicyStore: BiometricPolicyStore,
    private val session: VaultSession,
    private val clock: Clock,
) {
    suspend operator fun invoke(reason: String): Result<Unit, AppError>
    // ГЕЙТИ (по порядку, всі FAIL-обсягання):
    // 1) biometricPolicyStore.failures() >= 5 → Failure(InvalidCredentials);(native UI питає пароль)
    // 2) lastSuccessfulUnlock() відсутній або давніший за 7 днів від clock.nowMillis() → Failure(InvalidCredentials);
    // 3) biometricGate.authenticate(reason) == false → recordFailure() + Failure(InvalidCredentials);
    // 4) vaultKey = secureKeyStore.loadVaultKey() ?? null → Failure(Unauthorized) (не встановлені біомет)
    // 5) success: resetFailures(), setLastSuccessfulUnlock(now), session.unlock(vaultKey)
}
```

«Після рестарту потрібен пароль»: не реалізується тут кодом — воно природно від `VaultSession` (new instance в Locked); тест-фікс, що нова `VaultSession` починається Locked.

- [ ] **Step 1: failing tests** (гейти 1–5 по одному; EnableBiometrics на locked-сесії → VaultLocked).
- [ ] **Step 2: verify fails / passes / Commit** `feat(application): biometric enable and unlock with policy gates`

---

### Task 5: KtorAuthApi і SessionAccessTokenProvider

**Files:**

- Create: `shared/data/src/commonMain/kotlin/com/kwazzar/lockruna/data/auth/KtorAuthApi.kt`
- Create: `shared/data/src/commonMain/kotlin/com/kwazzar/lockruna/data/auth/SessionAccessTokenProvider.kt`
- Create: `shared/data/src/commonMain/kotlin/com/kwazzar/lockruna/data/auth/InMemoryAuthStateStore.kt`
- Test: `shared/data/src/commonTest/kotlin/com/kwazzar/lockruna/data/auth/KtorAuthApiTest.kt`
- Test: `shared/data/src/commonTest/kotlin/com/kwazzar/lockruna/data/auth/InMemoryAuthStateStoreTest.kt`

**Interfaces:**

```kotlin
class KtorAuthApi(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : AuthApi {
    // prelogin   → POST /auth/prelogin  (PreloginRequest → PreloginResponse; 429 → RateLimited(Retry-After))
    // register   → POST /auth/register (RegisterRequest; 201 → AuthResponseDto; 409 → EmailAlreadyRegistered; 429 → RateLimited)
    // login      → POST /auth/login    (LoginRequest; 200 → AuthResponseDto; 401 → InvalidCredentials; 429 → RateLimited)
    // refresh    → POST /auth/refresh  (RefreshRequest; 200 → TokenPairDto; 401 → Unauthorized)
    // logout     → POST /auth/logout   (Bearer; 204 → Success; 401 → Unauthorized)
    // Мапінг DTO ↔ domain — ТОЛЬКИ тут (ADR-005 §4). GcmCiphertext → ByteArray конвертація через SealedBlob.pack/unpack (domain/auth).
}

class SessionAccessTokenProvider(
    private val authStateStore: AuthStateStore,
) // impl з domain/sync AccessTokenProvider (порт належить sync-хвилі!)
{
    suspend override fun token(): String? = authStateStore.loadSession()?.tokens?.accessToken
}

class InMemoryAuthStateStore : AuthStateStore   // тестовий, used in jvmТests; продакшн: Keychain/Keystore native-хвиля
```

- **AccessTokenProvider impl: тут є реалізація; порт у domain/sync — не создание.
- toml/build: ktor-client залежності приходитуть з client-sync (там же ktor-client-mock); ЯКЩО auth зливається першим — додати ті самі 3 рядки (згідно client-sync plan Task 3), дублікати бутер.
- Відповідь AuthResponseDto → `AuthenticatedUser`: `kdfSalt`, `kdfParams`, `wrappedVaultKey`; `wrappedVaultKey`-wire формат `nonce||ciphertext||tag` — СЕРВЕР ЛИШЕ КОПІЮВАє байти клієнта, тому формат порядку підтверджується unit-тестом package/unpack.

- [ ] **Step 1: failing test** — mock roundtrips: register 201 (DTO → AuthenticatedUser), login 401 обох видів → InvalidCredentials, register 409 → EmailAlreadyRegistered, 429 → RateLimited з Retry-After, refresh, logout 204.
- [ ] **Step 2: verify fails / passes.**
- [ ] **Step 3: Implement.**
- [ ] **Step 4: verify passes + ktlint/detekt зелено.**
- [ ] **Step 5: Commit** `feat(data): ktor auth api and session token provider`

---

### Task 6: Koin-модуль + архітектурний тест + повна верифікація

**Files:**

- Create: `shared/data/src/commonMain/kotlin/com/kwazzar/lockruna/data/di/AuthModule.kt`
- Create: `architecture-tests/src/test/kotlin/com/kwazzar/lockruna/architecture/AuthLayerTest.kt`

**Interfaces:**

```kotlin
fun authModule(
    httpClient: HttpClient,
    baseUrl: String,
) = module {
    single { KtorAuthApi(httpClient, baseUrl) }
    single<AuthApi> { get<KtorAuthApi>() }
    single { InMemoryAuthStateStore() }          // до native-хвилі; далі Keychain/Keystore
    single<AccessTokenProvider> { SessionAccessTokenProvider(get()) }
    single { Login(get(), get(), get(), get(), get(), get()) }
    single { Logout(get(), get(), get()) }
    single { UnlockWithPassword(get(), get(), get(), get(), get()) }
    single { Register(get(), get(), get(), get(), get(), get(), get()) }
    single { EnableBiometrics(get(), get(), get()) }
    single { UnlockWithBiometrics(get(), get(), get(), get(), get()) }
}
```
(Порядок параметрів — за конструкторами попередніх текстів; компілятор у коммені — попередня точка sync-плану: якщо auth зливається першим, ktor-rows у toml додати самостійно.)

Konsist-тест `AuthLayerTest`:
- `shared/domain/auth/**` не імпортує `io.ktor.`, `app.cash.sqldelight.`, `kotlinx.serialization.`;
- `shared/application/auth/**` не імпортує `io.ktor.`, `app.cash.sqldelight.`;
- `shared/data/auth/**` не імпортує `com.kwazzar.lockruna.presentation.`.

- [ ] **Step 1: konsist RED**
- [ ] **Step 2: модуль → GREEN**
- [ ] **Step 3: повна верифікація:**
  - `./gradlew ktlintCheck detekt`
  - `./gradlew :shared:domain:jvmTest :shared:application:jvmTest :shared:data:jvmTest :architecture-tests:test`
  - `gitleaks detect --source . --no-banner`
  - Expected: BUILD SUCCESSFUL.
- [ ] **Step 4: Commit** `feat(auth): di module and architecture guard`

## Не в обсязі цієї хвилі

- Нативні `SecureKeyStore` (Keychain/Keystore) і `BiometricGate` — важливо натив-хвиля; порт + InMemory fake тут.
- BIP39-кодування recoveryKey (12 слів) і повний recovery flow (`/auth/recovery/*`) — окрема хвиля.
- Авто-refresh токенів на 401 (інтерцептор) — нативна хвиля; `SessionAccessTokenProvider` вертає поточний токен.
- Зберігання пароля або vaultKey на диску — заборонено; перевірити `gitleaks` перед комітом.
- Ком позиція DI всіх store-ів, нативні екрани Login/Register — презентаційна/нативна хвиля.

## Порядок злиття

Незалежний від інших хвиль. Точки дотику:
- `AppError.kt`: гілка `EmailAlreadyRegistered` (merge з `IdempotencyConflict` sync-хвилі).
- `gradle/libs.versions.toml`: ktor-client-рядки від sync-хвилі; їх дублювати тільки якщо auth зливається першим.
- `domain/session/SessionPorts.kt`: додавання `VaultKey.from(bytes)` — дві рядки коду.
