# Declarative HTTP

**Version 1.0.0** — `io.github.khezyapp:declarative-http` — a framework-agnostic declarative HTTP engine for Java: declare "how to talk to a REST API" as plain data, and the core executes it — auth, pagination, response shaping, and security included.

---

## Overview

Most HTTP integrations are written the same way, over and over: build a URL, add headers, serialize a body, handle the response, page through results, refresh the token. `declarative-http` moves that knowledge out of your Java code and into a **spec** — an immutable description of *what* an API interaction looks like. The **engine** decides *how* to execute it.

The design is a Java port of n8n's routing-node architecture. You write **records** (the spec), not request glue. This library is a **core**: it does not assume Spring, Quarkus, or any HTTP client — it ships a default transport and lets you plug in your own.

---

## When to use it

`declarative-http` shines for **dynamic HTTP integrations driven by dynamic configuration** — for example, an integration framework built on top of an existing HTTP client (Spring RestClient, OkHttp, Apache HttpClient) where new APIs are added at runtime, not compile time. It is also the best fit for *any* task that needs dynamic HTTP integration with dynamic configuration.

Typical problem statements:

- **"I need to build an integration framework where new APIs can be added without writing Java."** Load specs from JSON/YAML at runtime and execute them through one engine.
- **"I need to let users configure how to call a REST API — URL, headers, body mapping, pagination — without redeploying."** The spec is data; it can come from a database or a config file.
- **"I need OAuth2 handled for many credentials — consent flow, token persistence, refresh-on-expiry, refresh-on-401."** The two-phase flow and the request-time token lifecycle are built in.
- **"I need a design-time UI with searchable, paginated dropdowns that fetch options from an API."** `describe(...)` returns `OptionPage`s without production code.
- **"I need the same response shaping everywhere — root property, filters, caps."** Post-receive actions are declarative and reusable.

### When NOT to use it

If you have a single, hard-coded endpoint called from one place, plain `OkHttp`/`RestClient` is simpler — you do not need a spec engine. Likewise, request flows that do not fit the fixed pipeline (select → plan → pre-send → auth → transport → post-receive) are better written imperatively.

---

## Quick Start

The smallest runnable path: wire the config, declare one operation, execute, print records. All types live under `io.github.khezyapp.dhttp`.

```java
final class QuickStart {

    public static void main(final String[] args) throws Exception {
        // 1. A transport that "speaks HTTP" for us — here a fake that never leaves the JVM.
        final var transport = (HttpTransport) request -> HttpResult.of(200,
                "{\"data\":{\"items\":[{\"id\":1,\"name\":\"SOK\"},{\"id\":2,\"name\":\"VISAL\"}]}}");

        // 2. Wire the core once. The key provider owns the credential master key.
        final var config = DeclarativeHttpConfig.builder()
                .transport(transport)
                .keyProvider(QuickStart::newKey)
                .build();
        final var http = DeclarativeHttp.create(config);

        // 3. Declare "how to talk to the API" as plain data.
        final var shape = new RequestShape(HttpMethod.GET, "/contacts", Map.of(), Map.of(),
                null, null);
        final var operation = new Operation("contact.list",
                new Route(shape, List.of(),
                        new Output(50, List.of(new PostReceive.RootProperty("data.items"))),
                        null, List.of()));
        final var spec = new HttpRequestSpec("https://api.example.com", Map.of(), 30_000L, false,
                List.of(operation), null, null, SecurityPolicy.defaults());

        // A route may also carry its own base URL; an absolute path or the route base URL
        // wins over the spec-level base URL (useful for routing one operation id to
        // different providers by Condition).

        // 4. Execute one operation over one "item" of parameters.
        final var records = http.execute(spec, new RequestContext("contact.list", Map.of()));

        // 5. Records are just data.
        records.forEach(record -> System.out.println(record.json().get("name")));
    }

    private static SecretKey newKey() throws Exception {
        final var generator = KeyGenerator.getInstance("AES");
        generator.init(256);
        return generator.generateKey();
    }
}
```

