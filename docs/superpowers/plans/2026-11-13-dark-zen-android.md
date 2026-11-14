# Dark Zen UI для Lockruna — Android Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Перенести фіксовану тему Dark Zen на всі наявні Android-екрани Lockruna, не змінюючи stores, routing, security чи продуктову поведінку.

**Architecture:** Один `DarkZenComponents.kt` містить палітру, starfield і shared Compose controls; `Theme.kt` надає явну dark Material 3 color scheme та root background. Наявні screen files переоформлюють UI локально, зберігаючи поточні intents/effects і callback signatures.

**Tech Stack:** Kotlin 2.4.20, Jetpack Compose BOM `2026.09.00`, Material 3 1.4.0, minSdk 28, targetSdk 36, KMP `shared` modules.

**Spec:** `docs/superpowers/specs/2026-11-13-dark-zen-ui-design.md`

## Global Constraints

- Dark Zen — єдина фіксована тема; не додавати picker, persistence, `MainTheme`, `MnThemeManager` чи premium-логіку.
- Не змінювати `shared/**`, `server/**`, `api-contract/**`, `architecture-tests/**`, stores, intents, effects, routing, sync чи crypto.
- Canvas: `#0F1424`; accent: `#DB34F2`; primary text: `#FFFFFF`; secondary text: `#99FFFFFF`.
- Card: white 6%; form/editor panel: white 5%; toolbar/outline: white 10%.
- Зорі: 40, white 8%, radius 1 dp, positions створюються один раз через `remember`.
- Material 3 roles повинні бути dark і задані явно; `Scaffold`, `TopAppBar`, `AlertDialog`, dropdown не повинні закривати starfield випадковим tonal surface.
- Primary CTA: opaque `#DB34F2`, white bold label, min height 50 dp, radius 12; auth CTA може бути capsule.
- Dynamic Type не обрізає labels; мінімум 48 dp touch target; decorative stars мають `contentDescription = null`.
- Не додавати залежності, material-icons-extended, шрифти, assets, navigation-compose або нові екрани.
- Зберегти `FLAG_SECURE`; не додавати screenshots/logs/fixtures з recovery words або plaintext item data.

## Review Focus

1. **IME + великий текст:** CTA/поля залишаються доступними на малому Android device з open keyboard і великим font scale.
2. **Системна світла тема:** Material controls залишаються dark, бо `MaterialTheme.colorScheme` явно dark; dialogs/menus мають власний dark container.
3. **Стабільність фону:** starfield не змінюється при recomposition/navigation.
4. **Privacy:** `FLAG_SECURE` і opaque privacy surfaces не змінюються; screenshots не містять secrets.
5. **Touch/accessibility:** icon-only FAB/buttons мають content description і ≥48 dp target; selection/error мають текст/checkmark, не лише колір.

## File Map

- Create: `androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/DarkZenComponents.kt` — palette, starfield, shared controls.
- Modify: `androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/Theme.kt` — Material scheme, shapes, root background.
- Modify: `androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/UnlockScreen.kt` — auth shell.
- Modify: `androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/LoginScreen.kt` — auth shell.
- Modify: `androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/RegisterScreen.kt` — register/recovery/dialog.
- Modify: `androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/VaultListScreen.kt` — vault shell/cards.
- Modify: `androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/ItemDetailScreen.kt` — detail cards/actions.
- Modify: `androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/ItemEditScreen.kt` — form/dropdown/folders.
- Modify: `androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/DevicesScreen.kt` — devices/settings.
- Create: `androidApp/src/main/res/values/colors.xml` — cold-start colors.
- Modify: `androidApp/src/main/res/values/themes.xml` — dark window background/accent.
- Modify: `androidApp/src/main/AndroidManifest.xml` — `adjustResize`.
- Unchanged: `androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/AppRoot.kt`, `MainActivity.kt`, `shared/**`.

