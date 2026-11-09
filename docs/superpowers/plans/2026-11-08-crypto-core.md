# Крипто-ядро клієнта (Crypto Core) — Implementation Plan

> **Для агентних виконавців:** ОБОВ'ЯЗКОВО: використати `superpowers:subagent-driven-development` (рекомендовано) або `superpowers:executing-plans` для виконання цього плану по завданнях. Кроки використовують checkbox (`- [ ]`) для відстеження.

**Мета:** Реалізувати клієнтський крипто-шар за ADR-003: порти `CryptoService` / `Argon2` / `PasswordNormalizer` у `shared/domain`, реалізації Argon2id, HKDF-SHA256, AES-256-GCM, NFKC-нормалізації та виведення ієрархії ключів, з еталонними тест-векторами на JVM та iOS.

**Архітектура:** `shared/domain/crypto/` — порти без залежностей (domain не знає про бібліотеки). `shared/data/crypto/` — реалізації: окремий source set `jvmAndAndroidMain` на BouncyCastle (JVM + Android) і `iosMain` на libsodium через ionspin. Жодного власного криптоалгоритму: HKDF і GCM беруться з BouncyCastle / libsodium, Argon2id — з BouncyCastle (`Argon2BytesGenerator`) та libsodium (`crypto_pwhash`).

**Технології:** Kotlin 2.4.20, KMP (jvm / android / iosArm64 / iosSimulatorArm64), `org.bouncycastle:bcprov-jdk18on:1.86`, `com.ionspin.kotlin:multiplatform-crypto-libsodium-bindings:0.9.5`, `kotlin.test`.

**Специфікація:** `docs/requirements.md` (US-01, US-03, нефункціональні вимоги про KAT), `docs/adr/003-cryptography.md`, `docs/adr/005-client-shared.md` (§2, §10), `docs/threat-model.md`.

## Global Constraints

- KDF-параметри зафіксовані ADR-003: Argon2id, `m = 65536` KiB, `t = 3`, `p = 1`, salt рівно 16 байт, вихід 32 байти. **Не змінювати без нового ADR.**
- HKDF info-рядки зафіксовані: `passkeeper/auth/v1`, `passkeeper/kek/v1`, `passkeeper/recovery-wrap/v1`, `passkeeper/recovery-auth/v1`. **Не змінювати.**
- Шифрування: AES-256-GCM, випадковий nonce 12 байт на операцію, тег 16 байт.
- AAD: `wrappedVaultKey` → `userId`; запис → `itemId | type | version`.
- Власні криптоалгоритми заборонені. Ключовий матеріал не логується, не потрапляє в імена тестів, фікстури та винятки.
- Секретів у репозиторії; конфіг лише з оточення.
- Код, коментарі, повідомлення комітів — англійською. `docs/` — українською.
- JDK 25: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.
- Точка входу для перевірки: `./scripts/verify.sh`.
- Нові залежності — тільки з точним піном; додавати в кінець блоків `[versions]` / `[libraries]` у `gradle/libs.versions.toml` (не вставляти в середину — цим файлом володіє тільки цей план).

## Review Focus

- Argon2id на iOS і Android дає **побайтово однаковий** вивід для однакових (`password`, `salt`, `params`) — інакше `HMAC(pepper, authKey)` не збігатиметься між пристроями.
- NFKC-нормалізація однакова на iOS і Android для кирилиці, комбінованих акцентів, лігатур і full-width символів.
- HKDF виводить **різні** ключі для різних info-рядків; `authKey` ≠ `kek`; `kek` не залишає клієнта.
- GCM-дешифрування з неправильним AAD або зміненим ciphertext **кидає помилку**, а не повертає порожній результат.
- Ключовий матеріал не потрапляє в `toString()` жодного доменного типу і не в логи.

## Власність файлів (паралельна робота)

Цей план володіє:

- `shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/crypto/**`
- `shared/domain/src/commonTest/kotlin/com/kwazzar/lockruna/domain/crypto/**`
- `shared/data/src/commonMain/kotlin/com/kwazzar/lockruna/data/crypto/**`
- `shared/data/src/commonTest/kotlin/com/kwazzar/lockruna/data/crypto/**`
- `shared/data/src/jvmAndAndroidMain/kotlin/com/kwazzar/lockruna/data/crypto/**`
- `shared/data/src/iosMain/kotlin/com/kwazzar/lockruna/data/crypto/**`
- `shared/data/build.gradle.kts`, `gradle/libs.versions.toml`
- `docs/adr/003-cryptography.md`, `docs/adr/005-client-shared.md`
- `architecture-tests/src/test/kotlin/com/kwazzar/lockruna/architecture/CryptoLayerTest.kt` (новий файл)