That is the whole idea: the request, the auth, the parsing, and the shaping are **data**. The engine does the rest.

---

## Core Concepts

Three layers, strictly separated:

- **Spec** — immutable records that describe an API (`HttpRequestSpec`, `Operation`, `Route`, `RequestShape`, `Send`, `PostReceive`, ...). Pure data, no behavior.
- **Engine** — stateless executor (`DeclarativeHttpEngine`, reached through the `DeclarativeHttp` facade) that turns `(spec, context)` into output records.
- **Transport** — pluggable SPI (`HttpTransport`) that performs the actual I/O. The default is `JdkHttpTransport` over the JDK `HttpClient`.

### Request lifecycle

Every `execute(spec, ctx)` call runs this fixed pipeline:

```
1. Select operation   — the first Operation whose `when` conditions match the context
2. Plan               — merge spec defaults, resolve expressions, build the HttpRequest
3. Pre-send           — optional request transforms
4. Auth               — resolve the credential and attach auth (header, Basic, Bearer)
5. Transport          — send, get the HttpResult
6. Post-receive       — shape the response into OutputRecords
7. Pagination         — loop back to step 3 while another page exists
8. Cap                — trim to maxResults and return the records
```

### The per-item RequestContext

The engine is stateless: every per-call input travels in a `RequestContext` (`operationId`, `item`, `parameters`, `credentials`, `variables`, `onResponse`):

- `operationId` — which operation to run (matched against the spec).
- `item` — the current input record, or `null`.
- `parameters` — resolved operation/route parameters (the `$parameter` binding).
- `credentials` — decrypted credential fields (the `$credentials` binding).
- `variables` — environment/instance variables (the `$env` binding).
- `onResponse` — optional callback fed with each page's raw `HttpResult`, fired right after the transport send and before post-receive shaping. It covers every page fetched by a pagination strategy, the final result of an OAuth2 retry, and every item in `executeAll`.

`executeAll(spec, contexts)` runs one operation over many contexts, with optional batching pacing defined by `BatchingSpec`.

#### Capturing raw responses with `onResponse`

Set the callback when you need the raw response beyond the shaped records — logging, debugging, or reading response headers such as rate limits or pagination links:

```java
final var pages = new ArrayList<HttpResult>();
final var ctx = new RequestContext("contact.list", Map.of(), pages::add);

final var records = http.execute(spec, ctx);

for (final var page : pages) {
    log.info("page status={} rate-limit-remaining={}",
            page.status(), page.headers().get("X-RateLimit-Remaining"));
}
```

When a pagination strategy is active, the callback fires once per fetched page in order, so you can correlate each page with its response. The `onResponse` callback is optional — leave it out (the two-argument `RequestContext(operationId, parameters)` constructor sets it to `null`) when you only need the shaped records.

---

## Configuration & customization

The entry point is `DeclarativeHttpConfig.builder()`; assemble it and pass it to `DeclarativeHttp.create(config)`.

| Builder method | Default | What it controls |
|---|---|---|
| `transport(HttpTransport)` | `new JdkHttpTransport()` | every HTTP send, including the OAuth2 token endpoint |
| `evaluator(ExpressionEvaluator)` | `new JexlExpressionEvaluator()` | `=` / `{{ ... }}` expression resolution |
| `jsonMapper(JsonMapper)` | `JacksonJsonMapper.INSTANCE` | JSON shared by planning, shaping, credentials, OAuth2 |
| `tokenStore(TokenStore)` | `new InMemoryTokenStore()` | OAuth2 token persistence (warm-token reuse, refresh) |
| `registry(ActionRegistry)` | `ActionRegistry.withBuiltins()` | post-receive actions (seven built-ins + custom) |
| `pagination(PaginationRegistry)` | `PaginationRegistry.withBuiltins()` | pagination strategies (four built-ins + custom) |
| `registerPagination(mode, factory)` | — | shorthand to add one custom pagination strategy on top of the built-ins |
| `credentialService(CredentialService)` | — | full credential CRUD; also wires the engine-facing store |
| `credentialStore(CredentialStore)` | — | read-only engine-facing store (bypasses CRUD) |
| `keyProvider(KeyProvider)` | — | master key for the default cipher-backed service |

