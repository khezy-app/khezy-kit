# declarative-http — Scrum Workspace

Core-library design for a **declarative HTTP** engine for the Java ecosystem, ported from n8n's routing-node / `IHttpRequestOptions` architecture.

## Contents

| Doc                                                                                      | Purpose                                                                                                                                                                                      |
| ---------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| [`research/n8n_declarative_http_research.md`](research/n8n_declarative_http_research.md) | How n8n's declarative HTTP system works (spec → RoutingNode → transport), real-world usage from actual nodes, and the extracted **user requirements R1–R16**.                                |
| [`design/declarative_http_core_design.md`](design/declarative_http_core_design.md)       | **Principle design** for the Java core library `io.github.khezyapp.dhttp`: package structure, core abstractions, pipeline, SPI contracts, security contract, and requirement→design mapping. |

## Flow

1. **Research** — study n8n (this repo, `packages/core/src/execution-engine/routing-node.ts`, `packages/workflow/src/interfaces.ts`, `packages/@n8n/backend-network/src/http/**`, real nodes) → extract user requirements.
2. **Design** — turn those requirements into a framework-agnostic Java core-library principle design (spec ⇄ engine ⇄ transport separation, pluggable SPIs, security-first).
3. **Build** — implement the core per the design, using the acceptance snapshot in §9 as the first milestone.

## Status: Milestone 2 done (default JDK transport + batching throttle)

Built on the completed v1 core (below):

- **Default transport** (§10.1): `JdkHttpTransport` — real `java.net.http.HttpClient` mapping for every
  `HttpRequest` field (query per `ArrayFormat`, all body kinds incl. multipart, auth fallback,
  timeout, trust-all `skipSsl`, per-request proxy, manual redirects with SSRF re-validation and
  cross-origin credential stripping, non-2xx ⇒ `HttpApiException`). `DeclarativeHttpConfig` now
  defaults to it when none is injected. Proven end to end against a local `HttpServer`/`HttpsServer`
  (13 tests in `JdkHttpTransportTest`).
- **Batching throttle** (§10.2): `BatchingSpec(batchSize, batchIntervalMillis)` on `HttpRequestSpec`
  + `DeclarativeHttp.executeAll(spec, contexts)` — n8n V3 semantics (one request per item, pause
  before each batch boundary, `maxResults` cap across items). Acceptance in `BatchingAcceptanceTest`.

## Status: Implementation done (v1 milestone)

The core library `io.github.khezyapp.dhttp` (module `http/declarative-http`) is implemented end to end:

- **Spec model** (R1–R3): `HttpRequestSpec` / `Operation` / `Route` / `Send` / `Condition` + default-merge planning.
- **Planner** (R4–R5, R16): `RequestPlanner` — JEXL expression resolution from `$parameter`/`$credentials`/`$env`/`$item`, DynamicObjects dot-path resolution, deterministic plans.
- **Pipeline** (R6–R9, R11): `Pipeline` — preSend → auth → guarded transport → postReceive → offset/cursor pagination → `maxResults` cap.
- **Security** (R11–R12, §7): `SsrfGuard` allow-list, `SecretRedactor` (no tokens/keys in errors or logs), `RedirectPolicy`, `HttpErrorFactory`.
- **Credentials** (R10, §6.2): `CredentialService` + `CredentialRepository` + AES-256-GCM `AesGcmCredentialCipher`/`KeyProvider` (consumer-owned key) + `CredentialStore` engine read-SPI.
- **OAuth2** (R10, §6.6): two-phase config (`OAuth2AuthorizationFlow` — authorization URL, code exchange, persist, validate) + request-time lifecycle (`OAuth2RequestAuthenticator` — warm-token reuse, refresh, 401 retry, single-flight).
- **Post-receive actions** (R7, R15): `rootProperty`/`filter`/`limit`/`setValue`/`setKeyValue`/`sortByKey`/`binaryData` + custom `ActionRegistry`.
- **Engine + facade**: `DeclarativeHttpEngine` (validate / execute / describe) and `DeclarativeHttp`/`DeclarativeHttpConfig` as the single wiring entry point.

**Acceptance** — all 8 items in §9 of the design doc are proven end to end (only public core API through `DeclarativeHttp`/`DeclarativeHttpConfig`, real `FakeTransport`, no real HTTP) in:

`src/test/java/io/github/khezyapp/dhttp/acceptance/`

| Test | §9 items |
| --- | --- |
| `BrevoSpecAcceptanceTest` | 1, 2, 4 |
| `PaginationAcceptanceTest` | 3 |
| `DescribeAcceptanceTest` | 5 |
| `OAuth2ConfigTimeAcceptanceTest` | 6 |
| `OAuth2RequestTimeAcceptanceTest` | 7 |
| `CredentialLifecycleAcceptanceTest` | 8 |

`./gradlew :declarative-http:build` is green (compile + tests + Checkstyle).