Інші плани ці файлів не чіпають. `graphify update .` у воркспейсі **не запускати** — один раз після злиття.

## Не в обсязі цього плану

- BIP39-кодування recovery key (12 слів) — наступна хвиля разом із identity use cases: потрібен wordlist-ресурс і рішення щодо бандлінгу в iOS framework.
- `SecureKeyStore` (Keychain / Keystore) і `BiometricGate` — нативний шар, наступна хвиля.
- Клієнтський sync-рушій і use cases реєстрації/входу — наступні хвилі.
- Зміна KDF-параметрів, ротація `pepper`, серверний `HMAC` — не тут.

## Розглянуті підходи

1. **Власна реалізація Argon2/HKDF/GCM у Kotlin** — відхилено: прямо заборонено правилом «no custom crypto».
2. **Один ionspin на обох платформах** — відхилено (ADR-003): лише iOS, експериментальна залежність, Android лишається безArgon2.
3. **BouncyCastle (JVM+Android) + libsodium через ionspin (iOS)** — вибрано: обидві реалізації — vetted-бібліотеки, нуль власної криптографії, обидві вже мають стабільні KAT.
4. **Чиста Kotlin-бібліотека (cryptography-kotlin)** — відхилено: не опублікована на Maven Central, лише JitPack; для security-проєкту не брати.

---

### Task 1: Доменні порти, KDF-параметри та HKDF-рядки

**Files:**

- Create: `shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/crypto/Argon2.kt`
- Create: `shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/crypto/CryptoService.kt`
- Create: `shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/crypto/PasswordNormalizer.kt`
- Create: `shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/crypto/HkdfLabels.kt`
- Test: `shared/domain/src/commonTest/kotlin/com/kwazzar/lockruna/domain/crypto/KdfParamsTest.kt`

**Interfaces:**

- Produces (далі їх споживають Task 2-5 і план `client-vault-core`):

```kotlin
package com.kwazzar.lockruna.domain.crypto

data class KdfParams(
    val alg: String = "argon2id",
    val memoryKiB: Int = 65536,
    val iterations: Int = 3,
    val parallelism: Int = 1,
    val saltLength: Int = 16,
    val outputLength: Int = 32,
) {
    companion object {
        const val MIN_MEMORY_KIB: Int = 65536
        const val MIN_ITERATIONS: Int = 3
        const val MIN_PARALLELISM: Int = 1
        fun isAcceptable(params: KdfParams): Boolean
    }
}

interface Argon2 {
    suspend fun hash(password: ByteArray, salt: ByteArray, params: KdfParams): ByteArray
}

interface PasswordNormalizer {
    /** NFKC, потім UTF-8. */
    fun normalize(raw: String): ByteArray
}

data class GcmCiphertext(val ciphertext: ByteArray, val nonce: ByteArray, val tag: ByteArray)

interface CryptoService {
    fun hkdfSha256(inputKey: ByteArray, salt: ByteArray?, info: ByteArray, length: Int = 32): ByteArray
    fun encryptGcm(key: ByteArray, plaintext: ByteArray, aad: ByteArray): GcmCiphertext
    fun decryptGcm(key: ByteArray, ciphertext: GcmCiphertext, aad: ByteArray): ByteArray
    fun randomBytes(size: Int): ByteArray
}
```

- `object HkdfLabels` — `const val AUTH = "passkeeper/auth/v1"`, `const val KEK = "passkeeper/kek/v1"`, `const val RECOVERY_WRAP = "passkeeper/recovery-wrap/v1"`, `const val RECOVERY_AUTH = "passkeeper/recovery-auth/v1"`.

**Step 1: Write the failing test**

`shared/domain/src/commonTest/kotlin/com/kwazzar/lockruna/domain/crypto/KdfParamsTest.kt`:

```kotlin
class KdfParamsTest {
    @Test
    fun `default params are acceptable`() {
        assertTrue(KdfParams.isAcceptable(KdfParams()))
    }

    @Test
    fun `params below the built-in minimum are rejected`() {
        assertFalse(KdfParams.isAcceptable(KdfParams(memoryKiB = 32768)))
        assertFalse(KdfParams.isAcceptable(KdfParams(iterations = 2)))
        assertFalse(KdfParams.isAcceptable(KdfParams(parallelism = 0)))
        assertFalse(KdfParams.isAcceptable(KdfParams(saltLength = 8)))
        assertFalse(KdfParams.isAcceptable(KdfParams(outputLength = 16)))
        assertFalse(KdfParams.isAcceptable(KdfParams(alg = "argon2i")))
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :shared:domain:jvmTest --tests '*KdfParamsTest*'`
Expected: FAIL — `KdfParams` unresolved.