Two rules to remember:

1. **A key (or service/store) is required.** The core never generates or stores a master key. `build()` throws `IllegalArgumentException` when you provide none of `keyProvider`, `credentialService`, or `credentialStore`.
2. **Defaults are sensible but replaceable.** Inject your own `HttpTransport` (OkHttp, RestClient adapter), your own `ExpressionEvaluator`, your own `JsonMapper`, or your own `TokenStore` — the engine never hard-codes them.

---

## Object lifecycle

Build the config and facade **once** and share them across threads. The engine is stateless and thread-safe; all per-call inputs travel in `RequestContext`.

| Object | When | Notes |
|---|---|---|
| `DeclarativeHttpConfig` + `DeclarativeHttp` | create once | immutable, thread-safe |
| `ActionRegistry` | create once | register custom actions *before* `build()` |
| `PaginationRegistry` | create once | register custom pagination strategies *before* `build()` |
| `CredentialService` | create once | stateful: owns the repository + cipher |
| `KeyProvider` | create once | consumer-owned master key |
| `TokenStore` | create once | **deliberately stateful**: OAuth2 tokens persist across requests and JVM restarts |
| `RequestContext` | per call | the only per-item input |
| `JdkHttpTransport` | create once | lazily caches derived clients (never-redirect, skip-SSL) |

The OAuth2 token store and the credential repository are the two pieces you want to share deliberately: give the same `TokenStore` to the config-time flow and the request-time engine so tokens survive both phases, and use a `CredentialRepository` backed by a real database so credentials survive restarts.

### Design time: `describe(...)`

`describe(spec, ctx, loadKey)` runs the same pipeline but applies a single option-shaping action and returns an `OptionPage` — designed for searchable/paginated dropdowns in a UI. The option-shaping action stamps `hasMore` / `nextCursor` (or a structured `nextParameters` map) into record metadata, and the engine aggregates them into the page.

---

## Expressions

Values in headers, queries, URLs, literal body leaves, and post-receive steps can be plain literals or **expressions**. The syntax is JEXL:

- `= {{ expression }}` — exactly one expression with no surrounding text → returns the evaluated object with its runtime type (a number, map, or list, not a string).
- `= Hello {{ name }}` — text and/or several `{{ ... }}` blocks → rendered as a string.
- `{{ expression }}` without a leading `=` — same string-template rendering.
- anything else → treated as a literal.

The evaluator resolves bindings from `EvaluationScope`:

| Binding | Meaning |
|---|---|
| `$parameter` | resolved operation/route parameters (from `RequestContext.parameters`) |
| `$credentials` | decrypted credential fields |
| `$env` | environment/instance variables |
| `$item` | the current input record's JSON |
| `$response` | the received response body |
| `$responseItem` | the current item while shaping a list of records |
| `$value` | the value being processed (filter/set actions) |
| `$index` | the current index while shaping a list |
| `$parent` | the parent item (nested loops) |

The `doa` namespace bridges the KHEZY dynamic-object library for dot-path access, e.g. `doa.get($response, "data.items[0].id")`. A `Send` with a `valueOverride` may carry an `Expression` instead of reading a parameter; `Condition` values are plain literals matched against parameters.

---

## Extending the library

Every major behavior is an SPI. Register or inject your implementation and the engine uses it.

### Custom post-receive action

Register a `PostReceiveFactory` under a name, then reference it from the spec via `PostReceive.CustomPostReceive`:

```java
final var registry = ActionRegistry.withBuiltins().register("nameToUpper",
        (descriptor, evaluator) -> (records, response) -> records.stream()
                .map(record -> OutputRecord.ofJson(Map.of(
                        "name", String.valueOf(record.json().get("name")).toUpperCase())))
                .toList());

final var config = DeclarativeHttpConfig.builder()
        .transport(transport)
        .keyProvider(QuickStart::newKey)
        .registry(registry)
        .build();

// in the spec:
new PostReceive.CustomPostReceive("nameToUpper", Map.of())
```

### Custom HttpTransport

Implement `HttpTransport` directly for a fake/in-memory transport, or extend `AbstractHttpTransport`
to reuse the shared pipeline (query serialization, manual redirects with SSRF re-validation and
credential stripping, body/header preparation, non-2xx → `HttpApiException`). A real adapter only
implements `execute` — build a native client request from the neutral `HttpRequest`, send it, and
return the raw status/headers/body:

```java
final class OkHttpTransport extends AbstractHttpTransport {

    private final OkHttpClient client;

    OkHttpTransport(final OkHttpClient client) {
        this.client = client;
    }

    @Override
    protected RawResponse execute(final HttpRequest request, final URI uri) throws HttpApiException {
        final var prepared = prepareBody(request.body());              // shared: bytes + content type
        final var builder = new Request.Builder().url(uri.toURL());
        effectiveHeaders(request).asMap().forEach((name, values) ->
                values.forEach(v -> builder.header(name, v)));         // shared: auth fallback, Accept
        builder.method(request.method().name(),
                prepared.hasBody() ? RequestBody.create(prepared.bytes()) : null);
        try (final var response = client.newCall(builder.build()).execute()) {
            final var body = response.body() == null ? null : response.body().bytes();
            return RawResponse.of(response.code(), response.headers().toMultimap(), body);
        } catch (final IOException e) {
            throw new HttpApiException(HttpApiException.NO_STATUS, "transport", -1,
                    "HTTP transport I/O failure", e);
        }
    }
}
```

The template method then applies redirect following, SSRF allow-list checks, credential stripping on
cross-origin hops, and error mapping for every adapter consistently.

### Custom ExpressionEvaluator

```java
final class SpelEvaluator implements ExpressionEvaluator {

    @Override
    public boolean isExpression(final String value) {
        return value.startsWith("=") || value.contains("{{");
    }

    @Override
    public <T> T evaluate(final String expression, final EvaluationScope scope, final Class<T> type) {
        // resolve `$parameter`, `$credentials`, ... from scope.bindings()
        return null;
    }
}
```

Wire with `.evaluator(new SpelEvaluator())`.

### Custom TokenStore

```java
final class RedisTokenStore implements TokenStore {

    @Override
    public Optional<OAuth2Token> load(final String credentialId) { /* ... */ }

    @Override
    public void save(final String credentialId, final OAuth2Token token) { /* ... */ }

    @Override
    public void clear(final String credentialId) { /* ... */ }
}
```

Wire with `.tokenStore(new RedisTokenStore(redis))`. The request-time authenticator reads/writes whole tokens keyed by credential id.

### Custom CredentialRepository / CredentialCipher / KeyProvider

```java
final var service = new CredentialService(
        new JdbcCredentialRepository(dataSource),    // your own CredentialRepository
        new AesGcmCredentialCipher(() -> masterKey), // or any CredentialCipher
        JacksonJsonMapper.INSTANCE);

final var http = DeclarativeHttp.create(DeclarativeHttpConfig.builder()
        .transport(new JdkHttpTransport())
        .credentialService(service)
        .build());
```

### Custom pagination

Implement `PaginationStrategy` (`initRequest`, `shouldPaginate`, `nextRequest`, `collect`) for APIs that page by link headers or other schemes, and register a factory under a mode string. A `PaginationSpec` with that `mode` then resolves through your factory — the planner looks up the route's mode in the registry, so unknown modes fail fast instead of silently disabling pagination.

