# AGENTS.md

PassKeeper: zero-knowledge password manager (KMP core, native iOS/Android
UI, Ktor server). The server only ever sees ciphertext. Specs are in docs/
(requirements, openapi.yaml, threat-model, adr/). If code and docs
disagree, stop and ask; never change either silently.

## graphify

When a graph exists at `graphify-out/`, it has god nodes, community structure,
and cross-file relationships.

When the user types `/graphify`, use the installed graphify skill or
instructions before doing anything else.

Rules:
- For codebase questions, first run `graphify query "<question>"` when
  `graphify-out/graph.json` exists. Use `graphify path "<A>" "<B>"` for
  relationships and `graphify explain "<concept>"` for focused concepts.
  These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md
  or raw grep output.
- **Two graphs:** root `graphify-out/` covers the repo (Kotlin in `shared/`
  and `server/`, Swift in `iosApp/`); `androidApp/graphify-out/` covers the
  Android UI when present. For Android-focused questions run
  query/path/explain from inside `androidApp/` (`cd androidApp` or set the
  tool workdir). After modifying Android code, run `graphify update .` inside
  `androidApp/`.
- Dirty graphify-out/ files are expected after hooks or incremental updates;
  dirty graph files are not a reason to skip graphify. Only skip graphify if
  the task is about stale or incorrect graph output, or the user explicitly
  says not to use it.
- If `graphify-out/wiki/index.md` exists, use it for broad navigation instead
  of raw source browsing.
- Read `graphify-out/GRAPH_REPORT.md` only for broad architecture review or
  when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current
  (AST-only, no API cost).

## Architecture & DDD (Lockruna)

Goal: generated code follows DDD (per Wlaschin/Ozun) and the project's existing
architecture instead of inventing a new one every time.

### Stack

- Kotlin Multiplatform core in `shared/`: `domain`, `application`, `data`,
  `presentation`; `api-contract/` holds the DTOs shared with the server.
- Native UI: Swift/SwiftUI in `iosApp/`, Jetpack Compose in `androidApp/`.
  Swift stays thin and holds no domain rules.
- Persistence: SQLDelight. The local DB holds sealed records only — no
  SQLCipher.
- DI: Koin only.
- Server: Ktor. Layers are `api → application → domain ← infrastructure`;
  config comes from environment variables.
- Crypto: `CryptoService` and the Argon2 port; parameters, HKDF labels and AAD
  layout are fixed by ADR-003.
- Sync: all vault writes go through `POST /sync/push` with `Idempotency-Key`,
  `baseVersion` checks and rollback detection.
- Errors: `Result<T, AppError>` and a sealed `AppError`; no exception crosses
  the Swift boundary.

### Layers and dependency direction

```
Presentation (Stores + native UI)
        ↓
Application (use cases)
        ↓
Domain (aggregates, value objects, ports)
        ↑
Data (SQLDelight and Ktor implementations)
```

Dependencies point inward, toward the domain.

Hard rules:
- `domain` has no framework imports (no Ktor, SQLDelight, Koin,
  serialization, UI or platform types).
- `data` maps rows and DTOs; row and DTO types never leave `data`.
- Data implementations implement domain ports; the domain never imports them.
- Stores and UI call use cases only — never repositories, Ktor or
  `CryptoService`.
- `State` holds `ItemUi`, never aggregates or secrets.
- Invariants live in aggregates, not in use cases or Stores.
- Server: `api → application → domain ← infrastructure`.
- If unsure which layer a piece of code belongs in, ask instead of guessing.

### Project terminology

- **Aggregate** — entity with identity, private state and behavior methods. It
  protects its invariants, bumps `version` and marks itself dirty; callers
  never copy those rules.
- **Value Object** — immutable value with its own validity rules (`<X>Id`,
  validated title/URL, sealed secret wrapper).
- **`<X>Repository` / `<X>Api`** — domain ports. Implementations are
  `SqlDelight<X>Repository` / `Ktor<X>Api` in `data`; the test fake is
  `InMemory<X>Repository`. There is no generic
  `Repository<Item, LocalDS, RemoteDS>`.
- **Use case** — `<Verb><Noun>` with a single `operator fun invoke`, returning
  `Result<T, AppError>`.
- **MVI** — `<Screen>Store` / `<Screen>State` / `<Screen>Intent` /
  `<Screen>Effect` in `shared/presentation`. "Store" is reserved for MVI
  screens, never for persistence.
- **`AppError`** — sealed hierarchy, one branch per context.
- **`CryptoService` + Argon2 port** — the only crypto entry points; ADR-003
  owns parameters, labels and AAD.
- **Koin module** — the only DI wiring; no ad-hoc construction or service
  locator.
- **Sync push** — the only vault write path; see "Stack".

### How to approach a new feature

**Step 1 — Domain first.** Before writing code, briefly describe:
- Which new or changed aggregates or Value Objects appear.
- Which invariants they protect and where those invariants live.
- Which port (`<X>Repository`/`<X>Api`) exists or is needed.
- Whether the change touches crypto, auth, sync, schema or the API contract.

