# Task 15 — Acceptance milestone (§9 of the design doc)

## Objective

Write the **acceptance snapshot** as end-to-end tests that prove the entire core library works
together (§9 items 1–8) using ONLY the public core API assembled through `DeclarativeHttp` /
`DeclarativeHttpConfig`. This is the definition-of-done for the v1 milestone.

## Hand-off context

- **Design doc:** §9 (8 acceptance items). Full context in the design doc's earlier sections.
- **Already done (prior tasks):** everything — spec model, transport SPI, json, expr, error, security,
  plan/context, credential CRUD, generic auth, OAuth2, actions, pagination, engine, config facade.
- **Location:** `src/test/java/io/github/khezyapp/dhttp/acceptance/`.
- **Test double:** reuse `FakeTransport` (Task 02 test scope) — records every `HttpRequest`, returns
  scripted `HttpResult`s. Use in-memory repos/stores (Task 08/10) and a test `KeyProvider`.
- **Conventions:** `@Nested` grouping; `@DisplayName`; Khmer/Cambodia sample data. Read
  `.opencode/skills/khezy-coding-style/SKILL.md` and `.opencode/skills/khezy-ast-evaluator-testing/SKILL.md`
  (its anonymous-stub / dry-run patterns apply to the fake transport).

## Files to create (all in `src/test/java/io/github/khezyapp/dhttp/acceptance/`)

1. `BrevoSpecAcceptanceTest.java` — **items 1, 2, 4:**
   - Build a `Brevo`-style spec: `baseUrl` + 2 operations (`contact.create`, `contact.list`) + `Send`
     body mapping + `RootProperty("data.items")` postReceive, using only core API.
   - Run against `FakeTransport`; assert the **exact resolved `HttpRequest`** (method, path, headers,
     body) and the **exact output records**.
   - Prove **JEXL** resolution from `$parameter` and `$credentials` per item, and **`DynamicObjects`**
     dot-path resolution in `Send`/`RootProperty`.
2. `PaginationAcceptanceTest.java` — **item 3:** offset pagination with `pageSize` + `maxResults`
   capping; assert request count and total records.
3. `DescribeAcceptanceTest.java` — **item 5:** `describe(...)` returns shaped `OptionItem`s for a dropdown.
4. `OAuth2ConfigTimeAcceptanceTest.java` — **item 6:**
   - (a) `OAuth2AuthorizationFlow.authorizationUrl()` produces a provider URL carrying `client_id`,
     `redirect_uri`, `scope`, `state`.
   - (b) a consumer-style callback handler calls `exchangeCode(code)` → `persist(token)`.
   - (c) `engine.validate(spec)` on a Phase B spec **passes** with a valid stored token and **throws
     `OAuth2NotConfiguredException`** when none — with **no HTTP request sent** (FakeTransport zero sends).
5. `OAuth2RequestTimeAcceptanceTest.java` — **item 7:**
   - (a) warm token reused with **no** token-endpoint call;
   - (b) expired token triggers a `refresh_token` grant;
   - (c) 401 on a warm token triggers a single refresh + retry;
   - (d) concurrent requests share **one** refresh (single-flight lock).
6. `CredentialLifecycleAcceptanceTest.java` — **item 8:**
   - (a) `create("oauth2", config)` stores **no plaintext** in `StoredCredential.data`;
   - (b) `get(id)` round-trips decrypt → Map → type-safe `OAuth2Credentials`;
   - (c) Map-form and type-safe overloads produce **identical** stored payloads;
   - (d) `update`/`delete` mutate the repository;
   - (e) `list()` exposes only id + type;
   - (f) a consumer-supplied `KeyProvider` key drives the cipher (key never inside core);
   - (g) `CredentialStore.resolve` (engine) delegates to the service while the engine never touches CRUD.

## Design notes

- **Single entry:** construct everything via `DeclarativeHttpConfig`/`DeclarativeHttp` in a shared
  `@BeforeEach` fixture so tests exercise the real wiring, not hand-assembled internals.
- **Determinism (R16):** same spec + fake transport → identical plans and outputs; assert exact values,
  not fuzzy.
- **No HTTP:** all requests flow through `FakeTransport`; OAuth2 token-endpoint calls also go through
  the fake (assert call counts to prove cache/refresh behavior).
- **Redaction (§7.4):** assert no `access_token`/`refresh_token`/Authorization value appears in any
  exception message or logged request string.

## Acceptance criteria

- All six acceptance test classes pass; overall `./gradlew :declarative-http:build` is green
  (compile + tests + Checkstyle).
- Each §9 item has at least one `@Test` that maps 1:1 to the numbered requirement.
- No unused imports; `final` everywhere; method length < 150.

## Completion

This is the final task. When green:
1. Run the full module build and confirm `./gradlew :declarative-http:build` passes.
2. Update the scrum README (`scrum/declarative-http/README.md`) to note the implementation is done
   and point to the acceptance tests.
3. Run `graphify update .` at the repo root to refresh the knowledge graph.
