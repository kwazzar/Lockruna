# AGENTS.md

PassKeeper: zero-knowledge password manager (KMP core, native iOS/Android
UI, Ktor server). The server only ever sees ciphertext. Specs are in docs/
(requirements, openapi.yaml, threat-model, adr/). If code and docs
disagree, stop and ask; never change either silently.

## graphify

When a graph exists at graphify-out/, it has god nodes, community structure,
and cross-file relationships.

When the user types `/graphify`, use the installed graphify skill or
instructions before doing anything else.

Rules:
- For codebase questions, first run `graphify query "<question>"` when
  graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for
  relationships and `graphify explain "<concept>"` for focused concepts. These
  return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw
  grep output.
- **Two graphs:** root `graphify-out/` = iOS/Swift side (`iosApp/`);
  `androidApp/graphify-out/` = Android/Kotlin side (Compose UI, stores, tests,
  Gradle). For Android-focused questions run query/path/explain from inside
  `androidApp/` (`cd androidApp` or set the tool workdir) with the same
  commands. After modifying Android code, run `graphify update .` inside
  `androidApp/`.
- Dirty graphify-out/ files are expected after hooks or incremental updates;
  dirty graph files are not a reason to skip graphify. Only skip graphify if the
  task is about stale or incorrect graph output, or the user explicitly says not
  to use it.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of
  raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when
  query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current
  (AST-only, no API cost).

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

## Skills
Load on demand with the skill tool.
- new-aggregate: adding or re-persisting an aggregate.
- add-endpoint: any endpoint or DTO change.
- ddd-audit: before finishing a significant feature.
- security-review: before committing crypto, auth, sync, logging,
  persistence or dependency changes.
- verification-before-completion (global): at the end of every task.
