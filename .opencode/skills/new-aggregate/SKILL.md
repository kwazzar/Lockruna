---
name: new-aggregate
description: Use when adding or re-persisting a domain aggregate in Lockruna — a new entity, value object, SQLDelight table, repository port, test fake, or sync payload. Triggers "add a new entity", "new aggregate", "persist X", "add a table", "store X in the vault". Not for endpoints or DTO changes (use add-endpoint).
---

# Adding an aggregate — Lockruna

Recipe for a new persisted aggregate in this project's DDD/KMP layout. Ground
every decision in `docs/` (requirements, openapi.yaml, threat-model, `adr/`) and
`AGENTS.md`; if code and docs disagree, stop and ask instead of guessing.

## Step 0 — Plan before code

Answer briefly:

1. Aggregate root, Value Object, or a field/child of an existing aggregate?
   Extending an existing aggregate beats adding a new one.
2. Which invariants does it protect — what must never become possible?
3. Is it synced? Synced changes travel only through `POST /sync/push`.
4. Does it touch plaintext secrets, keys, auth or the sync schema? If yes,
   ADR-003 and the `security-review` skill are mandatory.
5. What is the smallest vertical slice: domain → port → data → use case → store?

Post the plan (max 5 points) and wait for approval. Required for schema, sync,
auth, crypto and contract changes.

## Step 1 — Domain (`shared/domain`)

- No framework imports: no Ktor, SQLDelight, Koin, serialization, UI or platform
  types.
- Keep state `private`/`private(set)` and expose behavior, never public mutable
  fields.
- Invariants live in the aggregate. Version bump, dirty flag, valid transitions
  and "an edit implies a change" are aggregate rules, not caller rules.
- Validate in Value Objects (`<X>Id`, sealed secret wrapper, validated title/URL).
- No persistence rows, DTOs, logging or crypto implementation here. Crypto is a
  port (`CryptoService`, Argon2); its parameters are fixed by ADR-003.

```kotlin
class <Aggregate> private constructor(
    val id: <Aggregate>Id,
    val version: Long,
    private val <state>: <ValueObject>,
) {
    // Validates the invariant and returns the next version — callers never bump it.
    fun <change>(...): <Aggregate> = ...
}
```

Use the project's real types and naming; the sketch shows placement, not the
final API.

## Step 2 — Port (`shared/domain`)

Define `<X>Repository` (or `<X>Api` for a remote port) in domain:

```kotlin
interface <X>Repository {
    suspend fun get(id: <X>Id): Result<<X>, AppError>
    suspend fun save(item: <X>): Result<Unit, AppError>
}
```

- Every fallible call returns `Result<T, AppError>`; no exception crosses a
  boundary.
- Implementations: `SqlDelight<X>Repository` / `Ktor<X>Api` in `data`; the test
  fake is `InMemory<X>Repository`.
- No generic `BaseRepository` or `Any<X>` abstraction.

## Step 3 — Data (`shared/data`)

- Add the SQLDelight table/queries and a migration. The local DB stores sealed
  records only: no plaintext item data, no SQLCipher.
- Implement `SqlDelight<X>Repository`; map rows ↔ aggregate only here. Row and
  DTO types never leave `data`.
- Add `InMemory<X>Repository` in test sources and cover the same contract as the
  real implementation.
- If the aggregate syncs, map the sync payload/DTO only in `data` and keep the
  wire contract in `docs/openapi.yaml`.

## Step 4 — Application (`shared/application`)

- Add use cases named `<Verb><Noun>` with a single `operator fun invoke`; they
  orchestrate ports and return `Result<T, AppError>`.
- Do not duplicate invariants — call aggregate methods.
- Writes that reach the server go through `POST /sync/push` with
  `Idempotency-Key`, `baseVersion` checks and rollback detection. Never add a
  direct vault-write endpoint.

## Step 5 — Presentation and DI

- `<Screen>Store` depends on use cases only — never repositories, Ktor,
  SQLDelight or `CryptoService`. `State` holds `ItemUi`, never aggregates or
  secrets. Use `<Screen>Intent` / `<Screen>Effect`.
- Wire the port in the existing Koin module. No ad-hoc construction, no service
  locator, no `Base*` abstractions.

## Step 6 — Tests and done

- Domain: invariant and transition tests.
- Data: repository contract against `InMemory<X>Repository`, row mapping,
  migration.
- Sync: idempotency, `baseVersion`, rollback — only if the aggregate syncs.
- Run the commands in `AGENTS.md`. Its `Commands` section is UNVERIFIED until
  the skeleton exists — report that instead of inventing a replacement. Konsist
  must pass.
- No secrets in fixtures, test names or logs.
- Finish with the `ddd-audit` skill; run `security-review` for crypto, auth,
  sync or persistence changes.

## Common mistakes

| Mistake | Correct |
|---|---|
| `repo.save(item.copy(version = item.version + 1, dirty = true))` | `repo.save(item.update(...))` — the rule lives in the aggregate |
| Invariant checked in a use case or Store | Enforce it in the aggregate |
| Store calls `SqlDelight<X>Repository` or Ktor | Store calls a use case |
| `State` holds the aggregate or plaintext | `State` holds `ItemUi` |
| New generic `BaseRepository` | Per-aggregate port + implementation |
| Row/DTO used in domain or presentation | Map only in `data` |
| Plaintext at rest or SQLCipher | Sealed records in the local DB |
