# Declarative HTTP — v1 Implementation Action Plan

Actionable, dependency-ordered tasks for implementing the Java core library described in
`../design/declarative_http_core_design.md` (the **design doc**). Each task is self-contained:
build a vertical slice, verify it compiles + tests + passes Checkstyle, then move on.

## Module & conventions (read first)

- **Module:** `http/declarative-http` (composite-build member, already scaffolded, empty `src/`).
- **Package root:** `io.github.khezyapp.dhttp` (mirrors repo convention `io.github.khezyapp.*`).
- **Build:** `khezy.java-library` + `khezy.java-lombok` already applied in `build.gradle`.
  - **Do NOT edit** `http/declarative-http/build.gradle` deps unless a task explicitly says so.
- **Versions already declared:** `dynamic-object:1.0.1`, `commons-jexl3:3.7.0`,
  `handlebars:4.5.3`, `tools.jackson.core:jackson-databind:3.2.1`.
- **Commands:**
  ```sh
  ./gradlew :declarative-http:build        # compile + test + checkstyle + assemble
  ./gradlew :declarative-http:test         # tests only
  ./gradlew :declarative-http:checkstyleMain
  ```
- **Coding style:** see `.opencode/skills/khezy-coding-style/SKILL.md`.
- **Checkstyle gotchas:** see `.opencode/skills/khezy-checkstyle-gotchas/SKILL.md`.
  Recurring violations: `final` params, `final var` locals, no `{}` empty blocks (use `{ }`),
  opening brace `{` is last char on its line, 150-line method cap, no unused imports.

## Dependency graph

Tasks are strictly ordered. A task may only depend on the artifacts of earlier tasks.

```
01 spec  ─┐
02 transp─┼─► 07 plan ─► 11 actions ─► 13 engine ─► 14 config ─► 15 acceptance
03 json  ─┘     │          ▲
04 expr  ──────►│          │
05 error ──────►│          │
06 security ───►│          │
08 cred  ──────►10 auth ───┴─────────────► 13 engine
09 auth ────────┘
12 pagination ───────────────────────────► 13 engine
```

Legend: `─►` = dependency edge (downstream needs upstream's public types).
OAuth2 (10) builds on credential (08) + generic auth (09) + transport (02) + json (03) + error (05).

## Task list

| # | Task file | Builds | Depends on |
|---|-----------|--------|-----------|
| 00 | `00-INDEX.md` | plan (this) | — |
| 01 | `01-spec-model.md` | `spec/` records + supporting types | — |
| 02 | `02-transport.md` | `transport/` value object + `HttpTransport` SPI | — |
| 03 | `03-json.md` | `json/` `JsonMapper` SPI + Jackson 3 default | — |
| 04 | `04-expression.md` | `expr/` evaluator SPI + JEXL + `doa.*` namespace | — |
| 05 | `05-error.md` | `error/` exceptions + factory | — |
| 06 | `06-security.md` | `security/` SSRF / redirect / allow-list / redactor | — |
| 07 | `07-plan-context.md` | `plan/` context, planner, fragment merge, conditions | 01, 02, 04, 05 |
| 08 | `08-credential.md` | `auth/credential/` CRUD, cipher, repo, store | 03, 05 |
| 09 | `09-auth-generic.md` | `auth/` `Authenticator` + `GenericAuthenticator` | 01, 02, 08, 05 |
| 10 | `10-oauth2.md` | `auth/oauth2/` two-phase config + token lifecycle | 02, 03, 05, 08, 09 |
| 11 | `11-actions.md` | `action/` pre/post processors + registry + builtins | 01, 02, 04, 07 |
| 12 | `12-pagination.md` | `pagination/` strategy + offset/cursor builtins | 02, 04, 07 |
| 13 | `13-engine.md` | `engine/` `Pipeline` + `DeclarativeHttpEngine` + `describe` | 07, 09, 10, 11, 12 |
| 14 | `14-config.md` | `config/` `DeclarativeHttpConfig` + `DeclarativeHttp` facade | 13 (everything) |
| 15 | `15-acceptance.md` | `test/` acceptance snapshot (§9 of design) | 14 (everything) |

## Sequencing notes

- Tasks 01–06 are **leaf tasks** with no internal dependency — do them in any order, in parallel if
  using sub-agents. Each is a self-contained compile-able slice.
- 07 is the first consumer and ties 01/02/04/05 together; it is the natural "integration checkpoint".
- 08 (credential CRUD) and 09 (generic auth) form one coherent vertical slice; 10 (OAuth2) depends
  on both plus the transport/json SPI.
- 13 (engine) is the orchestrator; 14 wires everything into a builder facade; 15 proves §9.
- After any task, keep the graph fresh: run `graphify update .` at the repo root.

## Cross-cutting acceptance guardrails

Each task's "Done" bar is: **compiles clean, JUnit 5 tests pass, Checkstyle green, no unused imports,
`final` everywhere, records/sealed types for the data model.** The final milestone (15) re-verifies
the full §9 snapshot end to end.