> Android app не має наявного Compose UI-test target, а spec забороняє додавати залежності/test infrastructure лише для styling. Найменша доступна перевірка — Gradle compile contract, `mobile-mcp` runtime hierarchy/screenshots і manual flow. Не додавати snapshot-test dependency без окремого запиту.

---

### Task 1: Dark Zen palette, components та Material theme

**Files:**
- Create: `androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/DarkZenComponents.kt`
- Modify: `androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/Theme.kt`
- Create: `androidApp/src/main/res/values/colors.xml`
- Modify: `androidApp/src/main/res/values/themes.xml`
- Modify: `androidApp/src/main/AndroidManifest.xml:13-15`

**Interfaces:**
- Produces: `object DarkZenPalette` з exact color constants.
- Produces: `@Composable fun DarkZenBackground(modifier: Modifier = Modifier)`.
- Produces: `@Composable fun DarkZenTextField(...)`.
- Produces: `@Composable fun DarkZenCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit)`.
- Produces: `@Composable fun DarkZenPrimaryButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit)`.
- Produces: `@Composable fun DarkZenSecondaryButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit)`.
- Produces: `@Composable fun DarkZenDangerButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit)`.
- Preserves: existing public `Theme(content: @Composable () -> Unit)`.

- [ ] **Step 1: Створити compile contract для theme**

У `Theme.kt` використати `DarkZenPalette.Background` до створення object.

