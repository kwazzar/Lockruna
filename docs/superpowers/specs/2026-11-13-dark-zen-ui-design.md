# Dark Zen UI для Lockruna — дизайн

**Статус:** запропоновано, очікує схвалення  
**Дата:** 13 листопада 2026  
**Джерело дизайну:** `/Users/nazar/Swift/MeditateAndNote` (лише читається)

## 1. Намір і контекст

Lockruna має отримати єдиний Dark Zen вигляд на iOS та Android, узгоджений із наявною темою Dark Zen у `MeditateAndNote`. Перенесення охоплює всі користувацькі екрани Lockruna, але не змінює його продуктову поведінку, модель даних, маршрутизацію чи безпеку.

Dark Zen буде **єдиною фіксованою темою**. Вибір тем, збереження теми, premium-гейти та налаштування теми не переносяться.

## 2. Рішення, які вже прийнято

- Переносити дизайн на **iOS і Android**.
- Використовувати **Dark Zen design layer**: токени та спільні компоненти, поверх яких переоформлюються наявні екрани.
- Зберігати всі stores, intents, effects, routing, sync і security-механіки без змін.
- Працювати в окремому git worktree: `feature/meditate-note-dark-design`.

## 3. Цілі

1. Усі наявні екрани Lockruna на iOS та Android виглядають як єдиний Dark Zen продукт.
2. Відтворено ключові візуальні риси Dark Zen: нічний фон зі стабільними зорями, темні напівпрозорі поверхні, фіолетовий акцент, матеріали, картки, поля, кнопки та стану завантаження/помилки.
3. Системні елементи — поля, picker, toggle, menu, dialog — мають темний вигляд навіть тоді, коли система у світлій темі.
4. UI залишається придатним для Dynamic Type, VoiceOver/TalkBack, клавіатури, safe areas і малих екранів.
5. Жодна зміна не послаблює privacy shield, `FLAG_SECURE`, захист recovery key або інші security-гарантії.

## 4. Не входить у роботу

- Перемикач тем, `MainTheme`, theme manager, persistence або premium-логіка.
- Meditation, breathing, notes, streak, library, paywall та інші функції `MeditateAndNote`, яких немає в Lockruna.
- Нова навігація, `NavigationStack`/`Navigation Compose`, tab bar або переробка route-моделі.
- Нові пошукові, empty-state, folder-navigation чи delete-confirmation можливості.
- Нові startup/loading/empty-state branches або повідомлення: оформлюються лише вже наявні UI-стани; store states, які зараз не показуються, не починають показуватися без окремого запиту.
- Зміни у `shared/`, `server/`, `api-contract/`, domain/application/data шарах.
- Нові залежності, шрифти, іконки або asset-файли.
- Зміна app icon, product name або privacy overlay.

## 5. Обмеження

- iOS підтримує iOS 17+, тому використовується `.ultraThinMaterial`, а не iOS 26-only `.glassEffect`.
- Android використовує наявні Compose BOM та Material 3; кодуємо під фактично встановлені API, без копіювання старих API джерела без перевірки.
- Swift-код залишається в `iosApp/` і не переносить бізнес-логіку в presentation-файли.
- `MeditateAndNote` є референсом дизайну, а не runtime-залежністю: жоден його файл не копіюється в Lockruna.
- Стандартні системні тексти залишаються мовою поточного продукту.

## 6. Розглянуті підходи

### A. Dark Zen design layer — вибрано

Один фіксований дизайн-шар у кожному нативному клієнті: токени, фон і спільні стилі/компоненти. Наявні екрани переоформлюються, а їхня поведінка залишається недоторканою.

**Переваги:** цілісний результат на обох платформах, помірний diff, продуктові ризики залишаються в нативних шарах.  
**Недоліки:** потрібно вручну перенести компонентні правила в дві мови.

### B. Тільки палітра й фон

Змінюються колір і зорі, а стандартні `List`/`Form`/Material-контроли лишаються.

**Переваги:** найменший diff.  
**Недоліки:** результат не виглядає як Dark Zen; системні контроли залишаються несогласованими.

### C. Структурний порт `MeditateAndNote`

Копіюються структури екранів і navigation source, а функції Lockruna вміщуються в них.

**Переваги:** максимальна структурна близькість до source.  
**Недоліки:** переписування UI, ризик для поточної логіки, зайві source-патерни для meditation/note-продукту.

## 7. Візуальна мова Dark Zen

### 7.1 Кольори

| Роль | Значення | Використання |
|---|---|---|
| Canvas | `#0F1424` | Фон усього застосунку |
| Primary text | `#FFFFFF` | Заголовки, основний текст |
| Secondary text | `#99FFFFFF` | Підписи, metadata |
| Card | `#0FFFFFFF` | Картки та list rows (white 6%) |
| Editor/form panel | `#0DFFFFFF` | Форми, detail panels (white 5%) |
| Toolbar/surface | `#1AFFFFFF` | Toolbar chips та обрані обрани (white 10%) |
| Divider/outline | `#1AFFFFFF` | Hairline та межі |
| Accent | `#DB34F2` | Primary CTA, selected/focus states |
| Secondary accent | `#BF5AF2` | Допоміжний selected/focus |
| Tertiary accent | `#0A84FF` | Вторинний selected state |
| Success | `#30D158` | Успішні стани |
| Warning | `#FF9F0A` | Warnings |
| Danger | `#FF453A` | Помилки та destructive text/actions |
| Muted | `#33FFFFFF` | Disabled |

