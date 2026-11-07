---
name: ddd-audit
description: Use when reviewing a feature, PR, or diff in Lockruna for DDD and architecture compliance — layer boundaries, anemic aggregates, invariant placement, port/use-case misuse, sealed persistence, sync contract, and secrets handling. Triggers "check this for DDD", "audit this for DDD", "review architecture", "before finishing a significant feature".
---

# DDD audit — Lockruna

Check the code against the project's DDD rules and `AGENTS.md`. Give each item
✅ / ⚠️ / ❌ with a one-line reason and, for ⚠️/❌, a concrete fix. Base findings
on the code and `docs/`; if they disagree, stop and ask.

## 1. Layer boundaries

- [ ] `domain` imports no framework (Ktor, SQLDelight, Koin, serialization,
      UI/platform). ❌
- [ ] Dependencies point inward: data → application → domain; presentation →
      application. ❌ if a lower layer imports upward.
- [ ] Server: api → application → domain ← infrastructure. ❌
- [ ] Rows and DTOs are mapped only in `data`; they do not appear in domain,
      application or presentation. ❌
- [ ] Swift stays in `iosApp` and contains no domain rules. ⚠️/❌

## 2. Aggregate / anemic model

- [ ] Aggregates keep state `private`/`private(set)` and expose behavior; no
      public mutable fields. ⚠️/❌
- [ ] Invariants are enforced inside the aggregate, not in use cases, Stores or
      repositories. ❌
- [ ] Version bump, dirty flag and valid transitions are aggregate methods
      (`item.update(...)`), not `copy(version = version + 1, dirty = true)` at
      call sites. ❌
- [ ] No invariant check duplicated across callers. ⚠️

## 3. Value Objects

- [ ] Values with validity rules (ids, titles, URLs, sealed secret wrappers,
      timestamps) are VOs, not bare `String`/`Long`. ⚠️
- [ ] VOs are immutable and compared by value. ⚠️

## 4. Consistency boundaries

- [ ] One owner enforces multi-entity invariants; no "save two aggregates and
      hope both succeed". ❌
- [ ] Local writes and sync follow one defined order/transaction, not
      best-effort manual sequencing. ⚠️/❌

## 5. Ports, repositories, use cases

- [ ] Ports are `<X>Repository`/`<X>Api` in domain; implementations are
      `SqlDelight<X>Repository`/`Ktor<X>Api`; tests use
      `InMemory<X>Repository`. ⚠️
- [ ] No generic `BaseRepository`, `Any<X>`, or parallel abstraction invented
      next to the existing pattern. ❌
- [ ] Use cases are `<Verb><Noun>` with one `operator fun invoke` and return
      `Result<T, AppError>`. ⚠️
- [ ] Use cases orchestrate and do not reimplement aggregate invariants. ❌ if
      duplicated.

## 6. Presentation

- [ ] `<Screen>Store` calls use cases only — not repositories, Ktor, SQLDelight
      or `CryptoService`. ❌
- [ ] `State` holds `ItemUi`, never aggregates, rows, DTOs or secrets. ❌
- [ ] MVI naming is `<Screen>Store/State/Intent/Effect`; "Store" is not used
      for persistence. ⚠️

## 7. Errors and logging

- [ ] Failures are `Result<T, AppError>`; no exception crosses the Swift
      boundary and no generic `Exception` in domain/application. ❌
- [ ] `AppError` is a sealed hierarchy, one branch per context. ⚠️
- [ ] No master password, authKey, recoveryKey, vaultKey, kek or plaintext item
      data in logs, exceptions, test names or fixtures. ❌ — stop and report as
      a security finding.

## 8. Persistence and sync

- [ ] The local DB holds sealed records only; no plaintext item data and no
      SQLCipher. ❌
- [ ] Schema changes include a migration. ❌ if missing.
- [ ] All vault writes go through `POST /sync/push`; no direct write endpoint.
      ❌
- [ ] `Idempotency-Key`, `baseVersion` checks and rollback detection are intact.
      ❌ if weakened.
- [ ] Server queries are scoped by `user_id`; comparisons are constant-time;
      prelogin, login and recovery responses are indistinguishable. ❌

## 9. DI and crypto

- [ ] Koin is the only DI; no ad-hoc construction or service locator. ⚠️
- [ ] Crypto goes through `CryptoService`/the Argon2 port; ADR-003 parameters,
      HKDF labels and AAD layout are unchanged. ❌ if changed without an ADR
      update and explicit approval.
- [ ] Key material is zeroized on lock (best effort). ⚠️
- [ ] No new dependency unless necessary, pinned exactly, and kept out of crypto
      paths. ⚠️

## 10. Done criteria

- [ ] Tests for touched modules pass and Konsist passes (never suppress it). ❌
      if not run.
- [ ] No secret material in the diff. ❌
- [ ] `docs/` updated when behavior or contract changed. ❌

## Response format

1. ❌ must fix — file/line and a concrete fix.
2. ⚠️ worth considering — non-blocking.
3. Brief "already good" — so it does not read as pure criticism.
4. Do not rewrite non-trivial fixes (public protocol, several call sites)
   without the user's confirmation.
