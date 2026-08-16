# Task 14 — Config facade (`config/`)

## Objective

Implement the **assembled configuration + entry point**: `DeclarativeHttpConfig` (a builder holding
mapper, transport, evaluator, tokenStore, credentialService/store, cipher/keyProvider, engine) and
`DeclarativeHttp` (the top-level facade for `engine + validate`). Consumers wire everything once here.

## Hand-off context

- **Design doc:** §2 (`config/` tree), §6.2 "Wiring" (config builder + credential wiring), §3.3
  (engine entry points), §5 R10.
- **Already done (prior tasks):**
  - 03 `json/`: `JsonMapper` + `JacksonJsonMapper`.
  - 02 `transport/`: `HttpTransport` (SPI; no real impl — facade defaults may use a stub or accept an
    injected transport).
  - 04 `expr/`: `ExpressionEvaluator` + `JexlExpressionEvaluator`.
  - 08 `auth/credential/`: `CredentialService`, `CredentialRepository`, `InMemoryCredentialRepository`,
    `CredentialCipher`, `AesGcmCredentialCipher`, `KeyProvider`, `CredentialStore`.
  - 10 `auth/oauth2/`: `TokenStore` + `InMemoryTokenStore`.
  - 13 `engine/`: `DeclarativeHttpEngine`.
- **Package:** `io.github.khezyapp.dhttp.config`.
- **Conventions:** builder pattern; `final` classes. Read `.opencode/skills/khezy-coding-style/SKILL.md`.

## Files to create (`src/main/java/io/github/khezyapp/dhttp/config/`)

1. `DeclarativeHttpConfig.java` — immutable assembled config:
   - Fields: `JsonMapper jsonMapper`, `HttpTransport transport`, `ExpressionEvaluator evaluator`,
     `TokenStore tokenStore`, `CredentialStore credentialStore`, `CredentialService credentialService`,
     `DeclarativeHttpEngine engine`.
   - Fluent inner `Builder`: `.jsonMapper(...)`, `.transport(...)`, `.evaluator(...)`,
     `.tokenStore(...)`, `.credentialService(service)` (also wires `.credentialStore(service.asStore())`),
     `.credentialStore(...)`, `.keyProvider(...)` (builds `AesGcmCredentialCipher`), terminal `build()`.
   - `build()` validates required pieces and constructs defaults where sensible (e.g. `JacksonJsonMapper`,
     `JexlExpressionEvaluator`, `InMemoryTokenStore`), then builds the `DeclarativeHttpEngine`.
   - Accessors for all fields.
2. `DeclarativeHttp.java` — `public final class` facade / builder entry point (per §2 comment
   "engine + validate"):
   - `static DeclarativeHttp create(DeclarativeHttpConfig config)` or a `builder()` returning the config
     builder.
   - Delegates: `void validate(HttpRequestSpec spec)` and
     `List<OutputRecord> execute(HttpRequestSpec spec, RequestContext ctx)` to the underlying engine.
   - Exposes `engine()`, `credentialService()` for consumers to manage credentials.

## Design notes

- **Wiring (§6.2):** `credentialService(service)` sets both the service and the engine-facing
  `credentialStore` via `service.asStore()` — the engine never touches CRUD.
- **Key safety (§7.6):** if a `KeyProvider` is provided, build `AesGcmCredentialCipher` from its key;
  the core never generates or defaults a master key. `build()` should require an explicit `keyProvider`
  before constructing a cipher-backed service (fail fast with a clear message).
- **Transport default:** there is no real JDK transport in scope (per Non-Goals); `build()` requires
  an injected `HttpTransport` (test fake) rather than defaulting to real I/O.
- Keep `DeclarativeHttpConfig` immutable; all collections defensively copied.

## Acceptance criteria

- Compiles + Checkstyle green.
- `DeclarativeHttpConfigTest`: build a full config with a fake transport, in-memory credential repo +
  service + a `KeyProvider`, and a `JexlExpressionEvaluator`; assert accessors return the injected pieces
  and `engine()` is non-null.
- `DeclarativeHttpTest`:
  - `validate(spec)` delegates to the engine (OAuth2-credential spec without token → throws
    `OAuth2NotConfiguredException`).
  - `execute(spec, ctx)` returns output records via the fake transport.
  - `credentialService()` lets a consumer `create("basic-auth", ...)` and resolve it back.
  - `build()` without a `keyProvider` fails with a clear message (key is consumer-owned).
- No unused imports; `final` everywhere; method length < 150.

## Hand-off to next task

Task 15 (acceptance) constructs everything through `DeclarativeHttp`/`DeclarativeHttpConfig` and
proves the §9 snapshot end to end. Keep the builder method names and `DeclarativeHttp` delegation
stable.