```java
final var registry = PaginationRegistry.withBuiltins().register("linkHeader",
        (spec, evaluator, jsonMapper) -> new LinkHeaderPagination(jsonMapper));

final var http = DeclarativeHttp.create(DeclarativeHttpConfig.builder()
        .transport(new JdkHttpTransport())
        .keyProvider(QuickStart::newKey)
        .pagination(registry)
        // ... or, for a single strategy:
        // .registerPagination("linkHeader",
        //         (spec, evaluator, jsonMapper) -> new LinkHeaderPagination(jsonMapper))
        .build());

// in the spec: a route whose pagination mode matches the registered key
new PaginationSpec("linkHeader", 30, "data.items", null, null, true, null)
```

The built-in registry resolves the `offset`, `page`, `cursor`, and `nextUrl` modes from `PaginationSpec`; a fresh strategy is created per plan, so page cursors/offsets never leak across executions. The `offset` and `page` modes seed their parameters (e.g. `limit=10&offset=0`) on the first request via `initRequest`, and `cursor`/`nextUrl` send the optional `limitParam` with every request, so the request shape itself does not need to configure them. `pageSize` is optional and validated only when configured.

### Pre-send hooks

`PreSendAction` is a functional interface that rewrites the request just before it is sent (`HttpRequest apply(HttpRequest)`), and `PreSend` descriptors can declare hooks on a route. The pipeline applies `RequestPlan.preSends()` in order before auth; the built-in planner currently materializes an empty pre-send list, so today the hook surface is the SPI itself.

---

## Building on top of the core

The library is a core — real products build an adapter layer on top. A common pattern: load specs from JSON/YAML and map them into `HttpRequestSpec`. Here is a tiny `SpecLoader` that reads a config map (as produced by your favorite YAML/JSON parser) and builds a spec with a single GET operation:

```java
final class SpecLoader {

    /** Reads a small declarative config map into an HttpRequestSpec. */
    static HttpRequestSpec load(final Map<String, Object> config) {
        @SuppressWarnings("unchecked")
        final var operations = ((List<Map<String, Object>>) config.get("operations")).stream()
                .map(SpecLoader::operation)
                .toList();
        return new HttpRequestSpec((String) config.get("baseUrl"), Map.of(), 30_000L, false,
                operations, null, null, SecurityPolicy.defaults());
    }

    private static Operation operation(final Map<String, Object> op) {
        final var shape = new RequestShape(
                HttpMethod.valueOf((String) op.get("method")),
                (String) op.get("path"),
                Map.of(), Map.of(), null, null);
        final var postReceive = new ArrayList<PostReceive>();
        final var rootProperty = (String) op.get("rootProperty");
        if (rootProperty != null) {
            postReceive.add(new PostReceive.RootProperty(rootProperty));
        }
        final var route = new Route(shape, List.of(), new Output(50, postReceive), null, List.of());
        return new Operation((String) op.get("id"), route);
    }
}
```

The config it consumes:

```yaml
baseUrl: https://api.brevo.com/v3
operations:
  - id: contact.list
    method: GET
    path: /contacts
    rootProperty: data.items
```

Your application wires the credential service and the config, then executes:

```java
final var spec = SpecLoader.load(yamlAsMap); // produced by your YAML/JSON parser

// CredentialService with a consumer-owned master key (AES-256-GCM at rest)
final var service = new CredentialService(
        new InMemoryCredentialRepository(),
        new AesGcmCredentialCipher(QuickStart::newKey, JacksonJsonMapper.INSTANCE),
        JacksonJsonMapper.INSTANCE);
final var apiKeyId = service.create("api-key",
        Map.of("headerName", "api-key", "value", "xkeysib-super-secret"));

final var http = DeclarativeHttp.create(DeclarativeHttpConfig.builder()
        .transport(new JdkHttpTransport())  // real HTTP via the JDK HttpClient
        .credentialService(service)
        .build());

final var records = http.execute(spec, new RequestContext("contact.list", Map.of()));
```

The spec is now data: load it from a database, cache it, version it — no Java code changes.

---

## Scenario demos

### (a) An api-key REST API with two operations

