# Клієнт: життєвий цикл акаунта (Client Account Lifecycle) — Implementation Plan

> **Для агентних виконавців:** ОБОВ'ЯЗКОВО: використати `superpowers:subagent-driven-development` (рекомендовано) або `superpowers:executing-plans` для виконання цього плану по завданнях. Кроки використовують checkbox (`- [ ]`) для відстеження.

**Воркспейс:** гілка `feature/client-account-lifecycle` від `main` (після мерджу WS1 для DTO). Володіє: `shared/domain/**`, `shared/application/**`, `shared/data/**`, `shared/presentation/**`. `server/**`, `androidApp/**`, `iosApp/**`, `shared/umbrella/**` — не редагуються (umbrella — WS3).

**Goal:** Закрити клієнтські діри US-01/US-03/US-07/US-08/US-10/US-11 і частково US-04/US-05: реєстрація з BIP39-recovery key, відновлення доступу, пристрої, налаштування автоблокування, видалення акаунта, CARD/NOTE і папки, плюс фікс першого біометричного розблокування.

**Architecture:** Domain-порти `RecoveryApi`, `DeviceApi`, `AutoLockSettingsStore`; use cases у `application/auth` і `application/vault`; Ktor-реалізації в `data/auth` (DTO-мапінг лише тут); MVI-стори в `presentation`. BIP39-кодек — у `domain/crypto` через новий метод `CryptoService.sha256()` (ADR-003 доповнюється одним методом) і вендорений англійський wordlist (2048 слів). `EnableBiometrics` виправляється: ставить `lastSuccessfulUnlock` і скидає failures.

**Tech Stack:** Kotlin 2.4.20, KMP, Ktor client 3.6.1, SQLDelight 2.4.1, kotlinx-coroutines 1.11.0, Koin 4.2.2, kotlin.test.

**Spec:** `docs/requirements.md` (US-01, US-03, US-04, US-05, US-07, US-08, US-10, US-11), `docs/openapi.yaml`, `docs/adr/003-cryptography.md`, `docs/adr/005-client-shared.md`, `docs/threat-model.md`.

**Залежність:** WS1 (DTO). До мерджу WS1 — домен/application/presentation робляться проти власних result-типів; data-мапінг — останнім.

## Global Constraints

- Порти в domain: `RecoveryApi`, `DeviceApi`, `AutoLockSettingsStore` (інтерфейси; реалізації — у data).
- Інваріанти живуть в агрегатах; Store/use case не роблять `copy(version = ...)`.
- Store викликає тільки use cases, ніколи репозиторії/Ktor/CryptoService.
- Секрети не в State, логах, `toString()`, іменах тестів; тест-рядки `"secret-value"`.
- `vaultKey` обнуляється при блокуванні (best effort).
- Автоблокування рахується від `Clock.monotonicMillis()`.
- Новий метод `CryptoService.sha256()` — єдине доповнення крипто-порту; реалізації на BouncyCastle (jvm/android) і libsodium (ios); KAT-вектор (SHA-256 порожнього рядка) на обох платформах.
- `gradle/libs.versions.toml` не редагується (нових залежностей немає).
- CARD/NOTE у цій хвилі зберігаються у наявних полях `VaultItem` (title/username/secret/url); нові типізовані поля агрегату — окрема хвиля, щоб не чіпати схему/міграції/AAD.
- `AutoLockSettingsStore`: якщо SQLDelight-таблиці немає, використати платформні prefs через expect/actual; домен бачить лише порт.
- Код/коментарі/коміти англійською; docs/ українською.
- JDK 25: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.

## Review Focus

- BIP39: 12 слів, 4-бітний checksum; round-trip encode→decode; невірний checksum → помилка, а не тихе прийняття.
- `EnableBiometrics` після виправлення робить перше біометричне розблокування успішним (раніше завжди `InvalidCredentials`).
- `RecoveryStore`: пароль ніколи не потрапляє в State/`toString()`; подвійний Submit ігнорується.
- `DeviceApi`/`DevicesStore`: IDOR — список/відкликання тільки своїх пристроїв; поточний пристрій не можна відкликати (або підтверджується — за openapi).
- Налаштування автоблокування персистяться і застосовуються при наступному resume.
- CARD/NOTE: тип обирається при створенні; `secret` не обов'язковий для CARD/NOTE, але обов'язковий для LOGIN.
- Rollback-захист: клієнт не перезаписує рядок при `version < max_seen_version`.

## File Structure

