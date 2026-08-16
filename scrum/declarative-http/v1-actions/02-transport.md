# Task 02 — Transport value object + SPI (`transport/`)

## Objective

Implement the **transport-neutral request value object** (`HttpRequest`) and the **`HttpTransport`
SPI**, plus the `HttpResult` response carrier. This is the "wire" contract every engine call
round-trips through. Nothing here performs real I/O — the JDK/OkHttp adapters are out of scope
(only the interface + value object are required for the core to compile/test).

## Hand-off context

- **Design doc:** §2 (`transport/` tree), §3.4 (full `HttpRequest` spec), §6.4 (`HttpTransport`),
  §4 (pipeline step "Transport.send"), §5 R11, R14.
- **Already done (prior tasks):** none — leaf task. Downstream (07 plan, 09/10 auth, 12 pagination,
  13 engine) all build `HttpRequest` and receive `HttpResult`.
- **Package:** `io.github.khezyapp.dhttp.transport`.
- **Conventions:** fluent `HttpRequestBuilder`; immutable `HttpRequest`; `@FunctionalInterface`
  transport. Read `.opencode/skills/khezy-coding-style/SKILL.md`.

## Files to create (`src/main/java/io/github/khezyapp/dhttp/transport/`)

1. `HttpRequest.java` — immutable value object mirroring `IHttpRequestOptions` (§3.4). Public fields
   via accessors: `url`/`baseUrl`, `method`, `headers` (case-insensitive multi-value map), `query`
   (`Map<String,Object>` + `ArrayFormat`), `body` (`Body` of kind JSON|FORM|URLENCODED|RAW|BINARY|NONE),
   `auth` (basic/digest/bearer + `sendImmediately` flag), `proxy`, `timeout`, `skipSsl`,
   `maxRedirects`, `disableFollowRedirect`, `encoding`, `jsonAccept`, `ignoreStatusErrors`,
   `abortSignal`, `allowedDomains`, `returnFullResponse`. Provide `toBuilder()`.
2. `HttpRequestBuilder.java` — fluent builder: `url(String)`, `method(HttpMethod)`, `header(String,String)`,
   `headers(Map)`, `query(String,Object)`, `body(Body)`, `auth(Auth)`, `timeout(long)`,
   `returnFullResponse(boolean)`, terminal `build()`. Validates required `url`/`method`.
3. `HttpMethod.java` — **reuse** the one from Task 01 (`spec.HttpMethod`); import it here rather than
   duplicating. If a compile issue arises, move it to a shared location and re-export.
4. `Body.java` — sealed interface `permits JsonBody, FormBody, RawBody, BinaryBody, NoBody` (+ kind
   enum `NONE`). Each variant carries its payload (e.g. `RawBody(String contentType, byte[] bytes)`).
5. `ArrayFormat.java` — enum `INDICES, BRACKETS, REPEAT, COMMA` (query array serialization).
6. `Auth.java` — sealed interface: `record BasicAuth(String username, String password)`,
   `record BearerAuth(String token)`, `record NoAuth()` (+ `sendImmediately` flag). No secrets logged.
7. `HttpResult.java` — `record HttpResult(int status, Map<String,List<String>> headers, byte[] body, String bodyText)` with helpers `ok()`, `bodyString()`. (Used by §4 "Return full response? yes → HttpResult".)
8. `HttpTransport.java` — `@FunctionalInterface public interface HttpTransport { HttpResult send(HttpRequest request) throws HttpApiException; }` (import `HttpApiException` from Task 05 — if 05 is not built yet, add a temporary `throws Exception` and relax in Task 05; prefer building 05 first per index).

## Design notes

- **Multi-value case-insensitive headers:** implement with a small `Headers` helper or
  `TreeMap<String, List<String>>` with `CASE_INSENSITIVE_ORDER`. Keep it in this package.
- **No real network.** Do not implement `JdkHttpTransport`/`OkHttpTransport` now; tests use an
  in-memory fake transport. (Adapting to the real JDK client is deferred per design Non-Goals.)
- **`HttpResult`** deliberately supports full-response mode (`returnFullResponse`) vs body-only —
  expose both `body()` bytes and `bodyText()`.
- **Immutable:** all collections defensively copied in the builder `build()`.

## Acceptance criteria

- Compiles under `./gradlew :declarative-http:build`; Checkstyle green; `final` everywhere.
- Unit test `HttpRequestTest`: build a GET with query array `format=BRACKETS`, one header, `NoBody`;
  assert `toBuilder()` round-trip; assert headers are case-insensitive when read back.
- `HttpResultTest`: construct `200` with JSON body, assert `ok()`, `bodyText()`, headers.
- A `FakeTransport implements HttpTransport` records the last `HttpRequest` and returns a canned
  `HttpResult` — this is the test double all later engine tests reuse.
- No unused imports; empty blocks `{ }`.

## Hand-off to next task

Downstream builds: `HttpRequest request = HttpRequest.builder().url(..).method(..).build();` and
`HttpResult result = transport.send(request)`. Expose `FakeTransport` in test scope for reuse
(start a shared test utility, e.g. `src/test/java/.../testutil/FakeTransport.java`).
