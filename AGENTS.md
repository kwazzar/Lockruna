# AGENTS.md

PassKeeper: zero-knowledge password manager (KMP core, native iOS/Android
UI, Ktor server). The server only ever sees ciphertext.

Specs (read before related work): docs/requirements.md, docs/openapi.yaml,
docs/threat-model.md, docs/adr/001..005. If code and docs disagree, stop
and ask; never change either silently.

Modules: KMP core in `shared/` (domain, application, data, presentation), DTOs
shared with the server in `api-contract/`, SwiftUI in `iosApp/`, Compose in
`androidApp/`, Ktor server in `server/`. Details in ADR-001/002/005.

## graphify

Use the installed graphify skill before anything else when the user types
`/graphify`. When `graphify-out/graph.json` exists, prefer it over raw grep:

- `graphify query "<question>"` for codebase questions; `graphify path "<A>"
  "<B>"` for relationships; `graphify explain "<concept>"` for focused concepts.
- Root `graphify-out/` covers the repo (Kotlin `shared/`, `server/`, Swift
  `iosApp/`); `androidApp/graphify-out/` covers Android UI when present — run
  the same commands from inside `androidApp/`.
- If `graphify-out/wiki/index.md` exists, use it for broad navigation; read
  `graphify-out/GRAPH_REPORT.md` only for broad architecture review.
- Dirty graph files after hooks or incremental updates are expected — not a
  reason to skip graphify.
- After changing code, run `graphify update .` to keep the graph current.

## Rules that are easy to break
Architecture
- Dependencies point inward: data -> application -> domain; presentation ->
  application. `domain` has no framework imports. Server: api ->
  application -> domain <- infrastructure.
- Stores and UI call use cases only, never repositories, Ktor or
  CryptoService. State holds ItemUi, never aggregates or secrets.
- Map rows and DTOs only in `data`. Errors are Result<T, AppError>; no
  exceptions across the Swift boundary.
- Invariants live in aggregates, not in use cases or Stores.
- All vault writes go through POST /sync/push. Local DB holds sealed
  records, no SQLCipher. DI is Koin only. No generic Base* abstractions.
- Swift lives only in iosApp and stays thin.

Security
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

## Invariants live in the aggregate
Bad:  repo.save(item.copy(title = t, version = item.version + 1, dirty = true))
Good: repo.save(item.update(title = t, secret = s))
Why: "an edit bumps version and marks the item dirty" is a domain rule;
copying it into Stores or use cases lets it drift.

## Workflow
- Before coding, post a plan of at most 5 points and wait for approval.
  Mandatory for crypto, auth, sync, schema and contract changes.
- Small vertical slices; Conventional Commits.
- Code, comments and commits in English; docs in Ukrainian.
- Do not refactor or reformat code you were not asked to change.
- If a requirement, trade-off or layer is unclear, ask; do not guess.

## Commands
Verified against the scaffold; if one fails, report it; do not invent a
replacement. Run from the repo root.
- JDK: the build needs JDK 25. On macOS use the Android Studio runtime:
  `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.
- Services: `docker compose up -d`; reset: `docker compose down -v`
- Shared tests: `./gradlew :api-contract:jvmTest :shared:domain:jvmTest :shared:application:jvmTest :shared:presentation:jvmTest :shared:data:jvmTest`
  (`allTests` also runs the native targets and therefore needs macOS)
- Architecture tests (Konsist): `./gradlew :architecture-tests:test`
- iOS simulator tests (slow, macOS): `./gradlew :shared:data:iosSimulatorArm64Test`
- Shared framework for iOS: `./gradlew :shared:umbrella:assemble`
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