Store an api-key credential, declare create + list with body mapping and `RootProperty` shaping, then execute by parameter:

```java
final var apiKeyId = http.credentialService().create("api-key",
        Map.of("headerName", "api-key", "value", "xkeysib-super-secret"));

final var createShape = new RequestShape(HttpMethod.POST, "/contacts",
        Map.of("X-Contact-Name", "= {{ $parameter.contact.name }}",
                "X-Api-Key", "= {{ $credentials.value }}"),
        Map.of(), null, null);
final var createOp = new Operation("contact.create",
        List.of(new Condition("action", "create")),
        new Route(createShape, List.of(new Send("contact", Target.BODY, "attributes", true, null)),
                new Output(50, List.of(new PostReceive.RootProperty("data"))),
                null, List.of()));

final var listShape = new RequestShape(HttpMethod.GET, "/contacts", Map.of(), Map.of(), null, null);
final var listOp = new Operation("contact.list",
        List.of(new Condition("action", "list")),
        new Route(listShape, List.of(),
                new Output(50, List.of(new PostReceive.RootProperty("data.items"))),
                null, List.of()));

final var spec = new HttpRequestSpec("https://api.brevo.com/v3",
        Map.of("Accept", "application/json"), 30_000L, false,
        List.of(createOp, listOp), CredentialRef.of("api-key", apiKeyId), null,
        SecurityPolicy.defaults());

final var created = http.execute(spec, new RequestContext("contact.create", null,
        Map.of("action", "create",
                "contact", Map.of("name", "SOK", "attributes", Map.of("city", "Battambang"))),
        Map.of("value", "xkeysib-super-secret"), Map.of(), null));
```

### (b) OAuth2, Google-Sheets style (two phases)

Phase A — config time: store the client registration, build the consent URL, exchange the code, persist the token. Phase B — reference the credential in a spec and validate before executing:

```java
final var oauth2 = new OAuth2Credentials(
        "client-1", "s3cr3t",
        "https://auth.example.com/token",
        "https://auth.example.com/authorize",
        "https://app.example.com/callback",
        "sheets.read",
        OAuth2Grant.AUTHORIZATION_CODE,
        Map.of());
final var id = http.credentialService().create("oauth2", oauth2);

final var tokenStore = new InMemoryTokenStore();
final var flow = OAuth2AuthorizationFlow.create(id, oauth2,
        new OAuth2TokenClient(transport, JacksonJsonMapper.INSTANCE), tokenStore);

final var consentUrl = flow.authorizationUrl();   // send the user here
// ... user consents; your callback receives ?code=... ...
final var token = flow.exchangeCode("the-code");
flow.persist(token);

// Phase B — the request-time engine reuses the same TokenStore
final var http = DeclarativeHttp.create(DeclarativeHttpConfig.builder()
        .transport(transport)
        .tokenStore(tokenStore)
        .keyProvider(QuickStart::newKey)
        .build());

final var spec = new HttpRequestSpec("https://sheets.googleapis.com/v4", Map.of(), 30_000L,
        false, List.of(operation), CredentialRef.of("oauth2", id), null,
        SecurityPolicy.defaults());
http.validate(spec); // OAuth2NotConfiguredException whens no valid token is stored
```

At request time the engine reuses warm tokens, refreshes on expiry, and retries once on a `401` — single-flight per credential.

### (c) Searchable/paginated dropdown via `describe(...)`

Register an option-shaping action, point a spec's output at it, and call `describe`:

```java
final var registry = ActionRegistry.withBuiltins().register("loadContacts",
        (descriptor, evaluator) -> (records, response) -> {
            final var data = JacksonJsonMapper.INSTANCE.read(response.bodyString(), Map.class);
            final var items = (List<Map<String, Object>>) data.get("data");
            return items.stream()
                    .map(item -> OutputRecord.ofJson(Map.of(
                            "name", item.get("name"),
                            "value", item.get("id"))))
                    .toList();
        });

final var http = DeclarativeHttp.create(DeclarativeHttpConfig.builder()
        .transport(transport)
        .keyProvider(QuickStart::newKey)
        .registry(registry)
        .build());

// the operation's output carries the custom action
final var shape = new RequestShape(HttpMethod.GET, "/contacts", Map.of(), Map.of(), null, null);
final var operation = new Operation("contact.list", new Route(shape, List.of(),
        new Output(50, List.of(new PostReceive.CustomPostReceive("loadContacts", Map.of()))),
        null, List.of()));
final var spec = new HttpRequestSpec("https://api.example.com", Map.of(), 30_000L, false,
        List.of(operation), null, null, SecurityPolicy.defaults());

final var page = http.describe(spec, new RequestContext("contact.list", Map.of()), "loadContacts");
page.items().forEach(option -> System.out.println(option.name() + " -> " + option.value()));
```

`OptionPage` carries `items()`, `hasMore()`, `nextCursor()`, and `nextParameters()` so a UI can render a searchable, paginated dropdown.

### (d) Guidelines: custom post-receive action for `describe(...)`

`describe(...)` is not a generic shaping mode. The engine scans the active operation's post-receive
steps for a `PostReceive.CustomPostReceive` whose `actionKey` equals the `loadKey` argument and runs
**only that step** — a plain built-in like `RootProperty` cannot drive it. So the operation's output
must reference your custom action:

```java
new Output(50, List.of(new PostReceive.CustomPostReceive("loadRegions", Map.of())))
```

The action has two responsibilities:

**1. Emit dropdown-shaped records.** The engine maps each returned record to an `OptionItem` by
reading these exact JSON keys — `name` and `value` (required) plus `description`, `icon`, `group`,
`disabled` (optional). If the API uses different field names, the action must rename them. Prefer
`response` (the raw `HttpResult`) here — you are *extracting* items from the raw body. Use `records`
only when your action *transforms* a list the pipeline already built (rename/filter/sort), not when
it needs to re-derive options from scratch:

```java
final var registry = ActionRegistry.withBuiltins().register("loadRegions",
        (descriptor, evaluator) -> (records, response) -> {
            final var data = JacksonJsonMapper.INSTANCE.read(response.bodyString(), Map.class);
            final var items = (List<Map<String, Object>>) data.get("data");
            return items.stream()
                    .map(item -> OutputRecord.ofJson(Map.of(
                            "name", item.get("name"),
                            "value", item.get("code"))))
                    .toList();
        });
```

**2. Drive pagination through record metadata.** For a paginated dropdown, read the paging state
from the response envelope and stamp it into every record's metadata. The engine aggregates it into
the returned `OptionPage`. Two shapes are supported:

- **Single cursor** — the API exposes one token (e.g. `nextCursor`). Stamp `hasMore` /
  `nextCursor`:

  ```java
  (records, response) -> {
      final var data = JacksonJsonMapper.INSTANCE.read(response.bodyString(), Map.class);
      final var items = (List<Map<String, Object>>) data.get("data");
      final var hasMore = Boolean.TRUE.equals(data.get("hasMore"));
      final var nextCursor = (String) data.get("nextCursor");
      return items.stream()
              .map(item -> OutputRecord.ofJson(
                      Map.of("name", item.get("name"), "value", item.get("code")),
                      Map.of("hasMore", hasMore, "nextCursor", nextCursor)))
              .toList();
  }
  ```

- **Structured parameters** — the API paginates with several parameters (offset + limit, page +
  size, a date range, keyset). Stamp `hasMore` / `nextParameters` as a map instead of encoding a
  single string:

  ```java
  (records, response) -> {
      final var data = JacksonJsonMapper.INSTANCE.read(response.bodyString(), Map.class);
      final var items = (List<Map<String, Object>>) data.get("regions");
      final var hasMore = Boolean.TRUE.equals(data.get("hasMore"));
      final var next = Map.of("offset", data.get("nextOffset"), "limit", data.get("nextLimit"));
      return items.stream()
              .map(item -> OutputRecord.ofJson(
                      Map.of("name", item.get("name"), "value", item.get("code")),
                      Map.of("hasMore", hasMore, "nextParameters", next)))
              .toList();
  }
  ```