- [ ] **Step 2: Переконатися, що контракт падає**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :androidApp:compileDebugKotlin
```

Expected: FAIL з unresolved reference `DarkZenPalette`.

- [ ] **Step 3: Створити exact palette**

`DarkZenComponents.kt` повинен мати:

```kotlin
object DarkZenPalette {
    val Background = Color(0xFF0F1424)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0x99FFFFFF)
    val Card = Color(0x0FFFFFFF)
    val Panel = Color(0x0DFFFFFF)
    val Toolbar = Color(0x1AFFFFFF)
    val Divider = Color(0x1AFFFFFF)
    val Accent = Color(0xFFDB34F2)
    val SecondaryAccent = Color(0xFFBF5AF2)
    val TertiaryAccent = Color(0xFF0A84FF)
    val Success = Color(0xFF30D158)
    val Warning = Color(0xBFFF9F0A)
    val Danger = Color(0xFFFF453A)
    val Muted = Color(0x33FFFFFF)
}
```

- [ ] **Step 4: Реалізувати stable starfield**

`DarkZenBackground`:

- `Box(modifier.fillMaxSize().background(DarkZenPalette.Background))`;
- `val stars = remember { List(40) { Random.nextFloat() to Random.nextFloat() } }`;
- `Canvas` малює circle radius `1.dp`, color `Color(0x14FFFFFF)`;
- жодного touch/semantics content description.

- [ ] **Step 5: Реалізувати shared controls**

Exact public signatures:

```kotlin
@Composable
fun DarkZenTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    minLines: Int = 1,
)
```

- Field: `OutlinedTextField`, radius 12, `Card` container, transparent/default-on colors з accent focused cursor/border, secondary label, `minLines` passthrough.
- `DarkZenCard`: fill `Card`, radius 12, `Divider` border 1 dp, padding 14.
- `DarkZenPrimaryButton`: fill `Accent`, white label, radius 12, `heightIn(min = 50.dp)`, `fillMaxWidth()`.
- `DarkZenSecondaryButton`: transparent/white-5 fill, `Divider` border, radius 12, white label, `heightIn(min = 48.dp)`.
- `DarkZenDangerButton`: transparent fill, danger label, radius 12, `heightIn(min = 48.dp)`.

Усі wrappers мають `enabled` там, де це потрібно для semantic disabled state.

- [ ] **Step 6: Замінити light Material scheme**

У `Theme.kt` створити `private val DarkZenColorScheme = darkColorScheme(...)` з exact roles:

```kotlin
primary = DarkZenPalette.Accent
onPrimary = DarkZenPalette.TextPrimary
primaryContainer = Color(0x33DB34F2)
onPrimaryContainer = DarkZenPalette.TextPrimary
secondary = DarkZenPalette.SecondaryAccent
onSecondary = DarkZenPalette.TextPrimary
secondaryContainer = Color(0x26BF5AF2)
onSecondaryContainer = DarkZenPalette.TextPrimary
tertiary = DarkZenPalette.TertiaryAccent
onTertiary = DarkZenPalette.TextPrimary
tertiaryContainer = Color(0x1A0A84FF)
onTertiaryContainer = DarkZenPalette.TextPrimary
background = DarkZenPalette.Background
onBackground = DarkZenPalette.TextPrimary
surface = DarkZenPalette.Background
onSurface = DarkZenPalette.TextPrimary
surfaceVariant = Color(0xFF1B202F)
onSurfaceVariant = Color(0xFF9FA1A7)
surfaceContainerLowest = DarkZenPalette.Background
surfaceContainerLow = Color(0xFF141A28)
surfaceContainer = Color(0xFF1B202F)
surfaceContainerHigh = Color(0xFF222735)
surfaceContainerHighest = Color(0xFF272C3A)
outline = Color(0xFF272C3A)
outlineVariant = DarkZenPalette.Divider
error = DarkZenPalette.Danger
onError = DarkZenPalette.Background
errorContainer = Color(0x33FF453A)
onErrorContainer = Color(0xFFFFB4AB)
scrim = Color.Black
surfaceTint = Color.Transparent
```

- Задати `Shapes(small = RoundedCornerShape(8.dp), medium = RoundedCornerShape(12.dp), large = RoundedCornerShape(16.dp))`.
- `Theme` wraps content у `Box(Modifier.fillMaxSize()) { DarkZenBackground(); content() }`.

- [ ] **Step 7: Налаштувати cold start та IME**

`colors.xml`:

```xml
<color name="dark_zen_background">#0F1424</color>
<color name="dark_zen_accent">#DB34F2</color>
```

`themes.xml`:

- `android:windowBackground = @color/dark_zen_background`;
- `android:colorAccent = @color/dark_zen_accent`;
- `android:windowLightStatusBar = false`;
- `android:windowLightNavigationBar = false`.

Manifest activity:

```xml
android:windowSoftInputMode="adjustResize"
```

Не змінювати `MainActivity.kt`: `FLAG_SECURE` і поточний `Theme { AppRoot() }` уже коректні.

- [ ] **Step 8: Зібрати й перевірити theme**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :androidApp:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

Через mobile-mcp: `mobile_list_available_devices`, вибрати local Android emulator, `mobile_install_app` для `androidApp/build/outputs/apk/debug/androidApp-debug.apk`, `mobile_launch_app` для `com.kwazzar.lockruna`, `mobile_list_elements_on_screen`, screenshot.

Expected: dark screen, no white flash/background, stable 40-star field; при system theme `light` app залишається dark.

- [ ] **Step 9: Commit**

```bash
git add androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/DarkZenComponents.kt androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/Theme.kt androidApp/src/main/res/values/colors.xml androidApp/src/main/res/values/themes.xml androidApp/src/main/AndroidManifest.xml
git commit -m "feat(android): add Dark Zen design layer"
```

---

### Task 2: Темизація authentication screens

**Files:**
- Modify: `androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/UnlockScreen.kt`
- Modify: `androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/LoginScreen.kt`
- Modify: `androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/RegisterScreen.kt`

**Interfaces:**
- Consumes: `DarkZenTextField`, `DarkZenPrimaryButton`, `DarkZenSecondaryButton`, `DarkZenPalette` з Task 1.
- Preserves: `UnlockScreen`, `LoginScreen`, `RegisterScreen`, `RecoveryScreen` public signatures та усі intents/effects.

- [ ] **Step 1: Перенести Unlock/Login у scrollable auth shell**

Для кожного:

- `Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(24.dp))`;
- title `titleLarge` bold: `Unlock` для Unlock та `Sign in` для Login;
- fields через `DarkZenTextField`;
- submit через `DarkZenPrimaryButton`;
- biometric/register/recovery через `DarkZenSecondaryButton` або text button з secondary color;
- spinner accent;
- error `Danger`.

Не змінювати effect collection, dispatch order, password masking чи biometric condition.

- [ ] **Step 2: Перенести Register form**

- Title `Create account` у `titleLarge` bold.
- Shared fields/button/spinner/error treatment.
- Strength — `bodySmall` secondary; warnings — `Warning`.
- Exact intent order та `DisposableEffect` cleanup.

- [ ] **Step 3: Перенести recovery-key dialog**

- `AlertDialog(containerColor = DarkZenPalette.Background, titleContentColor = TextPrimary, textContentColor = TextSecondary)`.
- `onDismissRequest = { }` залишити.
- Words — `DarkZenCard`, readable окремо для TalkBack.
- Confirmation — `DarkZenTextField`.
- Confirm — accent text; error — danger.
- Не додавати preview/test fixture з recovery words.

- [ ] **Step 4: Перенести Recovery screen**

- Title `Recover access` у `titleLarge` bold.
- Scrollable/IME-safe column, shared fields/button.
- Exact splitting, intent order, local cleanup та effect navigation.

- [ ] **Step 5: Зібрати й перевірити auth states**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :androidApp:assembleDebug
```

