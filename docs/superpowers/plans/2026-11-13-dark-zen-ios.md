# Dark Zen UI для Lockruna — iOS Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Перенести фіксовану тему Dark Zen на всі наявні iOS-екрани Lockruna, не змінюючи маршрутизацію, stores, security чи продуктову поведінку.

**Architecture:** Один новий `DarkZenTheme.swift` містить статичні токени, стабільний starfield і невеликі SwiftUI styles, які повторно використовують екрани. `ContentView.swift` встановлює root background і dark color scheme; `AccountViews.swift` та `ContentView.swift` лише переоформлюють наявні views, зберігаючи всі intents/effects/local state.

**Tech Stack:** SwiftUI, Swift 5, Xcode 26, iOS 17+, KMP `Shared` framework, XcodeBuildMCP.

**Spec:** `docs/superpowers/specs/2026-11-13-dark-zen-ui-design.md`

## Global Constraints

- Dark Zen — єдина фіксована тема; не додавати picker, persistence, `MainTheme` чи `ThemeManager`.
- Не змінювати `shared/**`, `server/**`, `api-contract/**`, `architecture-tests/**`, stores, intents, effects, routing, sync чи crypto.
- Canvas: `#0F1424`; accent: `#DB34F2`; primary text: `#FFFFFF`; secondary text: `#99FFFFFF`.
- Card: white 6%; form/editor panel: white 5%; toolbar/outline: white 10%.
- Зорі: 40, white 8%, 2×2 pt, positions генеруються один раз і не змінюються при redraw.
- Primary CTA: opaque `#DB34F2`, white bold label, capsule для auth або radius 12 для inline CTA.
- Семантичні Dynamic Type fonts; мінімум 44 pt touch target; decorative background недоступний для VoiceOver.
- Не додавати залежності, шрифти, assets, новий Xcode test target або нові екрани.
- iOS build/test/run — тільки XcodeBuildMCP; не використовувати raw `xcodebuild`.
- Privacy shield та app-switcher overlay не стають translucent.

## Review Focus

1. **Keyboard + великий текст:** CTA/поля мають залишатися доступними на малому iPhone при Dynamic Type та software keyboard.
2. **Системна світла тема:** TextField, SecureField, Picker, Toggle, Alert і Menu повинні бути dark через `.preferredColorScheme(.dark)`.
3. **Стабільність фону:** зорі не повинні мерехтіти під час навігації чи зміни стану.
4. **Privacy:** background/app-switcher snapshot не розкриває vault content; recovery words та plaintext не потрапляють у screenshots/logs/fixtures.
5. **Touch/accessibility:** back/close/action targets не менші 44 pt; selection/error мають текст абоcheckmark, не лише колір.

## File Map

- Create: `iosApp/Lockruna/DarkZenTheme.swift` — токени, starfield, field/button styles.
- Modify: `iosApp/Lockruna/ContentView.swift` — root, auth, vault list/detail/edit.
- Modify: `iosApp/Lockruna/AccountViews.swift` — register/recovery, devices/settings.
- Modify: `iosApp/Lockruna.xcodeproj/project.pbxproj` — явне підключення нового Swift file.
- Unchanged: `iosApp/Lockruna/LockrunaApp.swift`, `iosApp/Lockruna/PrivacyShield.swift`.

> У проєкті немає UI-test target, а spec забороняє додавати залежності/test target лише для styling. Найменша доступна перевірка — compile contract через XcodeBuildMCP, runtime `snapshot_ui`/`screenshot` і manual flow. Не імітувати автоматизований UI test, якого тут немає.

---

### Task 1: Dark Zen design layer та root shell

**Files:**
- Create: `iosApp/Lockruna/DarkZenTheme.swift`
- Modify: `iosApp/Lockruna/ContentView.swift:12-53`
- Modify: `iosApp/Lockruna.xcodeproj/project.pbxproj:9-53,141-153`

**Interfaces:**
- Produces: `enum DarkZen` зі static colors/radii.
- Produces: `struct DarkZenBackground: View`.
- Produces: `struct DarkZenTextFieldStyle: TextFieldStyle`.
- Produces: `struct DarkZenPrimaryButtonStyle: ButtonStyle` з `init(cornerRadius: CGFloat = DarkZen.cardRadius)`.
- Consumes: лише SwiftUI; жодних Shared types.

- [ ] **Step 1: Створити compile contract для root**

У `ContentView.body` додати перед `Group`:

```swift
DarkZenBackground()
```

та до кореневого `ZStack`:

```swift
.preferredColorScheme(.dark)
```

Не реалізувати типи на цьому кроці.