**Step 3: Implement the ports**

Create the four files above with the exact signatures. `isAcceptable` returns `true` iff `alg == "argon2id"`, `memoryKiB >= MIN_MEMORY_KIB`, `iterations >= MIN_ITERATIONS`, `parallelism >= MIN_PARALLELISM`, `saltLength == 16`, `outputLength == 32`.

**Step 4: Run test to verify it passes**

Run: `./gradlew :shared:domain:jvmTest --tests '*KdfParamsTest*'`
Expected: PASS.

**Step 5: Commit**

```bash
git add shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/crypto \
        shared/domain/src/commonTest/kotlin/com/kwazzar/lockruna/domain/crypto
git commit -m "feat(domain): add crypto ports and fixed KDF parameters"
```

---

### Task 2: Реалізація для JVM і Android (BouncyCastle)

**Files:**

- Modify: `gradle/libs.versions.toml` (додати в кінець `[versions]`: `bouncycastle = "1.86"`; в кінець `[libraries]`: `bouncycastle = { module = "org.bouncycastle:bcprov-jdk18on", version.ref = "bouncycastle" }`)
- Modify: `shared/data/build.gradle.kts` (source set + залежність)
- Create: `shared/data/src/jvmAndAndroidMain/kotlin/com/kwazzar/lockruna/data/crypto/PlatformCrypto.kt`
- Create: `shared/data/src/jvmAndAndroidMain/kotlin/com/kwazzar/lockruna/data/crypto/PlatformNormalizer.kt`
- Create: `shared/data/src/commonMain/kotlin/com/kwazzar/lockruna/data/crypto/CryptoServiceFactory.kt`
- Test: `shared/data/src/commonTest/kotlin/com/kwazzar/lockruna/data/crypto/CryptoVectors.kt`
- Test: `shared/data/src/commonTest/kotlin/com/kwazzar/lockruna/data/crypto/CryptoServiceTest.kt`

**Interfaces:**

- Consumes: Task 1 (`CryptoService`, `KdfParams`, `PasswordNormalizer`).
- Produces:

```kotlin
// commonMain
expect class PlatformCrypto() : CryptoService
expect class PlatformNormalizer() : PasswordNormalizer

// jvmAndAndroidMain
actual class PlatformCrypto actual constructor() : CryptoService {
    actual fun hkdfSha256(inputKey: ByteArray, salt: ByteArray?, info: ByteArray, length: Int): ByteArray
    actual fun encryptGcm(key: ByteArray, plaintext: ByteArray, aad: ByteArray): GcmCiphertext
    actual fun decryptGcm(key: ByteArray, ciphertext: GcmCiphertext, aad: ByteArray): ByteArray
    actual fun randomBytes(size: Int): ByteArray
    // + Argon2id через Argon2BytesGenerator (BouncyCastle), окремий клас Argon2BouncyCastle : Argon2
}

actual class PlatformNormalizer actual constructor() : PasswordNormalizer {
    actual fun normalize(raw: String): ByteArray
}
```

**Step 1: Write the failing test**

`shared/data/src/commonTest/kotlin/com/kwazzar/lockruna/data/crypto/CryptoServiceTest.kt` — тести на еталонних векторах (значення — у `CryptoVectors.kt`, hex-рядки):

- `hkdfSha256` == RFC 5869 Test Case 1 (SHA-256, IKM `0x0b`×22, salt `0x000102...0c`, info `0xf0f1...f9`, L=42) → `OKM` = `3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865`.
- `hkdfSha256` без salt (RFC 5869 Test Case 3, IKM `0x0b`×22, info пустий, L=42) → `8da4e775a563c18f715f802a063c5a31b8a11f5c5ee1879ec3454e5f3c738d2d9d201395faa4b61a96c8`.
- `encryptGcm`/`decryptGcm` — NIST GCM (AES-256) test case 13: key `0x00`×32, iv `0x00`×12, plaintext `0x00`×16, aad пустий → ciphertext `0xcea7403d4d606b6e074ec5d3baf17d18`, tag `0xdc20e2d83f25705bb49e439eca56de25`.
- `decryptGcm` зі зміненим AAD кидає `IllegalArgumentException` (надалі Task 4 перевірить це на iOS).

