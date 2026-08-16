# Task 01 — Spec model (`spec/`)

## Objective

Implement the **immutable declarative model** ("the description DSL") — the records a consumer
writes to describe how to talk to a REST API. Pure data carriers; no behavior beyond defensive
copying / validation. Nothing in this task touches the network, expressions, or auth.

## Hand-off context

- **Design doc:** §2 (`spec/` tree), §3.1 (full record sketches), §5 R1, R3, R4, R7, R8.
- **Already done (prior tasks):** none — this is a leaf task. It defines types that downstream
  tasks (07 plan, 09 auth, 11 actions) consume, so keep signatures aligned with §3.1 exactly.
- **Package:** `io.github.khezyapp.dhttp.spec`.
- **Conventions:** records with compact constructors for defensive copies; sealed interface for
  `PostReceive`; enums for `Target`. Read `.opencode/skills/khezy-coding-style/SKILL.md`.

## Files to create (`src/main/java/io/github/khezyapp/dhttp/spec/`)

All are `public record` unless noted. Follow §3.1 exactly for field names and types.

1. `Condition.java` — R3 precondition: `record Condition(String property, Object equals, String exists)` or a small sealed/flag form. Must be usable to gate an `Operation` (`when`).
2. `HttpRequestSpec.java` — root spec (§3.1):
   `record HttpRequestSpec(String baseUrl, Map<String,String> defaultHeaders, long defaultTimeoutMillis, boolean defaultSkipSsl, List<Operation> operations, CredentialRef defaultCredential, PaginationSpec defaultPagination, SecurityPolicy security)`.
3. `Operation.java` — `record Operation(String id, List<Condition> when, Route route)`.
4. `Route.java` — `record Route(RequestShape request, List<Send> sends, Output output, PaginationSpec pagination, List<PreSend> preSends)`.
5. `Send.java` — R4 parameter→request mapping:
   `record Send(String fromParam, Target target, String property, boolean dotNotation, Expression valueOverride)`.
6. `Output.java` — `record Output(int maxResults, List<PostReceive> postReceive)`.
7. `PostReceive.java` — **sealed interface** with record variants (§3.1):
   permits `RootProperty(String property)`, `FilterItems(Expression pass)`, `LimitItems(int max)`,
   `SetValue(Expression value)`, `SortByKey(String key, boolean desc)`,
   `SetKeyValue(Map<String,Expression> fields)`, `BinaryData(String destinationProperty)`,
   `CustomPostReceive(String actionKey, Map<String,Object> props)`.
   The variants are nested `record`s inside the sealed interface.
8. `PreSend.java` — R6 hook descriptor: `record PreSend(String actionKey, Map<String,Object> props)`.
9. `PaginationSpec.java` — R9: `record PaginationSpec(String mode, int pageSize, String rootProperty, String limitParam, String offsetParam, boolean inQuery, String continueExpression)`.
10. `SecurityPolicy.java` — R12 config: `record SecurityPolicy(List<String> allowedDomains, boolean allowIpLiteral, boolean stripCrossOriginCredentials, List<String> sensitiveOutputFields)`.

**Supporting types (same package or a shared sub-package) — NOT in the §2 tree but referenced by
§3.1; create them here so downstream compiles:**

- `CredentialRef.java` — `record CredentialRef(String type, String id)` with static factory
  `CredentialRef.of(String type, String id)`.
- `RequestShape.java` — `record RequestShape(HttpMethod method, String path, Map<String,String> headers, Map<String,Object> query, String json, String encoding)`.
- `HttpMethod.java` — enum `GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS`.
- `Expression.java` — wrapper for a value that may be a plain value or an expression string:
  `record Expression(String raw)` with `boolean isExpression()` (true when `raw` starts with `=` or is a `{{...}}` template) and `String literal()`.
- `Target.java` — enum `BODY, QUERY`.

## Design notes

- **Dot-notation contract:** every `*property*` string (in `Send.property`, `RootProperty.property`,
  `SetKeyValue` keys, `PaginationSpec.rootProperty`) is resolved later through `dynamic-object`
  `DynamicObjects` — this task only carries the string, it never parses paths.
- **Defaulted fields:** use overloaded static factories / compact constructors so tests can build
  minimal specs (e.g. `Output.of(int maxResults)` with empty postReceive). Keep records immutable;
  defensively copy all `Map`/`List` fields in compact constructors.
- **No JSR-305 / null annotations** — use `Objects.requireNonNull` for required params.

## Acceptance criteria

- All types compile under `./gradlew :declarative-http:build` with Checkstyle green.
- A unit test `HttpRequestSpecTest` builds a full Brevo-style spec:
  `baseUrl`, 2 operations, one `Send(..., BODY, "attributes", true, null)`, an
  `Output` with a `RootProperty("data.items")` postReceive — using only these public types.
- `PostReceive` is sealed: a switch on it covers all 8 variants (add a helper `Pattern`-switch test
  to prove exhaustiveness).
- Maps/lists returned from accessors are unmodifiable (defensive copy verified).
- No unused imports; `final` params/locals; method length < 150.

## Hand-off to next task

Leave a note in the PR/repo: 07 (plan), 09 (auth), 11 (actions) import `HttpRequestSpec`,
`Route`, `Operation`, `Send`, `PostReceive`, `CredentialRef`, `SecurityPolicy`, `Expression`,
`Target`. Keep these type names and component orders stable.