- [ ] **Step 2: Переконатися, що контракт падає**

Через XcodeBuildMCP:

1. `session_show_defaults()`.
2. Якщо defaults не вказують на worktree project, `session_set_defaults` з `projectPath: <worktree>/iosApp/Lockruna.xcodeproj`, `scheme: Lockruna`; simulator вибрати з `list_sims()`.
3. `build_run_sim({})`.

Expected: FAIL з unresolved identifier `DarkZenBackground`.

- [ ] **Step 3: Створити `DarkZenTheme.swift` і підключити його до Xcode project**

Додати file reference, build file, group child і Sources entry. Використати вільні ідентифікатори, наприклад `1A0000000000000000000032` (build file) та `1A0000000000000000000033` (file reference), перевіривши, що вони не зайняті.

- [ ] **Step 4: Реалізувати точні токени**

`DarkZen` повинен мати щонайменше такі static values:

```swift
static let background = Color(red: 0.06, green: 0.08, blue: 0.14)
static let accent = Color(red: 0.859, green: 0.204, blue: 0.949)
static let textPrimary = Color.white
static let textSecondary = Color.white.opacity(0.60)
static let card = Color.white.opacity(0.06)
static let panel = Color.white.opacity(0.05)
static let toolbar = Color.white.opacity(0.10)
static let divider = Color.white.opacity(0.10)
static let danger = Color.red.opacity(0.80)
static let warning = Color.orange.opacity(0.75)
static let fieldRadius: CGFloat = 8
static let cardRadius: CGFloat = 12
static let panelRadius: CGFloat = 16
```

- [ ] **Step 5: Реалізувати starfield**

`DarkZenBackground` має:

- fill `#0F1424` і `ignoresSafeArea()`;
- `@State private var stars: [UnitPoint] = []`;
- у `onAppear` згенерувати рівно 40 `UnitPoint` через `.random(in: 0...1)`;
- `Canvas` малює кожну зірку ellipse `2×2` з `DarkZen.textPrimary.opacity(0.08)`;
- `allowsHitTesting(false)` та `accessibilityHidden(true)`.

- [ ] **Step 6: Реалізувати field та primary button styles**

`DarkZenTextFieldStyle._body(configuration:)` повинен:

- додати `.padding(10)`;
- background `.ultraThinMaterial`;
- `clipShape(RoundedRectangle(cornerRadius: DarkZen.fieldRadius))`;
- додати inset stroke `DarkZen.divider`, line width 2, inset 1;
- не задавати власний fixed height.

`DarkZenPrimaryButtonStyle` має `init(cornerRadius: CGFloat = DarkZen.cardRadius)` і `makeBody(configuration:)` повинен:

- label: `.font(.headline)` та `DarkZen.textPrimary`;
- `frame(maxWidth: .infinity, minHeight: 50)`;
- fill `DarkZen.accent`; pressed state зменшує opacity;
- `RoundedRectangle(cornerRadius: cornerRadius)`; auth screens передають `999`, inline screens — default `12`;
- не додавати власну animation.

- [ ] **Step 7: Зібрати й перевірити root**

Через XcodeBuildMCP виконати `build_run_sim({})`, потім `snapshot_ui()` і `screenshot({ returnFormat: "path" })`.

Expected:

- build status `SUCCEEDED`;
- runtime screen має dark canvas і не містить білого system background;
- при simulator system appearance `light` app залишається dark (`preferredColorScheme(.dark)`);
- screenshot не містить plaintext item data або recovery words.

- [ ] **Step 8: Commit**

```bash
git add iosApp/Lockruna/DarkZenTheme.swift iosApp/Lockruna/ContentView.swift iosApp/Lockruna.xcodeproj/project.pbxproj
git commit -m "feat(ios): add Dark Zen design layer"
```

---

### Task 2: Темизація authentication screens

**Files:**
- Modify: `iosApp/Lockruna/DarkZenTheme.swift` — add `DarkZenAuthShell`.
- Modify: `iosApp/Lockruna/ContentView.swift:56-147` — `UnlockView`, `LoginView`.
- Modify: `iosApp/Lockruna/AccountViews.swift:9-197` — `RegisterView`, `RecoveryView`.

**Interfaces:**
- Consumes: `DarkZen`, `DarkZenTextFieldStyle`, `DarkZenPrimaryButtonStyle` з Task 1.
- Produces: `struct DarkZenAuthShell<Content: View>: View` з `init(title: String, @ViewBuilder content: () -> Content)`; shell містить scrollable centered VStack, title і content.
- Auth primary buttons використовують `DarkZenPrimaryButtonStyle(cornerRadius: 999)`; confirm у recovery-key branch — теж capsule.