**Step 2: Run test to verify it fails**

Run: `./gradlew :shared:data:jvmTest --tests '*CryptoServiceTest*'`
Expected: FAIL — `PlatformCrypto` unresolved.

**Step 3: Add the dependency and the shared source set**

У `shared/data/build.gradle.kts` (в кінець блоку `sourceSets`):

```kotlin
val jvmAndAndroidMain by creating {
    dependsOn(commonMain.get())
}
jvmMain.get().dependsOn(jvmAndAndroidMain)
androidMain.get().dependsOn(jvmAndAndroidMain)
```

і в `jvmAndAndroidMain.dependencies`: `implementation(libs.bouncycastle)`.

У `gradle/libs.versions.toml` — додати рядки в кінець відповідних блоків.

**Step 4: Implement the JVM/Android primitives**

- HKDF: `org.bouncycastle.crypto.generators.HKDFBytesGenerator` з `HKDFParameters(HKDFParameters.kdfTypeHmacSHA256, ikm, salt, info)`.
- GCM: `org.bouncycastle.crypto.modes.GCMBlockCipher` + `AEADParameters(new KeyParameter(key), 128, nonce, aad)`; тег зберігається окремо, `ciphertext` без тегу.
- Випадкові числа: `java.security.SecureRandom`.
- Argon2id: `org.bouncycastle.crypto.generators.Argon2BytesGenerator` з `Argon2Parameters.Builder(Argon2Parameters.ARGON2_id).withVersion(Argon2Parameters.ARGON2_VERSION_13)`; `saltLength`, `outputLength`, `memoryKiB`, `iterations`, `parallelism` з `KdfParams`.
- Нормалізатор: `java.text.Normalizer.normalize(raw, Normalizer.Form.NFKC).toByteArray(Charsets.UTF_8)`.

**Step 5: Run test to verify it passes**

Run: `./gradlew :shared:data:jvmTest --tests '*CryptoServiceTest*'`
Expected: PASS.

**Step 6: Commit**

```bash
git add gradle/libs.versions.toml shared/data/build.gradle.kts shared/data/src
git commit -m "feat(data): add Argon2id, HKDF and GCM on BouncyCastle for JVM and Android"
```

---

### Task 3: Реалізація для iOS (libsodium через ionspin)

**Files:**

- Modify: `gradle/libs.versions.toml` (додати в кінець `[versions]`: `ionspin = "0.9.5"`; в кінець `[libraries]`: `ionspin-libsodium = { module = "com.ionspin.kotlin:multiplatform-crypto-libsodium-bindings", version.ref = "ionspin" }`)
- Modify: `shared/data/build.gradle.kts` (`iosMain.dependencies`: `implementation(libs.ionspin.libsodium)`)
- Create: `shared/data/src/iosMain/kotlin/com/kwazzar/lockruna/data/crypto/PlatformCrypto.kt`
- Create: `shared/data/src/iosMain/kotlin/com/kwazzar/lockruna/data/crypto/PlatformNormalizer.kt`

**Interfaces:**

- Consumes: Task 1 (порти), Task 2 (`expect`-декларації, які треба реалізувати `actual`).
- Produces: `actual class PlatformCrypto`, `actual class PlatformNormalizer` — ті ж сигнатури, що в Task 2.

**Step 1: Verify the bindings expose the required primitives**

Створити тимчасовий тест `shared/data/src/iosTest/kotlin/.../IonspinProbeTest.kt`, який викликає `crypto_pwhash`, `crypto_kdf_hkdf_sha256`, `crypto_aead_aes256gcm_encrypt` і `randombytes_buf` з пакета `com.ionspin.kotlin.crypto.bindings` і перевіряє, що вони компілюються під Kotlin 2.4.20:

Run: `./gradlew :shared:data:iosSimulatorArm64Test --tests '*IonspinProbeTest*'`
Expected: компілюється. Якщо будь-яка функція недоступна — зупинитися і не писати реалізацію: випадок «немає HKDF/AEAD у bindings» вимагає окремого рішення (запасний варіант — `crypto_kdf_derive_from_key` не підходить, тоді розглядається вендоринг libsodium C-джерел із cinterop; це рішення не приймається мовчки).

**Step 2: Implement the iOS primitives**