- Modify: `shared/domain/src/commonMain/.../crypto/CryptoService.kt` — додати `sha256(input: ByteArray): ByteArray`.
- Implementations: `shared/data/src/jvmAndAndroidMain/.../crypto/*` (BouncyCastle), `shared/data/src/iosMain/.../crypto/*` (libsodium).
- Create: `shared/domain/src/commonMain/.../crypto/Bip39.kt` + wordlist resource.
- Create: `shared/domain/src/commonMain/.../auth/RecoveryApi.kt`, `DeviceApi.kt`, `AutoLockSettingsStore.kt`.
- Create: `shared/application/src/commonMain/.../auth/StartRecovery.kt`, `CompleteRecovery.kt`, `DeleteAccount.kt`, `ListDevices.kt`, `RevokeDevice.kt`.
- Modify: `shared/application/src/commonMain/.../auth/EnableBiometrics.kt` (фікс).
- Create: `shared/data/src/commonMain/.../auth/KtorRecoveryApi.kt`, `KtorDeviceApi.kt`, `SqlDelightAutoLockSettingsStore.kt` (або DataStore — за наявності).
- Create: `shared/presentation/src/commonMain/.../auth/RegisterStore.kt`, `RecoveryStore.kt`, `shared/presentation/.../vault/DevicesStore.kt`, `SettingsStore.kt`.
- Modify: `shared/presentation/src/commonMain/.../vault/ItemEditStore.kt` — тип запису + CARD/NOTE-поля.
- Modify: `shared/presentation/src/commonMain/.../app/AppScreen.kt` — нові екрани.
- Modify: DI-модулі `AuthModule`, `CoreModule`, `PresentationModule`.
- Modify: `shared/data/src/commonMain/.../di/AuthModule.kt` (реєстрація нових API/use cases).

## Task 1: `CryptoService.sha256()` + KAT

**Files:**
- Modify: `shared/domain/.../crypto/CryptoService.kt`
- Modify: `shared/data/src/jvmAndAndroidMain/.../crypto/*`, `shared/data/src/iosMain/.../crypto/*`
- Test: `shared/data/src/commonTest/.../crypto/Sha256KatTest.kt` (або jvmTest/iosTest)

**Interfaces:**
- Produces: `CryptoService.sha256(input: ByteArray): ByteArray` (32 байти).

- [ ] **Step 1:** Додати метод у порт.
- [ ] **Step 2:** Реалізація BouncyCastle (`DigestFactory`/`SHA256Digest`) і libsodium (`crypto_hash_sha256`).
- [ ] **Step 3:** KAT: SHA-256("") = `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`.
- [ ] **Step 4:** Commit: `feat(crypto): add sha256 to CryptoService with KAT`.

## Task 2: BIP39 кодек

**Files:**
- Create: `shared/domain/src/commonMain/.../crypto/Bip39.kt`
- Create: `shared/domain/src/commonMain/resources/bip39-english.txt` (2048 слів)
- Test: `shared/domain/src/commonTest/.../crypto/Bip39Test.kt`

**Interfaces:**
- Produces: `object Bip39 { fun encode(entropy: ByteArray): List<String> (12 слів); fun decode(words: List<String>): ByteArray; }` — кидає `AppError.InvalidRecoveryKey` при невірному checksum/слові.

- [ ] **Step 1:** Тест encode/decode round-trip для 16 байт; невірний checksum → failure; невідоме слово → failure.
- [ ] **Step 2:** Реалізувати: 128 біт → 12 слів (11 біт кожне), 4-бітний checksum SHA-256(entropy) у 12-му слові; decode — зворотне + перевірка checksum.
- [ ] **Step 3:** Commit: `feat(domain): add BIP39 codec for recovery key`.

## Task 3: Порти `RecoveryApi`, `DeviceApi`, `AutoLockSettingsStore`

**Files:**
- Create: `shared/domain/src/commonMain/.../auth/RecoveryApi.kt`, `DeviceApi.kt`, `AutoLockSettingsStore.kt`

**Interfaces:**
- `RecoveryApi.start(email): Result<RecoveryStartData(userId, recoveryWrappedVaultKey), AppError>`
- `RecoveryApi.complete(email, recoveryAuthKey, newAuthKey, newKdfSalt, newKdfParams, newWrappedVaultKey, device): Result<AuthenticatedUser, AppError>`
- `DeviceApi.list(): Result<List<Device>, AppError>`; `DeviceApi.revoke(deviceId): Result<Unit, AppError>`
- `AutoLockSettingsStore.load(): AutoLockPolicy`; `save(policy)`.

- [ ] **Step 1:** Визначити інтерфейси + доменні типи `RecoveryStartData`, `Device`.
- [ ] **Step 2:** Konsist-тест (якщо є) на розташування портів.
- [ ] **Step 3:** Commit: `feat(domain): add recovery, device and auto-lock ports`.

## Task 4: Use cases

**Files:**
- Create: `shared/application/src/commonMain/.../auth/StartRecovery.kt`, `CompleteRecovery.kt`, `DeleteAccount.kt`, `ListDevices.kt`, `RevokeDevice.kt`
- Modify: `shared/application/src/commonMain/.../auth/EnableBiometrics.kt`