- [ ] **Step 1: Перенести Unlock/Login у scrollable auth layout**

Для кожного екрану:

- обгорнути existing fields/actions у `DarkZenAuthShell(title: "Unlock" | "Sign in")`;
- shell має `ScrollView` + centered `VStack(spacing: 12)`, title `.title.bold()` і 24 pt outer inset;
- fields у `TextFieldStyle`/`SecureFieldStyle` через `.textFieldStyle(DarkZenTextFieldStyle())`;
- submit button через `.buttonStyle(DarkZenPrimaryButtonStyle(cornerRadius: 999))`;
- biometric/Create account/Lost access — `.buttonStyle(.plain)` і `DarkZen.textSecondary`;
- error — `DarkZen.danger`;
- loading `ProgressView` — `.tint(DarkZen.accent)`;
- keyboard-safe scrolling, але не `ignoresSafeArea(.keyboard)`.

Не змінювати dispatch order, task cancellation, biometric condition чи password cleanup.

- [ ] **Step 2: Перенести Register form**

Обгорнути form у `DarkZenAuthShell(title: "Create account")`. Зберегти exact intent order (`EmailChanged`, `PasswordChanged`, `DeviceNameChanged`, `Submit`) і branches `isSubmitting`/`error`. Strength text — `DarkZen.textSecondary`; warnings — `DarkZen.warning`; submit — primary style.

- [ ] **Step 3: Перенести recovery-key branch без розкриття секретів у preview**

Закрити 12 words у card:

- fill `DarkZen.panel`;
- radius `DarkZen.cardRadius`;
- white-10 border;
- numbered words readable окремо для VoiceOver.

Confirmation field — `DarkZenTextFieldStyle`; confirm button — primary style; error — danger. Не додавати preview/fixture з реальними words.

- [ ] **Step 4: Перенести Recovery screen**

Обгорнути form у `DarkZenAuthShell(title: "Recover access")`. Зберегти splitting `words.split(separator: " ").map(String.init).filter { !$0.isEmpty }`, exact intent order, cleanup on disappear та effect-driven navigation.

- [ ] **Step 5: Зібрати та перевірити auth states**

Через XcodeBuildMCP:

- `build_run_sim({})` — expected `SUCCEEDED`;
- `snapshot_ui()` на Login: labels Email/Password/Device name/Sign in/Create account/Lost access доступні;
- відкрити Register і Recovery через refs; snapshot має всі поля;
- увімкнути software keyboard і переконатися, що CTA/scroll достатні;
- screenshot не робити на екрані з фактичними recovery words.

- [ ] **Step 6: Перевірити збереження поведінки**

`git diff -- iosApp/Lockruna/ContentView.swift iosApp/Lockruna/AccountViews.swift` повинен показувати лише layout/style зміни; жоден dispatch/effect/local-state block не змінює порядок чи cleanup semantics.

- [ ] **Step 7: Commit**

```bash
git add iosApp/Lockruna/DarkZenTheme.swift iosApp/Lockruna/ContentView.swift iosApp/Lockruna/AccountViews.swift
git commit -m "feat(ios): theme authentication screens"
```

---

### Task 3: Темизація vault list, detail та edit

**Files:**
- Modify: `iosApp/Lockruna/ContentView.swift:149-281` — `VaultContainer`, `VaultListView`, `ItemDetailView`, `ItemEditView`.

**Interfaces:**
- Consumes: `DarkZen` styles з Task 1.
- Preserves: `VaultListIntentItemClicked`, `SessionIntentLock`, `ItemDetailIntent*`, `ItemEditIntent*`, `route`, `selectedId`, `model.sync()`.

- [ ] **Step 1: Переоформити Vault list**

- Title `Vault` у `.title.bold()`.
- Замінити default `List` rows на `ScrollView` + `LazyVStack` cards: `DarkZen.card`, radius 12, white-10 border, padding 14.
- Item click, `selectedId` та route залишити без змін.
- Existing Lock/Sync/Devices/Settings перетворити на themed secondary controls у compact action bar/capsule.
- New item — primary accent CTA.
- Не додавати search, empty-state copy, swipe actions чи folders.

- [ ] **Step 2: Переоформити Item Detail без показу secret**

- Title/username у panel (`DarkZen.panel`, radius 16).
- Reveal — primary style; Copy/Edit — secondary outlined/plain; Hide — secondary text; Delete — danger.
- Наявний Back має бути back affordance з мінімальним 44 pt target та accessibility label.
- **Не додавати rendering secret**, навіть якщо `Reveal` intent існує.