Post this as the plan required by Workflow (max 5 points) and wait for
approval. Approval is mandatory for crypto, auth, sync, schema and contract
changes. For a new domain type, use the `new-aggregate` skill.

**Step 2 — Implement layer by layer**, in this order: Domain (aggregate, VOs,
port) → Data (SQLDelight table/migration and `SqlDelight<X>Repository`, or
`Ktor<X>Api`) → Application (use case) → Presentation (`<Screen>Store`, UI) →
Koin wiring → tests.

### Bad / Good example

Bad (rule copied into the caller):

```kotlin
repo.save(item.copy(title = t, version = item.version + 1, dirty = true))
```

Good (behavior lives in the aggregate):

```kotlin
repo.save(item.update(title = t, secret = s))
```

Why: "an edit bumps version and marks the item dirty" is a domain rule.
Copying it into Stores or use cases lets it drift.

### Forbidden

- Business logic (validation, invariants, state transitions) in Stores, use
  cases or repositories.
- Stores or UI calling repositories, Ktor or `CryptoService` directly.
- `State` holding aggregates, rows, DTOs or secrets instead of `ItemUi`.
- Row or DTO types outside `data`.
- Vault writes outside `POST /sync/push`.
- Plaintext item data in the local DB, or SQLCipher.
- Custom crypto; changing ADR-003 parameters, HKDF labels or AAD without an
  ADR update and explicit approval.
- Weakening minimum KDF params, `Idempotency-Key`, `baseVersion` checks,
  rollback detection or refresh-token reuse detection.
- Generic `Base*` abstractions or parallel repository/manager patterns.
- DI outside Koin; constructing dependencies inside Stores or UI.
- Exceptions crossing the Swift boundary.
- Secrets in logs, exceptions, fixtures or test names.
- Swift outside `iosApp`; domain rules in Swift.

## Security

- Never log, print, serialize or put into exceptions, fixtures or test
  names: master password, authKey, recoveryKey, vaultKey, kek, plaintext
  item data.
- No custom crypto. Use CryptoService and the Argon2 port. Parameters,
  HKDF labels and AAD layout are fixed by ADR-003; changing them needs an
  ADR update and explicit approval.
- No secrets in the repo; config from environment variables.
- Server: constant-time comparison; every query scoped by user_id;
  prelogin, login and recovery responses indistinguishable for known vs
  unknown email.
- Never weaken: minimum KDF params on the client, Idempotency-Key,
  baseVersion checks, rollback detection, refresh-token reuse detection.
- New dependency only if necessary; pin the exact version, mention it in
  the commit, avoid it in crypto paths.
- Zeroize key material on lock (best effort).

## Naming

- Ports in domain: `<X>Repository`, `<X>Api`. Implementations:
  `SqlDelight<X>Repository`, `Ktor<X>Api`; test fake `InMemory<X>Repository`.
- Use cases: `<Verb><Noun>` with a single `operator fun invoke`.
- MVI: `<Screen>Store`, `<Screen>State`, `<Screen>Intent`, `<Screen>Effect`.
  "Store" is reserved for MVI screens, never for persistence.
- Errors: sealed hierarchy under `AppError`, one branch per context.

## Workflow

- Before coding, post a plan of at most 5 points and wait for approval.
  Mandatory for crypto, auth, sync, schema and contract changes.
- Small vertical slices; Conventional Commits.
- Code, comments and commits in English; docs in Ukrainian.
- Do not refactor or reformat code you were not asked to change.
- If a requirement or trade-off is unclear, ask; do not guess.

## Commands

UNVERIFIED until the skeleton exists; replace with exact working commands
after the first successful run. If one fails, report it; do not invent a
replacement. Run from the repo root.
- Services: `docker compose up -d`; reset: `docker compose down -v`
- Shared tests: `./gradlew :shared:domain:allTests :shared:application:allTests :shared:data:allTests`
- iOS simulator tests (slow): `./gradlew :shared:data:iosSimulatorArm64Test`
- Server tests (needs Docker): `./gradlew :server:test`
- Android build: `./gradlew :androidApp:assembleDebug`
- iOS build and tests: XcodeBuildMCP only, never raw xcodebuild
- Lint / format: `./gradlew ktlintCheck detekt` / `./gradlew ktlintFormat`
- Secrets scan: `gitleaks detect --source . --no-banner`
- Full verify: `scripts/verify.sh` (`--ios` adds simulator tests)

## Done means

- You ran the command and read its output; never say "done" or "passing"
  without it. A command that failed for environmental reasons (Docker
  down, no simulator) is reported as such, not as success.
- Tests for touched modules pass, Konsist passes (never suppress it), no
  secret material in the diff, docs updated if behavior or contract changed.
- Crypto, auth or sync changes: known-answer vectors pass on both
  platforms and the security-review subagent has run on the diff.
- Significant features: run the `ddd-audit` skill before calling the task
  complete.

## Skills

Load on demand with the skill tool.
- new-aggregate: adding or re-persisting an aggregate.
- add-endpoint: any endpoint or DTO change.
- ddd-audit: before finishing a significant feature.
- security-review: before committing crypto, auth, sync, logging,
  persistence or dependency changes.
- verification-before-completion (global): at the end of every task.
