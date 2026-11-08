# Graph Report - Lockruna  (2026-11-08)

## Corpus Check
- 47 files · ~13,308 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 281 nodes · 249 edges · 43 communities (31 shown, 12 thin omitted)
- Extraction: 100% EXTRACTED · 0% INFERRED · 0% AMBIGUOUS · INFERRED: 1 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `d36a76f7`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- AppError
- Рішення
- PassKeeper: модель загроз (STRIDE)
- opencode.json
- Рішення
- PassKeeper: вимоги до MVP
- command
- Рішення
- DDD audit — Lockruna
- AGENTS.md
- Adding an aggregate — Lockruna
- ContentView.swift
- ItemTypeDto
- KdfParamsDtoTest
- LayerDependencyTest
- DatabaseDriverFactory
- ADR-001: KMP-ядро + нативний UI
- ADR-002: Clean Architecture + DDD-lite, MVI у presentation
- VaultItemType
- MainActivity
- gradlew
- ServerConfigTest
- DatabaseDriverFactory
- DatabaseDriverFactory
- DatabaseDriverFactory
- MasterPasswordTest
- graphify.js
- ServerConfig.kt
- verify.sh
- ItemUi.kt
- SharedCore.kt

## God Nodes (most connected - your core abstractions)
1. `PassKeeper: вимоги до MVP` - 15 edges
2. `DDD audit — Lockruna` - 12 edges
3. `AppError` - 11 edges
4. `Рішення` - 11 edges
5. `Рішення` - 11 edges
6. `PassKeeper: модель загроз (STRIDE)` - 10 edges
7. `Adding an aggregate — Lockruna` - 9 edges
8. `Рішення` - 8 edges
9. `ADR-004: Архітектура бекенду` - 8 edges
10. `6. STRIDE по компонентах` - 7 edges

## Surprising Connections (you probably didn't know these)
- `of()` --references--> `Result`  [EXTRACTED]
  shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/identity/MasterPassword.kt → shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/core/Result.kt
- `of()` --references--> `AppError`  [EXTRACTED]
  shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/identity/MasterPassword.kt → shared/domain/src/commonMain/kotlin/com/kwazzar/lockruna/domain/error/AppError.kt

## Import Cycles
- None detected.

## Communities (43 total, 12 thin omitted)

### Community 0 - "AppError"
Cohesion: 0.09
Nodes (20): E, ValidateMasterPassword, Failure, Result, Success, AppError, InvalidCredentials, ItemCorrupted (+12 more)

### Community 1 - "Рішення"
Cohesion: 0.09
Nodes (21): 10. Тестування, 1. Структура, 2. Стек, 3. Sync, 4. Токени й пристрої, 5. Автентифікація й анти-enumeration, 6. Rate limiting, 7. Аудит (+13 more)

### Community 2 - "PassKeeper: модель загроз (STRIDE)"
Cohesion: 0.12
Nodes (16): 1. Обсяг, 2. Припущення (поза межами моделі), 3. Активи, 4. Межі довіри, 5. Зловмисники, 6.1 `/auth`: реєстрація, вхід, токени, 6.2 `/auth/recovery/*`, 6.3 `/sync` і `/devices` (+8 more)

### Community 3 - "opencode.json"
Cohesion: 0.12
Nodes (16): agent, build, plan, git commit*, git push*, permission, permission, bash (+8 more)

### Community 4 - "Рішення"
Cohesion: 0.12
Nodes (15): 10. Тестування, 1. Модулі Gradle, 2. Domain, 3. Application: use cases, 4. Помилки, 5. Локальне сховище без SQLCipher, 6. Папки, 7. Sync-рушій на клієнті (+7 more)

### Community 5 - "PassKeeper: вимоги до MVP"
Cohesion: 0.12
Nodes (15): PassKeeper: вимоги до MVP, US-01 Реєстрація і майстер-пароль, US-02 Вхід, US-03 Recovery key і відновлення доступу, US-04 Створення запису, US-05 Редагування і видалення, US-06 Перегляд і копіювання, US-07 Біометричне розблокування (+7 more)