Material-ролі Android отримують явні dark-значення. Напівпрозорі кольори source зберігаються для карток; для системних surface-ролей використовуються непрозорі композитні кольори, щоб dialog/menu не отримали випадкові tone elevation.

### 7.2 Фон

- Суцільний `#0F1424`, без gradient.
- 40 стабільно згенерованих зірок білого кольору з 8% opacity.
- Зорі не змінюють положення під час recomposition/render і не перехоплюють touch.
- Фон один на весь app root; лише він ігнорує safe area.

### 7.3 Форма та поверхні

- Поля: radius 8 (iOS) / 12 dp (Android), внутрішня відстань 10, white 6% surface, hairline border.
- Картки: radius 12, white 6%, white 10% border, м'яка тінь radius 5 / offset 2.
- Settings/form panels: radius 16, white 5%, padding 20.
- Floating/action capsule: material surface, white 10% border, radius 5 shadow.
- Primary CTA: непрозорий `#DB34F2`, білий bold label, capsule для auth або radius 12 для inline CTA.
- Secondary CTA: текст/white 10% surface, без системного синього tint.
- Destructive CTA: red text/action; не заповнювати red-кнопкою дрібний текст без контрасту.

### 7.4 Типографіка та spacing

- Використовуються системні семантичні шрифти (`title`, `headline`, `body`, `caption`; Material type roles), без нових font files.
- Screen title: `titleLarge`/`title` bold.
- Section title: `headline`/`titleMedium` semibold.
- Основний body: `body`/`bodyMedium`.
- Supporting text: `subheadline`/`bodySmall`.
- Зовнішній inset: 16; auth forms: 24; card padding: 14–20; gaps: 8–20.

## 8. iOS-дизайн

### 8.1 Структура

- Новий `iosApp/Lockruna/DarkZenTheme.swift`: статичні кольори, стабільний starfield, спільні field/card/button/surface styles.
- `ContentView.swift`: root background, `.preferredColorScheme(.dark)`, auth, vault list, item detail та item edit.
- `AccountViews.swift`: register/recovery, devices та settings.
- `Lockruna.xcodeproj/project.pbxproj`: додати новий Swift file, бо target використовує явні file references.
- `LockrunaApp.swift` та `PrivacyShield.swift` не змінюються функціонально.

### 8.2 Екрани

| Екран | Dark Zen-адаптація |
|---|---|
| Initial loading | Наявний screen з порожнім route (`""`) отримує dark canvas і accent spinner; окремий startup flow не додається |
| Unlock | Scrollable auth form, themed fields, purple capsule CTA, biometric secondary action |
| Login | Та сама auth shell; register/recovery — secondary links |
| Register | Auth form + strength/warning states; recovery words у card, confirmation field |
| Recovery | Auth shell, multi-line recovery field, accent submit CTA |
| Vault list | Title, item cards, themed action row, purple New item CTA; без пошуку та нових дій |
| Item detail | Dark detail panel для title/username, accent Reveal, secondary Copy/Edit, danger Delete, наявний Back semantics; iOS не починає показувати secret, якого зараз не рендерить |
| Item edit | Themed fields/picker, folder rows у card, purple Save CTA |
| Devices | Explicit title замість покладання на `navigationTitle`, device cards, loading/error states |
| Settings | Scrollable stack із трьох panel-ів; без theme picker |

## 9. Android-дизайн

### 9.1 Структура

- Новий `androidApp/.../ui/DarkZenComponents.kt`: палітра, shared field/card/button styles та starfield, якщо його вигідніше розмістити поруч із theme.
- `Theme.kt`: fixed Dark Zen `MaterialTheme`, background/starfield, явні Material role/shape values.
- Наявні screen files: unlock, login, register/recovery, vault list, detail, edit, devices/settings.
- Android theme resources: dark window/splash background, щоб cold start не показував світлий flash.
- System bars отримують dark/transparent treatment; `FLAG_SECURE` залишається без змін.
- Manifest отримує `adjustResize`, а scrollable forms — `imePadding()`, щоб кнопка submit не ховалася за клавіатурою.

### 9.2 Екрани