Через mobile-mcp перевірити Login, Register, Recovery labels, open keyboard та scroll reachability. Screenshot робити лише до показу recovery words.

- [ ] **Step 6: Перевірити behavior diff**

`git diff -- androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/{UnlockScreen,LoginScreen,RegisterScreen}.kt` повинен містити layout/style changes; dispatch/effect/cleanup order не змінюється.

- [ ] **Step 7: Commit**

```bash
git add androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/UnlockScreen.kt androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/LoginScreen.kt androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/RegisterScreen.kt
git commit -m "feat(android): theme authentication screens"
```

---

### Task 3: Темизація Vault list, Item Detail та Item Edit

**Files:**
- Modify: `androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/VaultListScreen.kt`
- Modify: `androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/ItemDetailScreen.kt`
- Modify: `androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/ItemEditScreen.kt`

**Interfaces:**
- Consumes: shared Dark Zen controls/palette з Task 1.
- Preserves: public screen signatures, item/store callbacks, existing intents/effects.

- [ ] **Step 1: Переоформити Vault list**

- `Scaffold(containerColor = Color.Transparent)`.
- `TopAppBar(containerColor = Color.Transparent, title = Text("Vault"))`.
- Existing Sync/Devices/Settings/Lock actions — secondary text, без system blue tint.
- Search — `DarkZenTextField`.
- Item rows — `DarkZenCard`, title `titleMedium` semibold, username `bodySmall` secondary.
- FAB — source-like white-6 surface, white-10 border, `contentDescription = "Add item"`, ≥48 dp.
- Не додавати loading/empty-state branches, search features чи new actions.

- [ ] **Step 2: Переоформити Item Detail**

- Title/username — `DarkZenCard`; revealed secret — panel/card з monospace secondary treatment.
- Reveal/Copy — accent actions; Hide/Edit — secondary; Delete — danger.
- Показати вже наявний `onBack` callback як back affordance; не додавати новий route.
- Зберегти reveal/hide/copy/edit/delete dispatch та local `revealed` lifecycle.

- [ ] **Step 3: Переоформити Item Edit**

- Усі fields через `DarkZenTextField`.
- Material 3 1.4 dropdown anchor: `Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)`; не копіювати застарілий menuAnchor API source.
- Dropdown container — dark Material role/explicit background.
- Folder choices — chips: unselected white 10%, selected accent 15–20%; `No folder` — secondary.
- Save — primary; Back — secondary.
- Зберегти type rules, NOTE unmasked transformation, folder `null`, save callback.