- Argon2id: `crypto_pwhash(out, outLen, passwd, salt, opslimit = iterations, memlimit = memoryKiB, ALG_ARGON2ID13)`. Salt рівно 16 байт, `p = 1` ✅ (сумісне з обмеженнями libsodium).
- HKDF: `crypto_kdf_hkdf_sha256(out, outLen, key, salt, info)`.
- GCM: `crypto_aead_aes256gcm_encrypt` / `crypto_aead_aes256gcm_decrypt` з `aad` і nonce 12 байт; тег останні 16 байт виводу.
- Випадкові числа: `randombytes_buf`.
- Нормалізатор: `platform.Foundation.NSString` → `precomposedStringWithCompatibilityMapping` (NFKC через системний ICU), далі `.toByteArray(Charsets.UTF_8)`.

**Step 3: Run the probe test again**

Run: `./gradlew :shared:data:iosSimulatorArm64Test --tests '*IonspinProbeTest*'`
Expected: PASS.

**Step 4: Commit**

```bash
git add gradle/libs.versions.toml shared/data/build.gradle.kts shared/data/src/iosMain shared/data/src/iosTest
git commit -m "feat(data): add Argon2id, HKDF and GCM on libsodium for iOS"
```

---

### Task 4: Спільні KAT-вектори й збіжність платформ

**Files:**

- Modify: `shared/data/src/commonTest/kotlin/com/kwazzar/lockruna/data/crypto/CryptoVectors.kt` (Argon2id і NFKC вектори)
- Create: `shared/data/src/commonTest/kotlin/com/kwazzar/lockruna/data/crypto/Argon2VectorsTest.kt`
- Create: `shared/data/src/commonTest/kotlin/com/kwazzar/lockruna/data/crypto/NormalizationVectorsTest.kt`

**Interfaces:**

- Consumes: Task 1-3.
- Produces: `CryptoVectors` — об'єкт із еталонними значеннями, спільними для JVM та iOS.

**Step 1: Generate the reference Argon2id vectors**

Відтворити еталонні значення Argon2id для `p = 1`, `m = 65536`, `t = 3`, salt 16 байт, пароль `correct horse battery staple`:

```bash
argon2 -id -t 3 -m 16 -p 1 -l 16 -r <<< 'correct horse battery staple'
```

(Команда `argon2` з референсної реалізації; якщо її немає в системі — згенерувати еталон через `python3 -m argon2` або зафіксувати вектор із Task 2-результату BouncyCastle **і** додати окремий крок перевірки проти `argon2` CLI в CI). Записати вивід у `CryptoVectors` як hex.

Додатково: три вектори з різною ентропією salt, щоб перехоплення одного виходу не було достатнім.

**Step 2: Write the failing test**

`Argon2VectorsTest.kt`:

```kotlin
class Argon2VectorsTest {
    @Test
    fun `argon2id matches the reference vectors`() = runTest {
        val argon2 = Argon2Factory.create()
        CryptoVectors.argon2Cases.forEach { case ->
            val actual = argon2.hash(case.password, case.salt, KdfParams())
            assertContentEquals(case.expected, actual, "mismatch for salt=${case.salt.toHex()}")
        }
    }
}
```

`NormalizationVectorsTest.kt` — випадки: `"пароль"` (кирилиця), `"e\u0301"` (комбінований акцент → `é`), `"ﬁ"` (лігатура → `fi`), `"Ａ"` (full-width → `A`), `"ẞ"` → `"ß"`, `"１２３"` → `"123"`. Очікувані значення — UTF-8 hex у `CryptoVectors`.

**Step 3: Run test to verify it fails**

Run: `./gradlew :shared:data:jvmTest --tests '*Argon2VectorsTest*' --tests '*NormalizationVectorsTest*'`
Expected: FAIL — векторів і `Argon2Factory` ще немає.

**Step 4: Implement the shared fixtures**

`CryptoVectors` — data-класи `Argon2Case(password, salt, expected)`, `NormalizationCase(raw, expected)`, константи HKDF/GCM з Task 2. `Argon2Factory.create()` — expect/actual, що повертає платформну реалізацію `Argon2` (JVM+Android — BouncyCastle, iOS — libsodium).

**Step 5: Run the tests on both platforms**

Run: `./gradlew :shared:data:jvmTest` і окремо `./gradlew :shared:data:iosSimulatorArm64Test`
Expected: PASS на обох. Якщо вектори розходяться — зупинитися, не «підганяти» очікувані значення.

**Step 6: Commit**

```bash
git add shared/data/src/commonTest shared/data/src/jvmAndAndroidMain shared/data/src/iosMain
git commit -m "test(data): pin Argon2id and NFKC vectors for JVM and iOS"
```