| Екран | Dark Zen-адаптація |
|---|---|
| Initial loading | Перший поточний frame має dark theme; окремий startup state не додається |
| Unlock/Login | Scrollable form, shared themed fields, accent CTA, secondary links |
| Register | Auth form; non-dismissible recovery dialog із dark container і recovery-word panel |
| Recovery | Auth shell, multi-line field, accent submit CTA |
| Vault list | Transparent scaffold, themed top bar, search field, item cards, accent FAB/action |
| Item detail | Detail card, revealed-secret panel, accent reveal/copy, danger delete |
| Item edit | Themed fields/dropdown/folder chips, accent Save CTA |
| Devices | Device cards, themed revoke dialog, loading/error states |
| Settings | Settings section cards, purple selected/toggle state, destructive delete section |

## 10. Поведінка, яка не змінюється

- Набори та порядок dispatch intent, ефекти stores та умови переходу між екранами.
- Password masking, biometric flow, recovery-key one-time flow, secret reveal/hide/copy semantics.
- Sync, lock/unlock, device revocation, account deletion та biometric settings.
- Поточний string-based routing; жодного navigation rewrite. Нові back routes не додаються. Наявні back buttons/callbacks залишаються робочими; уже наявний Android Item Detail `onBack` можна показати як back affordance, але iOS-екрани без наявного return callback не отримують новий маршрут.
- Лише вже відображені branches/states переоформлюються. Наявні, але зараз не показані store states не починають показуватися; нові loading/empty copy не додаються.
- iOS Item Detail не починає рендерити secret: поточні Reveal/Hide intents і semantics залишаються без зміни.
- Existing local-state lifecycle, task cancellation і cleanup у SwiftUI/Compose.
- Privacy shield, app-switcher protection, Android `FLAG_SECURE`, відсутність secret-даних у темі, preview, logs або fixtures.

## 11. Доступність і платформні деталі

- Усі controls мають semantic label, disabled state та мінімум 44–48 pt/dp touch target.
- Starfield і purely decorative elements приховані від accessibility.
- Selection/error не передаються лише кольором: залишаються checkmark, text та SF Symbols/icons.
- SwiftUI forms використовують semantic Dynamic Type та `ScrollView`; жорсткі 120 pt card height source не копіюється.
- Compose buttons використовують `heightIn(min = ...)`, а не фіксовану висоту, щоб не обрізати текст при великому шрифті.
- Keyboard-safe layouts враховують IME/safe areas; лише background ігнорує safe area.
- iOS `List`/`Form`, якщо залишаться, не повинні закривати starfield: прозорий background і власні rows.
- Android dialogs/menus отримують explicit Dark Zen container colors.

## 12. Межі реалізації

### Дозволені зміни

- `iosApp/Lockruna/**` та Xcode project file.
- `androidApp/src/main/kotlin/com/kwazzar/lockruna/ui/**`.
- Лише потрібні Android theme/manifest/system-bar resource changes.
- Цей design document і, за потреби, коротка візуальна документація.

### Заборонені зміни

- `shared/**`, `server/**`, `api-contract/**`, `architecture-tests/**`.
- Crypto, auth, sync, persistence і contract code.
- Нові third-party dependencies.

## 13. Перевірка та критерії приймання

1. Shared JVM-тести, Konsist і Android debug build проходять.
2. `./gradlew ktlintCheck detekt` проходить без suppressions.
3. iOS target збирається й тестується через XcodeBuildMCP; raw `xcodebuild` не використовується.
4. На iOS та Android перевірено всі перелічені екрани й уже наявні стани: light keyboard, loading, поточний empty list без додавання empty-state copy, error, dialogs, biometric unavailable, delete/recovery flows.
5. Starfield стабільний, системні controls dark, основний текст читабельний, CTA не обрізається при великому шрифті.
6. Privacy shield та `FLAG_SECURE` працюють; у diff немає secrets.
7. `gitleaks detect --source . --no-banner` не знаходить секретів.
8. Після змін виконується `graphify update .`.

## 14. Рішення, залишені виконавцю

Точний breakpoint і внутрішня композиція складних карток можуть бути реалізовані за наявними патернами source, якщо вони не змінюють поведінку, accessibility чи safe-area правила. Ці деталі не потребують додаткового продуктового рішення.

## 15. Референси

### `MeditateAndNote` iOS

- `MeditateAndNote/Views/Theme/MainTheme.swift`
- `MeditateAndNote/Views/Root/CustomTabBar.swift`
- `MeditateAndNote/Views/Root/RootContainer.swift`
- `MeditateAndNote/Views/Components/NoteCard.swift`
- `MeditateAndNote/Views/NoteMenu/SearchBar.swift`
- `MeditateAndNote/Views/Settings/SettingsView.swift`
- `MeditateAndNote/Views/Onboarding/OnboardingView.swift`

### `MeditateAndNote` Android

- `Android/app/src/main/java/com/mn/android/presentation/theme/ui/MnColors.kt`
- `Android/app/src/main/java/com/mn/android/presentation/theme/ui/MnTheme.kt`
- `Android/app/src/main/java/com/mn/android/presentation/theme/ui/MnButtons.kt`
- `Android/app/src/main/java/com/mn/android/presentation/notes/ui/NoteMenuScreen.kt`
- `Android/app/src/main/java/com/mn/android/presentation/settings/ui/SettingsScreen.kt`