- [ ] **Step 3: Переоформити Item Edit**

- Title, picker, folders — themed controls.
- Picker label/selection — accent.
- Folder rows — card/panel; selected state — accent, `No folder` — secondary.
- Save — primary style; existing Back — secondary style з мінімальним 44 pt target.
- Зберегти type index dispatch, folder derivation, `nil` folder selection, save order, route та sync.

- [ ] **Step 4: Зібрати й перевірити compile contract**

Через XcodeBuildMCP `build_run_sim({})`.

Expected: `SUCCEEDED`; runtime logs без Swift compile warnings, пов’язаних з theme file.

- [ ] **Step 5: Перевірити vault flow**

На локальному test account без реальних secrets:

- відкрити Vault;
- відкрити item, повернутися, створити/зберегти item;
- перевірити card hierarchy, action hierarchy, 44 pt targets та route behavior.

Не робити screenshot після Reveal.

- [ ] **Step 6: Commit**

```bash
git add iosApp/Lockruna/ContentView.swift
git commit -m "feat(ios): theme vault screens"
```

---

### Task 4: Темизація Devices та Settings

**Files:**
- Modify: `iosApp/Lockruna/AccountViews.swift:199-358` — `DevicesView`, `SettingsView`.

**Interfaces:**
- Consumes: `DarkZen` styles з Task 1.
- Preserves: Devices load/revoke effects, Settings load/policy/biometrics/delete effects, deletion-password lifecycle.

- [ ] **Step 1: Переоформити Devices**

- Explicit title `Devices`; не покладатися на `navigationTitle` без navigation container.
- Device rows — cards: name primary, platform/current secondary, revoke danger text.
- Existing loading/error branches — accent spinner та danger text у card.
- Existing revoke alert залишити system destructive alert, але зберігати exact device ID flow.

- [ ] **Step 2: Переоформити Settings**

- Scrollable stack з трьома panel-ів: Auto-lock, Biometric unlock, Delete account.
- Panel: `DarkZen.panel`, radius 16, padding 20, gap 20.
- Selected auto-lock — accent checkmark; biometric toggle — accent tint; delete — danger.
- Master-password field — `DarkZenTextFieldStyle`.
- Не додавати theme picker або Dark Zen setting row.

- [ ] **Step 3: Зібрати та перевірити screens**

Через XcodeBuildMCP `build_run_sim({})` та `snapshot_ui()`.

Expected: build `SUCCEEDED`; Devices/Settings labels доступні; controls мають semantic labels; delete disabled без password.

- [ ] **Step 4: Commit**

```bash
git add iosApp/Lockruna/AccountViews.swift
git commit -m "feat(ios): theme account screens"
```

---

### Task 5: iOS acceptance verification

**Files:**
- Modify: тільки якщо verification виявить дефект; виправлення належать відповідному task file set.

**Interfaces:**
- Consumes: усі iOS tasks.
- Produces: перевірену Dark Zen гілку без змін у shared/security.

- [ ] **Step 1: Виконати повну iOS build/run**

Через XcodeBuildMCP:

1. `session_show_defaults()`;
2. `build_run_sim({})`;
3. `snapshot_ui()` на кожному доступному екрані;
4. `screenshot({ returnFormat: "path" })` лише на екранах без recovery words/revealed secrets.

Expected: `SUCCEEDED`, stable runtime, відсутність crash logs.

- [ ] **Step 2: Перевірити keyboard та accessibility**

- software keyboard не ховає CTA;
- VoiceOver/semantic UI labels присутні;
- touch targets ≥44 pt;
- error/selection не кодуються лише кольором;
- starfield недоступний VoiceOver.

- [ ] **Step 3: Перевірити privacy**

Перевести app у background і підтвердити, що app-switcher overlay/PrivacyShield не показує item data. Не виводити plaintext у console, screenshot чи fixture.

- [ ] **Step 4: Перевірити diff і whitespace**

```bash
git diff --check
git status --short
git diff main...HEAD -- iosApp/
```

Expected: тільки затверджені iOS files; `local.properties` залишається ignored і не входить у diff.

- [ ] **Step 5: Оновити graph та перевірити secrets**

```bash
graphify update .
gitleaks detect --source . --no-banner
```

Expected: graph оновлено; gitleaks не знаходить секретів.

- [ ] **Step 6: Commit verification fixes, якщо були**

Якщо verification вимагав виправлень, повторити точкову XcodeBuildMCP build і commit:

```bash
git add <only-fixed-ios-files>
git commit -m "fix(ios): polish Dark Zen accessibility"
```