### Community 6 - "command"
Cohesion: 0.14
Nodes (16): XCODEBUILDMCP_ENABLED_WORKFLOWS, XCODEBUILDMCP_SENTRY_DISABLED, mcp, mobile-mcp, XcodeBuildMCP, command, enabled, type (+8 more)

### Community 7 - "Рішення"
Cohesion: 0.14
Nodes (13): 1. Примітиви, 2. Ієрархія ключів, 3. Шифрування записів і AAD, 4. Відновлення, 5. Argon2id: гібрид за портом `Argon2`, 6. Біометрія, 7. Пам'ять, ADR-003: Криптографічна модель (+5 more)

### Community 8 - "DDD audit — Lockruna"
Cohesion: 0.15
Nodes (12): 10. Done criteria, 1. Layer boundaries, 2. Aggregate / anemic model, 3. Value Objects, 4. Consistency boundaries, 5. Ports, repositories, use cases, 6. Presentation, 7. Errors and logging (+4 more)

### Community 9 - "AGENTS.md"
Cohesion: 0.20
Nodes (8): Commands, Done means, graphify, Invariants live in the aggregate, Naming, Rules that are easy to break, Skills, Workflow

### Community 10 - "Adding an aggregate — Lockruna"
Cohesion: 0.20
Nodes (9): Adding an aggregate — Lockruna, Common mistakes, Step 0 — Plan before code, Step 1 — Domain (`shared/domain`), Step 2 — Port (`shared/domain`), Step 3 — Data (`shared/data`), Step 4 — Application (`shared/application`), Step 5 — Presentation and DI (+1 more)

### Community 11 - "ContentView.swift"
Cohesion: 0.22
Nodes (7): App, ContentView, LockrunaApp, Scene, Shared, SwiftUI, View

### Community 12 - "ItemTypeDto"
Cohesion: 0.33
Nodes (5): ItemTypeDto, CARD, FOLDER, LOGIN, NOTE

### Community 15 - "DatabaseDriverFactory"
Cohesion: 0.40
Nodes (3): Context, DatabaseDriverFactory, SqlDriver

### Community 16 - "ADR-001: KMP-ядро + нативний UI"
Cohesion: 0.33
Nodes (5): ADR-001: KMP-ядро + нативний UI, Альтернативи, Контекст, Наслідки, Рішення

### Community 17 - "ADR-002: Clean Architecture + DDD-lite, MVI у presentation"
Cohesion: 0.33
Nodes (5): ADR-002: Clean Architecture + DDD-lite, MVI у presentation, Альтернативи, Контекст, Наслідки, Рішення

### Community 18 - "VaultItemType"
Cohesion: 0.33
Nodes (5): VaultItemType, CARD, FOLDER, LOGIN, NOTE

### Community 19 - "MainActivity"
Cohesion: 0.40
Nodes (3): MainActivity, Bundle, ComponentActivity

### Community 20 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

## Knowledge Gaps
- **142 isolated node(s):** `LOGIN`, `CARD`, `NOTE`, `FOLDER`, `Shared` (+137 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **12 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `mcp` connect `command` to `opencode.json`?**
  _High betweenness centrality (0.007) - this node is a cross-community bridge._
- **What connects `LOGIN`, `CARD`, `NOTE` to the rest of the system?**
  _142 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `AppError` be split into smaller, more focused modules?**
  _Cohesion score 0.09 - nodes in this community are weakly interconnected._
- **Should `Рішення` be split into smaller, more focused modules?**
  _Cohesion score 0.09090909090909091 - nodes in this community are weakly interconnected._
- **Should `PassKeeper: модель загроз (STRIDE)` be split into smaller, more focused modules?**
  _Cohesion score 0.11764705882352941 - nodes in this community are weakly interconnected._
- **Should `opencode.json` be split into smaller, more focused modules?**
  _Cohesion score 0.125 - nodes in this community are weakly interconnected._
- **Should `Рішення` be split into smaller, more focused modules?**
  _Cohesion score 0.125 - nodes in this community are weakly interconnected._