- [ ] **Step 4: Зібрати та перевірити compile/API contract**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :androidApp:assembleDebug
```

Expected: `BUILD SUCCESSFUL`; Material 3 1.4 dropdown API компілюється без warnings.

- [ ] **Step 5: Перевірити runtime flow**

На local test account без plaintext screenshots:

- open Vault, open item, back, create/save item;
- перевірити card/action hierarchy, dropdown, FAB label/target, keyboard reachability.

Не робити screenshot після Reveal.

- [ ] **Step 6: Commit**

```bash
git add androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/VaultListScreen.kt androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/ItemDetailScreen.kt androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/ItemEditScreen.kt
git commit -m "feat(android): theme vault screens"
```

---

### Task 4: Темизація Devices та Settings

**Files:**
- Modify: `androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/DevicesScreen.kt`

**Interfaces:**
- Consumes: shared Dark Zen controls/palette з Task 1.
- Preserves: `DevicesScreen(store)` та `SettingsScreen(store, onAccountDeleted)` signatures, intents/effects.

- [ ] **Step 1: Переоформити Devices**

- Title `titleLarge` bold.
- Device rows — `DarkZenCard`: name primary, platform/current secondary, revoke danger text.
- Existing loading — accent spinner; existing error — danger у card.
- Revoke `AlertDialog`: `containerColor = Background`, title primary, body secondary, confirm danger, cancel secondary.

- [ ] **Step 2: Переоформити Settings**

- Scrollable column із section cards: Auto-lock, Biometric unlock, Delete account.
- Cards: `DarkZenCard`/panel treatment, radius 16, padding 20, gap 20.
- Radio selected accent; Switch checked accent; disabled state preserved.
- Delete account text/dialog danger; password field shared.
- Не додавати theme picker.

- [ ] **Step 3: Зібрати та перевірити screens**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :androidApp:assembleDebug
```

Через mobile-mcp перевірити labels, selected/disabled states, revoke/delete dialogs та відсутність system blue tint.

- [ ] **Step 4: Commit**

```bash
git add androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/DevicesScreen.kt
git commit -m "feat(android): theme account screens"
```

---

### Task 5: Android acceptance verification

**Files:**
- Modify: тільки якщо verification виявить дефект; виправлення належать відповідному task file set.

**Interfaces:**
- Consumes: усі Android tasks.
- Produces: перевірену Dark Zen Android UI без змін у shared/security.

- [ ] **Step 1: Виконати повну Gradle verification**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :api-contract:jvmTest :shared:domain:jvmTest :shared:application:jvmTest :shared:presentation:jvmTest :shared:data:jvmTest :architecture-tests:test :androidApp:assembleDebug
./gradlew ktlintCheck detekt
```

Expected: обидва commands `BUILD SUCCESSFUL`; Konsist без suppressions.

- [ ] **Step 2: Перевірити runtime на Android emulator**

Через mobile-mcp:

1. `mobile_list_available_devices` і вибрати local Android device.
2. Встановити debug APK та запустити `com.kwazzar.lockruna`.
3. `mobile_list_elements_on_screen` для доступності labels.
4. Screenshots — лише без recovery words/revealed secrets.
5. Перевірити keyboard reachability, touch targets, dark dialogs/dropdown та starfield stability.

Якщо local server/test account недоступні, не вигадувати vault state: відмітити відповідний flow як environment-blocked.

- [ ] **Step 3: Перевірити privacy**

Підтвердити, що `MainActivity` і далі додає `FLAG_SECURE`; не змінювати `PrivacyShield`/`MainActivity` без доказу проблеми.

- [ ] **Step 4: Перевірити diff**

```bash
git diff --check
git status --short
git diff main...HEAD -- androidApp/
```

Expected: тільки затверджені Android files; жодних змін у shared/server.

- [ ] **Step 5: Оновити graph та перевірити secrets**

```bash
graphify update .
gitleaks detect --source . --no-banner
```

Expected: graph оновлено; gitleaks не знаходить секретів.

- [ ] **Step 6: Commit verification fixes, якщо були**

Якщо verification вимагав виправлень, повторити assemble/lint і commit:

```bash
git add <only-fixed-android-files>
git commit -m "fix(android): polish Dark Zen accessibility"
```