**Interfaces:**
- `StartRecovery(email): Result<RecoveryStartOutcome(recoveryWrappedVaultKey, userId), AppError>`
- `CompleteRecovery(command): Result<Unit, AppError>` (розгортає vaultKey, встановлює новий пароль, зберігає сесію)
- `DeleteAccount(authKey): Result<Unit, AppError>`
- `ListDevices(): Result<List<Device>, AppError>`
- `RevokeDevice(deviceId): Result<Unit, AppError>`
- `EnableBiometrics`: додати `biometricPolicyStore.setLastSuccessfulUnlock(clock.nowMillis())` після збереження ключа; скинути failures.

- [ ] **Step 1:** Тести на кожен use case з fake-ами (InMemory).
- [ ] **Step 2:** Реалізація; `CompleteRecovery` використовує `KeyDerivation` (як `Register`/`Login`) і `Bip39` (decode → recoveryKey → HKDF).
- [ ] **Step 3:** Фікс `EnableBiometrics`.
- [ ] **Step 4:** Commit: `feat(application): add account lifecycle use cases and fix biometric first-use`.

## Task 5: Data-реалізації

**Files:**
- Create: `shared/data/src/commonMain/.../auth/KtorRecoveryApi.kt`, `KtorDeviceApi.kt`
- Create: `shared/data/src/commonMain/.../auth/SqlDelightAutoLockSettingsStore.kt` (або in-memory + platform)
- Modify: `shared/data/src/commonMain/.../di/AuthModule.kt`, `CoreModule.kt`

**Interfaces:**
- Consumes: `HttpClient`, `baseUrl`, DTO з `api-contract`.
- Produces: реалізації портів; Koin-реєстрація `RecoveryApi`, `DeviceApi`, `AutoLockSettingsStore`, use cases.

- [ ] **Step 1:** Ktor-реалізації з мапінгом DTO ↔ домен (лише тут).
- [ ] **Step 2:** Koin-реєстрація + тести `AuthModule`/`CoreModule`.
- [ ] **Step 3:** Commit: `feat(data): implement recovery/device APIs and auto-lock settings`.

## Task 6: MVI-стори

**Files:**
- Create: `shared/presentation/src/commonMain/.../auth/RegisterStore.kt`, `RecoveryStore.kt`
- Create: `shared/presentation/src/commonMain/.../vault/DevicesStore.kt`, `SettingsStore.kt`
- Modify: `shared/presentation/src/commonMain/.../vault/ItemEditStore.kt` — тип + CARD/NOTE-поля.
- Modify: `shared/presentation/src/commonMain/.../app/AppScreen.kt` — `REGISTER`, `RECOVERY`, `DEVICES`, `SETTINGS`.
- Modify: `shared/presentation/src/commonMain/.../di/PresentationModule.kt`.

**Interfaces:**
- `RegisterStore`: State (`email`, `password`, `hasPassword`, `recoveryWords: List<String>?`, `isSubmitting`, `error`), Intents, Effects (`RecoveryKeyReady(words)`).
- `RecoveryStore`: State (`email`, `recoveryWordsInput`, `newPassword`, `isSubmitting`, `error`), Effect `Recovered`.
- `DevicesStore`: State (`devices`, `isLoading`, `error`), Intent `Load`, `Revoke(id)`.
- `SettingsStore`: State (`policy: AutoLockPolicy`, `biometricsEnabled`), Intent `PolicyChanged`, `BiometricsToggled`.

- [ ] **Step 1:** Тести на кожен стор (подвійний Submit, redaction пароля, reveal тощо).
- [ ] **Step 2:** Реалізація; `ItemEditStore` додає `type: VaultItemType` + поля `cardNumber`, `expiry`, `cvv`, `noteBody` (або узагальнені).
- [ ] **Step 3:** Koin-реєстрація + Konsist.
- [ ] **Step 4:** Commit: `feat(presentation): add account lifecycle and settings stores`.

## Task 7: Перевірка

- [ ] **Step 1:** `./gradlew :shared:domain:jvmTest :shared:application:jvmTest :shared:presentation:jvmTest :shared:data:jvmTest`.
- [ ] **Step 2:** `./gradlew :architecture-tests:test`.
- [ ] **Step 3:** `gitleaks detect --source . --no-banner`.
- [ ] **Step 4:** Commit: `docs(shared): update graphify`.

## Spec coverage

US-01 (реєстрація + recovery key), US-03 (клієнтський флоу), US-04/US-05 (CARD/NOTE/папки в сторах), US-07 (вмикання біометрії + фікс), US-08 (налаштування), US-10 (пристрої), US-11 (видалення акаунта). Екрани — WS3. Аудит — окрема хвиля.