**Feeding the paging state back into the request.** `describe` is page-by-page: the UI passes the
previous paging state back as request parameters on the next call, and the operation's request shape
binds each name with `$parameter`.

Single cursor — an API that paginates through a `cursor` query parameter:

```java
final var shape = new RequestShape(HttpMethod.GET,
        "/api/v1/options/regions?cursor={{ $parameter.cursor ?: '' }}",
        Map.of(), Map.of(), null, null);
final var operation = new Operation("regions.get", new Route(shape, List.of(),
        new Output(50, List.of(new PostReceive.CustomPostReceive("loadRegions", Map.of()))),
        null, List.of()));
```

Structured parameters — the same operation bound to the `offset` / `limit` names the action stamped:

```java
final var shape = new RequestShape(HttpMethod.GET,
        "/api/v1/options/regions?offset={{ $parameter.offset ?: 0 }}"
                + "&limit={{ $parameter.limit ?: 30 }}",
        Map.of(), Map.of(), null, null);
```

First page — no paging state yet; the `?:` guards yield defaults so the API still returns page one:

```java
final var page1 = http.describe(spec,
        new RequestContext("regions.get", Map.of()), "loadRegions");
```

Next pages — pass the paging state returned by the previous page (only when `hasMore()` is true).
The UI just echoes `nextParameters()` back; it never needs to know the API's pagination scheme:

```java
var page = page1;
while (page.hasMore()) {
    final var next = http.describe(spec,
            new RequestContext("regions.get", page.nextParameters()), "loadRegions");
    render(next.items());
    page = next;
}
```

When the action stamps neither metadata key, `hasMore()` is `false`, `nextCursor()` is `null`, and
`nextParameters()` is empty.

---

## Included defaults & security

- **Transport**: `JdkHttpTransport` over the JDK `HttpClient` — multipart/form-urlencoded bodies, proxy support, per-request `skipSsl`, manual redirect following, and non-2xx responses surfaced as `HttpApiException`.
- **Credential encryption at rest**: `AesGcmCredentialCipher` (AES-256-GCM). The master key is **consumer-owned** via `KeyProvider`; the core never generates, stores, or defaults one.
- **SSRF guard**: `SsrfGuard` validates hosts against `SecurityPolicy.allowedDomains` — by name and by every resolved address. IP literals are rejected unless `SecurityPolicy.allowIpLiteral` is set.
- **Redirect safety**: `RedirectPolicy` strips credentials (`Authorization`, `Cookie`, ...) on cross-origin redirects, unless `SecurityPolicy.stripCrossOriginCredentials` opts out.
- **Secret redaction**: `SecretRedactor` masks tokens/passwords in error messages; `HttpErrorFactory` redacts bearer tokens.
- **Sensitive output fields**: `SecurityPolicy.sensitiveOutputFields` masks dotted fields (nested maps and list elements) with `***` in the records returned by `execute`/`executeAll`/`describe`, so sensitive values never leak into logged output.

---

## Building & Testing

```sh
# Build, test, and checkstyle this module
./gradlew :declarative-http:build

# Run tests only
./gradlew :declarative-http:test

# Checkstyle only
./gradlew :declarative-http:checkstyleMain

# Build the whole composite (all modules)
./gradlew build
```

The module targets Java 17 bytecode (`--release 17`) and builds with the JDK 21 toolchain. Tests use JUnit 5.

---

## Further reading

- [Design: declarative_http_core_design.md](../../scrum/declarative-http/design/declarative_http_core_design.md) — the principle design (spec/engine/transport separation, two-phase OAuth2, security contracts).
- [Research: n8n_declarative_http_research.md](../../scrum/declarative-http/research/n8n_declarative_http_research.md) — the 16 requirements (R1–R16) extracted from n8n's routing-node usage.