---

### Task 5: Ієрархія ключів, архітектурний тест і правка ADR

**Files:**

- Create: `shared/data/src/commonTest/kotlin/com/kwazzar/lockruna/data/crypto/KeyHierarchyTest.kt`
- Create: `architecture-tests/src/test/kotlin/com/kwazzar/lockruna/architecture/CryptoLayerTest.kt`
- Modify: `docs/adr/003-cryptography.md`
- Modify: `docs/adr/005-client-shared.md`

**Interfaces:**

- Consumes: Task 1-4.
- Produces: документований контракт `masterKey → authKey/kek → wrappedVaultKey` для наступних хвиль.

**Step 1: Write the failing test**

`KeyHierarchyTest.kt` (commonTest, JVM):

```kotlin
class KeyHierarchyTest {
    @Test
    fun `vault key survives a wrap and unwrap round trip`() {
        val crypto = PlatformCrypto()
        val masterKey = crypto.hkdfSha256(ByteArray(32) { 1 }, null, "master".toByteArray(), 32)
        val kek = crypto.hkdfSha256(masterKey, null, HkdfLabels.KEK.toByteArray(), 32)
        val authKey = crypto.hkdfSha256(masterKey, null, HkdfLabels.AUTH.toByteArray(), 32)
        val vaultKey = crypto.randomBytes(32)
        val userId = "user-1"

        val wrapped = crypto.encryptGcm(kek, vaultKey, userId.toByteArray())
        val unwrapped = crypto.decryptGcm(kek, wrapped, userId.toByteArray())

        assertContentEquals(vaultKey, unwrapped)
        assertFalse(authKey.contentEquals(kek))
    }

    @Test
    fun `unwrap with a wrong user id fails`() {
        // decryptGcm мусить кинути помилку, а не повернути сміття
    }
}
```

**Step 2: Run test to verify it passes** (реалізація вже є — тест падає лише через відсутність helper-ів)

Run: `./gradlew :shared:data:jvmTest --tests '*KeyHierarchyTest*'`
Expected: PASS після додавання тесту; якщо ні — виправити реалізацію, а не тест.

**Step 3: Add the architecture test**

`architecture-tests/src/test/kotlin/com/kwazzar/lockruna/architecture/CryptoLayerTest.kt` — Konsist-перевірки:

- файли в `shared/domain/.../domain/crypto/` не імпортують `org.bouncycastle.`, `com.ionspin.`, `io.ktor.`, `app.cash.sqldelight.`, `kotlinx.`;
- `shared/data/.../data/crypto/` не імпортує `com.kwazzar.lockruna.presentation.`;
- у `shared/domain/crypto/` немає класів, названих `Base*`.

Run: `./gradlew :architecture-tests:test`
Expected: PASS.

**Step 4: Update the ADRs**

- `docs/adr/003-cryptography.md`: у розділі про нормалізацію замінити «iOS — системний ICU4C (`unorm2`) через тонкий C-шім» на «iOS — `NSString.precomposedStringWithCompatibilityMapping` (NFKC через системний ICU, реалізація в `shared/data/iosMain`)» і додати рядок, що C-шім не використовується, бо бандлінг ресурсів/бібліотек в XCFramework не потрібен. Зафіксувати, що KDF-параметри, HKDF-рядки та AAD не змінилися.
- `docs/adr/005-client-shared.md`: у §2 оновити, що `PasswordNormalizer` має реалізацію в `shared/data` (Android — `java.text.Normalizer`, iOS — Foundation), а не в `iosApp`; BIP39-кодування recovery key винесено за межі цього етапу.

**Step 5: Run the full verification**

Run: `./gradlew ktlintCheck detekt` і `./scripts/verify.sh`
Expected: обидва — BUILD SUCCESSFUL / «All checks passed».

**Step 6: Commit**

```bash
git add shared/data/src/commonTest architecture-tests docs/adr
git commit -m "test(data): cover the key hierarchy and document the crypto contracts"
```

---

## Порядок злиття

1. `feature/crypto-core` — зливається першим, бо від нього залежать порти для `client-vault-core`.
2. `feature/client-vault-core` — перед злиттям перебазувати на `feature/crypto-core` (або злити crypto першим і перебазувати).
3. `feature/server-auth-sync` — незалежний, зливається в будь-якому порядку.

Після злиття всіх трьох — виконати `graphify update .` один раз у `main` і закомітити оновлення `graphify-out/`.
